package com.coolwifi.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
//import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import com.igexin.sdk.PushManager;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.http.util.EncodingUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.coolwifi.updatemanager.*;
import com.coolwifi.wifiadmin.*;
import com.xiaohong.wificoolconnect.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
 
import android.content.pm.PackageInfo;
import android.os.Bundle;

public class MainActivity extends Activity {
	private static final String TAG = "[WifiAdmin]";
	private WebView webView;
//	private WifiManager wifiManager;
	private UpdateManager updateManager;
	private WifiAdmin wifiAdmin;

    private BroadcastReceiver mWifiConnectReceiver = new BroadcastReceiver() {
		@Override
    	public void onReceive(Context context, Intent intent) {
	        Log.d(TAG, "Wifi onReceive action = " + intent.getAction());
	        if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
	            int message = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
	            Log.d(TAG, "liusl wifi onReceive msg=" + message);
	            switch (message) {
		            case WifiManager.WIFI_STATE_DISABLED:
		                Log.d(TAG, "WIFI_STATE_DISABLED");
		                break;
		            case WifiManager.WIFI_STATE_DISABLING:
		                Log.d(TAG, "WIFI_STATE_DISABLING");
		                break;
		            case WifiManager.WIFI_STATE_ENABLED:
		                Log.d(TAG, "WIFI_STATE_ENABLED");
	//	                threadSleep(10000);
	//	                pingWifiGateway(EthUtils.getWifiGateWay());
		                break;
		            case WifiManager.WIFI_STATE_ENABLING:
		                Log.d(TAG, "WIFI_STATE_ENABLING");
		                break;
		            case WifiManager.WIFI_STATE_UNKNOWN:
		                Log.d(TAG, "WIFI_STATE_UNKNOWN");
		                break;
		            default:
		                break;
	            }
	        }
	    }
    };
	
	@Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
//        setContentView(R.layout.activity_main);             
        try {
			init();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	@SuppressLint("SetJavaScriptEnabled") private void init() throws JSONException{

		registerWIFI();
		PushManager.getInstance().initialize(this.getApplicationContext());
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(connectionReceiver, intentFilter); 
		
        wifiAdmin = new WifiAdmin(getBaseContext()); 
        updateManager = new UpdateManager(MainActivity.this);
        boolean open = wifiAdmin.openWifi();
        Log.i(TAG, "wifi open:" + open);
        wifiAdmin.startScan();

        webView = new WebView(this);
        webView.setVerticalScrollBarEnabled(false);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setSupportZoom(false);
//        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

//        webSettings.setUserAgentString(getUserAgent());
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        
        
//        String html = getAssetsFileContent("appBase.html");
//        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
//      webView.loadUrl("http://app.milkpapa.com:8080/?_="+(int)(Math.random()*10000));
      webView.loadUrl("file:///android_asset/appBase.html");
        CookieManager.getInstance().setAcceptCookie(true);
            //覆盖WebView默认使用第三方或系统默认浏览器打开网页的行为，使网页用WebView打开
        webView.setWebViewClient(new WebViewClient(){
            @Override
	        public boolean shouldOverrideUrlLoading(WebView view, String url) {
	            // TODO Auto-generated method stub
	               //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
	             view.loadUrl(url);
	            return true;
	        }

	       	@Override
	    	public void onPageFinished(WebView view, String url) 
	    	 {
	    	        super.onPageFinished(view, url);
	    	        String jsonStr = null;
					try {
						jsonStr = wifiListJsonString();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    Log.d("tag", jsonStr);
	    	        webView.loadUrl("javascript: refreshWifiList()" );
	                webView.loadUrl("javascript: wifiStatusChanged()" );
	    	 }

        });
        webView.addJavascriptInterface(this, "android");
        setContentView(webView);

        updateManager.checkUpdate();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCoder,KeyEvent event){
        if(webView.canGoBack() && keyCoder == KeyEvent.KEYCODE_BACK){
              webView.goBack();   //goBack()表示返回webView的上一页面
                 return true;
           }
        return false;
    }
    
//    @JavascriptInterface
    public void downloadApp(String appUrl) {
    	Log.d(TAG, "download app");
    	try {
			updateManager.downloadApk(appUrl, "_"+(int)(Math.random()*100000)+".apk");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
//    @JavascriptInterface
    public void connectWifi(String ssid, String passwd) {
    	Log.d(TAG, "Try to connect wifi");
    //  1.WIFICIPHER_NOPASS
    //  2.WIFICIPHER_WEP
    //  3.WIFICIPHER_WPA   
    	int type = 3;
    	if (passwd.equals("")) {
    		type = 1;
    	}
    	wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(ssid, passwd, type));
    }
    
//    @JavascriptInterface
    public boolean isWifiAvailable() {
        ConnectivityManager conMan = (ConnectivityManager)(getBaseContext())
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        State wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .getState();
        if (State.CONNECTED == wifi) {
            return true;
        } else {
            return false;
        }
    }

//    @JavascriptInterface
    public String wifiListJsonString() throws JSONException {
        JSONArray jsonArray = new JSONArray();

        // test data
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("SSID", "Mary");
//        jsonObject.put("level", 10);
//        jsonArray.put(jsonObject);
//        JSONObject jsonObject2 = new JSONObject();
//        jsonObject2.put("SSID", "NetGear");
//        jsonObject2.put("level", 90);
//        jsonArray.put(jsonObject2);

        for (ScanResult scanResult : wifiAdmin.getWifiList()) {
            JSONObject jsonObject = new JSONObject();  
			jsonObject.put("SSID", scanResult.SSID);
			jsonObject.put("level", scanResult.level);
            jsonArray.put(jsonObject);
        }

        JSONObject jsonObject3 = new JSONObject();
        jsonObject3.put("wifilist", jsonArray);
    	return jsonObject3.toString();
    }
    
    private void registerWIFI() {
        IntentFilter mWifiFilter = new IntentFilter();
        mWifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(mWifiConnectReceiver, mWifiFilter);
    }
    
    private String getAssetsFileContent(String fileName) {
        String res="";   
        try{   
          
           //得到资源中的asset数据流  
           InputStream in = getResources().getAssets().open(fileName);   
          
           int length = in.available();           
           byte [] buffer = new byte[length];          
          
           in.read(buffer);              
           in.close();  
           res = EncodingUtils.getString(buffer, "UTF-8");       
          
        }catch(Exception e){   
          
              e.printStackTrace();           
          
        }
        return res;
    }

    BroadcastReceiver connectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            webView.loadUrl("javascript: wifiStatusChanged()");
//            ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
//            NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//            if (!wifiNetInfo.isConnected()) {
//                Log.i(TAG, "unconnect");
//                // unconnect network
//
//            }else {
//                // connect network
//                
//            }
        }
    };

//    @JavascriptInterface
    public boolean isAppInstalled(String appName, int versionCode) {
        ArrayList<AppInfo> list = getAllAppList();
        for (Iterator<AppInfo> iterator = list.iterator(); iterator.hasNext();) {
            AppInfo appInfo = (AppInfo) iterator.next();
            if (appName == appInfo.getAppname()) { // todo: 
                return true;
            }
        }
        return false;
    }

    private ArrayList<AppInfo> getAllAppList() {
        ArrayList<AppInfo> appList = new ArrayList<AppInfo>();
        List<PackageInfo> packageInfos=getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packageInfos.size(); i++) {
            PackageInfo pInfo=packageInfos.get(i);
            AppInfo appInfo= new AppInfo();
            appInfo.setAppname(pInfo.applicationInfo.loadLabel(getPackageManager()).toString());//应用程序的名称
//            appInfo.setPackagename(pInfo.packageName);//应用程序的包
            appInfo.setVersionCode(pInfo.versionCode);//版本号
            appList.add(appInfo);
        }
        return appList;
    }

}
