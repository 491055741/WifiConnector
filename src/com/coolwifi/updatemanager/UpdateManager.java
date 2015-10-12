package com.coolwifi.updatemanager;

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

import com.coolwifi.httpconnection.*;
import com.coolwifi.httpconnection.HttpTask.HttpTaskHandler;
import com.xiaohong.wificoolconnect.R;

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
//import android.widget.Toast;

public class UpdateManager
{
	public String channel = null;
    private static final int DOWNLOAD = 1;
    private static final int DOWNLOAD_FINISH = 2;
    
    private static String TAG = "UpdateManager";
    HashMap<String, String> mHashMap; // update info

    private Context mContext;
    private int mVersionCode;
    private ProgressBar mProgress;
    private Dialog mDownloadDialog;
    private Downloader mDownloader;
    private long mDownloadId;
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
                if (mDownloadDialog != null) {
                    mDownloadDialog.dismiss();
                    mDownloadDialog = null;
                }
//                mDownloader.installApk( msg.obj.toString());
                break;
            default:
                break;
            }
        };
    };

    public UpdateManager(Context context)
    {
        this.mContext = context;
        mDownloader = new Downloader(context, mHandler);
        mVersionCode = 0;
    }

    public void checkUpdate() throws NotFoundException, JSONException
    {
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
	//    	task.execute("http://115.159.76.147/cb/klappversion?versionCode="+mVersionCode);
	    	String url = "http://livew.mobdsp.com/cb/klappversion?versionCode="+mVersionCode;
	    	if (channel != null && channel.length( ) > 0) {
	    		url += "&channel="+channel;
	    	}
	    	task.execute(url);
    }

	private int getVersionCode(Context context)
	{
	    if (mVersionCode == 0) {
		    try {
		        // 获取软件版本号，对应AndroidManifest.xml下android:versionCode
		        mVersionCode = context.getPackageManager().getPackageInfo("com.xiaohong.wificoolconnect", 0).versionCode;
		    } catch (NameNotFoundException e) {
		        e.printStackTrace();
		    }
	    }
	    return mVersionCode;
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
        if (mHashMap.get("force") == null || mHashMap.get("force").equals("NO")) {
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
                mDownloader.cancelDownload(mDownloadId);
            }
        });
        mDownloadDialog = builder.create();
        mDownloadDialog.show();
        mDownloadId = mDownloader.downloadApk(mHashMap.get("url"), mHashMap.get("name"));
    }

}