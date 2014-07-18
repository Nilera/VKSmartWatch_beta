package com.airwhip.vksmartwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class VKBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        intent.setClass(context, VKExtensionService.class);
        context.startService(intent);
    }
}