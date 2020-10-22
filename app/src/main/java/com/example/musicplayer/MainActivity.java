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
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer.domain.MusicContent;
import com.example.musicplayer.ui.MusicFragment;
import com.example.musicplayer.ui.MusicRecyclerViewAdapter;
import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {
    private static final int FILE_REQUEST_CODE = 1;
    private MusicFragment musicFragment;
    private RecyclerView recyclerView;
    private MusicService musicService;
    ImageButton playButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        requestPermissions();
        recyclerViewInit();
        buttonInit();
        serviceInit();
    }

    private void requestPermissions() {
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
            public void onChildViewAttachedToWindow(@NonNull @NotNull View view) {
                final MusicRecyclerViewAdapter.MusicViewHolder viewHolder = (MusicRecyclerViewAdapter.MusicViewHolder) recyclerView.getChildViewHolder(view);
                view.setOnClickListener(v -> {
                    int n = recyclerView.getChildAdapterPosition(view);
                    Log.v("item", viewHolder.mItem.toString());
                    Log.v("position", String.valueOf(n));
                    musicService.play(n);
                });
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull @NotNull View view) {
            }
        });
    }

    private void serviceInit() {
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.v("service", "connection");
                MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
                musicService = binder.getService();
                musicService.setCurrentDuration(findViewById(R.id.current_duration));
                musicService.setDuration(findViewById(R.id.duration));
                musicService.setSeekBar(findViewById(R.id.seekBar));
                musicService.setRecyclerView(recyclerView);
                musicService.setOnCompletionListener(false, false, false);
                musicService.setPlayButton(playButton);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        Intent i = new Intent(this, MusicService.class);
        bindService(i, conn, BIND_AUTO_CREATE);
    }

    private void buttonInit() {
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
        item.setChecked(!item.isChecked());
        switch (item.getItemId()) {
            case R.id.clear:
                musicService.clearItem();
                musicFragment.clearItem();
                break;
            case R.id.add:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
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
        musicService.setOnCompletionListener(single, loop, random);
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            assert data != null;
            Uri uri = data.getData();
            Log.v("uri", uri.toString());
            musicFragment.addItem(MusicContent.fromUriToMusic(MainActivity.this, uri));
        }
    }
}