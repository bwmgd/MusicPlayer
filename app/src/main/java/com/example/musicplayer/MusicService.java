package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.*;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer.ui.MusicRecyclerViewAdapter;

import java.util.concurrent.ThreadLocalRandom;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer; //音乐播放器
    private SeekBar seekBar; //进度条
    private TextView currentDuration; //当前持续时间文本框
    private TextView duration; //总时长文本框
    private TextView playingView; //正在播放音乐的文本框
    private ImageButton playButton; //播放按钮
    private int position = -1; //播放列表
    private RecyclerView recyclerView;
    private MusicRecyclerViewAdapter adapter;
    private MediaPlayer.OnCompletionListener onCompletionListener;
    private boolean flag = false;
    private Handler handler;

    public final MediaPlayer.OnCompletionListener SINGLE_ONCE = mp -> {
        resetMediaPlayer();
        resetTextView();
    };

    public final MediaPlayer.OnCompletionListener SINGLE_LOOP = MediaPlayer::start;

    public final MediaPlayer.OnCompletionListener MULTI_ONCE = mp -> {
        resetMediaPlayer();
        resetTextView();
        if (position < adapter.getItemCount() - 1) play(position + 1);
        else playButton.setImageDrawable(
                ContextCompat.getDrawable(MusicApplication.getContext(), android.R.drawable.ic_media_play));
    };

    public final MediaPlayer.OnCompletionListener MULTI_LOOP = mp -> {
        resetMediaPlayer();
        resetTextView();
        play(position + 1);
    };

    public final MediaPlayer.OnCompletionListener RANDOM_MOOD = mp -> {
        resetMediaPlayer();
        resetTextView();
        play(ThreadLocalRandom.current().nextInt(adapter.getItemCount()));
    };

    private void resetMediaPlayer() {
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    private void resetTextView() {
        playingView.setTextColor(Color.BLACK);
        playingView.setTextSize(16);
    }

    public void setCurrentDuration(TextView currentDuration) {
        this.currentDuration = currentDuration;
    }

    public void setDuration(TextView duration) {
        this.duration = duration;
    }

    public void setPlayButton(ImageButton playButton) {
        this.playButton = playButton;
    }

    public void setOnCompletionListener(Boolean single, Boolean loop, boolean random) {
        if (random) {
            setOnCompletionListener(RANDOM_MOOD);
            return;
        }
        setOnCompletionListener(single ? loop ? SINGLE_LOOP : SINGLE_ONCE : loop ? MULTI_LOOP : MULTI_ONCE);
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
        if (mediaPlayer != null) mediaPlayer.setOnCompletionListener(onCompletionListener);
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.adapter = (MusicRecyclerViewAdapter) recyclerView.getAdapter();
    }

    public void setSeekBar(SeekBar seekBar) {
        this.seekBar = seekBar;
        seekBar.setEnabled(false);// 没开始播放的时候进度条是不能拖动的
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }
        });
        handler = new Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) {
                if (mediaPlayer == null) {
                    seekBar.setProgress(0);
                    seekBar.setEnabled(false);
                    currentDuration.setText("00:00");
                }
                else if (msg.what == 0) {
                    // 播放和SeekBar同步
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    currentDuration.setText(formatTime(currentPosition));
                }
            }
        };
    }

    private void play() {
        if (adapter.getItemCount() > 0) {
            if (position < 0) position = 0;
            play(position);
        }
    }

    public void play(int position) {
        if (adapter.getItemCount() < 1) {
            if (mediaPlayer != null) resetMediaPlayer();
            return;
        }
        if (position < 0) position = adapter.getItemCount() + position;
        if (this.position >= 0) {
            playingView = recyclerView.getChildAt(this.position).findViewById(R.id.name);
            resetTextView();
        }
        this.position = position;
        playingView = recyclerView.getChildAt(position).findViewById(R.id.name);
        playingView.setTextColor(Color.BLUE);
        playingView.setTextSize(24);
        playButton.setImageDrawable(
                ContextCompat.getDrawable(MusicApplication.getContext(), android.R.drawable.ic_media_pause));
        String path = adapter.getItem(position).getPath();
        if (mediaPlayer == null) {
            playMusic(path);
        }
        else if (mediaPlayer.isPlaying()) {
            resetTextView();
            playMusic(path);
        }
        else {
            mediaPlayer.start();
        }
        flag = true;
        // 这个线程用来控制进度条的更新
        Thread th = new Thread(() -> {
            while (flag) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message msg = new Message();
                msg.what = 0;
                handler.sendMessage(msg);
            }
        });
        th.start();
    }

    private void playMusic(String path) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            // 准备监听事件
            mediaPlayer.setOnPreparedListener(mp -> {
                mediaPlayer.start();
                // 设置SeekBar
                int max = mediaPlayer.getDuration();
                int cur = mediaPlayer.getCurrentPosition();
                duration.setText(formatTime(max));
                Log.e("TAG", max + "==" + cur);
                seekBar.setMax(max);
                seekBar.setProgress(cur);
                seekBar.setEnabled(true);
            });
            // 音乐播放完毕的监听器
            mediaPlayer.setOnCompletionListener(onCompletionListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playOrPause() {
        if (flag) pause();
        else play();
    }

    private void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            flag = false;
            playButton.setImageDrawable(
                    ContextCompat.getDrawable(MusicApplication.getContext(), android.R.drawable.ic_media_play));
        }
    }

    public void previous() {
        play(position - 1);
    }

    public void next() {
        play(position == adapter.getItemCount() - 1 ? 0 : position + 1);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MusicBinder();
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            Log.v("service", "getService");
            return MusicService.this;
        }
    }

    private static String formatTime(int time) {
        return String.format("%02d:%02d", (time /= 1000) / 60, time % 60);
    }

    public void clearItem() {
        position = -1;
    }
}
