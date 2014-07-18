package com.airwhip.vksmartwatch.misc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final String SOURCE_ID = "source_id";
    public static final String USER_ID = "user_id";
    public static final String TABLE_NAME = "source_to_user_id";
    private static final String CREATE_TABLE = "CREATE TABLE "
            + TABLE_NAME + " (" + SOURCE_ID + " VARCHAR(255) PRIMARY KEY,"
            + USER_ID + " VARCHAR(255));";
    private final static String DATABASE_NAME = "sourcetouserid.db";
    private final static int VERSION = 1;
    private SQLiteDatabase sqLiteDatabase;

    public DBHelper(Context context) {
        super(context, TABLE_NAME, null, VERSION);
        sqLiteDatabase = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    public void clear() {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    public void updateValue(String sourceId, String userId) {
        ContentValues cv = new ContentValues();
        cv.put(USER_ID, userId);

        Cursor cursor = sqLiteDatabase.query(TABLE_NAME, new String[]{SOURCE_ID, USER_ID, USER_ID}, SOURCE_ID + "=\'" + sourceId + "\'", null, null, null, null);
        if (cursor.moveToFirst()) {
            sqLiteDatabase.update(TABLE_NAME, cv, SOURCE_ID + "=\'" + sourceId + "\'", null);
        } else {
            cv.put(SOURCE_ID, sourceId);
            sqLiteDatabase.insert(TABLE_NAME, null, cv);
        }
        cursor.close();
    }

    public String getValue(String sourceId) {
        try {
            Cursor cursor = sqLiteDatabase.query(TABLE_NAME, new String[]{SOURCE_ID, USER_ID}, SOURCE_ID + "=\'" + sourceId + "\'", null, null, null, null);
            cursor.moveToNext();
            String value = cursor.getString(cursor.getColumnIndex(USER_ID));
            cursor.close();
            return value;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
