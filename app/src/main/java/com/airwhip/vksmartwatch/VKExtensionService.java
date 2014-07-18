package com.airwhip.vksmartwatch;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;
import com.airwhip.vksmartwatch.misc.Constants;
import com.airwhip.vksmartwatch.misc.DBHelper;

public class VKExtensionService extends ExtensionService {
    public static final String VK_CHAT_URL = "https://vk.com/im?sel=%s";

    public VKExtensionService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onViewEvent(Intent intent) {
        String action = intent.getStringExtra(Notification.Intents.EXTRA_ACTION);
        int eventId = intent.getIntExtra(Notification.Intents.EXTRA_EVENT_ID, -1);
        if (Notification.SourceColumns.ACTION_1.equals(action)) {
            doAction1(eventId);
        }
    }

    private void doAction1(int eventId) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(Notification.Event.URI, null, Notification.EventColumns._ID + " = " + eventId, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                long sourceId = cursor.getLong(cursor.getColumnIndex(Notification.EventColumns.SOURCE_ID));
                DBHelper dbHelper = new DBHelper(getApplicationContext());
                Intent openChat = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(VK_CHAT_URL, dbHelper.getValue(String.valueOf(sourceId)))));
                openChat.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(openChat);
                dbHelper.close();
            }
        } catch (Exception e) {
            Log.e(Constants.ERROR_TAG, "Failed to query event", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public ControlExtension createControlExtension(String hostAppPackageName) {
        return null;
    }

    @Override
    protected void onRefreshRequest() {
    }

    @Override
    protected RegistrationInformation getRegistrationInformation() {
        return new VKRegistrationInformation(this);
    }

    @Override
    protected boolean keepRunningWhenConnected() {
        return false;
    }
}