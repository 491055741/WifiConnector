package com.coolwifi.main;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;

import com.igexin.sdk.PushManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.coolwifi.httpconnection.HttpRequest;
import com.coolwifi.updatemanager.Downloader;
import com.coolwifi.updatemanager.UpdateManager;
import com.coolwifi.wifiadmin.*;
import com.umeng.analytics.MobclickAgent;
import com.xiaohong.wificoolconnect.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "WifiConnector";
	private static final int DOWNLOAD = 1;
	private static final int DOWNLOAD_FINISH = 2;
	private static Boolean isExit = false;
	private WebView webView;
	private Downloader mDownloader;
	private WifiAdmin wifiAdmin;
	private HashMap<String, String> mDownloadAppInfoHashMap;
	private HashMap<String, String> mDownloadIdHashMap;
	private ActionBar mActionbar;
	private boolean mIsActive = true; // 是否进入后台
	private SmsObserver smsObserver;
	private MyBroadcastReceiver mConnectionReceiver;

	class SmsObserver extends ContentObserver {

		public SmsObserver(Context context, Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			// 每当有新短信到来时，使用我们获取短消息的方法
			getSmsFromPhone();
		}
	}

	public Handler smsHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			System.out.println("smsHandler 执行了.....");
		};
	};

	private Uri SMS_INBOX = Uri.parse("content://sms/");

	public void getSmsFromPhone() {
		ContentResolver cr = getContentResolver();
		String[] projection = new String[] { "body", "address", "person" };// "_id",
																			// "address",
		// "person",, "date",
		// "type
		String where = " date >  "
				+ (System.currentTimeMillis() - 10 * 60 * 1000);
		Cursor cur = cr.query(SMS_INBOX, projection, where, null, "date desc");
		if (null == cur)
			return;
		if (cur.moveToNext()) {
			String number = cur.getString(cur.getColumnIndex("address"));// 手机号
			String name = cur.getString(cur.getColumnIndex("person"));// 联系人姓名列表
			String body = cur.getString(cur.getColumnIndex("body"));

			System.out.println(">>>>>>>>>>>>>>>>手机号：" + number);
			System.out.println(">>>>>>>>>>>>>>>>联系人姓名列表：" + name);
			System.out.println(">>>>>>>>>>>>>>>>短信的内容：" + body);

			// 这里我是要获取自己短信服务号码中的验证码~~
			// 【小鸿网络】您的验证码是8992。如非本人操作，请忽略本短信
			Pattern pattern = Pattern.compile("【小鸿网络】您的验证码是[0-9]{4}");
			Matcher matcher = pattern.matcher(body);
			if (matcher.find()) {
				String match = matcher.group();
				String res = match.substring(12, 16);// 获取短信中的验证码

				System.out.println(res);
				webView.loadUrl("javascript: receivedVerifyCode('" + res + "')");
				// stop observer
				getContentResolver().unregisterContentObserver(smsObserver);
			}
		}
	}

	// @JavascriptInterface
	public void shenZhouShuMaAuth() {
		new Thread(shenMaAuthTask).start();
	}

	private void sendShenZhouAuthRequest() throws Exception {
		Log.d(TAG, "sendShenZhouAuthRequest");
		String url = "http://www.baidu.com";
		String redictURL = getRedirectUrl(url);
		if (redictURL == null) {
			Log.d(TAG, "url not redirected");
			return;
		}
		String ip = getUrlPara(redictURL, "ip");
		String gw = getUrlPara(redictURL, "gw");

		String authUrl = "http://" + gw
				+ ":8800/dcmecloud/interface/RestHttpAuth.php?har={\"ip\":\"" + ip
				+ "\",\"tool\":\"onekey\"}";
		webView.loadUrl("javascript: alert(" + authUrl + ")");
		HttpURLConnection conn = (HttpURLConnection) new URL(authUrl)
				.openConnection();
		conn.setInstanceFollowRedirects(false);
		conn.setConnectTimeout(5000);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				conn.getInputStream(), "utf-8"));
		String line = "";
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
		}
	}

	private String getUrlPara(String url, String key) {
		String params = url.substring(url.indexOf("?") + 1);
		Pattern pattern = Pattern.compile("(^|&)" + key + "=([^&]*)(&|$)");
		Matcher m = pattern.matcher(params);
		while (m.find()) {
			String value = m.group(2);
			return value;
		}
		return null;
	}

	private String getRedirectUrl(String path) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(path)
				.openConnection();
		conn.setInstanceFollowRedirects(false);
		conn.setConnectTimeout(5000);
		return conn.getHeaderField("Location");
	}

	private BroadcastReceiver mAppInstallReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String packageName = null;
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)
					|| intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
				packageName = intent.getData().getSchemeSpecificPart();
				ApplicationInfo applicationInfo = null;
				PackageManager packageManager = null;
				try {
					packageManager = context.getPackageManager();
					applicationInfo = packageManager.getApplicationInfo(packageName, 0);
					String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
					Log.d(TAG, "installed [" + applicationName + "] pkg-name: "	+ applicationInfo.packageName);
					String appId = mDownloadAppInfoHashMap.get(applicationInfo.packageName);
					if (appId != null) {
						Toast.makeText(context, "安装成功: " + applicationName, Toast.LENGTH_LONG).show();
						webView.loadUrl("javascript: appInstallFinished(" + appId + ")");
						mDownloadAppInfoHashMap.remove(applicationName);
					}
				} catch (PackageManager.NameNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	};

	private BroadcastReceiver mAppLanchReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String packageName = null;
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_FIRST_LAUNCH)) {
				packageName = intent.getData().getSchemeSpecificPart();

				ApplicationInfo applicationInfo = null;
				PackageManager packageManager = null;
				try {
					packageManager = context.getPackageManager();
					applicationInfo = packageManager.getApplicationInfo(packageName, 0);
					String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
					Log.d(TAG, "Lanched [" + applicationName + "] pkg-name: "	+ applicationInfo.packageName);
					Toast.makeText(context, "运行成功: " + applicationName, Toast.LENGTH_LONG).show();
					webView.loadUrl("javascript: appLanched('" + applicationInfo.packageName + "')");
				} catch (PackageManager.NameNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	};

	private class MyBroadcastReceiver extends BroadcastReceiver {  
	    @Override  
	    public void onReceive(Context context, Intent intent) {  
	        Log.v(TAG, "onReceive");  
	        requestCheckConnection();
	    }  
	}

	Runnable shenMaAuthTask = new Runnable() {
		@Override
		public void run() {
			try {
				sendShenZhouAuthRequest();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private void checkConnection() {
		ConnectivityManager nw = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netinfo = nw.getActiveNetworkInfo();
		if (netinfo != null && netinfo.isAvailable()) {
			webView.loadUrl("javascript: checkLogin()");
		}

		ConnectivityManager conMan = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (State.CONNECTED != conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState()) {
			Log.i(TAG, "unconnect");
			webView.loadUrl("javascript: wifiStatusChanged()");
		} else {
			Log.i(TAG, "connected");
			WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			String ssid = wifiInfo.getSSID();
			Log.d("SSID", ssid);
			webView.loadUrl("javascript: wifiStatusChanged('" + ssid + "')");
		}
	}

	private Handler mWebviewHandler = new Handler() {
		public void handleMessage(Message msg) {// 定义一个Handler，用于处理webview线程与UI间通讯
			if (!Thread.currentThread().isInterrupted()) {
				switch (msg.what) {
				case 0:
					boolean isShow = (msg.arg1 == 1);
					Button backBtn = (Button) mActionbar.getCustomView()
							.findViewById(R.id.back_btn);
					if (isShow) {
						backBtn.setVisibility(View.VISIBLE);
					} else {
						backBtn.setVisibility(View.INVISIBLE);
					}
					break;
				case 1:
					checkConnection();
					break;
				case 2:

					break;
				}
			}
			super.handleMessage(msg);
		}
	};

	private Handler mDownloadHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DOWNLOAD: {
				// Log.i(TAG, "download progress:"+msg.arg1);
				String downloadIdStr = String.valueOf(msg.arg1);
				String appId = mDownloadIdHashMap.get(downloadIdStr);
				String progressStr = String.valueOf(msg.arg2);
				webView.loadUrl("javascript: updateDownloadProgress(" + appId
						+ "," + progressStr + ")");
				break;
			}
			case DOWNLOAD_FINISH: {
				Log.i(TAG, "download finished.");
				String downloadIdStr = String.valueOf(msg.arg1);
				String appId = mDownloadIdHashMap.get(downloadIdStr);
				webView.loadUrl("javascript: finishDownloadProgress('" + appId
						+ "')");
				mDownloadIdHashMap.remove(downloadIdStr);
				break;
			}
			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		// setContentView(R.layout.activity_main);
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
		if (mConnectionReceiver != null) {
			unregisterReceiver(mConnectionReceiver);
			mConnectionReceiver = null;
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void init() throws JSONException {

		String path = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
		Log.d("tag", "download path: " + path);

		String sdpath = Environment.getExternalStorageDirectory() + "/";
		String mSavePath = sdpath + "download";
		Log.d("tag", "mSavePath path: " + mSavePath);

		UpdateManager updateManager = new UpdateManager(MainActivity.this);
		updateManager.channel= getChannel();
		try {
			updateManager.checkUpdate();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		initCustomActionBar();

		checkDownloadManager();
		PushManager.getInstance().initialize(this.getApplicationContext());

		wifiAdmin = new WifiAdmin(getBaseContext());
		mDownloader = new Downloader(MainActivity.this, mDownloadHandler);
		mDownloadAppInfoHashMap = new HashMap<String, String>();
		mDownloadIdHashMap = new HashMap<String, String>();

		registerConnection();
		registerAppInstall();
		registerAppLanch();
		
		boolean open = wifiAdmin.openWifi();
		Log.i(TAG, "wifi open:" + open);
		wifiAdmin.startScan();
		smsObserver = new SmsObserver(this, smsHandler);
		webView = new WebView(this);
		webView.setVerticalScrollBarEnabled(false);
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setBuiltInZoomControls(false);
		webSettings.setSupportZoom(false);
		webSettings.setDomStorageEnabled(true);
		// webSettings.setAppCacheMaxSize(1024*1024*8);
		String appCachePath = getApplicationContext().getCacheDir()
				.getAbsolutePath();
		webSettings.setAppCachePath(appCachePath);
		webSettings.setAllowFileAccess(true);
		webSettings.setAppCacheEnabled(true);
		webSettings.setDatabaseEnabled(true);
		webSettings.setDatabasePath("/data/data/" + webView.getContext().getPackageName() + "/databases/");
		String ua = webSettings.getUserAgentString();
		webSettings.setUserAgentString(ua + ";WIFICoolConnect;");

		webView.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cm) {
				Log.d("MyApplication", cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
				return true;
			}

			@Override
			public void onReceivedTitle(WebView view, String title) {

				TextView titleView = (TextView) mActionbar.getCustomView()
						.findViewById(R.id.title_text);
				titleView.setText(title);
				super.onReceivedTitle(view, title);
			}
		});

		webView.loadUrl("file:///android_asset/appBase.html");
		CookieManager.getInstance().setAcceptCookie(true);
		webView.addJavascriptInterface(this, "android");
		setContentView(webView);
	}

	// @JavascriptInterface
	public String getChannel() {
//		return "shenma";
		return "";
	}
	
	// @JavascriptInterface
	public void showBackBtn(boolean isShow) {
		Log.d(TAG, "Show back button: " + isShow);
		Message msg = new Message();
		msg.what = 0;
		msg.arg1 = isShow ? 1 : 0;
		mWebviewHandler.sendMessage(msg);
	}

	// @JavascriptInterface
	public void startVerifyCodeObserver() {
		getContentResolver().registerContentObserver(SMS_INBOX, true, smsObserver);
	}

	// @JavascriptInterface
	public void requestCheckConnection() {
		Log.d(TAG, "requestCheckConnection");
		Message msg = new Message();
		msg.what = 1;
		mWebviewHandler.sendMessage(msg);
	}

	// @JavascriptInterface
	public void httpRequst(String url, String method, String data) {
		Log.d(TAG, "httpRequest:[" + method + "]" + url + "?" + data);
		HttpRequest.post(url, data);
	}

	// @JavascriptInterface
	public void saveItem(String key, String value) {
		SharedPreferences.Editor editor = getSharedPreferences("xiaohong",
				MODE_WORLD_WRITEABLE).edit();
		editor.putString(key, value);
		editor.commit();
	}

	// @JavascriptInterface
	public String getItem(String key) {
		SharedPreferences read = getSharedPreferences("xiaohong", MODE_PRIVATE);
		String val = read.getString(key, "");
		Log.d(TAG, "getItem:" + key + " val:" + val);
		return val;
	}

	private boolean initCustomActionBar() {

		if (mActionbar == null) {
			mActionbar = getSupportActionBar();
			if (mActionbar == null) {
				return false;
			}
		}
		mActionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		mActionbar.setDisplayShowCustomEnabled(true);
		mActionbar.setCustomView(R.layout.top_back_center_bar);
		Button backBtn = (Button) mActionbar.getCustomView().findViewById(
				R.id.back_btn);
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
		MobclickAgent.onResume(this);
		if (!mIsActive) {// app从后台唤醒，进入前台
			mIsActive = true;
			checkConnection();
		}
		super.onResume();
	}

	public void onPause() {
		MobclickAgent.onPause(this);
		super.onPause();
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
		// if (id == R.id.action_settings) {
		// return true;
		// }
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyDown(int keyCoder, KeyEvent event) {
		if (keyCoder != KeyEvent.KEYCODE_BACK) {
			return false;
		}
		Button backBtn = (Button) mActionbar.getCustomView().findViewById(
				R.id.back_btn);
		boolean backBtnShown = (backBtn.getVisibility() == View.VISIBLE);
		if (webView.canGoBack() && backBtnShown) {
			webView.goBack(); // goBack()表示返回webView的上一页面
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
			Toast.makeText(this, "再按一次退出WIFI酷连", Toast.LENGTH_SHORT).show();
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

	// @JavascriptInterface
	public void downloadApp(String appId, String appName, String pkgName,
			String appUrl) {
		Log.d(TAG, "download app");
		mDownloadAppInfoHashMap.put(pkgName, appId);
		try {
			Long downloadId = mDownloader.downloadApk(appUrl, appName); // "_"+(int)(Math.random()*100000)
			mDownloadIdHashMap.put(String.valueOf(downloadId), appId);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// @JavascriptInterface
	public void connectWifi(String ssid, String passwd, String encType) {
		Log.d(TAG, "Try to connect wifi");

		WifiManager wifiManager = (WifiManager) getBaseContext()
				.getSystemService(Context.WIFI_SERVICE);
		wifiManager.setWifiEnabled(true);

		// 1.WIFICIPHER_NOPASS
		// 2.WIFICIPHER_WEP
		// 3.WIFICIPHER_WPA
		int type = 1;
		if (passwd.equals("")) {
			type = 1;
		} else if (encType.equals("WEP")) {
			type = 2;
		} else if (encType.equals("WPA")) {
			type = 3;
		}

		wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(ssid, passwd, type));
	}

	// @JavascriptInterface
	public boolean isWifiAvailable() {
		ConnectivityManager conMan = (ConnectivityManager) (getBaseContext())
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		State wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.getState();
		if (State.CONNECTED == wifi) {
			return true;
		} else {
			return false;
		}
	}

	// @JavascriptInterface
	public String wifiListJsonString() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if (MainActivity.isEmulator(getBaseContext())) {
			// test data
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("SSID", "Mary");
			jsonObject.put("level", 10);
			jsonObject.put("encrypt",
					wifiEncryptType("[WPA-PSK-CCMP][WPA2-PSK-CCMP][ESS]"));
			jsonArray.put(jsonObject);
			JSONObject jsonObject2 = new JSONObject();
			jsonObject2.put("SSID", "@小鸿科技");
			jsonObject2.put("level", 90);
			jsonObject.put("encrypt", wifiEncryptType("[ESS]"));
			jsonArray.put(jsonObject2);
		} else {
			wifiAdmin.startScan();
			for (ScanResult scanResult : wifiAdmin.getWifiList()) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("SSID", scanResult.SSID);
				jsonObject.put("encrypt",
						wifiEncryptType(scanResult.capabilities));
				jsonObject.put("level", scanResult.level);
				// Log.d(TAG,
				// "SSID["+scanResult.SSID+"] cap: "+scanResult.capabilities);
				jsonArray.put(jsonObject);
			}
		}

		JSONObject jsonObject3 = new JSONObject();
		jsonObject3.put("wifilist", jsonArray);
		return jsonObject3.toString();
	}
	// @JavascriptInterface
	public String getMobileInfo() {
		return "model:"+android.os.Build.MODEL + ",manufacturer:" + android.os.Build.MANUFACTURER + ",os:" + android.os.Build.VERSION.RELEASE;
	}
	// @JavascriptInterface
	public String getIMSI() {
		TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String imsi = telManager.getSubscriberId();
		return imsi;
	}

	private String wifiEncryptType(String capabilities) {
		ArrayList<String> encTypes = new ArrayList<String>();
		encTypes.add("WEP");
		encTypes.add("WPA");
		encTypes.add("WAPI");
		for (String type : encTypes) {
			if (capabilities.indexOf(type) != -1) {
				return type;
			}
		}
		return "";
	}

	private void checkDownloadManager() {
		int state = this.getPackageManager().getApplicationEnabledSetting(
				"com.android.providers.downloads");

		if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
				|| state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
			String packageName = "com.android.providers.downloads";

			try {
				// Open the specific App Info page:
				Intent intent = new Intent(
						android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
				intent.setData(Uri.parse("package:" + packageName));
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				// e.printStackTrace();
				// Open the generic Apps page:
				Intent intent = new Intent(
						android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
				startActivity(intent);
			}
		}
	}

	private void registerConnection() {
		if (mConnectionReceiver == null) {
			mConnectionReceiver = new MyBroadcastReceiver();
		}
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

	private void registerAppLanch() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_FIRST_LAUNCH);
		filter.addDataScheme("package");
		registerReceiver(mAppLanchReceiver, filter);
	}

	// @JavascriptInterface
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

	// @JavascriptInterface
	public String getVersion() {
		String version = "1.0Build0";
		try {
			// 获取软件版本号，对应AndroidManifest.xml下android:versionCode
			PackageInfo packageInfo = getBaseContext().getPackageManager()
					.getPackageInfo("com.xiaohong.wificoolconnect", 0);
			version = packageInfo.versionName + "build"
					+ packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return version;
	}

	// @JavascriptInterface
	public void openQQ(String qq) {
		String packageName = "com.tencent.mobileqq";
		if (MainActivity.isApkInstalled(getBaseContext(), packageName)) {
			String url = "mqqwpa://im/chat?chat_type=wpa&uin=" + qq;
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		} else {
			Toast.makeText(getBaseContext(), "请先安装QQ", Toast.LENGTH_LONG)
					.show();
		}
	}

	// @JavascriptInterface
	public void startAPP(String appPackageName) {
		Log.d(TAG, "startApp:" + appPackageName);
		try {
			Intent intent = this.getPackageManager().getLaunchIntentForPackage(
					appPackageName);
			startActivity(intent);
		} catch (Exception e) {
			Toast.makeText(this, "没有安装", Toast.LENGTH_LONG).show();
		}
	}

	// @JavascriptInterface
	public void socialShare() {

		ShareSDK.initSDK(this);
		OnekeyShare oks = new OnekeyShare();
		// 关闭sso授权
		oks.disableSSOWhenAuthorize();

		// 分享时Notification的图标和文字 2.5.9以后的版本不调用此方法
		// oks.setNotification(R.drawable.ic_launcher,
		// getString(R.string.app_name));
		// title标题，印象笔记、邮箱、信息、微信、人人网和QQ空间使用
		oks.setTitle(getString(R.string.share));
		// titleUrl是标题的网络链接，仅在人人网和QQ空间使用
		oks.setTitleUrl("http://sharesdk.cn");
		// text是分享文本，所有平台都需要这个字段
		oks.setText("我是分享文本");
		// imagePath是图片的本地路径，Linked-In以外的平台都支持此参数
		oks.setImagePath("/sdcard/test.jpg");// 确保SDcard下面存在此张图片
		// url仅在微信（包括好友和朋友圈）中使用
		oks.setUrl("http://sharesdk.cn");
		// comment是我对这条分享的评论，仅在人人网和QQ空间使用
		oks.setComment("我是测试评论文本");
		// site是分享此内容的网站名称，仅在QQ空间使用
		oks.setSite(getString(R.string.app_name));
		// siteUrl是分享此内容的网站地址，仅在QQ空间使用
		oks.setSiteUrl("http://sharesdk.cn");

		// 启动分享GUI
		oks.show(this);
	}

	private static final boolean isApkInstalled(Context context,
			String packageName) {
		try {
			context.getPackageManager().getApplicationInfo(packageName,
					PackageManager.GET_UNINSTALLED_PACKAGES);
			return true;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	private ArrayList<AppInfo> getAllAppList() {
		ArrayList<AppInfo> appList = new ArrayList<AppInfo>();
		List<PackageInfo> packageInfos = getPackageManager()
				.getInstalledPackages(0);
		for (int i = 0; i < packageInfos.size(); i++) {
			PackageInfo pInfo = packageInfos.get(i);
			AppInfo appInfo = new AppInfo();
			appInfo.setAppname(pInfo.applicationInfo.loadLabel(
					getPackageManager()).toString());// 应用程序的名称
			appInfo.setPackagename(pInfo.packageName);// 应用程序的包
			appInfo.setVersionCode(pInfo.versionCode);// 版本号
			appList.add(appInfo);
		}
		return appList;
	}

	public static boolean isEmulator(Context context) {
		try {
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			String imei = tm.getDeviceId();
			if (imei != null && imei.equals("000000000000000")) {
				return true;
			}
			return (Build.MODEL.equals("sdk"))
					|| (Build.MODEL.equals("google_sdk"));
		} catch (Exception ioe) {

		}
		return false;
	}

	// @JavascriptInterface
	public String getMacAddress() {
		return wifiAdmin.getMacAddress();
	}

	// @JavascriptInterface
	public String getBSSID() {
		return wifiAdmin.getBSSID();
	}

	public String getIMEI() {
		String imei = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE))
				.getDeviceId();
		return imei;
	}

	protected void onStop() {
		if (!isAppOnForeground()) {
			mIsActive = false;// 记录当前已经进入后台
		}
		super.onStop();
	}

	private boolean isAppOnForeground() {
		ActivityManager activityManager = (ActivityManager) getApplicationContext()
				.getSystemService(Context.ACTIVITY_SERVICE);
		String packageName = getApplicationContext().getPackageName();

		List<RunningAppProcessInfo> appProcesses = activityManager
				.getRunningAppProcesses();
		if (appProcesses == null)
			return false;
		for (RunningAppProcessInfo appProcess : appProcesses) {
			// The name of the process that this object is associated with.
			if (appProcess.processName.equals(packageName)
					&& appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
				return true;
			}
		}
		return false;
	}

}
