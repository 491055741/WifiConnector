package com.example.updatemanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.testa2.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import com.example.httpconnection.*;
//import android.widget.Toast;
import com.example.httpconnection.HttpTask.HttpTaskHandler;

public class UpdateManager
{
    /* 下载中 */
    private static final int DOWNLOAD = 1;
    /* 下载结束 */
    private static final int DOWNLOAD_FINISH = 2;
    
    private static String TAG = "UpdateManager";
    /* 保存解析的XML信息 */
    HashMap<String, String> mHashMap;
    /* 下载保存路径 */
    private String mSavePath;
    /* 是否取消更新 */
    private boolean cancelUpdate = false;

    private Context mContext;
    /* 更新进度条 */
    private ProgressBar mProgress;
    private Dialog mDownloadDialog;

    private Handler mHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
            case DOWNLOAD:
            	if (mProgress != null) {
                    mProgress.setProgress(msg.arg1);            		
            	}
                break;
            case DOWNLOAD_FINISH:
                installApk( msg.obj.toString());
                break;
            default:
                break;
            }
        };
    };

    public UpdateManager(Context context)
    {
        this.mContext = context;
    }

    public void checkUpdate() throws NotFoundException, JSONException
    {
//        HttpConnection httpConnection = new HttpConnection();
//        httpConnection.get("app.milkpapa.com:8080/static/json/version.json", 
//        		new HttpConnection.CallbackListener() {
//	            @Override
//	            public void callBack(String result) {
//	            	JSONObject jsonObj;
//					try {
//						if (result != "fail") {
//							jsonObj = new JSONObject(result);
//							if (jsonObj.get("ret_code") == "0") {
//						        mHashMap = new HashMap<String, String>();
//						        mHashMap.put("versionCode", "2");
//						        mHashMap.put("name", "WifiConnector.apk");
//						        mHashMap.put("url", "http://app.milkpapa.com:8080/static/WifiConnector.apk");
//						        mHashMap.put("force", "YES");
//						        int versionCode = getVersionCode(mContext);
//						        int serviceCode = Integer.parseInt(mHashMap.get("versionCode"));
//						        if (serviceCode > versionCode) {
//							        showNoticeDialog();
//						        }
//							} else {
//								Log.d(TAG, "Get version result error.");
//							}
//						} else {
//							Log.d(TAG, "Get version failed.");
//						}
//					} catch (JSONException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//	            }
//    		}
//        );

    	HttpTask task = new HttpTask();
    	task.setTaskHandler(new HttpTaskHandler(){
    	    public void taskSuccessful(String json) {
    	    JSONObject jsonObj;
				try {
				    if (json != "fail") {
						jsonObj = new JSONObject(json);
						if (jsonObj.getInt("ret_code") == 0) {
					        mHashMap = new HashMap<String, String>();
					        mHashMap.put("name", jsonObj.getString("name"));
					        mHashMap.put("url", jsonObj.getString("url"));
					        mHashMap.put("force", jsonObj.getString("force"));
					        int versionCode = getVersionCode(mContext);
					        int serviceCode = jsonObj.getInt("versionCode");
					        if (serviceCode > versionCode) {
						        showNoticeDialog();
					        }
						} else {
							Log.d(TAG, "Get version result error.");
						}
					} else {
						Log.d(TAG, "Get version failed.");
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

    	    }

    	    public void taskFailed() {
    	    }
    	});
    	task.execute("http://app.milkpapa.com:8080/static/json/version.json");
    }

	private int getVersionCode(Context context)
	{
	    int versionCode = 0;
	    try
	    {
	        // 获取软件版本号，对应AndroidManifest.xml下android:versionCode
	        versionCode = context.getPackageManager().getPackageInfo("com.example.testa2", 0).versionCode;
	    } catch (NameNotFoundException e)
	    {
	        e.printStackTrace();
	    }
	    return versionCode;
	}

    private void showNoticeDialog()
    {
        // 构造对话框
        AlertDialog.Builder builder = new Builder(mContext);
        builder.setTitle(R.string.soft_update_title);
        builder.setMessage(R.string.soft_update_info);
        // 更新
        builder.setPositiveButton(R.string.soft_update_updatebtn, new OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                // 显示下载对话框
                try {
					showDownloadDialog();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });
        // 稍后更新
        if (mHashMap.get("force") == null || mHashMap.get("force") == "NO") {
            builder.setNegativeButton(R.string.soft_update_later, new OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
        }
        Dialog noticeDialog = builder.create();
        noticeDialog.show();
    }

    private void showDownloadDialog() throws MalformedURLException
    {
        // 构造软件下载对话框
        AlertDialog.Builder builder = new Builder(mContext);
        builder.setTitle(R.string.soft_updating);
        // 给下载对话框增加进度条
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.softupdate_progress, null);
        mProgress = (ProgressBar) v.findViewById(R.id.update_progress);
        builder.setView(v);
        // 取消更新
        builder.setNegativeButton(R.string.soft_update_cancel, new OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                cancelUpdate = true;
            }
        });
        mDownloadDialog = builder.create();
        mDownloadDialog.show();
        downloadApk(mHashMap.get("url"), mHashMap.get("name"));
    }

    public void downloadApk(String url, String fileName) throws MalformedURLException
    {
    	new DownloadFileThread(url, fileName).start();
    }

    private class DownloadFileThread extends Thread
    {
    	private URL url;
    	private String fileName;
    	public DownloadFileThread(String url, String fileName) throws MalformedURLException {
    		this.url = new URL(url);
    		this.fileName = fileName;
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
                    } while (!cancelUpdate);// 点击取消就停止下载.
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
            // 取消下载对话框显示
            if (mDownloadDialog != null) {
                mDownloadDialog.dismiss();
                mDownloadDialog = null;
            }
        }
    };

    private void installApk(String fileName)
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