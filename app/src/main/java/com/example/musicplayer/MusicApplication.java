package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class MusicApplication extends Application {
    public static final int DELETE_CODE = 1; //handler删除码
    public static final int SWAP_CODE = 2; //交换码
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }
}
