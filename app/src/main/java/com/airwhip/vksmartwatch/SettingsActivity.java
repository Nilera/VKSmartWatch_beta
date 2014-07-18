package com.airwhip.vksmartwatch;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCaptchaDialog;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKSdkListener;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKError;
import com.airwhip.vksmartwatch.misc.Constants;
import com.airwhip.vksmartwatch.misc.DBHelper;
import com.airwhip.vksmartwatch.misc.Request;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

public class SettingsActivity extends PreferenceActivity {

    private CheckBoxPreference loggedIn;
    private CheckBoxPreference downloadImages;
    private Preference clear;
    private Preference feedback;

    private final VKSdkListener sdkListener = new VKSdkListener() {
        @Override
        public void onCaptchaError(VKError captchaError) {
            new VKCaptchaDialog(captchaError).show();
        }

        @Override
        public void onTokenExpired(VKAccessToken expiredToken) {
        }

        @Override
        public void onAccessDenied(VKError authorizationError) {
            loggedIn.setChecked(VKSdk.isLoggedIn());
            downloadImages.setEnabled(VKSdk.isLoggedIn());
        }

        @Override
        public void onReceiveNewToken(final VKAccessToken newToken) {
            newToken.saveTokenToSharedPreferences(getApplicationContext(), Constants.VK_ACCESS_TOKEN);
            loggedIn.setChecked(VKSdk.isLoggedIn());
            downloadImages.setEnabled(VKSdk.isLoggedIn());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Request.longPollRequest(getApplicationContext(), true);
                    startService(new Intent(getApplicationContext(), VKMessageService.class));
                }
            }).start();
        }

        @Override
        public void onAcceptUserToken(VKAccessToken token) {
        }
    };


    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VKUIHelper.onCreate(this);
        VKSdk.initialize(sdkListener, Constants.VK_APP_ID, VKAccessToken.tokenFromSharedPreferences(getApplicationContext(), Constants.VK_ACCESS_TOKEN));

        addPreferencesFromResource(R.xml.preferences);
        loggedIn = (CheckBoxPreference) findPreference(getText(R.string.preference_logged_in_key));
        downloadImages = (CheckBoxPreference) findPreference(getText(R.string.preference_download_images_key));
        clear = findPreference(getString(R.string.preference_clear_history_key));
        feedback = findPreference(getString(R.string.preference_feedback_key));

        loggedIn.setChecked(VKSdk.isLoggedIn());
        loggedIn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!VKSdk.isLoggedIn()) {
                    VKSdk.authorize(VKScope.MESSAGES);
                } else {
                    VKSdk.logout();
                    stopService(new Intent(getApplicationContext(), VKMessageService.class));
                    loggedIn.setChecked(false);
                    downloadImages.setEnabled(false);

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.remove(Constants.VK_ACCESS_TOKEN);
                    edit.commit();
                }
                return true;
            }
        });

        downloadImages.setEnabled(loggedIn.isChecked());
        downloadImages.setChecked(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.DOWNLOAD_IMAGES, true));
        downloadImages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(Constants.DOWNLOAD_IMAGES, downloadImages.isChecked());
                edit.commit();
                return true;
            }
        });


        clear.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDialog(1);
                return true;
            }
        });

        feedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "whiplash608@gmail.com", null)));
                return true;
            }
        });

        if (!ExtensionUtils.supportsHistory(getIntent())) {
            clear = findPreference(getString(R.string.preference_clear_history_key));
            getPreferenceScreen().removePreference(clear);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.preference_clear_history_dialog_text)
                .setTitle(R.string.preference_clear_history_text)
                .setIcon(android.R.drawable.ic_input_delete)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        new ClearEventsTask().execute();
                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        return builder.create();
    }

    private class ClearEventsTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {

            return NotificationUtil.deleteAllEvents(getApplicationContext());
        }

        @Override
        protected void onPostExecute(Integer id) {
            if (id != NotificationUtil.INVALID_ID) {
                DBHelper dbHelper = new DBHelper(getApplicationContext());
                dbHelper.clear();
                dbHelper.close();
                makeText(getApplicationContext(), R.string.preference_clear_history_dialog_text_success, LENGTH_SHORT).show();
            } else {
                makeText(getApplicationContext(), R.string.preference_clear_history_dialog_text_failure, LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        VKUIHelper.onResume(this);
    }

    @Override
    protected void onDestroy() {
        VKUIHelper.onDestroy(this);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        VKUIHelper.onActivityResult(SettingsActivity.this, requestCode, resultCode, data);
    }
}