package com.example.musicplayer;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer.domain.MusicContent;
import com.example.musicplayer.ui.MusicFragment;
import com.example.musicplayer.ui.MusicItemTouchCallback;
import com.example.musicplayer.ui.MusicRecyclerViewAdapter;
import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {
    private static final int FILE_REQUEST_CODE = 1; //打开文件管理器的code
    private MusicFragment musicFragment;
    private RecyclerView recyclerView;
    private MusicService musicService;
    ImageButton playButton;
    MusicItemTouchCallback musicItemTouchCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        requestPermissions();
        recyclerViewInit();
        serviceInit();
        buttonInit();
    }

    private void requestPermissions() { //请求文件访问,媒体访问权限
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_MEDIA_LOCATION}, 1);
        }
    }

    private void recyclerViewInit() {
        musicFragment = (MusicFragment) getSupportFragmentManager().findFragmentById(R.id.listFragment);
        assert musicFragment != null;
        recyclerView = (RecyclerView) musicFragment.getView();
        assert recyclerView != null;
        recyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull @NotNull View view) { //添加item点击事件
                view.setOnClickListener(v -> {
                    int n = recyclerView.getChildAdapterPosition(view);
                    musicService.play(n);
                });
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull @NotNull View view) {
            }
        });
        musicItemTouchCallback = new MusicItemTouchCallback((MusicRecyclerViewAdapter) recyclerView.getAdapter());
        ItemTouchHelper helper = new ItemTouchHelper(musicItemTouchCallback); //添加item拖动事件,实现修改位置,拖动删除
        helper.attachToRecyclerView(recyclerView);
    }

    private void serviceInit() {
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) { //连接服务
                Log.v("service", "connection");
                MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
                musicService = binder.getService();
                musicService.setCurrentDuration(findViewById(R.id.current_duration));
                musicService.setDuration(findViewById(R.id.duration));
                musicService.setSeekBar(findViewById(R.id.seekBar));
                musicService.setRecyclerView(recyclerView);
                musicService.setOnCompletionListener(false, false, false);
                musicService.setPlayButton(playButton);
                //为callback添加messenger,利用handler传递经过拖动或删除事件后的position
                musicItemTouchCallback.setMessenger(new Messenger(musicService.getHandler()));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.v("disCon", "d");
            }
        };
        Intent i = new Intent(this, MusicService.class); //打开服务
        bindService(i, conn, BIND_AUTO_CREATE);
    }

    private void buttonInit() { //按钮事件绑定
        (playButton = findViewById(R.id.play)).setOnClickListener(v -> musicService.playOrPause());
        findViewById(R.id.previous).setOnClickListener(v -> musicService.previous());
        findViewById(R.id.next).setOnClickListener(v -> musicService.next());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting, menu);
        return super.onCreateOptionsMenu(menu);
    }

    boolean single, loop, random;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        item.setChecked(!item.isChecked()); //item的选择框的转换
        switch (item.getItemId()) {
            case R.id.clear:
                musicService.clearItem();
                musicFragment.clearItem();
                break;
            case R.id.add:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT); //打开文件资源管理器
                intent.setType("audio/*"); //选择音频文件
                intent.addCategory(Intent.CATEGORY_OPENABLE); //隐式启动
                startActivityForResult(intent, FILE_REQUEST_CODE);
                break;
            case R.id.single:
                single = item.isChecked();
                break;
            case R.id.loop:
                loop = item.isChecked();
                break;
            case R.id.random:
                random = item.isChecked();
                break;
        }
        musicService.setOnCompletionListener(single, loop, random); //设置service的播放模式
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) { //获取选择的文件
            assert data != null;
            Uri uri = data.getData(); //获取文件uri
            Log.v("uri", uri.toString());
            musicFragment.addItem(MusicContent.fromUriToMusic(MainActivity.this, uri)); //uri转music对象添加进fragment
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}