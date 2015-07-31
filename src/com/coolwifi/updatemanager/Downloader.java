package com.coolwifi.updatemanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.coolwifi.main.MainActivity;

//import com.coolwifi.updatemanager.DownloadManager.DownloadFileThread;

import android.R.bool;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class Downloader
{
    private static final int DOWNLOAD = 1;
    private static final int DOWNLOAD_FINISH = 2;
    private Timer mTimer;
    private Context mContext;
    private Handler mHandler;
    private String mSavePath;
    private List<DownloadFileThread> mDownloadList;
//    private HashMap<String, DownloadFileInfo> mDownloadInfoHashMap;
    private ArrayList<String> mDownloadIds;
    private int maxProgress = 0;
    public Downloader(Context context, Handler handler)
    {
        mHandler = handler;
        mContext = context;
//        mDownloadList = new ArrayList<DownloadFileThread>();
  
//        mDownloadInfoHashMap = new HashMap<String, DownloadFileInfo>();
        mDownloadIds = new ArrayList<String>();
        dowanloadmanager = (DownloadManager)mContext.getSystemService(Context.DOWNLOAD_SERVICE);  
        mContext.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));   
        downloadObserver = new DownloadChangeObserver(null);
        mContext.getContentResolver().registerContentObserver(CONTENT_URI, true, downloadObserver);  
    }
    
    public void downloadApk(String url, String appName) throws MalformedURLException
    {
        Uri uri = Uri.parse(url);  
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdir();  
    
        long downloadId = dowanloadmanager.enqueue(new DownloadManager.Request(uri)  
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI)  
                .setAllowedOverRoaming(false)
                .setTitle(appName)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "_"+(int)(Math.random()*100000)+".apk"));//appName+".apk")
        mDownloadIds.add(Long.toString(downloadId));
        startTimer();
    }
    
    private DownloadManager dowanloadmanager = null;  
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

    private BroadcastReceiver receiver = new BroadcastReceiver() {     
            @Override     
            public void onReceive(Context context, Intent intent) {     
                //这里可以取得下载的id，这样就可以知道哪个文件下载完成了。适用与多个下载任务的监听    
            	if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                    Long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    Log.v("tag", "download complete broadcast: id: "+downloadId);

                    Uri downloadFileUri = dowanloadmanager.getUriForDownloadedFile(downloadId);
                    installApk(downloadFileUri);

                    mDownloadIds.remove(String.valueOf(downloadId));
                    dowanloadmanager.remove(downloadId);
                    if (mDownloadIds.size() == 0) {
	                    Message msg = new Message();
	                    msg.what = DOWNLOAD_FINISH;
	                    msg.arg1 = maxProgress;
	                    mHandler.sendMessage(msg);
                    }
            	}
            }
        };

    private void queryDownloadStatus() {     
        DownloadManager.Query query = new DownloadManager.Query();     
        maxProgress = 0;
        
        for (String downloadIdStr : mDownloadIds) {
        	Long downloadId = Long.parseLong(downloadIdStr);
            query.setFilterById(downloadId);
            Cursor c = dowanloadmanager.query(query);
            if(c!=null&&c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));     
                int reasonIdx = c.getColumnIndex(DownloadManager.COLUMN_REASON);    
                int titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE);    
                int fileSizeIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);        
                int bytesDLIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                int pathIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                String title = c.getString(titleIdx);
                String path = c.getString(pathIdx);
                int fileSize = c.getInt(fileSizeIdx);    
                int bytesDL = c.getInt(bytesDLIdx);    

                // Translate the pause reason to friendly text.    
                int reason = c.getInt(reasonIdx);    
                StringBuilder sb = new StringBuilder();    
                sb.append(title).append("\n");   
                sb.append("Downloaded ").append(bytesDL).append(" / " ).append(fileSize);    
                int progress = (int)((float)bytesDL/(float)fileSize * 100);
                // Display the status
                Log.d("tag", sb.toString());
                switch(status) {     
                    case DownloadManager.STATUS_PAUSED:     
                        Log.v("tag", "STATUS_PAUSED");    
                    case DownloadManager.STATUS_PENDING:     
                        Log.v("tag", "STATUS_PENDING");    
                    case DownloadManager.STATUS_RUNNING:     
                        Log.v("tag", "STATUS_RUNNING");    
                        break;     
                    case DownloadManager.STATUS_SUCCESSFUL:
                        Log.v("tag", "queryDownloadStatus:"+title+"下载完成");
                        break;
                    case DownloadManager.STATUS_FAILED:     
                        Log.v("tag", "STATUS_FAILED");
//                        dowanloadmanager.remove(downloadId);     ????? 下载失败怎么办？
                    break;
                }
                if (progress > maxProgress) {
                    maxProgress = progress;
//                    fileName = title;
//                    thread = t;
                }
            }
        }

    }
          
//        @Override  
//        protected void onDestroy() {  
//            // TODO Auto-generated method stub  
//            super.onDestroy();  
//              unregisterReceiver(receiver);    
//              getContentResolver().unregisterContentObserver(downloadObserver);  
//        }  
    
    public void installApk(Uri uri)
    {
//        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
//        File apkfile = new File(path);
//        if (!apkfile.exists())
//        {
//        	Log.d("tag", "installApk: "+path+" not found!");
//            return;
//        }
        // 通过Intent安装APK文件
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "application/vnd.android.package-archive");
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
                   if (maxProgress == 100) {
                       msg.what = DOWNLOAD_FINISH;
                   } else {
                       msg.what = DOWNLOAD;
                   }
//                   msg.obj = fileName;
                   msg.arg1 = maxProgress;
                   mHandler.sendMessage(msg);

               }   

           }, 1000, 1000);  
        }
    }
    
    public void cancelDownload(String taskName)
    {
    }

    private DownloadFileThread getThread(String taskName)
    {
        for (int i = 0; i < mDownloadList.size(); i++)  
        {  
            DownloadFileThread thread = mDownloadList.get(i);  
            if (thread.taskName.equals(taskName)) {
                return thread;
            }
        }
        return null;
    }
    
    private class DownloadFileThread extends Thread
    {
        private URL url;
        public String fileName;
        boolean cancelDownload;
        public String taskName;
        public int progress;
        public DownloadFileThread(String url, String fileName) throws MalformedURLException {
            this.url = new URL(url);
            this.taskName = url;
            this.fileName = fileName;
            this.cancelDownload = false;
        }
        @Override
        public void run()
        {
            try
            {
                // 判断SD卡是否存在，并且是否具有读写权限
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                {
                    // 获得存储卡的路径
                    String sdpath = Environment.getExternalStorageDirectory() + "/";
                    mSavePath = sdpath + "download";
                    // 创建连接
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();
                    // 获取文件大小
                    int length = conn.getContentLength();
                    // 创建输入流
                    InputStream is = conn.getInputStream();

                    File file = new File(mSavePath);
                    // 判断文件目录是否存在
                    if (!file.exists())
                    {
                        file.mkdir();
                    }
                    File apkFile = new File(mSavePath, fileName);
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    int count = 0;
                    // 缓存
                    byte buf[] = new byte[1024];
                    // 写入到文件中
                    do
                    {
                        int numread = is.read(buf);
                        count += numread;

                        if (numread <= 0) { // done
                            progress = 100;
                            break;
                        } else {
                            progress = (int) (((float) count / length) * 100);                            
                        }
                        fos.write(buf, 0, numread);
                    } while (!cancelDownload);// 点击取消就停止下载.
                    fos.close();
                    is.close();
                }
            } catch (MalformedURLException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
    };
    
  

    
}
