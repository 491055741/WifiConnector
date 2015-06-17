package com.xiaohong.wificoolconnect.main;

import com.xiaohong.wificoolconnect.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class SplashActivity extends Activity {  
    private Handler mMainHandler = new Handler() {  
        @Override
        public void handleMessage(Message msg) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClass(getApplication(), MainActivity.class);  
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
            startActivity(intent);
            // overridePendingTransition must be called AFTER finish() or startActivity, or it won't work.   
            // overridePendingTransition(R.anim.activity_in, R.anim.splash_out);  
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
}  