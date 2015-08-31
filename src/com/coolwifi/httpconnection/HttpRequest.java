package com.coolwifi.httpconnection;

import java.io.InputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class HttpRequest {

    static String TAG = "HttpRequest";
    public static void post(String urlStr, String data) {
        try
        {
            Log.d(TAG, "post");
//            HttpEntity requestHttpEntity = new UrlEncodedFormEntity(
//                    pairList);
            StringEntity entity = new StringEntity(data);
            entity.setContentType("application/x-www-form-urlencoded");
            
            // URL使用基本URL即可，其中不需要加参数
            HttpPost httpPost = new HttpPost(urlStr);
            // 将请求体内容加入请求中
            httpPost.setEntity(entity);
            // 需要客户端对象来发送请求
            HttpClient httpClient = new DefaultHttpClient();
            // 发送请求
            HttpResponse response = httpClient.execute(httpPost);
            // 显示响应
            String resp = response.getEntity().getContent().toString();
            Log.d(TAG, resp);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }   
}
