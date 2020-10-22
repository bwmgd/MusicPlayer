package com.example.musicplayer.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.example.musicplayer.domain.MusicContent;

public class MusicDBHelper extends SQLiteOpenHelper {
    private final static String DATABASE_NAME = "music.db"; //数据库名字
    private final static int DATABASE_VERSION = 1; //数据库版本
    //建表语句
    private final static String SQL_CREATE_DATABASE = "CREATE TABLE "
            + MusicContent.MusicColumns.TABLE_NAME + " (" +
            MusicContent.MusicColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            MusicContent.MusicColumns.COLUMN_NAME_NAME + " TEXT," +
            MusicContent.MusicColumns.COLUMN_NAME_PATH + " TEXT," +
            MusicContent.MusicColumns.COLUMN_NAME_URL + " TEXT" + " )";

    //删表语句
    private final static String SQL_DELETE_DATABASE = "DROP TABLE IF EXISTS " + MusicContent.MusicColumns.TABLE_NAME;

    public MusicDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DATABASE);
        Log.v("sqlite", "建表成功");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_DATABASE);
        Log.v("sqlite", "删表成功");
        onCreate(db);
    }
}
