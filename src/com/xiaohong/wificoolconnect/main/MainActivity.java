package com.xiaohong.wificoolconnect.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;
import android.webkit.WebResourceResponse;
//import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.Toast;

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
import com.umeng.analytics.AnalyticsConfig;
import com.umeng.analytics.MobclickAgent;
import com.xiaohong.wificoolconnect.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
 
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;


public class MainActivity extends Activity {
	private static final String TAG = "WifiConnector";
    private static final int DOWNLOAD = 1;
    private static final int DOWNLOAD_FINISH = 2;
    private static Boolean isExit = false;
	private WebView webView;
	private UpdateManager mUpdateManager;
    private DownloadManager mDownloadManager;
	private WifiAdmin wifiAdmin;
	private HashMap<String, String> mDownloadAppInfoHashMap;
	
	private BroadcastReceiver mAppInstallReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {

	        String packageName = null;
	        if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)
	            || intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
	            packageName = intent.getData().getSchemeSpecificPart();
	        }
//	        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {}

	        ApplicationInfo applicationInfo = null;
	        PackageManager packageManager = null;
	        try {
	            packageManager = context.getPackageManager();
	            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
    	        String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
    	        String appId = mDownloadAppInfoHashMap.get(applicationName); 
    	        if (appId != null) {
    	            Toast.makeText(context, "安装成功: "+applicationName, Toast.LENGTH_LONG).show();	            
    	            webView.loadUrl("javascript: appInstallFinished("+appId+")" );
    	            mDownloadAppInfoHashMap.remove(applicationName);
    	        }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
	};

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
	
    BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
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

    private Handler mDownloadHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
            case DOWNLOAD:
//                Log.i(TAG, "download progress:"+msg.arg1);
                webView.loadUrl("javascript: updateDownloadProgress("+msg.arg1+")" );
                break;
            case DOWNLOAD_FINISH:
                Log.i(TAG, "download finished.");
                webView.loadUrl("javascript: finishDownloadProgress()");
                String fileName = msg.obj.toString();
                mDownloadManager.installApk(fileName);
                break;
            default:
                break;
            }
        };
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

	@Override
	protected void onDestroy() {
	          // TODO Auto-generated method stub
	          super.onDestroy();
	          //当Activity销毁的时候取消注册BroadcastReceiver
	          unregisterReceiver(mAppInstallReceiver);
	}

	@SuppressLint("SetJavaScriptEnabled") private void init() throws JSONException{

	    AnalyticsConfig.setChannel("channel");
		registerWIFI();
		registerConnection();
		registerAppInstall();

		PushManager.getInstance().initialize(this.getApplicationContext());

		wifiAdmin = new WifiAdmin(getBaseContext());
        mUpdateManager = new UpdateManager(MainActivity.this);
        mDownloadManager = new DownloadManager(MainActivity.this, mDownloadHandler);
        mDownloadAppInfoHashMap = new HashMap<String, String>();

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
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheMaxSize(1024*1024*8);
        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        webSettings.setAppCachePath(appCachePath);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDatabasePath("/data/data/" + webView.getContext().getPackageName() + "/databases/");
//        webView.setWebChromeClient(new WebChromeClient() {
//            public boolean onConsoleMessage(ConsoleMessage cm) {
//              Log.d("MyApplication", cm.message() + " -- From line "
//                                   + cm.lineNumber() + " of "
//                                   + cm.sourceId() );
//              return true;
//            }
//          });
//        String html = getAssetsFileContent("appBase.html");
//        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
//      webView.loadUrl("http://app.milkpapa.com:8080/?_="+(int)(Math.random()*10000));
      webView.loadUrl("file:///android_asset/appBase.html");
        CookieManager.getInstance().setAcceptCookie(true);
        webView.setWebViewClient(new WebViewClient(){
            @Override
	        public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
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
						e.printStackTrace();
					}
                    Log.d("tag", jsonStr);
//	    	        webView.loadUrl("javascript: refreshWifiList()" );
//	                webView.loadUrl("javascript: wifiStatusChanged()" );
	    	}
        });
        webView.addJavascriptInterface(this, "android");
        setContentView(webView);

        mUpdateManager.checkUpdate();
    }
    
	public void onResume() {
	    super.onResume();
	    MobclickAgent.onResume(this);
    }
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
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
//        if (id == R.id.action_settings) {
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCoder,KeyEvent event) {
        if (keyCoder != KeyEvent.KEYCODE_BACK) {
            return false;
        }
        if (webView.canGoBack()) {
            webView.goBack();   //goBack()表示返回webView的上一页面
            return true;
        } else {
            exitBy2Click();
        }
        return false;
    }

    private void exitBy2Click() {
        Timer tExit = null;
        if (isExit == false) {
            isExit = true; // 准备退出
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false; // 取消退出
                }
            }, 2000); // 如果2秒钟内没有按下返回键，则启动定时器取消掉刚才执行的任务
        } else {
            finish();
            System.exit(0);
        }
    }

//    @JavascriptInterface
    public void downloadApp(String appId, String appName, String appUrl) {
    	Log.d(TAG, "download app");
    	mDownloadAppInfoHashMap.put(appName, appId);
    	try {
			mDownloadManager.downloadApk(appUrl, "_"+(int)(Math.random()*100000)+".apk");
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

        if (MainActivity.isEmulator(getBaseContext())){
            // test data
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("SSID", "Mary");
            jsonObject.put("level", 10);
            jsonArray.put(jsonObject);
            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("SSID", "NetGear");
            jsonObject2.put("level", 90);
            jsonArray.put(jsonObject2);
        } else {
            for (ScanResult scanResult : wifiAdmin.getWifiList()) {
                JSONObject jsonObject = new JSONObject();  
                jsonObject.put("SSID", scanResult.SSID);
                jsonObject.put("level", scanResult.level);
                jsonArray.put(jsonObject);
            }
        }

        JSONObject jsonObject3 = new JSONObject();
        jsonObject3.put("wifilist", jsonArray);
    	return jsonObject3.toString();
    }
    
//    private String getAssetsFileContent(String fileName) {
//        String res="";   
//        try{   
//          
//           //得到资源中的asset数据流  
//           InputStream in = getResources().getAssets().open(fileName);   
//          
//           int length = in.available();           
//           byte [] buffer = new byte[length];          
//          
//           in.read(buffer);              
//           in.close();  
//           res = EncodingUtils.getString(buffer, "UTF-8");       
//          
//        }catch(Exception e){   
//          
//              e.printStackTrace();           
//          
//        }
//        return res;
//    }
    private void registerWIFI() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(mWifiConnectReceiver, intentFilter);
    }

    private void registerConnection() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectionReceiver, intentFilter); 
    }

    private void registerAppInstall() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        registerReceiver(mAppInstallReceiver, filter);
    }

//    @JavascriptInterface
    public boolean isAppInstalled(String appName, int versionCode) {
        ArrayList<AppInfo> list = getAllAppList();
        for (Iterator<AppInfo> iterator = list.iterator(); iterator.hasNext();) {
            AppInfo appInfo = (AppInfo) iterator.next();
            if (appName.equals(appInfo.getAppname())) { 
                return true;
            }
        }
        return false;
    }

    //  @JavascriptInterface
    public void feedback(String qq) {

        String packageName = "com.tencent.mobileqq";
        if (MainActivity.isApkInstalled(getBaseContext(), packageName)) {
            String url = "mqqwpa://im/chat?chat_type=wpa&uin="+qq;  
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));  
        } else {
            Toast.makeText(getBaseContext(), "请先安装QQ", Toast.LENGTH_LONG).show();
        }
    }

    private static final boolean isApkInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
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

    public static boolean isEmulator(Context context){
        try{
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String imei = tm.getDeviceId();
            if (imei != null && imei.equals("000000000000000")){
                return true;
            }
            return  (Build.MODEL.equals("sdk")) || (Build.MODEL.equals("google_sdk"));
        }catch (Exception ioe) { 

        }
        return false;
    }
}
