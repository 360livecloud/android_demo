
package com.qihoo.videocloud.player.vod;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo.livecloud.play.GifRecordConfig;
import com.qihoo.livecloud.play.VideoRecordConfig;
import com.qihoo.livecloud.play.callback.PlayerCallback;
import com.qihoo.livecloud.sdk.QHVCSdk;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.tools.NetUtil;
import com.qihoo.livecloud.utils.FileUtils;
import com.qihoo.livecloud.utils.PlayerLogger;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.IQHVCPlayer;
import com.qihoo.videocloud.IQHVCPlayerAdvanced;
import com.qihoo.videocloud.QHVCPlayer;
import com.qihoo.videocloud.QHVCPlayerPlugin;
import com.qihoo.videocloud.player.LogAdapter;
import com.qihoo.videocloud.player.PlayConstant;
import com.qihoo.videocloud.utils.AndroidUtil;
import com.qihoo.videocloud.utils.NoDoubleClickListener;
import com.qihoo.videocloud.view.QHVCTextureView;
import com.qihoo.videocloud.widget.ViewHeader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qihoo.videocloud.player.PlayConstant.SHOW_MODEL_LAND;
import static com.qihoo.videocloud.player.PlayConstant.SHOW_MODEL_PORT;
import static com.qihoo.videocloud.player.PlayConstant.SHOW_MODEL_PORT_SMALL;

public class VodActivity extends Activity implements View.OnClickListener {

    private static final String TAG = VodActivity.class.getSimpleName();
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = 1000;

    @PlayConstant.ShowModel
    int currentShowModel = SHOW_MODEL_PORT_SMALL;

    private IQHVCPlayerAdvanced qhvcPlayer;
    private String url;
    private String channelId;
    private String businessId;
    private boolean autoDecoded;

    private QHVCTextureView playView;
    private RelativeLayout rlPlayerContainer;
    private ViewHeader viewHeaderMine;
    private TextView tvHeaderCenter;

    private boolean isRecording = false;
    private String videoRecordFilePath = null;
    private long recordBeginTick = 0;
    private View btnCut;
    private View btnRecord;

    private ImageView btnPlay;
    private TextView tvPlayTime;

    private SeekBar sbProgress;
    private int currentProgress;

    private TextView tvDuration;
    private View ivZoom;
    private ListView lvLog;
    private LogAdapter logAdapter;
    private List<String> logList = new ArrayList<>();

    private int videoWidth;
    private int videoHeight;

    private Map<String, Object> mediaInformationMap;
    private long downloadBitratePerSecond;//下行码率
    private long videoBitratePerSecond;// 视频码率
    private long videoFrameRatePerSecond;//视频帧率

    private long mBeginTick;

    private PopupWindow mChangeSpeedPopWindow;
    private PopupWindow mResolutionRatioPopWindow;
    private TextView speed1_0;
    private TextView speed1_5;
    private TextView speed2_0;
    private TextView resolutionRatio;
    private ImageView changeSpeed;
    private TextView resoulution_1080p;
    private TextView resoulution_720p;
    private TextView resoulution_480p;
    private TextView resoulution_320p;
    private TextView mChangeMessage;
    private String SN_1080P = "http://yunxianchang.live.ujne7.com/vod-system-bj/87926845_4_mp4-1516589235-1c953617-e268-c0a8.mp4";
    private String SN_720P = "http://yunxianchang.live.ujne7.com/vod-system-bj/87926845_2_mp4-1516589235-b10eb2c4-a8be-b6c6.mp4";
    private String SN_480P = "http://yunxianchang.live.ujne7.com/vod-system-bj/87926845_1_mp4-1516589235-622a3f1d-c62b-9c41.mp4";
    private String SN_360P = "http://yunxianchang.live.ujne7.com/vod-system-bj/87926845_0_mp4-1516589235-6cbce4d1-614c-7059.mp4";
    private String[] SN_SOURCE = {
            SN_1080P, SN_720P, SN_480P, SN_360P
    };
    private String[] SN_SOURCE_FLAG = {
            "1080P", "超清", "高清", "标清"
    };
    private float[] mPlayRate = {
            1f, 1.5f, 2f,
    };
    private boolean mNeedStartPlayer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //        hideSystemNavigationBar();
        super.onCreate(savedInstanceState);

        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.activity_vod);
        initView();
        initData();
        boolean checkResult = checkSelfPermissionAndRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
        if (checkResult) {
            vodProxy();
        } else {
            mNeedStartPlayer = true;
        }
    }

    private void initData() {
        Intent i = getIntent();
        businessId = i.getStringExtra("businessId");
        if (!TextUtils.isEmpty(businessId)) {
            QHVCSdk.getInstance().getConfig().setBusinessId(businessId);
        }

        channelId = i.getStringExtra("channelId");
        url = i.getStringExtra("url");
        autoDecoded = i.getBooleanExtra("autoDecoded", Boolean.FALSE);
    }

    private void initView() {
        rlPlayerContainer = (RelativeLayout) findViewById(R.id.rl_player_container);
        playView = (QHVCTextureView) findViewById(R.id.playView);
        viewHeaderMine = (ViewHeader) findViewById(R.id.viewHeaderMine);
        viewHeaderMine.setLeftText("点播");
        viewHeaderMine.getLeftIcon().setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                playerClose();
                finish();
            }
        });
        tvHeaderCenter = viewHeaderMine.getCenterTitle();

        lvLog = (ListView) findViewById(R.id.lv_log);
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            logAdapter = new LogAdapter(this, logList, R.color.white);
        } else {
            logAdapter = new LogAdapter(this, logList, R.color.color_666666);
        }
        lvLog.setAdapter(logAdapter);

        btnPlay = (ImageView) findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (qhvcPlayer != null && qhvcPlayer.isPlaying()) {
                    qhvcPlayer.pause();

                    btnPlay.setImageDrawable(null);
                    btnPlay.setImageDrawable(getResources().getDrawable(R.drawable.play));
                } else if (qhvcPlayer != null && qhvcPlayer.isPaused()) {
                    qhvcPlayer.start();

                    btnPlay.setImageDrawable(null);
                    btnPlay.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                }
            }
        });

        tvPlayTime = (TextView) findViewById(R.id.tv_play_time);
        sbProgress = (SeekBar) findViewById(R.id.sb_progress);
        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentProgress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                currentProgress = 0;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (qhvcPlayer != null) {

                    qhvcPlayer.seekTo((qhvcPlayer.getDuration() * currentProgress) / 100, false);
                }
            }
        });

        tvDuration = (TextView) findViewById(R.id.tv_duration);

        ivZoom = findViewById(R.id.iv_zoom);
        ivZoom.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                if (currentShowModel == SHOW_MODEL_PORT_SMALL) {

                    if (videoWidth != 0 && videoHeight != 0) {

                        Log.v(TAG, "width: " + videoWidth + " height: " + videoHeight);
                        if (videoHeight > videoWidth) {

                            currentShowModel = SHOW_MODEL_PORT;
                            portZoomIn();
                            initRecordBtnView();
                        } else {

                            currentShowModel = SHOW_MODEL_LAND;
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                            resolutionRatio.setVisibility(View.VISIBLE);
                            changeSpeed.setVisibility(View.VISIBLE);
                        }
                    } else {

                        Toast.makeText(VodActivity.this, "cannot zoom. width: " + videoWidth + " height: " + videoHeight, Toast.LENGTH_SHORT).show();
                    }
                } else if (currentShowModel == SHOW_MODEL_PORT) {

                    currentShowModel = SHOW_MODEL_PORT_SMALL;
                    portZoomOut();
                    initRecordBtnView();
                } else if (currentShowModel == SHOW_MODEL_LAND) {

                    currentShowModel = SHOW_MODEL_PORT_SMALL;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                // SHOW_MODEL_PORT have not zoom
            }
        });
        resolutionRatio = (TextView) findViewById(R.id.resolution_ratio);
        resolutionRatio.setOnClickListener(this);
        changeSpeed = (ImageView) findViewById(R.id.change_speed);
        changeSpeed.setOnClickListener(this);
        mChangeMessage = (TextView) findViewById(R.id.vod_changeMessage);

        initRecordBtnView();
    }

    private void initRecordBtnView() {
        if (currentShowModel == SHOW_MODEL_LAND || currentShowModel == SHOW_MODEL_PORT) {
            btnCut = findViewById(R.id.btn_cut);
            btnCut.setVisibility(View.VISIBLE);
            btnCut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (qhvcPlayer != null && (qhvcPlayer.isPlaying() || qhvcPlayer.isPaused())) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String recordRootPath = AndroidUtil.getAppDir() + "RecorderLocal" + File.separator;
                                FileUtils.createDir(recordRootPath);

                                String path = recordRootPath + AndroidUtil.getNewFileName(System.currentTimeMillis()) + ".bmp";
                                boolean ret = qhvcPlayer.snapshot(path);
                                Log.d(TAG, "qhvcPlayer.snapshot ret=" + ret + " path=" + path);
                                if (ret) {
                                    File f = new File(path);
                                    if (f != null && f.exists()) {
                                        Uri uri = Uri.fromFile(f);
                                        getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                                    }

                                    VodActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(VodActivity.this, "截图成功 已经保存到相册", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                        }).start();
                    } else {
                        Toast.makeText(VodActivity.this, "当前状态 不能截图", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            btnRecord = findViewById(R.id.btn_record);
            btnRecord.setVisibility(View.VISIBLE);
            btnRecord.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    boolean ret = false;
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        ret = true;
                        Log.d(TAG, "btn record. ACTION_UP");
                        if (isRecording) {
                            if (stopRecorder()) {
                                File f = new File(videoRecordFilePath);
                                if (f != null && f.exists()) {
                                    Uri uri = Uri.fromFile(f);
                                    getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                                }

                                Toast.makeText(VodActivity.this, "录制成功 已经保存到相册", Toast.LENGTH_SHORT).show();
                            }

                            isRecording = false;
                            videoRecordFilePath = null;
                            recordBeginTick = 0;
                        }
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        ret = true;

                        Log.d(TAG, "btn record. ACTION_DOWN");
                        String recordRootPath = AndroidUtil.getAppDir() + "RecorderLocal" + File.separator;
                        FileUtils.createDir(recordRootPath);

                        String path = recordRootPath + AndroidUtil.getNewFileName(System.currentTimeMillis()) + ".mp4";
                        if (startRecorderVideo(path)) {
                            isRecording = true;
                            videoRecordFilePath = path;
                            recordBeginTick = System.currentTimeMillis();
                        } else {
                            Toast.makeText(VodActivity.this, "录制 初始化失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                    return ret;
                }
            });
            tvHeaderCenter.setVisibility(View.VISIBLE);
        } else {
            if (tvHeaderCenter != null) {
                tvHeaderCenter.setVisibility(View.GONE);
            }
            if (btnCut != null) {
                btnCut.setVisibility(View.GONE);
            }
            if (btnRecord != null) {
                btnRecord.setVisibility(View.GONE);
            }
        }
    }

    private void vodProxy() {
        final QHVCPlayerPlugin qhvcPlayerPlugin = QHVCPlayerPlugin.getInstance();

        //若第三方未将播放器需要的so文件捆包，必须设置setDefaultPluginInstalled(false)
        qhvcPlayerPlugin.setDefaultPluginInstalled(true);

        if (qhvcPlayerPlugin.isDefaultPluginInstalled()) {
            vod();
        } else if (qhvcPlayerPlugin.isPluginInstalled()) {
            int result = qhvcPlayerPlugin.loadPlugin();
            if (result == QHVCPlayerPlugin.ERROR_SUCCESS) {
                vod();
            } else {
                Toast.makeText(this, "播放器插件加载失败" + "(" + result + ")", Toast.LENGTH_SHORT).show();
            }
        } else {
            qhvcPlayerPlugin.checkInstallPlugin(this, new QHVCPlayerPlugin.PluginCallback() {
                @Override
                public void onStart(Context context) {
                    Toast.makeText(context, "开始下载播放器插件", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProgress(Context context, int progress) {
                    Log.d(TAG, "插件下载进度：" + progress);
                }

                @Override
                public void onComplete(Context context, boolean background, int result) {
                    if (isFinishing()) {
                        return;
                    }

                    if (result == QHVCPlayerPlugin.ERROR_SUCCESS) {
                        result = qhvcPlayerPlugin.loadPlugin();
                    }
                    if (result == QHVCPlayerPlugin.ERROR_SUCCESS && !background) {
                        Toast.makeText(context, "播放器插件加载完成", Toast.LENGTH_SHORT).show();
                        vod();
                    } else {
                        Toast.makeText(context, "播放器插件加载失败" + "(" + result + ")", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancel(Context context) {
                    Toast.makeText(context, "取消下载播放器插件", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void vod() {

        qhvcPlayer = new QHVCPlayer(this);
        playView.onPlay();
        playView.setPlayer(qhvcPlayer);
        qhvcPlayer.setDisplay(playView);

        try {
            Map<String, Object> options = new HashMap<>();
            //            options.put(IQHVCPlayerAdvanced.KEY_OPTION_MUTE, true);
            //            options.put(IQHVCPlayerAdvanced.KEY_OPTION_POSITION, 30 * 1000);
            //            options.put(IQHVCPlayerAdvanced.KEY_OPTION_PLAY_MODE, IQHVCPlayerAdvanced.PLAYMODE_LOWLATENCY);
            //            options.put(IQHVCPlayerAdvanced.KEY_OPTION_RENDER_MODE, IQHVCPlayerAdvanced.RENDER_MODE_FULL);
            if (autoDecoded) {
                options.put(IQHVCPlayerAdvanced.KEY_OPTION_DECODE_MODE, IQHVCPlayerAdvanced.LIVECLOUD_SMART_DECODE_MODE);
            } else {
                options.put(IQHVCPlayerAdvanced.KEY_OPTION_DECODE_MODE, IQHVCPlayerAdvanced.LIVECLOUD_SOFT_DECODE_MODE);
            }
            qhvcPlayer.setDataSource(IQHVCPlayer.PLAYTYPE_VOD, url, channelId, "", options);
            //            qhvcPlayer.setDataSource(IQHVCPlayer.PLAYTYPE_VOD,
            //                    new String[] {
            //                            "resId0", "resId1", "resId2","resId3"
            //                    },
            //                    SN_SOURCE,
            //                    2,
            //                    getResources().getString(R.string.config_cid),
            //                    "",
            //                    options);
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
        qhvcPlayer.setOnVideoSizeChangedListener(new IQHVCPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(int handle, int width, int height) {
                Logger.w(TAG, "onInfo width: " + width + " ------height: " + height);
                videoWidth = width;
                videoHeight = height;
                if (playView != null) {
                    playView.setVideoRatio((float) width / (float) height);
                }
            }
        });
        qhvcPlayer.setOnInfoListener(new IQHVCPlayer.OnInfoListener() {
            @Override
            public void onInfo(int handle, int what, int extra) {
                Logger.w(TAG, "onInfo handle: " + handle + " what: " + what + " extra: " + extra);
                if (what == IQHVCPlayer.INFO_LIVE_PLAY_START) {
                    long endTick = System.currentTimeMillis();
                    Logger.d(TAG, "livecloud first render use tick: " + (endTick - mBeginTick));

                    printSdkVersion();
                } else if (what == IQHVCPlayer.INFO_DEVICE_RENDER_ERR) {
                    // err
                    if (Logger.LOG_ENABLE) {
                        Logger.e(TAG, "dvrender err");
                    }
                } else if (what == IQHVCPlayer.INFO_DEVICE_RENDER_QUERY_SURFACE) {

                    if (playView != null) {
                        if (qhvcPlayer != null && !qhvcPlayer.isPaused()) {
                            playView.render_proc(PlayerCallback.DEVICE_RENDER_QUERY_SURFACE, 0/*不使用此变量*/);
                        }
                    }
                } else if (what == IQHVCPlayer.INFO_RENDER_RESET_SURFACE) {

                    if (playView != null) {
                        playView.pauseSurface();
                    }
                }
            }
        });
        qhvcPlayer.setOnBufferingEventListener(new IQHVCPlayer.OnBufferingEventListener() {
            @Override
            public void onBufferingStart(int handle) {

                Log.w(TAG, "buffering event. start");
            }

            @Override
            public void onBufferingProgress(int handle, int progress) {
                Log.v(TAG, "buffering event. progress: " + progress);
            }

            @Override
            public void onBufferingStop(int handle) {
                Log.w(TAG, "buffering event. stop " + ((qhvcPlayer != null) ? qhvcPlayer.getCurrentPosition() : 0));

            }
        });
        qhvcPlayer.setOnErrorListener(new IQHVCPlayer.OnErrorListener() {
            @Override
            public boolean onError(int handle, int what, int extra) {
                Logger.w(TAG, "onError handle: " + handle + " what: " + what + " extra: " + extra);
                Toast.makeText(VodActivity.this, "error=" + what + " extra=" + extra, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        qhvcPlayer.setOnSeekCompleteListener(new IQHVCPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(int handle) {

                Logger.d(TAG, "seek complete");
            }
        });
        qhvcPlayer.setOnCompletionListener(new IQHVCPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(int handle) {

            }
        });
        qhvcPlayer.setOnPlayerNetStatsListener(new IQHVCPlayerAdvanced.OnPlayerNetStatsListener() {
            @Override
            public void onPlayerNetStats(int handle, long dvbps, long dabps, long dvfps, long dafps, long fps, long bitrate, long param1, long param2, long param3) {

                //Log.w(TAG, "dvbps: "  + dvbps + " dabps: " + dabps + " dvfps: " + dvfps + " dafps: " + dafps + " fps: " + fps +" bitrate: " +bitrate);

                downloadBitratePerSecond = dvbps + dabps;
                videoBitratePerSecond = bitrate;
                videoFrameRatePerSecond = fps;
            }
        });
        qhvcPlayer.setOnProgressChangeListener(new IQHVCPlayer.onProgressChangeListener() {
            @Override
            public void onProgressChange(int handle, final int total, final int progress) {
                if (progress != 0) {
                    sbProgress.setProgress(progress * 100 / total);
                } else {
                    sbProgress.setProgress(0);
                }

                tvPlayTime.setText(AndroidUtil.getTimeString(progress));
                tvDuration.setText(AndroidUtil.getTimeString(total));

                if (recordBeginTick > 0) {
                    tvHeaderCenter.setVisibility(View.VISIBLE);
                    tvHeaderCenter.setText(AndroidUtil.getTimeString(System.currentTimeMillis() - recordBeginTick));
                    Drawable drawableLeft = getResources().getDrawable(R.drawable.record_circle);
                    tvHeaderCenter.setCompoundDrawablesWithIntrinsicBounds(drawableLeft, null, null, null);
                    tvHeaderCenter.setCompoundDrawablePadding(7);
                } else {
                    tvHeaderCenter.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                    tvHeaderCenter.setVisibility(View.GONE);
                }

                showLog();
            }
        });
        qhvcPlayer.setOnBufferingUpdateListener(new IQHVCPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(int handle, int percent) {
                Log.d(TAG, "buffering: " + percent + " volume: " + qhvcPlayer.getVolume());
            }
        });
        qhvcPlayer.setOnSeekCompleteListener(new IQHVCPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(int handle) {
                Log.d(TAG, "onSeekComplete");
            }
        });

        try {
            mBeginTick = System.currentTimeMillis();
            qhvcPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, "prepareAsync 异常", Toast.LENGTH_SHORT).show();
        }
    }

    // 纵向显示-- 放大
    private void portZoomIn() {
        ViewGroup.LayoutParams layoutParams = rlPlayerContainer.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        rlPlayerContainer.setLayoutParams(layoutParams);
        rlPlayerContainer.postInvalidate();

        ViewGroup.LayoutParams videolayoutParams = playView.getLayoutParams();
        videolayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        videolayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        playView.setLayoutParams(videolayoutParams);
        if (qhvcPlayer != null)
            qhvcPlayer.setDisplay(playView);

        //        lvLog.setPadding(30, 140, 0, 0);
        logAdapter.setTextColorResId(R.color.white);
        logAdapter.notifyDataSetChanged();
        resolutionRatio.setVisibility(View.VISIBLE);
        changeSpeed.setVisibility(View.VISIBLE);
    }

    // 纵向显示-- 缩小
    private void portZoomOut() {

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density; // 屏幕密度（像素比例：0.75/1.0/1.5/2.0）
        int densityDPI = dm.densityDpi; // 屏幕密度（每寸像素：120/160/240/320）

        ViewGroup.LayoutParams layoutParams = rlPlayerContainer.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = (int) (density * 183.3);//ViewGroup.LayoutParams.MATCH_PARENT;
        rlPlayerContainer.setLayoutParams(layoutParams);
        rlPlayerContainer.postInvalidate();

        ViewGroup.LayoutParams videolayoutParams = playView.getLayoutParams();
        videolayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        videolayoutParams.height = (int) (density * 183.3);//ViewGroup.LayoutParams.MATCH_PARENT;
        playView.setLayoutParams(videolayoutParams);
        if (qhvcPlayer != null)
            qhvcPlayer.setDisplay(playView);

        //        lvLog.setPadding(30, 198, 0, 0);
        //        lvLog.setPadding(0, 0, 0, 0);
        logAdapter.setTextColorResId(R.color.color_666666);
        logAdapter.notifyDataSetChanged();
        resolutionRatio.setVisibility(View.GONE);
        changeSpeed.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void playerClose() {
        if (qhvcPlayer != null) {

            if (isRecording) {
                stopRecorder();
            }
            qhvcPlayer.stop();
            qhvcPlayer.release();
            qhvcPlayer = null;
        }
    }

    private void showLog() {
        if (mediaInformationMap == null || mediaInformationMap.isEmpty()) {
            mediaInformationMap = qhvcPlayer != null ? qhvcPlayer.getMediaInformation() : null;
        }

        logList.clear();

        logList.add("版本号: " + QHVCPlayer.getVersion());
        logList.add("播放url: " + url);
        if (mediaInformationMap != null && !mediaInformationMap.isEmpty()) {
            logList.add("分辨率: " + mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_VIDEO_WIDTH_INT) + "*" + mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_VIDEO_HEIGHT_INT));
            Logger.w(TAG, "-----with: " + mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_VIDEO_WIDTH_INT) +
                    "----- height: " + mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_VIDEO_HEIGHT_INT));
            logList.add("码率: " + videoBitratePerSecond / 1024 + "k");
            logList.add("帧率: " + videoFrameRatePerSecond);
            logList.add("音频格式: " + mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_AUDIO_FORMAT_STRING));
            logList.add("音频采样率: " + mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_AUDIO_SAMPLE_RATE_INT));
            logList.add("音频轨道: " + mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_AUDIO_CHANNEL_INT));
            logList.add("视频编码格式: " + mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_VIDEO_FORMAT_STRING));
        } else {
            logList.add("音频格式: ");
            logList.add("音频采样率: ");
            logList.add("音频轨道: ");
            logList.add("视频编码格式: ");
        }
        logList.add("下行流量: " + downloadBitratePerSecond / 1024 + "k");
        logList.add("网络类型: " + NetUtil.getNetWorkTypeToString(this));

        logAdapter.setList(logList);
        logAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (qhvcPlayer != null && (qhvcPlayer.isPlaying() || qhvcPlayer.isPaused())) {
            qhvcPlayer.disableRender(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (qhvcPlayer != null && (qhvcPlayer.isPlaying() || qhvcPlayer.isPaused())) {
            qhvcPlayer.disableRender(true);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);

        Log.w(TAG, "onConfigurationChanged");
        playView.stopRender();
        setContentView(R.layout.activity_vod);
        initView();
        if (qhvcPlayer != null) {
            playView.onPlay();
            playView.setPlayer(qhvcPlayer);
            qhvcPlayer.setDisplay(playView);

            if (videoWidth != 0 && videoHeight != 0) {
                playView.setVideoRatio((float) videoWidth / (float) videoHeight);
            }
        }
    }

    private void hideSystemNavigationBar() {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            View view = this.getWindow().getDecorView();
            view.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            //            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            //                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");

        playerClose();
        super.onBackPressed();
    }

    private boolean startRecorderGif(String filePath) {
        int ret = -1;
        if (qhvcPlayer != null) {

            Map<String, Object> map = qhvcPlayer.getMediaInformation();
            Logger.d(TAG, "media infomation: " + map.toString());
            GifRecordConfig config = new GifRecordConfig();
            config.setWidth((int) map.get(IQHVCPlayer.KEY_MEDIA_INFO_VIDEO_WIDTH_INT));
            config.setHeight((int) map.get(IQHVCPlayer.KEY_MEDIA_INFO_VIDEO_HEIGHT_INT));
            config.setOutputFps(5);
            config.setSampleInterval(100);

            ret = qhvcPlayer.startRecorder(filePath, IQHVCPlayerAdvanced.RECORDER_FORMAT_GIF, config, new IQHVCPlayerAdvanced.OnRecordListener() {
                @Override
                public void onRecordSuccess() {
                    Logger.d(TAG, "record gif success");
                }

                @Override
                public void onRecordFailed(int errorCode) {

                }
            });
            Logger.d(TAG, "start recorder. ret: " + ret);
        }
        return (ret == 0);
    }

    private boolean startRecorderVideo(String filePath) {
        int ret = -1;
        if (qhvcPlayer != null && qhvcPlayer.isPlaying()) {

            Map<String, Object> map = qhvcPlayer.getMediaInformation();
            Logger.d(TAG, "media infomation: " + map.toString());
            VideoRecordConfig config = new VideoRecordConfig();
            config.setWidth((int) map.get(IQHVCPlayer.KEY_MEDIA_INFO_VIDEO_WIDTH_INT));
            config.setHeight((int) map.get(IQHVCPlayer.KEY_MEDIA_INFO_VIDEO_HEIGHT_INT));
            config.setVideoBitrate((int) map.get(IQHVCPlayer.KEY_MEDIA_INFO_BITRATE_INT));
            config.setAudioSampleRate(44100);
            config.setAudioChannel(2);
            config.setAudioBitrate(32000 / 8);//32kB

            ret = qhvcPlayer.startRecorder(filePath, IQHVCPlayerAdvanced.RECORDER_FORMAT_MP4, config, new IQHVCPlayerAdvanced.OnRecordListener() {
                @Override
                public void onRecordSuccess() {
                    Logger.d(TAG, "record mp4 success");
                }

                @Override
                public void onRecordFailed(int errorCode) {

                }
            });
            Logger.d(TAG, "start recorder. ret: " + ret);
        }
        return (ret == 0);
    }

    private boolean stopRecorder() {
        int ret = -1;
        if (qhvcPlayer != null) {
            ret = qhvcPlayer.stopRecorder();
            Logger.d(TAG, "stop recorder. ret: " + ret);
        }
        return (ret == 0);
    }

    public boolean checkSelfPermissionAndRequest(String permission, int requestCode) {
        Logger.d(TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] {
                            permission
                    },
                    requestCode);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String permissions[], @NonNull int[] grantResults) {
        Logger.d(TAG, "onRequestPermissionsResult " + requestCode + " " + Arrays.toString(permissions) + " " + Arrays.toString(grantResults));
        switch (requestCode) {
            case PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mNeedStartPlayer) {
                        mNeedStartPlayer = false;
                        vodProxy();
                    }
                }
            }
                break;

            default:
                break;
        }
    }

    private void printSdkVersion() {
        if (qhvcPlayer instanceof QHVCPlayer) {
            Logger.d(TAG, "[player sdk] native: " + ((qhvcPlayer != null) ? ((QHVCPlayer) qhvcPlayer).getNativeVersion() : "") + " java: " + QHVCPlayer.getVersion());
        }
    }

    private void init() {

    }

    /**
     * 显示倍速PopWindow
     */
    private void showChangeSpeedPopWindow() {
        if (mChangeSpeedPopWindow == null) {
            View popView = LayoutInflater.from(this).inflate(R.layout.activity_vod_change_speedlayout, null);
            initChangeSpeedPopWindowView(popView);
            mChangeSpeedPopWindow = new PopupWindow(popView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
            mChangeSpeedPopWindow.setBackgroundDrawable(new ColorDrawable(0));
            mChangeSpeedPopWindow.setOutsideTouchable(true);
            mChangeSpeedPopWindow.setFocusable(true);
            mChangeSpeedPopWindow.setTouchable(true);
            mChangeSpeedPopWindow.setAnimationStyle(R.style.popupWindowAnimation);
            mChangeSpeedPopWindow.showAtLocation(playView, Gravity.CENTER, 0, 0);
            mChangeSpeedPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                }
            });
        } else {
            if (!mChangeSpeedPopWindow.isShowing()) {
                mChangeSpeedPopWindow.showAtLocation(playView, Gravity.CENTER, 0, 0);
            } else {
                mChangeSpeedPopWindow.dismiss();
            }
        }
    }

    private void initChangeSpeedPopWindowView(View popView) {
        speed1_0 = (TextView) popView.findViewById(R.id.vod_speed_1_0);
        speed1_0.setOnClickListener(this);
        speed1_5 = (TextView) popView.findViewById(R.id.vod_speed_1_5);
        speed1_5.setOnClickListener(this);
        speed2_0 = (TextView) popView.findViewById(R.id.vod_speed_2_0);
        speed2_0.setOnClickListener(this);
        speed1_0.setTextColor(getResources().getColor(R.color.change_speed_textclour));
    }

    /**
     * 显示切换分辨率PopWindow
     */
    private void showResolutionRatioPopWindow() {
        if (mResolutionRatioPopWindow == null) {
            View popView = LayoutInflater.from(this).inflate(R.layout.activity_vod_resolution_ratio_layout, null);
            initResolutionRatioPopWindowView(popView);
            mResolutionRatioPopWindow = new PopupWindow(popView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
            mResolutionRatioPopWindow.setBackgroundDrawable(new ColorDrawable(0));
            mResolutionRatioPopWindow.setOutsideTouchable(true);
            mResolutionRatioPopWindow.setFocusable(true);
            mResolutionRatioPopWindow.setTouchable(true);
            mResolutionRatioPopWindow.setAnimationStyle(R.style.popupWindowAnimation);
            mResolutionRatioPopWindow.showAtLocation(playView, Gravity.CENTER, 0, 0);
            mResolutionRatioPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                }
            });
            resolutionRatio.setText("高清");
        } else {
            if (!mResolutionRatioPopWindow.isShowing()) {
                mResolutionRatioPopWindow.showAtLocation(playView, Gravity.CENTER, 0, 0);
            } else {
                mResolutionRatioPopWindow.dismiss();
            }
        }
    }

    private void initResolutionRatioPopWindowView(View popView) {
        resoulution_1080p = (TextView) popView.findViewById(R.id.resoulution_1080p);
        resoulution_1080p.setOnClickListener(this);
        resoulution_720p = (TextView) popView.findViewById(R.id.resoulution_720p);
        resoulution_720p.setOnClickListener(this);
        resoulution_480p = (TextView) popView.findViewById(R.id.resoulution_480p);
        resoulution_480p.setOnClickListener(this);
        resoulution_320p = (TextView) popView.findViewById(R.id.resoulution_320p);
        resoulution_320p.setOnClickListener(this);
        resoulution_480p.setTextColor(getResources().getColor(R.color.change_speed_textclour));
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.vod_speed_1_0:
                setChangeSpeedPopSelect(speed1_0, 0);
                break;
            case R.id.vod_speed_1_5:
                setChangeSpeedPopSelect(speed1_5, 1);
                break;
            case R.id.vod_speed_2_0:
                setChangeSpeedPopSelect(speed2_0, 2);
                break;
            case R.id.resolution_ratio:
                showResolutionRatioPopWindow();
                break;
            case R.id.change_speed:
                showChangeSpeedPopWindow();
                break;
            case R.id.resoulution_1080p:
                setesoulutionRRatioPopSelect(resoulution_1080p, 0);
                break;
            case R.id.resoulution_720p:
                setesoulutionRRatioPopSelect(resoulution_720p, 1);
                break;
            case R.id.resoulution_480p:
                setesoulutionRRatioPopSelect(resoulution_480p, 2);
                break;
            case R.id.resoulution_320p:
                setesoulutionRRatioPopSelect(resoulution_320p, 3);
                break;

        }

    }

    private void setChangeSpeedPopSelect(TextView view, int index) {
        speed1_0.setTextColor(getResources().getColor(R.color.white));
        speed1_5.setTextColor(getResources().getColor(R.color.white));
        speed2_0.setTextColor(getResources().getColor(R.color.white));
        view.setTextColor(getResources().getColor(R.color.change_speed_textclour));
        qhvcPlayer.setPlayBackRate(mPlayRate[index]);
        mChangeSpeedPopWindow.dismiss();
    }

    private void setesoulutionRRatioPopSelect(TextView view, int index) {
        resoulution_1080p.setTextColor(getResources().getColor(R.color.white));
        resoulution_720p.setTextColor(getResources().getColor(R.color.white));
        resoulution_480p.setTextColor(getResources().getColor(R.color.white));
        resoulution_320p.setTextColor(getResources().getColor(R.color.white));
        view.setTextColor(getResources().getColor(R.color.change_speed_textclour));
        if (qhvcPlayer != null) {
            if (qhvcPlayer.isPaused()) {
                qhvcPlayer.start();
            }

            mChangeMessage.setText(SN_SOURCE_FLAG[index] + " 切换中...");
            qhvcPlayer.switchResolution(index, new IQHVCPlayerAdvanced.QHVCSwitchResolutionListener() {
                @Override
                public void onPrepare() {
                    Logger.e(TAG, "[hand] switch prepare...");
                }

                @Override
                public void onStart() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
                }

                @Override
                public void onSuccess(final int index, final String url) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(VodActivity.this, "success", Toast.LENGTH_SHORT).show();
                            mChangeMessage.setText("");
                            resolutionRatio.setText(SN_SOURCE_FLAG[index]);
                            PlayerLogger.i(TAG, "-----index:" + index + "-----url:" + url);
                            mediaInformationMap = qhvcPlayer.getMediaInformation();

                        }
                    });
                }

                @Override
                public void onError(final int errorCode, final String errorMsg) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mChangeMessage.setText("");
                            Toast.makeText(VodActivity.this, "error: " + errorCode + " " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }

        mResolutionRatioPopWindow.dismiss();
    }
}
