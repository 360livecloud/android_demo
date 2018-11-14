
package com.qihoo.videocloud.player.vod;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo.livecloud.play.callback.PlayerCallback;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.IQHVCPlayer;
import com.qihoo.videocloud.IQHVCPlayerAdvanced;
import com.qihoo.videocloud.QHVCPlayer;
import com.qihoo.videocloud.view.QHVCTextureView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VodSwitchResolutionActivity extends Activity {
    private final static String TAG = VodSwitchResolutionActivity.class.getSimpleName();

    private String SN_1080x1920 = "http://yunxianchang.live.ujne7.com/vod-system-bj/106692792_2_mp4-1522376285-52f7d75e-10cd-063f.mp4";
    private String SN_720x1280 = "http://yunxianchang.live.ujne7.com/vod-system-bj/106692792_1_mp4-1522376285-a07803a8-4f6f-d0ee.mp4";
    private String SN_360x640 = "http://yunxianchang.live.ujne7.com/vod-system-bj/106692792_0_mp4-1522376285-6dc7d759-0fff-2b06.mp4";

    //    private String SN_360x640 = "http://q3.v.k.360kan.com/vod-xinxiliu-tv-q3-bj/15984_64239be089746-79f8-4b53-afb7-ab82cd0b7a01.mp4";
    //    private String SN_720x1280 = "http://q3.v.k.360kan.com/vod-xinxiliu-tv-q3-bj/15984_64240ef08b20d-4e0e-44c2-94b5-d80a7bbed1dc.mp4";
    //    private String SN_1080x1920 = "http://q3.v.k.360kan.com/vod-xinxiliu-tv-q3-bj/15984_642421b2d08d5-2cd5-4f2c-b930-6bfc99df1f42.mp4";

    private String[] SN_SOURCE = {
            SN_1080x1920, SN_720x1280, SN_360x640
    };
    private String[] SN_SOURCE_FLAG = {
            "1080x1920", "720x1280", "360x640"
    };

    boolean mIsSpinnerFirst = true;
    int mLastSwitchSuccessSourceIndex = 0;
    Spinner mSpinnerResolution;
    TextView mTvResolution;
    SeekBar mSbPlayProgress;
    TextView mTvShowMsg;
    ProgressDialog mProgressDialog;

    private volatile IQHVCPlayerAdvanced iqhvcPlayer;
    private QHVCTextureView mVideoView;
    private String mSn;

    private int mProgress;
    private int mVolume;
    private float mPlaybackRate = 1.0f;

    private boolean mJustUpdateSpinner = false;
    IQHVCPlayerAdvanced.QHVCSwitchResolutionListener switchResolutionListener = new IQHVCPlayerAdvanced.QHVCSwitchResolutionListener() {
        @Override
        public void onPrepare() {
            Logger.e(TAG, "switch prepare...");
        }

        @Override
        public void onStart() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mProgressDialog != null && !mProgressDialog.isShowing()) {
                        mProgressDialog.setTitle("[auto] switching...");
                        mProgressDialog.show();
                    }
                }
            });
        }

        @Override
        public void onSuccess(final int index, final String url) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VodSwitchResolutionActivity.this, "switch success: " + SN_SOURCE[index], Toast.LENGTH_SHORT).show();
                    mTvResolution.setText(SN_SOURCE_FLAG[index]);

                    mJustUpdateSpinner = true;
                    mSpinnerResolution.setSelection(index);
                    mLastSwitchSuccessSourceIndex = index;

                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                }
            });
        }

        @Override
        public void onError(final int errorCode, final String errorMsg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VodSwitchResolutionActivity.this, "switch error: " + errorCode + " " + errorMsg, Toast.LENGTH_SHORT).show();
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }

                    // 切换失败
                    mJustUpdateSpinner = false;
                    mSpinnerResolution.setSelection(mLastSwitchSuccessSourceIndex);
                    mTvResolution.setText(SN_SOURCE_FLAG[mLastSwitchSuccessSourceIndex]);
                }
            });
        }
    };

    private boolean mIsAdapt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        //                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);

        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.activity_vod_switch_resolution);
        initView();
        mIsAdapt = getIntent().getBooleanExtra("isAdapt", false);

        mTvResolution.setText(SN_SOURCE_FLAG[0]);
        mSn = SN_SOURCE[0];
        vod();
    }

    private void initView() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);

        mTvShowMsg = (TextView) findViewById(R.id.tv_show_msg);
        mVideoView = (QHVCTextureView) findViewById(R.id.gLVideoView1);
        mVideoView.onPlay();
        mTvResolution = (TextView) findViewById(R.id.tv_resolution);

        ((CheckBox) findViewById(R.id.cb_resolution_adapt)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (iqhvcPlayer != null) {

                    iqhvcPlayer.setResolutionAdapt(isChecked, switchResolutionListener);

                    if (isChecked) {
                        Toast.makeText(VodSwitchResolutionActivity.this, "【打开】码率自适应", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(VodSwitchResolutionActivity.this, "【关闭】码率自适应", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        ((CheckBox) findViewById(R.id.cb_in_background)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (iqhvcPlayer != null) {
                    iqhvcPlayer.disableRender(isChecked);
                    mTvShowMsg.setText("");
                }
            }
        });

        ((CheckBox) findViewById(R.id.cb_pause)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (iqhvcPlayer != null) {
                    if (isChecked) {
                        iqhvcPlayer.pause();
                        mTvShowMsg.setText("");
                    } else {
                        iqhvcPlayer.start();
                    }
                }
            }
        });

        ((CheckBox) findViewById(R.id.cb_mute)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (iqhvcPlayer != null) {
                    iqhvcPlayer.setMute(isChecked);
                }
            }
        });
        ((SeekBar) findViewById(R.id.sb_volume)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mVolume = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mVolume = 0;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (iqhvcPlayer != null) {
                    iqhvcPlayer.setVolume((1.0f * mVolume) / 100);
                }
            }
        });

        findViewById(R.id.btn_playback_rate_sub).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (iqhvcPlayer != null && (iqhvcPlayer.isPlaying() || iqhvcPlayer.isPaused())) {

                    if (mPlaybackRate >= (QHVCPlayer.PLAYBACK_RATE_DEFAULT - QHVCPlayer.FLOAT_EPSINON)) {
                        mPlaybackRate -= 0.5f;
                        iqhvcPlayer.setPlayBackRate(mPlaybackRate);

                        Toast.makeText(VodSwitchResolutionActivity.this, "set playback rate: " + mPlaybackRate, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(VodSwitchResolutionActivity.this, "not set. current playback rate: " + mPlaybackRate, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        findViewById(R.id.btn_playback_rate_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (iqhvcPlayer != null && (iqhvcPlayer.isPlaying() || iqhvcPlayer.isPaused())) {

                    mPlaybackRate += 0.5f;
                    iqhvcPlayer.setPlayBackRate(mPlaybackRate);

                    Toast.makeText(VodSwitchResolutionActivity.this, "set playback rate: " + mPlaybackRate, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mSbPlayProgress = (SeekBar) findViewById(R.id.sb_play_progress);
        mSbPlayProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Logger.v(TAG, "onProgressChanged seekBar" + " progress=" + progress + " fromUser=" + fromUser);
                mProgress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mProgress = 0;
                Logger.v(TAG, "onStartTrackingTouch seekBar=" + seekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                Logger.e(TAG, "onStopTrackingTouch seekBar mProgress=" + mProgress);
                if (iqhvcPlayer != null) {
                    int duration = iqhvcPlayer.getDuration();
                    int seekTo = (iqhvcPlayer.getDuration() * mProgress) / 100;

                    Logger.e(TAG, "duration: " + duration + " seekTo: " + seekTo);

                    iqhvcPlayer.seekTo(seekTo);
                }
            }
        });

        mSpinnerResolution = (Spinner) findViewById(R.id.spinner_resolution);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < SN_SOURCE.length; i++) {
            list.add(SN_SOURCE_FLAG[i]);
        }
        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerResolution.setAdapter(adapter);
        mSpinnerResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {

                if (mIsSpinnerFirst) {
                    mIsSpinnerFirst = false;
                    return;
                }

                if (mJustUpdateSpinner) {
                    mJustUpdateSpinner = false;
                    Logger.e(TAG, "just update spinner.");
                    return;
                }

                Logger.e(TAG, "index: " + position + " url: " + SN_SOURCE[position]);

                ((CheckBox) findViewById(R.id.cb_resolution_adapt)).setChecked(false);
                if (iqhvcPlayer != null) {
                    if (iqhvcPlayer.isPaused()) {
                        iqhvcPlayer.start();

                        ((CheckBox) findViewById(R.id.cb_pause)).setChecked(false);
                    }

                    mTvShowMsg.setText(SN_SOURCE_FLAG[position] + " 切换中...");
                    iqhvcPlayer.switchResolution(position, new IQHVCPlayerAdvanced.QHVCSwitchResolutionListener() {
                        @Override
                        public void onPrepare() {
                            Logger.e(TAG, "[hand] switch prepare...");
                        }

                        @Override
                        public void onStart() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mProgressDialog != null && !mProgressDialog.isShowing()) {
                                        mProgressDialog.setTitle("[hand] switching...");
                                        mProgressDialog.show();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onSuccess(final int index, final String url) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(VodSwitchResolutionActivity.this, "[hand] switch success: " + SN_SOURCE[index], Toast.LENGTH_SHORT).show();
                                    mTvShowMsg.setText("");
                                    mTvResolution.setText(SN_SOURCE_FLAG[index]);
                                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                                        mProgressDialog.dismiss();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(final int errorCode, final String errorMsg) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTvShowMsg.setText("");
                                    Toast.makeText(VodSwitchResolutionActivity.this, "[hand] switch error: " + errorCode + " " + errorMsg, Toast.LENGTH_SHORT).show();

                                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                                        mProgressDialog.dismiss();
                                    }
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void vod() {

        Logger.d(TAG, "sn = " + mSn);
        iqhvcPlayer = new QHVCPlayer(this);
        mVideoView.onPlay();
        mVideoView.setPlayer(iqhvcPlayer);
        iqhvcPlayer.setDisplay(mVideoView);
        try {
            iqhvcPlayer.setDataSource(IQHVCPlayer.PLAYTYPE_VOD,
                    new String[] {
                            "resId0", "resId1", "resId2"
                    },
                    SN_SOURCE,
                    0,
                    getResources().getString(R.string.config_player_vod_cid),
                    "",
                    new HashMap<String, Object>(1));
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
            return;
        }

        iqhvcPlayer.setOnPreparedListener(new IQHVCPlayer.OnPreparedListener() {
            @Override
            public void onPrepared() {
                iqhvcPlayer.start();
            }
        });
        iqhvcPlayer.setOnVideoSizeChangedListener(new IQHVCPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(int handle, int width, int height) {
                if (mVideoView != null) {
                    mVideoView.setVideoRatio((float) width / (float) height);
                }
            }
        });
        iqhvcPlayer.setOnInfoListener(new IQHVCPlayer.OnInfoListener() {
            @Override
            public void onInfo(int handle, int what, int extra) {
                Logger.w(TAG, "onInfo handle: " + handle + " what: " + what + " extra: " + extra);
                if (what == IQHVCPlayer.INFO_LIVE_PLAY_START) {

                    findViewById(R.id.layout_btns).setVisibility(View.VISIBLE);
                    mSbPlayProgress.setVisibility(View.VISIBLE);
                    if (mIsAdapt) {
                        ((CheckBox) findViewById(R.id.cb_resolution_adapt)).setChecked(mIsAdapt);
                    }

                } else if (what == IQHVCPlayer.INFO_DEVICE_RENDER_ERR) {
                    // err
                    if (Logger.LOG_ENABLE) {
                        Logger.e(TAG, "dvrender err");
                    }
                } else if (what == IQHVCPlayer.INFO_DEVICE_RENDER_QUERY_SURFACE) {

                    if (mVideoView != null) {
                        if (iqhvcPlayer != null && !iqhvcPlayer.isPaused()) {
                            mVideoView.render_proc(PlayerCallback.DEVICE_RENDER_QUERY_SURFACE, 0/*不使用此变量*/);
                        }
                    }
                } else if (what == IQHVCPlayer.INFO_RENDER_RESET_SURFACE) {

                    if (mVideoView != null) {
                        mVideoView.pauseSurface();
                    }
                }
            }
        });
        iqhvcPlayer.setOnBufferingEventListener(new IQHVCPlayer.OnBufferingEventListener() {
            @Override
            public void onBufferingStart(int handle) {

                Logger.d(TAG, "buffering event. start");
            }

            @Override
            public void onBufferingProgress(int handle, int progress) {
                Log.v(TAG, "buffering event. progress: " + progress);
            }

            @Override
            public void onBufferingStop(int handle) {
                Logger.d(TAG, "buffering event. stop " + ((iqhvcPlayer != null) ? iqhvcPlayer.getCurrentPosition() : 0));

            }
        });
        iqhvcPlayer.setOnErrorListener(new IQHVCPlayer.OnErrorListener() {
            @Override
            public boolean onError(int handle, int what, int extra) {

                Logger.d(TAG, "onError. what: " + what + " extra: " + extra);
                Toast.makeText(VodSwitchResolutionActivity.this, "onError. what: " + what + " extra: " + extra, Toast.LENGTH_SHORT).show();
                stopPlay();
                finish();
                return false;
            }
        });
        iqhvcPlayer.setOnSeekCompleteListener(new IQHVCPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(int handle) {

                Logger.d(TAG, "seek complete");
            }
        });
        iqhvcPlayer.setOnCompletionListener(new IQHVCPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(int handle) {

            }
        });
        iqhvcPlayer.setOnPlayerNetStatsListener(new IQHVCPlayerAdvanced.OnPlayerNetStatsListener() {
            @Override
            public void onPlayerNetStats(int handle, long dvbps, long dabps, long dvfps, long dafps, long fps, long bitrate, long param1, long param2, long param3) {

                //Logger.d(TAG, "dvbps: "  + dvbps + " dabps: " + dabps + " dvfps: " + dvfps + " dafps: " + dafps + " fps: " + fps +" bitrate: " +bitrate);
            }
        });
        iqhvcPlayer.setOnProgressChangeListener(new IQHVCPlayer.onProgressChangeListener() {
            @Override
            public void onProgressChange(int handle, final int total, final int progress) {
                mSbPlayProgress.setProgress((progress * 100) / total);
            }
        });
        iqhvcPlayer.setOnBufferingUpdateListener(new IQHVCPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(int handle, int percent) {
                Log.v(TAG, "buffering: " + percent + " volume: " + iqhvcPlayer.getVolume());
            }
        });

        try {
            iqhvcPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, "prepareAsync 异常", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        Logger.i(TAG, "onBackPressed");

        stopPlay();
        super.onBackPressed();
    }

    private void stopPlay() {
        Logger.i(TAG, "stopPlay");

        if (iqhvcPlayer != null) {
            iqhvcPlayer.stop(IQHVCPlayerAdvanced.USER_CLOSE);
            iqhvcPlayer.release();
            iqhvcPlayer = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //        if (mLiveCloudPlayer != null) {
        //            mLiveCloudPlayer.disableRender(false);
        //        }
        //        if (mVideoView != null) {
        //            mVideoView.startRender();
        //        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //        if (mLiveCloudPlayer != null) {
        //            mLiveCloudPlayer.disableRender(true);
        //        }
        //        if (mVideoView != null) {
        //            mVideoView.stopRender();
        //        }
    }
}
