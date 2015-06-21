package com.coolwifi.updatemanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

//import com.coolwifi.updatemanager.DownloadManager.DownloadFileThread;

import android.R.bool;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class DownloadManager
{
    private static final int DOWNLOAD = 1;
    private static final int DOWNLOAD_FINISH = 2;
    private Timer mTimer;
    private Context mContext;
    private Handler mHandler;
    private String mSavePath;
    private List<DownloadFileThread> mDownloadList; 

    public DownloadManager(Context context, Handler handler)
    {
        mHandler = handler;
        mContext = context;
        mDownloadList = new ArrayList<DownloadFileThread>();
    }
    
    public void downloadApk(String url, String fileName) throws MalformedURLException
    {
        DownloadFileThread thread = new DownloadFileThread(url, fileName);
        mDownloadList.add(thread);
        thread.start();
        startTimer();
    }

    public void installApk(String fileName)
    {
        File apkfile = new File(mSavePath, fileName);
        if (!apkfile.exists())
        {
            return;
        }
        // 通过Intent安装APK文件
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
        mContext.startActivity(i);
    }

    private void startTimer()
    {
        if (mTimer == null) {
            mTimer = new Timer();   
            // 定义计划任务，根据参数的不同可以完成以下种类的工作：在固定时间执行某任务，在固定时间开始重复执行某任务，重复时间间隔可控，在延迟多久后执行某任务，在延迟多久后重复执行某任务，重复时间间隔可控   
            mTimer.schedule(new TimerTask() {   

               @Override
               public void run() {

                   if (mDownloadList.size() == 0) {
                       mTimer.cancel();
                       mTimer = null;
                       return;
                   }

                   DownloadFileThread thread = null;
                   int maxProgress = 0;
                   String fileName = null;
                   for (int i = 0; i < mDownloadList.size(); i++) {  
                       DownloadFileThread t = mDownloadList.get(i);
                       if (t.progress > maxProgress) {
                           maxProgress = t.progress;
                           fileName = t.fileName;
                           thread = t;
                       }
                   }
                   Message msg = new Message();
                   if (maxProgress == 100) {
                       msg.what = DOWNLOAD_FINISH;
                       mDownloadList.remove(thread);
                   } else {
                       msg.what = DOWNLOAD;                
                   }
                   msg.obj = fileName;
                   msg.arg1 = maxProgress;
                   mHandler.sendMessage(msg);
               }   

           }, 1000, 1000);  
        }
    }
    
    public void cancelDownload(String taskName)
    {
        DownloadFileThread thread = getThread(taskName);
        if (thread != null) {
            thread.cancelDownload = true;
        }
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
