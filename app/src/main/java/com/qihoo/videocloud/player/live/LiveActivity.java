
package com.qihoo.videocloud.player.live;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.qihoo.livecloud.play.callback.PlayerCallback;
import com.qihoo.livecloud.sdk.QHVCSdk;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.tools.MD5;
import com.qihoo.livecloud.tools.NetUtil;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.IQHVCPlayer;
import com.qihoo.videocloud.IQHVCPlayerAdvanced;
import com.qihoo.videocloud.QHVCPlayer;
import com.qihoo.videocloud.QHVCPlayerPlugin;
import com.qihoo.videocloud.player.LogAdapter;
import com.qihoo.videocloud.player.PlayConstant;
import com.qihoo.videocloud.utils.NoDoubleClickListener;
import com.qihoo.videocloud.view.QHVCTextureView;
import com.qihoo.videocloud.widget.ViewHeader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qihoo.videocloud.player.PlayConstant.SHOW_MODEL_LAND;
import static com.qihoo.videocloud.player.PlayConstant.SHOW_MODEL_PORT;
import static com.qihoo.videocloud.player.PlayConstant.SHOW_MODEL_PORT_SMALL;

public class LiveActivity extends Activity {

    private final static String TAG = LiveActivity.class.getSimpleName();

    private
    @PlayConstant.ShowModel
    int currentShowModel = SHOW_MODEL_PORT_SMALL;

    private IQHVCPlayerAdvanced qhvcPlayer;

    private boolean haveAddress;
    private String businessId;
    private String channelId;
    private String sn;
    private String url;
    private boolean autoDecoded;

    private QHVCTextureView playView;
    private RelativeLayout rlPlayerContainer;
    private ViewHeader viewHeaderMine;
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

    // 播放信息
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        hideSystemNavigationBar();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        initView();
        initData();
        liveProxy();
    }

    private void initView() {
        rlPlayerContainer = (RelativeLayout) findViewById(R.id.rl_player_container);
        playView = (QHVCTextureView) findViewById(R.id.playView);
        viewHeaderMine = (ViewHeader) findViewById(R.id.viewHeaderMine);
        viewHeaderMine.setLeftText("直播");
        viewHeaderMine.getLeftIcon().setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                playerClose();
                finish();
            }
        });

        lvLog = (ListView) findViewById(R.id.lv_log);
        if (currentShowModel == SHOW_MODEL_LAND || currentShowModel == SHOW_MODEL_PORT) {
            logAdapter = new LogAdapter(this, logList, R.color.white);
        } else {
            logAdapter = new LogAdapter(this, logList, R.color.color_666666);
        }
        lvLog.setAdapter(logAdapter);

        ivZoom = (View) findViewById(R.id.iv_zoom);
        ivZoom.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                if (currentShowModel == SHOW_MODEL_PORT_SMALL) {

                    if (videoWidth != 0 && videoHeight != 0) {

                        Log.v(TAG, "width: " + videoWidth + " height: " + videoHeight);
                        if (videoHeight > videoWidth) {

                            currentShowModel = SHOW_MODEL_PORT;
                            portZoomIn();
                        } else {

                            currentShowModel = SHOW_MODEL_LAND;
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        }
                    } else {

                        Toast.makeText(LiveActivity.this, "cannot zoom. width: " + videoWidth + " height: " + videoHeight, Toast.LENGTH_SHORT).show();
                    }
                } else if (currentShowModel == SHOW_MODEL_PORT) {

                    currentShowModel = SHOW_MODEL_PORT_SMALL;
                    portZoomOut();
                } else if (currentShowModel == SHOW_MODEL_LAND) {

                    currentShowModel = SHOW_MODEL_PORT_SMALL;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                // SHOW_MODEL_PORT have not zoom
            }
        });
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
        logAdapter.setTextColorResId(R.color.color_666666);
        logAdapter.notifyDataSetChanged();
    }

    private void initData() {
        Intent i = getIntent();
        haveAddress = i.getBooleanExtra("haveAddress", Boolean.FALSE);
        businessId = i.getStringExtra("businessId");
        channelId = i.getStringExtra("channelId");
        if (haveAddress) {
            url = i.getStringExtra("url");
            channelId = i.getStringExtra("channelId");
        } else {
            sn = i.getStringExtra("sn");
        }
        autoDecoded = i.getBooleanExtra("autoDecoded", Boolean.FALSE);

        if (!TextUtils.isEmpty(businessId)) {
            QHVCSdk.getInstance().getConfig().setBusinessId(businessId);
        }
        // 设置用户id
        QHVCSdk.getInstance().getConfig().setUserId("Duj2599");
    }

    private void liveProxy() {
        final QHVCPlayerPlugin qhvcPlayerPlugin = QHVCPlayerPlugin.getInstance();

        //若第三方未将播放器需要的so文件捆包，必须设置setDefaultPluginInstalled(false)
        qhvcPlayerPlugin.setDefaultPluginInstalled(true);

        if (qhvcPlayerPlugin.isDefaultPluginInstalled()) {
            live();
        } else if (qhvcPlayerPlugin.isPluginInstalled()) {
            int result = qhvcPlayerPlugin.loadPlugin();
            if (result == QHVCPlayerPlugin.ERROR_SUCCESS) {
                live();
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
                        live();
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

    private void live() {

        String sign = getSign();

        qhvcPlayer = new QHVCPlayer(this);
        playView.onPlay();
        playView.setPlayer(qhvcPlayer);
        qhvcPlayer.setDisplay(playView);

        qhvcPlayer.setOnAudioPCMListener(new IQHVCPlayerAdvanced.OnAudioPCMListener() {
            @Override
            public void onAudioPCM(int handle, int id, ByteBuffer buffer, long timestamp, int channels, int sampleRate, int bitsPerSample) {

            }
        });
        try {
            if (haveAddress) {
                qhvcPlayer.setDataSource(IQHVCPlayer.PLAYTYPE_LIVE, url, channelId);
            } else {
                Map<String, Object> options = new HashMap<>();
                //                options.put(IQHVCPlayerAdvanced.KEY_OPTION_MUTE, true);
                //                options.put(IQHVCPlayerAdvanced.KEY_OPTION_POSITION, 30);
                //                options.put(IQHVCPlayerAdvanced.KEY_OPTION_PLAY_MODE, IQHVCPlayerAdvanced.PLAYMODE_LOWLATENCY);
                //                options.put(IQHVCPlayerAdvanced.KEY_OPTION_RENDER_MODE, IQHVCPlayerAdvanced.RENDER_MODE_FULL);
                if (autoDecoded) {
                    options.put(IQHVCPlayerAdvanced.KEY_OPTION_DECODE_MODE, IQHVCPlayerAdvanced.LIVECLOUD_SMART_DECODE_MODE);
                } else {
                    options.put(IQHVCPlayerAdvanced.KEY_OPTION_DECODE_MODE, IQHVCPlayerAdvanced.LIVECLOUD_SOFT_DECODE_MODE);
                }
                qhvcPlayer.setDataSource(IQHVCPlayer.PLAYTYPE_LIVE, sn, channelId, sign, options);
            }

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
                videoWidth = width;
                videoHeight = height;
                if (playView != null) {
                    playView.setVideoRatio((float) width / (float) height);
                }
            }
        });
        qhvcPlayer.setOnPlayerNetStatsListener(new IQHVCPlayerAdvanced.OnPlayerNetStatsListener() {
            @Override
            public void onPlayerNetStats(int handle, long dvbps, long dabps, long dvfps, long dafps, long fps, long bitrate, long param1, long param2, long param3) {

                //Log.e(TAG, "dvbps: "  + dvbps + " dabps: " + dabps + " dvfps: " + dvfps + " dafps: " + dafps + " fps: " + fps +" bitrate: " +bitrate);

                downloadBitratePerSecond = dvbps + dabps;
                videoBitratePerSecond = bitrate;
                videoFrameRatePerSecond = fps;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLog();
                    }
                });
            }
        });
        qhvcPlayer.setOnInfoListener(new IQHVCPlayer.OnInfoListener() {
            @Override
            public void onInfo(int handle, int what, int extra) {
                Logger.w(TAG, "onInfo handle: " + handle + " what: " + what + " extra: " + extra);
                if (what == IQHVCPlayer.INFO_LIVE_PLAY_START) {

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
        qhvcPlayer.setOnErrorListener(new IQHVCPlayer.OnErrorListener() {
            @Override
            public boolean onError(int handle, int what, int extra) {
                Logger.w(TAG, "onError handle: " + handle + " what: " + what + " extra: " + extra);
                Toast.makeText(LiveActivity.this, "error=" + what + " extra=" + extra, Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        try {
            qhvcPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, "prepareAsync 异常", Toast.LENGTH_SHORT).show();
            return;
        }

    }

    private void playerClose() {
        if (qhvcPlayer != null) {
            playView.stopRender();
            qhvcPlayer.stop();
            qhvcPlayer.release();
            qhvcPlayer = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (qhvcPlayer != null) {
            try {
                qhvcPlayer.disableRender(false);
            } catch (IllegalStateException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        if (playView != null) {
            playView.resumeSurface();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (qhvcPlayer != null) {
            try {
                qhvcPlayer.disableRender(true);
            } catch (IllegalStateException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        if (playView != null) {
            playView.pauseSurface();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private String getSign() {
        String token = null;
        if (channelId != null && channelId.equals("live_yingshi")) {
            token = "channel__" + channelId + "sn__" + sn + "key_" + "0Zjurl^y5t{6O;<6L";
        } else {
            token = "channel__" + channelId + "sn__" + sn + "key_" + "2Zjurl^y5t{6O;<6L";
        }
        return MD5.encryptMD5(token);
    }

    private void showLog() {
        if (mediaInformationMap == null || mediaInformationMap.isEmpty()) {
            mediaInformationMap = qhvcPlayer != null ? qhvcPlayer.getMediaInformation() : null;
        }

        logList.clear();

        logList.add("版本号: " + QHVCPlayer.getVersion());
        if (haveAddress) {
            logList.add("播放url: " + url);
        } else {
            logList.add("播放url: " + sn);
        }
        logList.add("分辨率: " + videoWidth + "*" + videoHeight);
        logList.add("码率: " + videoBitratePerSecond / 1024 + "k");
        logList.add("帧率: " + videoFrameRatePerSecond);
        if (mediaInformationMap != null && !mediaInformationMap.isEmpty()) {
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
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);

        playView.stopRender();

        setContentView(R.layout.activity_live);
        initView();

        if (qhvcPlayer != null){
            playView.onPlay();
            playView.setPlayer(qhvcPlayer);
            qhvcPlayer.setDisplay(playView);
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
}
