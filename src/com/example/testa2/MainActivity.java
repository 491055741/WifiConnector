package com.example.testa2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.example.wifiadmin.*;

public class MainActivity extends Activity {
	private static final String TAG = "[WifiAdmin]";
	private WebView webView;
	private WifiManager wifiManager;
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

	private void init() throws JSONException{

        wifiAdmin = new WifiAdmin(getBaseContext()); 
        boolean open = wifiAdmin.openWifi();
        Log.i(TAG, "wifi open:" + open);
        wifiAdmin.startScan();

        webView = new WebView(this);
        webView.loadUrl("http://app.milkpapa.com:8080/?_="+(int)(Math.random()*10000));
        WebSettings webSettings = webView.getSettings();       
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
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
	    	        webView.loadUrl("javascript:refreshWifiList()" );
	    	 }

        });
        webView.addJavascriptInterface(this, "android");
        setContentView(webView);
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

    @JavascriptInterface
    public void clickOnWifi(int idx) {
    	Log.d("tag", "selected a wifi");
    	ScanResult r = wifiAdmin.getWifiList().get(idx);
    	wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(r.SSID, "mary8888", 3));
    }
    
    @JavascriptInterface
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
//        

        for (ScanResult scanResult : wifiAdmin.getWifiList()) {
            JSONObject jsonObject = new JSONObject();  
			jsonObject.put("SSID", scanResult.SSID);
			jsonObject.put("level", scanResult.level);
            jsonArray.put(jsonObject);
        }

        JSONObject jsonObject3 = new JSONObject();
        jsonObject3.put("wifilist", jsonArray);
        Log.d("tag", jsonObject3.toString());
    	return jsonObject3.toString();
   }

    
    private void registerWIFI() {
        IntentFilter mWifiFilter = new IntentFilter();
        mWifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(mWifiConnectReceiver, mWifiFilter);
    }
    
//    public void test() {
//
////        EthernetManager mEthManager = null;
//        WifiManager mWifiManager = null;
//        connectivityManager mConnectivityManager = null;
//        WifiAdmin wifiAdmin = null;
////        mEthManager = (EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
//        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        wifiAdmin = new WifiAdmin(context); 
////        mEthManager.setEthernetEnabled(false);
//        boolean open = wifiAdmin.openWifi();
//        Log.i(TAG, "wifi open:" + open);
//        wifiAdmin.startScan();
//        connectWifi();
//    }
}
