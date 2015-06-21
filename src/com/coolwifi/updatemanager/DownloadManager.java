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

//import com.coolwifi.updatemanager.DownloadManager.DownloadFileThread;

import android.R.bool;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

public class DownloadManager
{
    private static final int DOWNLOAD = 1;
    private static final int DOWNLOAD_FINISH = 2;

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
    }

    public void cancelDownload(String taskName)
    {
        for (int i = 0; i < mDownloadList.size(); i++)  
        {  
            DownloadFileThread thread = mDownloadList.get(i);  
            if (thread.taskName.equals(taskName)) {
                thread.cancelDownload = true;
                break;
            }
        }
    }
    
    private class DownloadFileThread extends Thread
    {
        private URL url;
        private String fileName;
        boolean cancelDownload;
        public String taskName;

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
                        // 计算进度条位置
                        // 更新进度
                        Message msg = new Message();
                        msg.what = DOWNLOAD;
                        msg.arg1 = (int) (((float) count / length) * 100);
                        mHandler.sendMessage(msg);
                        if (numread <= 0)
                        {
                            // 下载完成
                            Message msg2 = new Message();
                            msg2.what = DOWNLOAD_FINISH;
                            msg2.obj = fileName;
                            mHandler.sendMessage(msg2);

                            break;
                        }
                        // 写入文件
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
            mDownloadList.remove(this);
        }
        
    };

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
}
