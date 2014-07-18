package com.airwhip.vksmartwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.vk.sdk.VKAccessToken;
import com.airwhip.vksmartwatch.misc.Constants;
import com.airwhip.vksmartwatch.misc.Request;

/**
 * Created by Whiplash on 16.07.2014.
 */
public class ChangeStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if (netInfo != null && netInfo.isConnectedOrConnecting() && VKAccessToken.tokenFromSharedPreferences(context, Constants.VK_ACCESS_TOKEN) != null) {
                    Request.longPollRequest(context, false);
                    context.startService(new Intent(context, VKMessageService.class));
                } else {
                    context.stopService(new Intent(context, VKMessageService.class));
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
}
