
package com.qihoo.videocloud.player.preview;

import static com.qihoo.videocloud.IQHVCPlayer.INFO_DEVICE_RENDER_QUERY_SURFACE;
import static com.qihoo.videocloud.IQHVCPlayer.INFO_LIVE_PLAY_START;
import static com.qihoo.videocloud.IQHVCPlayer.INFO_RENDER_RESET_SURFACE;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.qihoo.livecloud.play.callback.PlayerCallback;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.IQHVCPlayer;
import com.qihoo.videocloud.IQHVCPlayerAdvanced;
import com.qihoo.videocloud.QHVCPlayer;
import com.qihoo.videocloud.view.QHVCTextureView;

import java.util.HashMap;
import java.util.Map;

/**
 * 硬解码自动切软解码 demo
 */
public class VodSmartDecodeActivity extends Activity {
    private final static String TAG = VodSmartDecodeActivity.class.getSimpleName();

    private QHVCPlayer qhvcPlayer;
    private QHVCTextureView videoView;
    private String mSn;

    private long m_timestat = 0;
    private Handler mHandler;

    boolean isSendTestMsg = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);

        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.activity_vod_smart_decode);
        mHandler = new Handler();

        videoView = (QHVCTextureView) findViewById(R.id.gLVideoView1);
        m_timestat = System.currentTimeMillis();

        vod();
    }

    private void vod() {

        Intent i = getIntent();
        String channelId = i.getStringExtra("channelId");
        String url = i.getStringExtra("url");

        Logger.d(TAG, "sn = " + mSn);

        qhvcPlayer = new QHVCPlayer(this);
        videoView.onPlay();
        videoView.setPlayer(qhvcPlayer);
        qhvcPlayer.setDisplay(videoView);

        try {
            Map<String, Object> options = new HashMap<>();
            options.put(IQHVCPlayerAdvanced.KEY_OPTION_DECODE_MODE, IQHVCPlayerAdvanced.LIVECLOUD_SMART_DECODE_MODE);
            qhvcPlayer.setDataSource(IQHVCPlayer.PLAYTYPE_VOD, url, channelId);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, "数据源异常", Toast.LENGTH_SHORT).show();
            return;
        }

        qhvcPlayer.setOnPreparedListener(new IQHVCPlayer.OnPreparedListener() {
            @Override
            public void onPrepared() {
                qhvcPlayer.start();
            }
        });
        qhvcPlayer.setOnInfoListener(new IQHVCPlayer.OnInfoListener() {
            @Override
            public void onInfo(int handle, int what, int extra) {
                Log.e(TAG, "onInfo. handle: " + handle + " what: " + what + " extra: " + extra);
                if (what == INFO_LIVE_PLAY_START) {
                    if (!isSendTestMsg) {
                        isSendTestMsg = true;

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                qhvcPlayer.test_hard_decoder_error();
                            }
                        }, 3000);
                    }
                } else if (what == INFO_DEVICE_RENDER_QUERY_SURFACE) {
                    if (videoView != null
                            && qhvcPlayer != null
                            && !qhvcPlayer.isPaused()) {
                        videoView.render_proc(PlayerCallback.DEVICE_RENDER_QUERY_SURFACE, 0);
                    }

                } else if (what == INFO_RENDER_RESET_SURFACE) {
                    if (videoView != null) {
                        videoView.pauseSurface();
                    }
                }
            }
        });
        qhvcPlayer.setOnErrorListener(new IQHVCPlayer.OnErrorListener() {
            @Override
            public boolean onError(int handle, int what, int extra) {
                Log.e(TAG, "onError. handle: " + handle + " what: " + what + " extra: " + extra);
                Toast.makeText(VodSmartDecodeActivity.this, "error=" + what + " extra=" + extra, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        qhvcPlayer.setOnVideoSizeChangedListener(new IQHVCPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(int handle, int width, int height) {
                if (videoView != null) {
                    videoView.setVideoRatio((float) width / (float) height);
                }
            }
        });
        try {
            qhvcPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, "prepareAsync 异常", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private void statTime() {
        long curTime = System.currentTimeMillis();
        String str = " first frame render diff time " + (curTime - m_timestat) + " ms";
        Logger.i(TAG, str);
        Toast.makeText(VodSmartDecodeActivity.this, str, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        Logger.d(TAG, "onBackPressed");

        stopPlay();
        super.onBackPressed();
    }

    private void stopPlay() {
        Logger.d(TAG, "stopPlay");

        if (qhvcPlayer != null) {
            qhvcPlayer.stop(IQHVCPlayerAdvanced.USER_CLOSE);
            qhvcPlayer.release();
            qhvcPlayer = null;
        }
    }

}
