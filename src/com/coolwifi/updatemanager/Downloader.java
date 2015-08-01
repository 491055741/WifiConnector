package com.coolwifi.updatemanager;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Downloader
{
    private static final int DOWNLOAD = 1;
    private static final int DOWNLOAD_FINISH = 2;
    private Timer mTimer;
    private Context mContext;
    private Handler mHandler;
    private ArrayList<String> mDownloadIds;
    private int maxProgress = 0;
    public Downloader(Context context, Handler handler)
    {
        mHandler = handler;
        mContext = context;
        mDownloadIds = new ArrayList<String>();
        downloadmanager = (DownloadManager)mContext.getSystemService(Context.DOWNLOAD_SERVICE);  
//        mContext.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));   
        downloadObserver = new DownloadChangeObserver(null);
        mContext.getContentResolver().registerContentObserver(CONTENT_URI, true, downloadObserver);  
    }
    
    public void downloadApk(String url, String appName) throws MalformedURLException
    {
        Uri uri = Uri.parse(url);  
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdir();  
    
        Request request = new DownloadManager.Request(uri)  
        .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI)  
        .setAllowedOverRoaming(false)
        .setTitle(appName)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "_"+(int)(Math.random()*100000)+".apk");//appName+".apk")
        request.allowScanningByMediaScanner();

        long downloadId = downloadmanager.enqueue(request);
        mDownloadIds.add(Long.toString(downloadId));
        startTimer();
    }
    
    private DownloadManager downloadmanager = null;  
    private DownloadChangeObserver downloadObserver;  
//    private long lastDownloadId = 0;  
    public static final Uri CONTENT_URI = Uri.parse("content://downloads/my_downloads");  

    class DownloadChangeObserver extends ContentObserver {  

        public DownloadChangeObserver(Handler handler) {  
            super(handler);  
            // TODO Auto-generated constructor stub  
        }

        @Override  
        public void onChange(boolean selfChange) {  
//              queryDownloadStatus();     
        }  
    }

//    private BroadcastReceiver receiver = new BroadcastReceiver() {     
//            @Override     
//            public void onReceive(Context context, Intent intent) {     
//                //这里可以取得下载的id，这样就可以知道哪个文件下载完成了。适用与多个下载任务的监听    
//            	if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
//                    Long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
//                    Log.v("tag", "download complete broadcast: id: "+downloadId);
//            	}
//            }
//        };

    private void queryDownloadStatus() {     
        DownloadManager.Query query = new DownloadManager.Query();     
        maxProgress = 0;

        for (String downloadIdStr : mDownloadIds) {
        	Long downloadId = Long.parseLong(downloadIdStr);
            query.setFilterById(downloadId);
            Cursor c = downloadmanager.query(query);
            if (c!=null&&c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));     
                int titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE);    
                int fileSizeIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);        
                int bytesDLIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                int pathIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);

                String title = c.getString(titleIdx);
                String path = c.getString(pathIdx);
                int fileSize = c.getInt(fileSizeIdx);    
                int bytesDL = c.getInt(bytesDLIdx);    
                int progress = (int)((float)bytesDL/(float)fileSize * 100);

                switch(status) {     
                    case DownloadManager.STATUS_PAUSED:     
//                        Log.v("tag", "STATUS_PAUSED");
                    case DownloadManager.STATUS_PENDING:     
//                        Log.v("tag", "STATUS_PENDING");
                    case DownloadManager.STATUS_RUNNING:     
//                        Log.v("tag", "STATUS_RUNNING");
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        Log.v("tag", title+" 下载完成");
                        installApk(path);
                        mDownloadIds.remove(String.valueOf(downloadId));
                        break;
                    case DownloadManager.STATUS_FAILED:
                        Log.v("tag", "STATUS_FAILED");
//                        downloadmanager.remove(downloadId);     ????? 下载失败怎么办？
                    break;
                }
                if (progress > maxProgress) {
                    maxProgress = progress;
                }
                c.close();
            }
        }
    }

    public void cancelDownload(String url)
    {
        // todo : cancel version update download
    }
    
    public void installApk(String filePath)
    {
        Log.d("tag", "installApk: "+filePath);
    	File apkfile = new File(filePath);
    	apkfile.setReadable(true, false);
        if (!apkfile.exists()) {
        	Log.d("tag", "installApk: "+filePath+" not found!");
            return;
        }
        // 通过Intent安装APK文件
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.fromFile(apkfile), "application/vnd.android.package-archive");
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }

    private void startTimer()
    {
        if (mTimer == null) {
            mTimer = new Timer();   
            mTimer.schedule(new TimerTask() {   

               @Override
               public void run() {

                   if (mDownloadIds.size() == 0) {
                       mTimer.cancel();
                       mTimer = null;
                       return;
                   }
                   queryDownloadStatus();

                   Message msg = new Message();
                   if (maxProgress == 100 || mDownloadIds.size() == 0) {
                       msg.what = DOWNLOAD_FINISH;
                   } else {
                       msg.what = DOWNLOAD;
                   }
                   msg.arg1 = maxProgress;
                   mHandler.sendMessage(msg);
               }   
           }, 1000, 1000);  
        }
    }
    
}
