
package com.qihoo.videocloud.player.preview;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.TimedMetaData;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloudrefactor.R;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MediaPlayerActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = MediaPlayerActivity.class.getSimpleName();

    private MediaPlayer mMediaPlayer;
    private SurfaceView mSurfaceView;
    private CheckBox mPlayPause;
    private SeekBar mSeekBar;

    private String mUrl;
    private int mCurrentTime = 0;
    private long mBeginTick = 0;

    private Handler mHandler;
    private Timer mTimer;
    private TimerTask mTimerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mediaplayer);

        initData(getIntent());
        initView();
    }

    private void initData(Intent intent) {
        mUrl = intent.getStringExtra("url");
    }

    private void initView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);

        mPlayPause = (CheckBox) findViewById(R.id.cb_play_pause);
        mPlayPause.setOnClickListener(this);

        mSeekBar = ((SeekBar) findViewById(R.id.seekbar));
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

        SurfaceHolder mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(mUrl);
        } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                Logger.d(TAG, "onPrepared. ");

                int videoWidth = mediaPlayer.getVideoWidth();
                int videoHeight = mediaPlayer.getVideoHeight();

                int screenWidth = getResources().getDisplayMetrics().widthPixels;

                android.view.ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
                lp.width = screenWidth;
                lp.height = (int) (((float) videoHeight / (float) videoWidth) * (float) screenWidth);
                mSurfaceView.setLayoutParams(lp);

                start();
            }
        });
        mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                Logger.d(TAG, "onVideoSizeChanged. width: " + width + " height: " + height);
            }
        });
        mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Logger.d(TAG, "onBufferingUpdate. percent: " + percent);
            }
        });
        mMediaPlayer.setOnTimedMetaDataAvailableListener(new MediaPlayer.OnTimedMetaDataAvailableListener() {
            @Override
            public void onTimedMetaDataAvailable(MediaPlayer mp, TimedMetaData data) {
                Logger.d(TAG, "onTimedMetaDataAvailable. data: " + new Gson().toJson(data));
            }
        });
        mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Logger.d(TAG, "onInfo. what: " + what + " extra: " + extra);
                if (MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START == what) {
                    long endTick = System.currentTimeMillis();
                    Logger.d(TAG, "system first render use tick: " + (endTick - mBeginTick));
                }
                return false;
            }
        });
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Logger.w(TAG, "what: " + what + " extra: " + extra);
                Toast.makeText(MediaPlayerActivity.this, "error=" + what + " extra=" + extra, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Logger.d(TAG, "onCompletion. ");
                stopTimer();
            }
        });

        mBeginTick = System.currentTimeMillis();
        mMediaPlayer.prepareAsync();
    }

    @Override
    protected void onPause() {
        pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stop();
        super.onDestroy();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logger.d(TAG, "surface changed");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Logger.d(TAG, "surface created");
        mMediaPlayer.setDisplay(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Logger.d(TAG, "surface destroyed");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cb_play_pause: {
                CheckBox playPause = (CheckBox) v;
                if (playPause.isChecked()) {
                    try {
                        start();
                    } catch (IllegalStateException e) {
                        Logger.d(TAG, "Illegal State Exception", e);
                    }
                } else {
                    if (mMediaPlayer.isPlaying()) {
                        pause();
                    }
                }
                break;
            }
        }
    }

    private void start() {
        Logger.d(TAG, "current time on resume " + mCurrentTime);
        mPlayPause.setEnabled(true);
        mPlayPause.setChecked(true);
        mMediaPlayer.seekTo(mCurrentTime);
        mMediaPlayer.start();

        startTimer();
    }

    private void pause() {
        if (mMediaPlayer.isPlaying()) {
            mPlayPause.setChecked(false);
            mCurrentTime = mMediaPlayer.getCurrentPosition();
            Logger.d(TAG, "current time on pause " + mCurrentTime);
            mMediaPlayer.pause();
        }
    }

    private void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        stopTimer();
    }

    private void startTimer() {
        if (mTimer != null && mTimerTask != null) {
            return;
        }

        mHandler = new Handler(Looper.getMainLooper());

        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) {
                            return;
                        }
                        if (mMediaPlayer != null) {
                            mSeekBar.setProgress(mMediaPlayer.getCurrentPosition() * 100 / mMediaPlayer.getDuration());
                        }
                    }
                });
            }
        };

        mTimer.scheduleAtFixedRate(mTimerTask, 0, 1000);
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        if (mHandler != null) {
            mHandler = null;
        }
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        private int mProgress = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            Logger.d(TAG, "onProgressChanged seekBar " + " progress=" + progress + " fromUser=" + fromUser);
            mProgress = progress;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mProgress = 0;
            Logger.d(TAG, "onStartTrackingTouch seekBar=" + seekBar);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Logger.d(TAG, "onStopTrackingTouch seekBar=" + seekBar + " mProgress=" + mProgress);
            if (mMediaPlayer != null) {
                mMediaPlayer.seekTo(mMediaPlayer.getDuration() * mProgress / 100);
            }
        }
    };
}
