package com.inc.tracks.yummobile.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FeedProgressDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "YumLocalData.db";

    public FeedProgressDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static final String SQL_CREATE_CARD_ENTRIES = "CREATE TABLE IF NOT EXISTS " + ProgressDbContract.FeedSavedCardEntry.CARD_TABLE + "(" +
            ProgressDbContract.FeedSavedCardEntry._ID + " INTEGER PRIMARY KEY, " +
            ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN + " TEXT, " +
            ProgressDbContract.FeedSavedCardEntry.CVV_COLUMN + " TEXT, " +
            ProgressDbContract.FeedSavedCardEntry.NAME_COLUMN + " TEXT, " +
            ProgressDbContract.FeedSavedCardEntry.EXP_MONTH_COLUMN + " INTEGER, " +
            ProgressDbContract.FeedSavedCardEntry.EXPIRY_YEAR_COLUMN + " INTEGER)";


    private static final String SQL_DELETE_CARD_ENTRIES = "DROP TABLE IF EXISTS " + ProgressDbContract.FeedSavedCardEntry.CARD_TABLE;


    public void onCreate(SQLiteDatabase db){
        db.execSQL(SQL_CREATE_CARD_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        deleteDb(db);
        onCreate(db);
    }

    void deleteDb(SQLiteDatabase db){
        db.execSQL(SQL_DELETE_CARD_ENTRIES);
    }
}
