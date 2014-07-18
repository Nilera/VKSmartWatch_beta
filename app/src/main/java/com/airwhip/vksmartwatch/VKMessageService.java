package com.airwhip.vksmartwatch;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.airwhip.vksmartwatch.misc.Constants;
import com.airwhip.vksmartwatch.misc.LongPolling;
import com.airwhip.vksmartwatch.misc.Message;
import com.airwhip.vksmartwatch.misc.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VKMessageService extends Service {

    public static final String LONG_POLL_URL = "http://%s?act=a_check&key=%s&ts=%s&wait=25&mode=2";

    public static LongPolling lp;
    private SharedPreferences prefs;

    public LongPolling.OnMessageListener handler = new LongPolling.OnMessageListener() {
        @Override
        public void onMessage(JSONObject response) {
            lp.disconnect();
            if (response.toString().contains("failed")) {
                Request.longPollRequest(getApplicationContext(), true);
            } else {
                try {
                    Log.d(Constants.DEBUG_TAG, response.toString());
                    prefs.edit().putString(Constants.TS_TOKEN, response.getString("ts")).commit();
                    JSONArray updates = response.getJSONArray("updates");
                    for (int i = 0; i < updates.length(); i++) {
                        // if it message
                        if (updates.getString(i).startsWith("[4,")) {
                            new Message(updates.getString(i), getApplicationContext());
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            longPollConnect();
        }
    };

    @Override
    public void onCreate() {
        Log.d(Constants.DEBUG_TAG, "START SERVICE");
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        longPollConnect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void longPollConnect() {
        String url = String.format(LONG_POLL_URL, prefs.getString(Constants.SERVER_TOKEN, ""),
                prefs.getString(Constants.KEY_TOKEN, ""),
                prefs.getString(Constants.TS_TOKEN, ""));
        lp = new LongPolling(getApplicationContext(), url, "", handler);
        lp.connect();
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.DEBUG_TAG, "STOP SERVICE");
        lp.disconnect();
        super.onDestroy();
    }
}
