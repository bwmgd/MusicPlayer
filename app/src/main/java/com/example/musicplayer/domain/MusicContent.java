package com.example.musicplayer.domain;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MusicContent {
    public static class Music {
        private long id; //音乐id
        private String name; //音乐名称
        private String path; //音乐绝对路径
        private Uri url; //音乐uri标识

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Uri getUrl() {
            return url;
        }

        public void setUrl(Uri url) {
            this.url = url;
        }
    }

    public static abstract class MusicColumns implements BaseColumns {

        public static final String TABLE_NAME = "musics";//表名

        public static final String COLUMN_NAME_NAME = "name";//字段：音乐名
        public static final String COLUMN_NAME_PATH = "path";//字段：路径
        public static final String COLUMN_NAME_URL = "url";//字段：路径
    }


    /**
     * 通过uri获取文件名及路径
     *
     * @param context content
     * @param uri     文件资源管理器中选取的uri
     * @return Music
     */
    public static Music fromUriToMusic(Context context, Uri uri) {
        Music music = new Music();
        String path = getFileAbsolutePath(context, uri);
        String[] t = path.split("/");
        String name = t[t.length - 1].substring(4);
        music.setUrl(uri);
        music.setPath(path);
        music.setName(name);
        Log.v("music", music.toString());
        return music;
    }

    /**
     * 根据Uri获取文件绝对路径，解决Android4.4以上版本Uri转换 兼容Android 10
     *
     * @param context  content
     * @param audioUri 音频uri
     */
    private static String getFileAbsolutePath(Context context, Uri audioUri) {
        if (context == null || audioUri == null) return null;

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && DocumentsContract.isDocumentUri(context, audioUri)) {
            if (isExternalStorageDocument(audioUri)) {
                String docId = DocumentsContract.getDocumentId(audioUri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageState() + "/" + split[1];
                }
            }
            else if (isDownloadsDocument(audioUri)) {
                String id = DocumentsContract.getDocumentId(audioUri);
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                return getDataColumn(context, contentUri, null, null);
            }
            else if (isMediaDocument(audioUri)) {
                String docId = DocumentsContract.getDocumentId(audioUri);
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }
                else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                }
                else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } // MediaStore (and general)
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            return uriToFileApiQ(context, audioUri);
        else if ("content".equalsIgnoreCase(audioUri.getScheme()))
            return getDataColumn(context, audioUri, null, null);// Return the remote address
            // File
        else if ("file".equalsIgnoreCase(audioUri.getScheme()))
            return audioUri.getPath();
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = MediaStore.Audio.Media.DATA;
        String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static String uriToFileApiQ(Context context, Uri uri) {
        File file = null;
        //android10以上转换
        if (uri.getScheme().equals(ContentResolver.SCHEME_FILE)) {
            file = new File(uri.getPath());
        }
        else if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //把文件复制到沙盒目录
            ContentResolver contentResolver = context.getContentResolver();
            @SuppressLint("Recycle") Cursor cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                try {
                    InputStream is = contentResolver.openInputStream(uri);
                    File cache = new File(context.getExternalCacheDir().getAbsolutePath(), Math.round((Math.random() + 1) * 1000) + displayName);
                    FileOutputStream fos = new FileOutputStream(cache);
                    FileUtils.copy(is, fos);
                    file = cache;
                    fos.close();
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        assert file != null;
        return file.getAbsolutePath();
    }
}
