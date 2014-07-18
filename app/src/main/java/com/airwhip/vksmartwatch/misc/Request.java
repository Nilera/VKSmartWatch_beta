package com.airwhip.vksmartwatch.misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.vk.sdk.VKAccessToken;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Whiplash on 08.07.2014.
 */
public class Request {
    public static final String LONG_POLL_REQUEST = "https://api.vk.com/method/messages.getLongPollServer?access_token=";
    public static final String USER_GET_REQUEST = "https://api.vk.com/method/users.get?user_ids=%s&fields=photo&access_token=";
    public static final String CHAT_GET_REQUEST = "https://api.vk.com/method/messages.getChat?chat_id=%s&fields=photo&access_token=";
    public static final String MESSAGE_GET_REQUEST = "https://api.vk.com/method/messages.getById?message_ids=%s&access_token=";

    private Request() {
    }

    public static String get(String url) {
        String result = null;
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url);
        HttpResponse response;
        try {
            response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                result = convertStreamToString(inputStream);
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void longPollRequest(final Context context, final boolean changeTs) {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String accessToken = VKAccessToken.tokenFromSharedPreferences(context, Constants.VK_ACCESS_TOKEN).accessToken;
                    String request = get(LONG_POLL_REQUEST + accessToken);
                    Log.d(Constants.DEBUG_TAG, request);
                    JSONObject json = new JSONObject(request).getJSONObject("response");
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putString(Constants.SERVER_TOKEN, json.getString("server"));
                    edit.putString(Constants.KEY_TOKEN, json.getString("key"));
                    if (changeTs) {
                        edit.putString(Constants.TS_TOKEN, json.getString("ts"));
                    }
                    edit.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        th.start();
        try {
            th.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String userRequest(Context context, String userId) {
        String accessToken = VKAccessToken.tokenFromSharedPreferences(context, Constants.VK_ACCESS_TOKEN).accessToken;
        return get(String.format(USER_GET_REQUEST, userId) + accessToken);
    }

    public static String chatRequest(Context context, String chatId) {
        String accessToken = VKAccessToken.tokenFromSharedPreferences(context, Constants.VK_ACCESS_TOKEN).accessToken;
        return get(String.format(CHAT_GET_REQUEST, chatId) + accessToken);
    }

    public static String messageRequest(Context context, String messageId) {
        String accessToken = VKAccessToken.tokenFromSharedPreferences(context, Constants.VK_ACCESS_TOKEN).accessToken;
        return get(String.format(MESSAGE_GET_REQUEST, messageId) + accessToken);
    }

    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
