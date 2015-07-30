package com.coolwifi.main;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.igexin.sdk.PushManager;
import java.net.MalformedURLException;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.coolwifi.httpconnection.HttpRequest;
import com.coolwifi.updatemanager.*;
import com.coolwifi.wifiadmin.*;
import com.umeng.analytics.AnalyticsConfig;
import com.umeng.analytics.MobclickAgent;
import com.umeng.fb.FeedbackAgent;
import com.xiaohong.wificoolconnect.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;


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
	private ActionBar mActionbar;
	private FeedbackAgent feedbackAgent;
	private boolean mIsActive = true; // 是否进入后台
	
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
    	        Log.d(TAG, "installed ["+ applicationName +"] pkg-name: "+applicationInfo.packageName);
    	        String appId = mDownloadAppInfoHashMap.get(applicationInfo.packageName); 
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
//            }else {
//                // connect network
//                
//            }
        }
    };

    private Handler mWebviewHandler = new Handler()
    {  
        public void handleMessage(Message msg) {// 定义一个Handler，用于处理webview线程与UI间通讯  
            if (!Thread.currentThread().isInterrupted()){
                switch (msg.what) {  
                case 0:
                    boolean isShow = (msg.arg1 == 1);
                    Button backBtn = (Button)mActionbar.getCustomView().findViewById(R.id.back_btn);
                    if (isShow) {
                        backBtn.setVisibility(View.VISIBLE);
                    } else {
                        backBtn.setVisibility(View.INVISIBLE);
                    }
                    break;  
                case 1:  
                    break;  
                }  
            }  
            super.handleMessage(msg);  
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
	    super.onDestroy();
        unregisterReceiver(mAppInstallReceiver);
	}

	@SuppressLint("SetJavaScriptEnabled") private void init() throws JSONException{

	    initCustomActionBar();
	    AnalyticsConfig.setChannel("channel");

	    registerWIFI();
		registerConnection();
		registerAppInstall();
        feedbackAgent = new FeedbackAgent(this);
		feedbackAgent.sync();
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
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheMaxSize(1024*1024*8);
        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        webSettings.setAppCachePath(appCachePath);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDatabasePath("/data/data/" + webView.getContext().getPackageName() + "/databases/");
        String ua = webSettings.getUserAgentString();
        webSettings.setUserAgentString(ua+";WIFICoolConnect;");
        

        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onConsoleMessage(ConsoleMessage cm) {
              Log.d("MyApplication", cm.message() + " -- From line "
                                   + cm.lineNumber() + " of "
                                   + cm.sourceId() );
              return true;
            }
            @Override 
            public void onReceivedTitle(WebView view, String title) { 

                TextView titleView = (TextView)mActionbar.getCustomView().findViewById(R.id.title_text);
                titleView.setText(title);
//                webView.loadUrl("javascript: configBackBtn()" );
                super.onReceivedTitle(view, title);
            }
        });

        webView.loadUrl("file:///android_asset/appBase.html");
        CookieManager.getInstance().setAcceptCookie(true);
        webView.addJavascriptInterface(this, "android");
        setContentView(webView);

        mUpdateManager.checkUpdate();
    }
	//  @JavascriptInterface
	public void showBackBtn(boolean isShow) {
	    Log.d(TAG, "Show back button: "+isShow);
        Message msg = new Message();
        msg.what = 0;
        msg.arg1 = isShow ? 1 : 0;
        mWebviewHandler.sendMessage(msg);
	}

	//  @JavascriptInterface
    public void httpRequst(String url, String method, String data) {
        Log.d(TAG, "httpRequest:["+method+"]"+url+"?"+data);
        HttpRequest.post(url, data);
    }
	
	private boolean initCustomActionBar() {

	    if (mActionbar == null) {
	        mActionbar = getActionBar();
	        if (mActionbar == null) {
	            return false;
	        }
	    }
	    mActionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
	    mActionbar.setDisplayShowCustomEnabled(true);
        mActionbar.setCustomView(R.layout.top_back_center_bar);
        Button backBtn = (Button)mActionbar.getCustomView().findViewById(R.id.back_btn);
        backBtn.setVisibility(View.INVISIBLE);
	    backBtn.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            Log.d(TAG, "Click back btn.");
	            if (webView.canGoBack()) {
	                webView.goBack();
	            } else {
	                Log.d(TAG, "webview.canGoBack() == false");
	            }
	        }
	    });
	    return true;
	}

	public void onResume() {
	    super.onResume();
	    MobclickAgent.onResume(this);
	    if (!mIsActive) {// app从后台唤醒，进入前台
	    	mIsActive = true;
	    	webView.loadUrl("javascript: wifiStatusChanged()");
	    }
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
        Button backBtn = (Button)mActionbar.getCustomView().findViewById(R.id.back_btn);
        boolean backBtnShown = (backBtn.getVisibility() == View.VISIBLE);
        if (webView.canGoBack() && backBtnShown) {
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
    public void downloadApp(String appId, String pkgName, String appUrl) {
    	Log.d(TAG, "download app");
    	mDownloadAppInfoHashMap.put(pkgName, appId);
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
    public boolean isAppInstalled(String pkgName, int versionCode) {
        ArrayList<AppInfo> list = getAllAppList();
        for (Iterator<AppInfo> iterator = list.iterator(); iterator.hasNext();) {
            AppInfo appInfo = (AppInfo) iterator.next();
            if (pkgName.equals(appInfo.getPackagename())) { 
                return true;
            }
        }
        return false;
    }

    //  @JavascriptInterface
	public String getVersion()
	{
	    String version = "1.0Build0";
	    try {
	        // 获取软件版本号，对应AndroidManifest.xml下android:versionCode
	    	PackageInfo packageInfo = getBaseContext().getPackageManager().getPackageInfo("com.xiaohong.wificoolconnect", 0);	
	        version =  packageInfo.versionName +"build"+ packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        e.printStackTrace();
	    }
	    return version;
	}
    
    //  @JavascriptInterface
    public void feedback() {
        feedbackAgent.startFeedbackActivity();
        return;
    }

    //  @JavascriptInterface
    public void openQQ(String qq) {
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
            appInfo.setPackagename(pInfo.packageName);//应用程序的包
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

    public String getMacAddress()
    {
        Context context = getBaseContext();
        WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE); 
        WifiInfo info = wifi.getConnectionInfo();
        String macAddress = info.getMacAddress(); //获取mac地址
        return macAddress;
    }

    public String getIMEI()
    {
        String imei = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId();
        return imei;
    }

    protected void onStop() {
    	super.onStop();
    	if (!isAppOnForeground()) {
    		mIsActive = false;// 记录当前已经进入后台
    	}
    }

    private boolean isAppOnForeground()
    {
    	ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
    	String packageName = getApplicationContext().getPackageName();
    
	    List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
	    if (appProcesses == null)
	    	return false;
	    for (RunningAppProcessInfo appProcess : appProcesses) {
	    	// The name of the process that this object is associated with.
	    	if (appProcess.processName.equals(packageName) && appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
	    		return true;
	    	}
	    }
	    return false;
    }

}
