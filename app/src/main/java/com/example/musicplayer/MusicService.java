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
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer.ui.MusicRecyclerViewAdapter;

import java.util.concurrent.ThreadLocalRandom;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer; //音乐播放器
    private SeekBar seekBar; //进度条
    private TextView currentDuration; //当前持续时间文本框
    private TextView duration; //总时长文本框
    private ImageButton playButton; //播放按钮
    private int position = -1; //播放列表
    private RecyclerView recyclerView;
    private MusicRecyclerViewAdapter adapter;

    /**
     * 单曲单次播放
     */
    private final MediaPlayer.OnCompletionListener SINGLE_ONCE = mp -> {
        resetMediaPlayer();
        playButton.setImageDrawable( //设置播放按钮
                ContextCompat.getDrawable(MusicApplication.getContext(), android.R.drawable.ic_media_play));
    };
    /**
     * 单曲循环模式
     */
    private final MediaPlayer.OnCompletionListener SINGLE_LOOP = MediaPlayer::start;
    /**
     * 获取position的handler
     */
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MusicApplication.DELETE_CODE) { //删除控件后的修改
                if (msg.arg1 <= position) position--; //若删除控件位置小于当前位置
            }
            else if (msg.what == MusicApplication.SWAP_CODE) { //交换控件后的修改
                if (msg.arg1 == position) position = msg.arg2; //arg1为原始位置,arg2为交换后位置,当前控件进行交换时,位置改变
                else if (msg.arg1 > position && msg.arg2 <= position) position++; //后面的控件放在前面,位置后移
                else if (msg.arg1 < position && msg.arg2 > position) position--; //前面的控件放在后面,位置前移
            }
        }
    };
    private boolean musicFlag; //音乐正在播放标志
    private Handler seekBarHandler; //与seekBar交互的handler
    private String path; //音乐播放路径
    private MediaPlayer.OnCompletionListener onCompletionListener; //播放完的事件监听
    /**
     * 列表播放模式
     */
    private final MediaPlayer.OnCompletionListener MULTI_ONCE = mp -> {
        resetMediaPlayer();
        if (position < adapter.getItemCount() - 1) play(position + 1);
        else playButton.setImageDrawable(
                ContextCompat.getDrawable(MusicApplication.getContext(), android.R.drawable.ic_media_play));
    };
    /**
     * 列表循环模式
     */
    private final MediaPlayer.OnCompletionListener MULTI_LOOP = mp -> {
        resetMediaPlayer();
        play(position + 1);
    };
    /**
     * 随机模式
     */
    private final MediaPlayer.OnCompletionListener RANDOM_MOOD = mp -> {
        resetMediaPlayer();
        int position = -1;
        if (adapter.getItemCount() > 1) {
            do {
                position = ThreadLocalRandom.current().nextInt(adapter.getItemCount()); //获取随机数
            } while (position == this.position); //随机数相同情况下重新随机
        }
        play(position);
    };

    /**
     * 毫秒格式化为分秒模式
     *
     * @param time 时间戳
     * @return 格式化时间字符串
     */
    private static String formatTime(int time) {
        return String.format("%02d:%02d", (time /= 1000) / 60, time % 60);
    }

    public Handler getHandler() {
        return handler;
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

    private void resetMediaPlayer() { //重置媒体播放器
        Log.v("reset", "player");
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
        if (mediaPlayer != null) mediaPlayer.setOnCompletionListener(onCompletionListener);
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.adapter = (MusicRecyclerViewAdapter) recyclerView.getAdapter();
    }

    public void setOnCompletionListener(Boolean single, Boolean loop, boolean random) {
        if (random) { //随机模式优先一切
            setOnCompletionListener(RANDOM_MOOD);
            return;
        }
        //根据布尔值判断播放模式,全部为假,即默认为列表顺序播放
        setOnCompletionListener(single ? loop ? SINGLE_LOOP : SINGLE_ONCE : loop ? MULTI_LOOP : MULTI_ONCE);
    }

    public void setSeekBar(SeekBar seekBar) {
        this.seekBar = seekBar;
        seekBar.setEnabled(false);// 没开始播放的时候进度条是不能拖动的
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress()); //进度条拖动绑定媒体播放器播放
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }
        });
        seekBarHandler = new Handler(Looper.getMainLooper()) { //媒体播放控制进度条进度
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) { //为空初始化
                if (mediaPlayer == null) {
                    seekBar.setProgress(0);
                    seekBar.setEnabled(false);
                    currentDuration.setText("00:00"); //播放时长置空
                }
                else if (msg.what == 0) { //播放和SeekBar同步
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    currentDuration.setText(formatTime(currentPosition));
                }
            }
        };
    }

    public void play(int position) {
        Log.v("position", this.position + ", " + position);
        if (adapter.getItemCount() < 1) { //列表为空,不播放
            if (mediaPlayer != null) resetMediaPlayer();
            return;
        }
        if (position < 0) position = adapter.getItemCount() - 1; //列表循环操作
        else if (position >= adapter.getItemCount()) position = 0;
        TextView playingView; //正在播放音乐的文本框
        if (this.position >= 0 && this.position < adapter.getItemCount()) {
            playingView = recyclerView.getChildAt(this.position).findViewById(R.id.name); //还原上一个文本框的状态
            playingView.setTextColor(Color.BLACK);
            playingView.setTextSize(16);
        }
        playingView = recyclerView.getChildAt(position).findViewById(R.id.name); //设置当前文本框的播放样式
        playingView.setTextColor(Color.BLUE);
        playingView.setTextSize(24);
        playButton.setImageDrawable( //按钮设为在暂停样式按钮
                ContextCompat.getDrawable(MusicApplication.getContext(), android.R.drawable.ic_media_pause));
        String path = adapter.getItem(position).getPath(); //播放路径
        if (mediaPlayer == null) { //为空(第一次播放)
            playMusic(path);
        }
        else if (this.path.equals(path)) { //非空,若播放路径相同,续播
            mediaPlayer.start();
        }
        else {
            resetMediaPlayer(); //路径不同,播放其他歌曲
            playMusic(path);
        }
        this.position = position;
        this.path = path;
        musicFlag = true;
        //子线程用来控制进度条的更新
        Thread th = new Thread(() -> {
            while (musicFlag) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message msg = Message.obtain();
                msg.what = 0;
                seekBarHandler.sendMessage(msg);
            }
        });
        th.start();
    }

    private void playMusic(String path) { //音乐播放
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.setOnPreparedListener(mp -> { // 准备监听事件
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

    private void pause() { //音乐暂停
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            musicFlag = false;
            playButton.setImageDrawable(
                    ContextCompat.getDrawable(MusicApplication.getContext(), android.R.drawable.ic_media_play));
        }
    }

    public void playOrPause() { //播放按钮绑定事件
        if (musicFlag) pause();
        else play(position);
    }

    public void previous() { //上一首
        play(position - 1);
    }

    public void next() { //下一首
        play(position + 1);
    }

    public void clearItem() { //清空
        position = -1;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MusicBinder();
    }

    public class MusicBinder extends Binder { //获取服务
        public MusicService getService() {
            Log.v("service", "getService");
            return MusicService.this;
        }
    }
}
