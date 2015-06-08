package com.example.httpconnection;
 
import java.util.ArrayList;
import java.util.List;
 
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
 
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
 
/**
 * Asynchronous HTTP connections
 * 
 * @author Greg Zavitz & Joseph Roth
 */
public class HttpConnection implements Runnable {
 
    public static final int DID_START = 0;
    public static final int DID_ERROR = 1;
    public static final int DID_SUCCEED = 2;
 
    private static final int GET = 0;
    private static final int POST = 1;
    private static final int PUT = 2;
    private static final int DELETE = 3;
    private static final int BITMAP = 4;
 
    private String url;
    private int method;
    private String data;
    private CallbackListener listener;
 
    private HttpClient httpClient;
 
     public HttpConnection() {
//    	 this(new Handler());
     }
 
    public void create(int method, String url, String data, CallbackListener listener) {
        this.method = method;
        this.url = url;
        this.data = data;
        this.listener = listener;
        ConnectionManager.getInstance().push(this);
    }
 
    public void get(String url, CallbackListener listener) {
        create(GET, url, null, listener);
    }
 
    public void post(String url, String data, CallbackListener listener) {
        create(POST, url, data, listener);
    }
 
    public void put(String url, String data) {
        create(PUT, url, data, listener);
    }
 
    public void delete(String url) {
        create(DELETE, url, null, listener);
    }
 
    public void bitmap(String url) {
        create(BITMAP, url, null, listener);
    }
 
    public interface CallbackListener {
        public void callBack(String result);
    }
 
    private static final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case HttpConnection.DID_START: {
                    break;
                }
                case HttpConnection.DID_SUCCEED: {
                    CallbackListener listener = (CallbackListener) message.obj;
                    Object data = message.getData();
                    if (listener != null) {
                        if(data != null) {
                            Bundle bundle = (Bundle)data;
                            String result = bundle.getString("callbackkey");
                            listener.callBack(result);
                        }
                    }
                    break;
                }
                case HttpConnection.DID_ERROR: {
                    break;
                }
            }
        }
    };
 
    public void run() {
        handler.sendMessage(Message.obtain(handler, HttpConnection.DID_START));
        httpClient = getHttpClient();
        try {
            HttpResponse httpResponse = null;
            switch (method) {
            case GET:
                httpResponse = httpClient.execute(new HttpGet(url));
                break;
            case POST:
                HttpPost httpPost = new HttpPost(url);
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                BasicNameValuePair valuesPair = new BasicNameValuePair("args",
                        data);
                params.add(valuesPair);
                httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
                httpResponse = httpClient.execute(httpPost);
                if (isHttpSuccessExecuted(httpResponse)) {
                    String result = EntityUtils.toString(httpResponse
                            .getEntity());
                    this.sendMessage(result);
                } else {
                    this.sendMessage("fail");
                }
                break;
            }
        } catch (Exception e) {
            this.sendMessage("fail");
        }
        ConnectionManager.getInstance().didComplete(this);
    }
 
    // private void processBitmapEntity(HttpEntity entity) throws IOException {
    // BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
    // Bitmap bm = BitmapFactory.decodeStream(bufHttpEntity.getContent());
    // handler.sendMessage(Message.obtain(handler, DID_SUCCEED, bm));
    // }
 
    private void sendMessage(String result) {
        Message message = Message.obtain(handler, DID_SUCCEED,
                listener);
        Bundle data = new Bundle();
        data.putString("callbackkey", result);
        message.setData(data);
        handler.sendMessage(message);
    }
 
    public static DefaultHttpClient getHttpClient() {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 20000);
        HttpConnectionParams.setSoTimeout(httpParams, 20000);
        // HttpConnectionParams.setSocketBufferSize(httpParams, 8192);
 
        DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
        return httpClient;
    }
 
    public static boolean isHttpSuccessExecuted(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        return (statusCode > 199) && (statusCode < 400);
    }
 
}