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

    /**
     * 插入音乐
     *
     * @param music 音乐
     */

    public void insert(Music music) {
        Log.v("sqlite", "insert");
        database = helper.getWritableDatabase();
        sql = "insert into " + MusicColumns.TABLE_NAME + " values(null,?,?,?)";
        database.execSQL(sql, new Object[]{music.getName(), music.getPath(), music.getUrl()});
        sql = "select last_insert_rowid() as MaxID from " + MusicColumns.TABLE_NAME; //获取新插入音乐的id
        cursor = database.rawQuery(sql, null);
        if (cursor.moveToFirst()) music.setId(cursor.getInt(cursor.getColumnIndex("MaxID")));
    }

    /**
     * 删除音乐
     *
     * @param id 音乐id
     */
    public void delete(long id) {
        Log.v("sqlite", "delete");
        database = helper.getReadableDatabase();
        sql = "delete from " + MusicColumns.TABLE_NAME + " where _id = ?";
        database.execSQL(sql, new Object[]{id});
    }

    /**
     * @return 返回全部音乐
     */
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

    /**
     * 清空数据库
     */
    public void clear() {
        Log.v("sqlite", "clear");
        helper.onUpgrade(database, 1, 1);//重新建表,重新开始自增长id
    }

    /**
     * 交换两行数据,用于适配器再次载入时保留修改
     *
     * @param a 交换音乐的id a
     * @param b 交换音乐的id b
     */
    public void swap(long a, long b) {
        Log.v("sqlite", "swap");
        database = helper.getWritableDatabase();
        //新建表,复制两行数据
        sql = "CREATE TABLE swap AS SELECT * FROM " + MusicColumns._ID + " WHERE _id = ? OR _id = ?";
        database.execSQL(sql, new Object[]{a, b});
        //分别更新两行数据
        sql = "UPDATE " + MusicColumns._ID + " SET name =(SELECT name FROM swap WHERE swap._id = ?)," +
                " path=(SELECT path FROM swap WHERE swap._id = ?)," +
                " url=(SELECT url FROM swap WHERE swap._id = ?) WHERE " + MusicColumns._ID + "._id = ?";
        database.execSQL(sql, new Object[]{b, b, b, a});
        sql = "UPDATE " + MusicColumns._ID + " SET name= (SELECT name FROM swap WHERE swap._id = ?)," +
                " path =(SELECT path FROM swap WHERE swap._id = ?)," +
                " url =(SELECT url FROM swap WHERE swap._id = ?) WHERE " + MusicColumns._ID + "._id = ?";
        database.execSQL(sql, new Object[]{a, a, a, b});
        //删除交换表
        sql = "DROP TABLE swap";
        database.execSQL(sql);
    }
}
