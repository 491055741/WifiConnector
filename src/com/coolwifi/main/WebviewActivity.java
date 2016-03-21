package com.coolwifi.main;


import com.xiaohong.wificoolconnect.R;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

public class WebviewActivity extends AppCompatActivity {
    private ActionBar mActionbar;
    private WebView webView;
    private class MyWebViewDownLoadListener implements DownloadListener{   
        @Override  
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,   
                                    long contentLength) {              
            Log.i("tag", "url="+url);
            Log.i("tag", "userAgent="+userAgent);
            Log.i("tag", "contentDisposition="+contentDisposition);            
            Log.i("tag", "mimetype="+mimetype);
            Log.i("tag", "contentLength="+contentLength);   
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);   
            startActivity(intent);              
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        String url = intent.getStringExtra("extra.url");
        String title = intent.getStringExtra("extra.title");

        setContentView(R.layout.activity_webview);
        initCustomActionBar(title);

        webView = (WebView)findViewById(R.id.webview);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setVerticalScrollBarEnabled(false);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setSupportZoom(false);
        webView.setDownloadListener(new MyWebViewDownLoadListener()); 
        webView.setWebViewClient(new WebViewClient() {
                // 重写此方法表明点击网页里面的链接还是在当前的webview里跳转，不跳到浏览器那边
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }
                });
        webView.loadUrl(url);
    }

    private boolean initCustomActionBar(String title) {
        if (mActionbar == null) {
            mActionbar = getSupportActionBar();
            if (mActionbar == null) {
                return false;
            }
        }
        mActionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        mActionbar.setDisplayShowCustomEnabled(true);
        mActionbar.setCustomView(R.layout.top_back_center_bar);
        
        TextView titleView = (TextView) mActionbar.getCustomView()
                .findViewById(R.id.title_text);
        titleView.setText(title);

        Button backBtn = (Button) mActionbar.getCustomView().findViewById(R.id.back_btn);
        backBtn.setVisibility(View.VISIBLE);
        backBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.d(TAG, "Click back btn.");
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
        return true;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_BACK) {
          if (webView.canGoBack()) {
              webView.goBack();
          } else {
              finish();
          }
          return true;
      }
      return super.onKeyDown(keyCode, event);
    }
}
