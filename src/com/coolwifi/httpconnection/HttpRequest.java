package com.coolwifi.httpconnection;

import java.io.InputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class HttpRequest {

    static String TAG = "HttpRequest";
    public static String post(String urlStr, String data) {
        try
        {
            Log.d(TAG, "post");
            StringEntity entity = new StringEntity(data);
            entity.setContentType("application/x-www-form-urlencoded");
            // URL使用基本URL即可，其中不需要加参数
            HttpPost httpPost = new HttpPost(urlStr);
            httpPost.setEntity(entity);
            HttpResponse response = new DefaultHttpClient().execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) { 
//                String resp = response.getEntity().getContent().toString();
            	String resp = EntityUtils.toString(response.getEntity());
                Log.d(TAG, resp);
                return resp;
            } else {
                Log.d(TAG, "Error Response"+response.getStatusLine().toString());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }   
}
