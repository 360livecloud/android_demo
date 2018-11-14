
package com.qihoo.videocloud.player.preview;

import android.app.Activity;
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
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.qihoo.livecloud.sdk.QHVCSdk;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.tools.MD5;
import com.qihoo.livecloud.tools.NetUtil;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.IQHVCPlayer;
import com.qihoo.videocloud.IQHVCPlayerAdvanced;
import com.qihoo.videocloud.QHVCPlayer;
import com.qihoo.videocloud.player.LogAdapter;
import com.qihoo.videocloud.player.PlayConstant;
import com.qihoo.videocloud.utils.NoDoubleClickListener;
import com.qihoo.videocloud.widget.ViewHeader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qihoo.videocloud.player.PlayConstant.SHOW_MODEL_LAND;
import static com.qihoo.videocloud.player.PlayConstant.SHOW_MODEL_PORT;
import static com.qihoo.videocloud.player.PlayConstant.SHOW_MODEL_PORT_SMALL;

public class LiveVideoViewActivity extends Activity {

    private final static String TAG = LiveVideoViewActivity.class.getSimpleName();

    private @PlayConstant.ShowModel int currentShowModel = SHOW_MODEL_PORT_SMALL;

    private boolean haveAddress;
    private String businessId;
    private String channelId;
    private String sn;
    private String url;
    private boolean autoDecoded;

    private QHVCVideoView playView;
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
        setContentView(R.layout.activity_live_videoview);

        initView();
        initData();
        live();
    }

    private void initView() {

        rlPlayerContainer = (RelativeLayout) findViewById(R.id.rl_player_container);
        playView = (QHVCVideoView) findViewById(R.id.playView);
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

                        Toast.makeText(LiveVideoViewActivity.this, "cannot zoom. width: " + videoWidth + " height: " + videoHeight, Toast.LENGTH_SHORT).show();
                    }
                } else if (currentShowModel == SHOW_MODEL_PORT) {

                    currentShowModel = SHOW_MODEL_PORT_SMALL;
                    portZoomOut(false);
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

        //        lvLog.setPadding(30, 140, 0, 0);
        logAdapter.setTextColorResId(R.color.white);
        logAdapter.notifyDataSetChanged();
    }

    // 纵向显示-- 缩小
    private void portZoomOut(boolean fromLand) {

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density; // 屏幕密度（像素比例：0.75/1.0/1.5/2.0）
        int densityDPI = dm.densityDpi; // 屏幕密度（每寸像素：120/160/240/320）

        ViewGroup.LayoutParams layoutParams = rlPlayerContainer.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = (int) (density * 183.3);//ViewGroup.LayoutParams.MATCH_PARENT;
        rlPlayerContainer.setLayoutParams(layoutParams);
        rlPlayerContainer.postInvalidate();

        if (fromLand) {
            FrameLayout.LayoutParams lgLogLayoutParams = new FrameLayout.LayoutParams(lvLog.getLayoutParams());
            lgLogLayoutParams.setMargins((int) (density * 14.6), (int) (density * 198), 0, 0);
            lgLogLayoutParams.height = (int) (density * 346.6);
            lvLog.setLayoutParams(lgLogLayoutParams);
        }
        logAdapter.setTextColorResId(R.color.color_666666);
        logAdapter.notifyDataSetChanged();
    }

    // 横向显示放大
    private void landZoomIn() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density; // 屏幕密度（像素比例：0.75/1.0/1.5/2.0）
        int densityDPI = dm.densityDpi; // 屏幕密度（每寸像素：120/160/240/320）

        ViewGroup.LayoutParams layoutParams = rlPlayerContainer.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        rlPlayerContainer.setLayoutParams(layoutParams);
        rlPlayerContainer.postInvalidate();

        FrameLayout.LayoutParams lgLogLayoutParams = new FrameLayout.LayoutParams(lvLog.getLayoutParams());
        lgLogLayoutParams.setMargins((int) (density * 18.3), (int) (density * 42.6), 0, (int) (density * 42));
        lgLogLayoutParams.height = (int) (density * 240);
        lvLog.setLayoutParams(lgLogLayoutParams);
        logAdapter.setTextColorResId(R.color.white);
        logAdapter.notifyDataSetChanged();
    }

    private void initData() {
        Intent i = getIntent();
        haveAddress = i.getBooleanExtra("haveAddress", Boolean.FALSE);
        if (haveAddress) {
            url = i.getStringExtra("url");
        } else {
            businessId = i.getStringExtra("businessId");
            channelId = i.getStringExtra("channelId");
            sn = i.getStringExtra("sn");
        }
        autoDecoded = i.getBooleanExtra("autoDecoded", Boolean.FALSE);

        if (!TextUtils.isEmpty(businessId)) {
            QHVCSdk.getInstance().getConfig().setBusinessId(businessId);
        }
        // 设置用户id
        QHVCSdk.getInstance().getConfig().setUserId("Duj2599");
    }

    private void live() {

        String sign = getSign();

        playView.setOnAudioPCMListener(new IQHVCPlayerAdvanced.OnAudioPCMListener() {
            @Override
            public void onAudioPCM(int handle, int id, ByteBuffer buffer, long timestamp, int channels, int sampleRate, int bitsPerSample) {

            }
        });
        try {
            if (haveAddress) {
                playView.setDataSource(IQHVCPlayer.PLAYTYPE_LIVE, url, null);
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
                playView.setDataSource(IQHVCPlayer.PLAYTYPE_LIVE, sn, channelId, sign, options);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, "数据源异常", Toast.LENGTH_SHORT).show();
            return;
        }

        playView.setOnPreparedListener(new IQHVCPlayer.OnPreparedListener() {
            @Override
            public void onPrepared() {

                playView.start();
            }
        });
        playView.setOnVideoSizeChangedListener(new IQHVCPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(int handle, int width, int height) {
                videoWidth = width;
                videoHeight = height;
                if (playView != null) {
                    playView.setVideoRatio((float) width / (float) height);
                }
            }
        });
        playView.setOnPlayerNetStatsListener(new IQHVCPlayerAdvanced.OnPlayerNetStatsListener() {
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
        playView.setOnInfoListener(new IQHVCPlayer.OnInfoListener() {
            @Override
            public void onInfo(int handle, int what, int extra) {

            }
        });

        playView.setOnErrorListener(new IQHVCPlayer.OnErrorListener() {
            @Override
            public boolean onError(int handle, int what, int extra) {
                Logger.w(TAG, "onError handle: " + handle + " what: " + what + " extra: " + extra);
                Toast.makeText(LiveVideoViewActivity.this, "error=" + what + " extra=" + extra, Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        try {
            playView.prepareAsync();
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, "prepareAsync 异常", Toast.LENGTH_SHORT).show();
            return;
        }

    }

    private void playerClose() {
        if (playView != null) {
            playView.stopRender();
            playView.stop();
            playView.release();
            playView = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playView != null) {
            try {
                playView.disableRender(false);
            } catch (IllegalStateException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (playView != null) {
            try {
                playView.disableRender(true);
            } catch (IllegalStateException e) {
                Log.e(TAG, e.getMessage());
            }
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
            mediaInformationMap = playView.getMediaInformation();
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

        Log.w(TAG, "onConfigurationChanged");

        // TODO: 2017/8/17 检查是否需要重新布局
        switch (currentShowModel) {
            case SHOW_MODEL_LAND: {
                landZoomIn();
            }
                break;

            case SHOW_MODEL_PORT_SMALL: {
                portZoomOut(true);
            }
                break;

            default:
                break;
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
