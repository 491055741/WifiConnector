package com.coolwifi.main;

import org.json.JSONException;

import com.umeng.analytics.MobclickAgent;
import com.xiaohong.wificoolconnect.R;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class SplashActivity extends Activity {  
    private Handler mMainHandler = new Handler() {  
        @Override
        public void handleMessage(Message msg) {

	        try {
				int versionCode = getBaseContext().getPackageManager().getPackageInfo("com.xiaohong.wificoolconnect", 0).versionCode;
	        	SharedPreferences preferences = getSharedPreferences("versionCode",MODE_WORLD_READABLE);
	            int latestVersionCode = preferences.getInt("version", 0);
//  move to mainActivity
//	            Editor editor = preferences.edit();
//                editor.putInt("version", versionCode);
//                editor.commit();

	            if (latestVersionCode < versionCode) {  // first time run for the current version
	            	Intent intent = new Intent(Intent.ACTION_MAIN);
	                intent.setClass(getApplication(), IntroActivity.class);  
	                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
	                startActivity(intent);
	            } else {
		        	Intent intent = new Intent(Intent.ACTION_MAIN);
		            intent.setClass(getApplication(), MainActivity.class);  
		            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
		            startActivity(intent);
	            }

	            // overridePendingTransition must be called AFTER finish() or startActivity, or it won't work.   
	            // overridePendingTransition(R.anim.activity_in, R.anim.splash_out);  
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    };

    @Override  
    public void onCreate(Bundle icicle) {  
        super.onCreate(icicle);
        getWindow().setBackgroundDrawableResource(R.drawable.splash);  
        mMainHandler.sendEmptyMessageDelayed(0, 1000);
    }
      
    // much easier to handle key events   
    @Override  
    public void onBackPressed() {  
    }
    
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}  