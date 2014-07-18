package com.airwhip.vksmartwatch.misc;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;
import com.airwhip.vksmartwatch.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Whiplash on 09.07.2014.
 */
public class Message {

    private static final String MESSAGE_REGEX = "^\\[(\\d+?),(\\d+?),(\\d+?),(\\d+?),(\\d+?),\"(.*?)\",\"(.*?)\",\\{([^\\{\\}]*?)\\}\\]$";
    private static final String USELESS_TITLE = " ... ";
    private static final String ATTACH_TYPE = "attach%d_type";
    private static final String ATTACH = "attach%d";
    private static final String STICKER_URL = "https://vk.com/images/stickers/%s/128.png";
    private static final int ATTACH_TYPE_SIZE = 7;
    private static final int[] INT_TO_ARRAY = {R.array.photo, R.array.video, R.array.audio, R.array.doc, R.array.wall, R.array.map};

    private long messageId;
    private int flags;
    private long id;
    private long time;
    private String title;
    private String text;

    private boolean isRead;

    private String name;
    private String photoUri;
    private int[] attachmentsCounter;
    private String stickerNumber = "";
    private String fromId;

    public Message(String str, final Context context) throws JSONException {
        Pattern pattern = Pattern.compile(MESSAGE_REGEX);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            messageId = Long.parseLong(matcher.group(2));
            flags = Integer.parseInt(matcher.group(3));
            id = Long.parseLong(matcher.group(4));
            time = System.currentTimeMillis();
            title = matcher.group(6);
            text = matcher.group(7);
            attachmentsCounter = new int[ATTACH_TYPE_SIZE];
            JSONObject attachmentsJSON = new JSONObject("{" + matcher.group(8) + "}");
            try {
                for (int i = 1; i <= attachmentsJSON.length() / 2; i++) {
                    countAttachments(attachmentsJSON.getString(String.format(ATTACH_TYPE, i)));
                    if (attachmentsCounter[ATTACH_TYPE_SIZE - 1] == 1) {
                        stickerNumber = attachmentsJSON.getString(String.format(ATTACH, i));
                        attachmentsCounter[ATTACH_TYPE_SIZE - 1]++;
                    }
                }
            } catch (Exception e) {
                // no attachments (repost, photo, video, audio, sticker)
            }
            try {
                attachmentsJSON.getString("geo");
                attachmentsCounter[5] = 1;
            } catch (Exception e) {
                // no map in attachments
            }

            try {
                attachmentsJSON.getString("fwd");
                attachmentsCounter[4]++;
            } catch (Exception e) {
                // no message repost in attachments
            }

            try {
                fromId = attachmentsJSON.getString("from");
            } catch (Exception e) {
                // no message from chat
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!isChat()) {
                            JSONObject info = new JSONObject(Request.userRequest(context, String.valueOf(id))).getJSONArray("response").getJSONObject(0);
                            name = info.getString("first_name") + " " + info.getString("last_name");
                            URL url = new URL(info.getString("photo"));
                            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), new File(url.getFile()).getName());
                            if (!file.exists() && PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.DOWNLOAD_IMAGES, true)) {
                                downloadImage(url, file);
                            }
                            if (file.exists()) {
                                photoUri = Uri.fromFile(file).toString();
                            }
                        } else {
                            JSONObject info = new JSONObject(Request.userRequest(context, fromId)).getJSONArray("response").getJSONObject(0);
                            name = info.getString("first_name") + " " + info.getString("last_name");
                            info = new JSONObject(Request.chatRequest(context, String.valueOf(id % 2000000000))).getJSONObject("response");
                            try {
                                URL url = new URL(info.getString("photo_50"));
                                File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), new File(url.getFile()).getName());
                                if (!file.exists() && PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.DOWNLOAD_IMAGES, true)) {
                                    downloadImage(url, file);
                                }
                                if (file.exists()) {
                                    photoUri = Uri.fromFile(file).toString();
                                }
                            } catch (Exception e) {
                                // no chat photo
                            }
                        }
                        try {
                            JSONObject json = new JSONObject(Request.messageRequest(context, String.valueOf(messageId)));
                            Log.d(Constants.DEBUG_TAG, json.toString());
                            isRead = json.getJSONArray("response").getJSONObject(1).getInt("read_state") == 1;
                        } catch (Exception e) {
                            Log.e(Constants.ERROR_TAG, "MESSAGE REQUEST FAILED");
                        }
                        publish(context);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            Log.e(Constants.DEBUG_TAG, "BAD MESSAGE");
        }
    }

    private static void downloadImage(URL url, File file) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();
    }

    private void publish(Context context) throws IOException {
        long sourceId = NotificationUtil.getSourceId(context, Constants.EXTENSION_SPECIFIC_ID);
        if (flags % 4 < 2 && !isRead && sourceId != NotificationUtil.INVALID_ID) {
            ContentValues eventValues = new ContentValues();
            eventValues.put(Notification.EventColumns.EVENT_READ_STATUS, false);
            eventValues.put(Notification.EventColumns.DISPLAY_NAME, name);
            eventValues.put(Notification.EventColumns.PERSONAL, 1);
            eventValues.put(Notification.EventColumns.PUBLISHED_TIME, time);
            eventValues.put(Notification.EventColumns.SOURCE_ID, sourceId);

            String message;
            if (!isChat()) {
                message = title.equals(USELESS_TITLE) ? "" : (title + "\n");
            } else {
                message = context.getString(R.string.chat) + " (" + title + ")\n";
            }
            message += text.isEmpty() ? "" : (text + "\n");
            if (hasAttachments()) {
                message += context.getString(R.string.attachment);
                for (int i = 0; i < attachmentsCounter.length - 1; i++) {
                    message += generateAttachmentText(i, context);
                }
            }
            // if has sticker
            if (attachmentsCounter[ATTACH_TYPE_SIZE - 1] > 1) {
                URL url = new URL(String.format(STICKER_URL, stickerNumber));
                File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), stickerNumber + "_" + new File(url.getFile()).getName());
                if (!file.exists() && PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.DOWNLOAD_IMAGES, true)) {
                    downloadImage(url, file);
                }
                if (file.exists()) {
                    eventValues.put(Notification.EventColumns.IMAGE_URI, Uri.fromFile(file).toString());
                } else {
                    message = context.getString(R.string.sticker);
                }
            }

            if (photoUri == null || photoUri.isEmpty()) {
                eventValues.put(Notification.EventColumns.PROFILE_IMAGE_URI, ExtensionUtils.getUriString(context, R.drawable.default_avatar));
            } else {
                eventValues.put(Notification.EventColumns.PROFILE_IMAGE_URI, photoUri);
            }
            message = message.replace("<br>", "\n");
            eventValues.put(Notification.EventColumns.MESSAGE, message);

            DBHelper dbHelper = new DBHelper(context);
            dbHelper.updateValue(String.valueOf(sourceId), String.valueOf(id));
            dbHelper.close();

            NotificationUtil.addEvent(context, eventValues);
        }
    }

    private boolean isChat() {
        return fromId != null && !fromId.isEmpty();
    }

    private String generateAttachmentText(int i, Context context) {
        if (attachmentsCounter[i] > 0) {
            return "\n - " + attachmentsCounter[i] + " " + context.getResources().getStringArray(INT_TO_ARRAY[i])[getNeededFormOfNoun(attachmentsCounter[i])];
        } else {
            return "";
        }
    }

    private int getNeededFormOfNoun(int i) {
        if (i % 100 == 1) {
            return 0;
        } else if (i % 100 < 5) {
            return 1;
        } else {
            return 2;
        }
    }

    private void countAttachments(String type) {
        if (type.equals("photo")) {
            attachmentsCounter[0]++;
        } else if (type.equals("video")) {
            attachmentsCounter[1]++;
        } else if (type.equals("audio")) {
            attachmentsCounter[2]++;
        } else if (type.equals("doc")) {
            attachmentsCounter[3]++;
        } else if (type.equals("wall")) {
            attachmentsCounter[4]++;
        } else if (type.equals("sticker")) {
            attachmentsCounter[ATTACH_TYPE_SIZE - 1]++;
        }
    }

    private boolean hasAttachments() {
        int attachmentsCount = 0;
        for (int count : attachmentsCounter) {
            attachmentsCount += count;
        }
        return attachmentsCount - attachmentsCounter[ATTACH_TYPE_SIZE - 1] > 0;
    }

}
