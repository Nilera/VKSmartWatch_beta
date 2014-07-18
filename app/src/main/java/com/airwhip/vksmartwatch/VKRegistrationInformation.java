package com.airwhip.vksmartwatch;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;
import com.airwhip.vksmartwatch.misc.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VKRegistrationInformation extends RegistrationInformation {
    private Context context;
    private String extensionKey;

    protected VKRegistrationInformation(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        this.context = context;
    }

    @Override
    public ContentValues getExtensionRegistrationConfiguration() {
        String iconHostApp = ExtensionUtils.getUriString(context, R.drawable.icon);
        String extensionIcon = ExtensionUtils.getUriString(context, R.drawable.icon_extension_36x36);
        String extensionIcon48 = ExtensionUtils.getUriString(context, R.drawable.icon_extension_48x48);

        ContentValues values = new ContentValues();
        values.put(Registration.ExtensionColumns.CONFIGURATION_ACTIVITY, SettingsActivity.class.getName());
        values.put(Registration.ExtensionColumns.CONFIGURATION_TEXT, context.getString(R.string.settings));

        values.put(Registration.ExtensionColumns.NAME, context.getString(R.string.app_name));
        values.put(Registration.ExtensionColumns.EXTENSION_KEY, getExtensionKey());

        values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI, extensionIcon);
        values.put(Registration.ExtensionColumns.EXTENSION_48PX_ICON_URI, extensionIcon48);
        values.put(Registration.ExtensionColumns.HOST_APP_ICON_URI, iconHostApp);
        values.put(Registration.ExtensionColumns.NOTIFICATION_API_VERSION, getRequiredNotificationApiVersion());
        values.put(Registration.ExtensionColumns.PACKAGE_NAME, context.getPackageName());

        return values;
    }

    @Override
    public int getRequiredNotificationApiVersion() { //нам не нужно управление нотификациями
        return 1;
    }

    @Override
    public int getRequiredSensorApiVersion() {
        return API_NOT_REQUIRED;
    }

    @Override
    public int getRequiredWidgetApiVersion() {
        return API_NOT_REQUIRED;
    }

    @Override
    public int getRequiredControlApiVersion() {
        return API_NOT_REQUIRED;
    }

    @Override
    public ContentValues[] getSourceRegistrationConfigurations() {
        List<ContentValues> bulkValues = new ArrayList<ContentValues>();
        bulkValues.add(getSourceRegistrationConfiguration(Constants.EXTENSION_SPECIFIC_ID));
        return bulkValues.toArray(new ContentValues[bulkValues.size()]);
    }


    public ContentValues getSourceRegistrationConfiguration(String extensionSpecificId) {
        String iconBig = ExtensionUtils.getUriString(context, R.drawable.message_30x30);
        String iconSmall = ExtensionUtils.getUriString(context, R.drawable.message_18x18);
        String iconBlackAndWhite = ExtensionUtils.getUriString(context, R.drawable.icon_18x18_black);
        String textToSpeech = context.getString(R.string.text_to_speech);

        ContentValues sourceValues = new ContentValues();
        sourceValues.put(Notification.SourceColumns.ENABLED, true);
        sourceValues.put(Notification.SourceColumns.ICON_URI_1, iconBig);
        sourceValues.put(Notification.SourceColumns.ICON_URI_2, iconSmall);
        sourceValues.put(Notification.SourceColumns.ICON_URI_BLACK_WHITE, iconBlackAndWhite);
        sourceValues.put(Notification.SourceColumns.UPDATE_TIME, System.currentTimeMillis());
        sourceValues.put(Notification.SourceColumns.NAME, context.getString(R.string.source_name));
        sourceValues.put(Notification.SourceColumns.EXTENSION_SPECIFIC_ID, extensionSpecificId);
        sourceValues.put(Notification.SourceColumns.PACKAGE_NAME, context.getPackageName());
        sourceValues.put(Notification.SourceColumns.TEXT_TO_SPEECH, textToSpeech);

        sourceValues.put(Notification.SourceColumns.ACTION_1, context.getString(R.string.view_on_phone));
        sourceValues.put(Notification.SourceColumns.ACTION_ICON_1, ExtensionUtils.getUriString(context, R.drawable.view_on_phone));


        sourceValues.put(Notification.SourceColumns.COLOR, context.getResources().getColor(R.color.title));
        return sourceValues;
    }

    @Override
    public synchronized String getExtensionKey() {
        if (TextUtils.isEmpty(extensionKey)) {
            SharedPreferences pref = context.getSharedPreferences(Constants.EXTENSION_KEY, Context.MODE_PRIVATE);
            extensionKey = pref.getString(Constants.EXTENSION_KEY, null);
            if (TextUtils.isEmpty(extensionKey)) {
                extensionKey = UUID.randomUUID().toString();
                pref.edit().putString(Constants.EXTENSION_KEY, extensionKey).commit();
            }
        }
        return extensionKey;
    }
}