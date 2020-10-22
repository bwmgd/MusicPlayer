package com.example.musicplayer.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import com.example.musicplayer.MusicApplication;
import com.example.musicplayer.domain.MusicContent.Music;
import com.example.musicplayer.domain.MusicContent.MusicColumns;

import java.util.ArrayList;

public class OperationDB {
    static MusicDBHelper helper;
    static OperationDB ins = new OperationDB();

    private SQLiteDatabase database;
    private String sql;
    private Cursor cursor;

    public static OperationDB getInstance() {
        return OperationDB.ins;
    }

    private OperationDB() {
        if (helper == null) {
            helper = new MusicDBHelper(MusicApplication.getContext());
        }
    }

    public void insert(Music music) {
        Log.v("sqlite", "insert");
        database = helper.getWritableDatabase();
        sql = "insert into " + MusicColumns.TABLE_NAME + " values(null,?,?,?)";
        database.execSQL(sql, new Object[]{music.getName(), music.getPath(), music.getUrl()});
        sql = "select last_insert_rowid() as MaxID from " + MusicColumns.TABLE_NAME;
        cursor = database.rawQuery(sql, null);
        if (cursor.moveToFirst()) music.setId(cursor.getInt(cursor.getColumnIndex("MaxID")));
    }

    public void delete(long id) {
        database = helper.getReadableDatabase();
        sql = "delete from " + MusicColumns.TABLE_NAME + " where _id = ?";
        database.execSQL(sql, new Object[]{id});
    }


    public ArrayList<Music> getAllMusic() {
        ArrayList<Music> list = new ArrayList<>();
        database = helper.getReadableDatabase();
        sql = "select * from " + MusicColumns.TABLE_NAME;
        cursor = database.rawQuery(sql, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Music music = new Music();
            music.setId(cursor.getInt(cursor.getColumnIndex(MusicColumns._ID)));
            music.setName(cursor.getString(cursor.getColumnIndex(MusicColumns.COLUMN_NAME_NAME)));
            music.setPath(cursor.getString(cursor.getColumnIndex(MusicColumns.COLUMN_NAME_PATH)));
            music.setUrl(Uri.parse(cursor.getString(cursor.getColumnIndex(MusicColumns.COLUMN_NAME_URL))));
            list.add(music);
        }
        return list;
    }

    public void clear() {
        helper.onUpgrade(database, 1, 1);
    }
}
