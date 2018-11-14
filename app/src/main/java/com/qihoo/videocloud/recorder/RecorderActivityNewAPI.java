
package com.qihoo.videocloud.recorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo.faceapi.QhFaceApi;
import com.qihoo.faceapi.util.QhUtils;
import com.qihoo.livecloud.livekit.api.QHVCConstants;
import com.qihoo.livecloud.livekit.api.QHVCFaceUCallBack;
import com.qihoo.livecloud.livekit.api.QHVCLiveKit;
import com.qihoo.livecloud.livekit.api.QHVCLiveKitAdvanced;
import com.qihoo.livecloud.livekit.api.QHVCMediaSettings;
import com.qihoo.livecloud.livekit.api.QHVCPublishSettings;
import com.qihoo.livecloud.livekit.api.QHVCRecorderCallBack;
import com.qihoo.livecloud.livekit.api.QHVCSurfaceView;
import com.qihoo.livecloud.recorder.logUtil.RecorderLogger;
import com.qihoo.livecloud.recorder.setting.BaseSettings;
import com.qihoo.livecloud.recorder.setting.MediaSettings;
import com.qihoo.livecloud.sdk.QHVCSdk;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.recorder.adapter.FaceURecylerViewAdapter;
import com.qihoo.videocloud.stats.tool.PublishStatsTool;
import com.qihoo.videocloud.utils.AndroidUtil;
import com.qihoo.videocloud.utils.QHVCSharedPreferences;
import com.qihoo.videocloud.view.BaseDialog;
import com.qihoo.videocloud.view.BeautyPopWindow;
import com.qihoo.videocloud.view.FaceUPopWindow;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 采集
 * Created by huchengming
 */
public class RecorderActivityNewAPI extends Activity implements View.OnClickListener {
    private final static String TAG = "RecorderActivityNewAPI";
    private QHVCSurfaceView mSurfaceView;
    private QHVCLiveKitAdvanced mQhvcLiveKitAdvanced;
    private ImageView finishButton;
    private ImageView startButton;
    private ImageView muteButton;
    private ImageView changeCameraButton;
    private ImageView beautyButton;
    private ImageView mirrorButton;
    private ImageView faceUButton;
    private ImageView cameraEnableButton;
    private boolean isFirstStart = true;/*是否是第一次开始*/
    private boolean isFirstIn = true;/*是否是第一次进入*/
    private LinearLayout controlerLayout;
    private ScrollView messageLayout;
    private TextView recordTimeTextView;
    private int recordTimeCount = 0;

    /*popWindow View*/
    private BeautyPopWindow mBeautyPopWindow;

    /*FaceUWindow view*/
    private FaceUPopWindow mFaceUPopWindow;

    /*recordMessage View*/
    private TextView sdkVersionTextView;
    private TextView rtmpTextView;
    private TextView encoderTypeTextView;
    private TextView encodeMethodTextView;
    private TextView resolutionRatioTextView;
    private TextView codeRateTextView;
    private TextView fpsTextView;
    private TextView videoStoreTextView;
    private TextView videoLostTextView;
    private TextView netTypeTextView;
    private TextView transportSpeedTextView;

    // 确认退出dialog
    private TextView mExitCancel;
    private TextView mExitConfirm;
    private BaseDialog mExitDialog;

    private boolean horizontalBoolean;
    private QHVCSharedPreferences sharedPreferences;
    private String channelId;/*渠道id*/
    private String URL;/*推流地址*/
    private String title;/*推流标题*/
    private static final int FLING_MIN_DISTANCE = 200;//  滑动最小距离
    private static final int FLING_MIN_VELOCITY = 200;// 滑动最大速度
    private GestureDetector mGestureDetector;
    //    private final String RTMP = "rtmp://ps4.live.huajiao.com/live_huajiao_v2/_LC_ps4_non_1000764515100385791154386_OX?sign=9b53c9784a9b0ca40a5bd2b2f8e5d2a7&ts=1510038579";
    private final String RTMP = "rtmp://ps0.live.huajiao.com/live_huajiao_v2/_LC_ps0_non_10011179150233398715" + new Random().nextInt(10000);
    private final String SAVEVIDEOFILEPATH = "/sdcard/123.mp4";
    private String encodeTypeSp;
    private String resolutionRatioSp;
    private boolean onlyVoiceBoolean;/*是否是纯音频*/


    /*面部特征点相关start 测试用*/
    private boolean DRAWFACAPOINTS = false;/*绘制面部特征点 （测试用）*/
    private SurfaceView mOverlap;
    public Matrix matrix;
    private Thread thread;
    private boolean killed = false;
    private PointF[] mPointFace = null;//人脸识别的face点
    /*面部特征点相关end*/

    private ExecutorService workThreadPoolExecutor = Executors.newSingleThreadExecutor();
    private PublishStatsTool mPublishStatsTool;/*推流打点*/

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        RecorderLogger.i(TAG, "LiveCloud--------onCreate");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);

        initData();
        initView();
        initAPI();
        showRecordMessage();
        mQhvcLiveKitAdvanced.prepare();/*准备*/
        mQhvcLiveKitAdvanced.startPreview();/*开始预览*/
        createPhoneListener();/*监听来电*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        RecorderLogger.i(TAG, "LiveCloud--------onResume");
        if (DRAWFACAPOINTS) {/*测试代码*/
            drawFacePoints();
        }
        if (!isFirstIn) {/*不能放在onRestart做，因为奇酷手机接听电话回来 不会调用onRestart方法*/
            mQhvcLiveKitAdvanced.resumePreview();
        }
        isFirstIn = false;
        mQhvcLiveKitAdvanced.userForeground();/*切前台打点*/
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        RecorderLogger.i(TAG, "LiveCloud--------onRestart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        RecorderLogger.i(TAG, "LiveCloud--------onPause");

        if (DRAWFACAPOINTS) {/*测试代码*/
            killed = true;
            if (thread != null) {
                try {
                    thread.join(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        if (mQhvcLiveKitAdvanced.isPublishState()) {/*如果是推流状态暂停推流*/
            mQhvcLiveKitAdvanced.pausePublish();
        }
        mQhvcLiveKitAdvanced.pausePreview();/*暂停预览*/
        stopClick();/*暂停录制计时*/
        mQhvcLiveKitAdvanced.userBackground();/*切后台打点*/
    }

    @Override
    protected void onDestroy() {
        RecorderLogger.i(TAG, "LiveCloud--------onDestroy");
        super.onDestroy();

    }

    @Override
    public void onBackPressed() {
        RecorderLogger.i(TAG, "LiveCloud--------onBackPressed");
        exitDialog();
    }

    @Override
    public void finish() {
        super.finish();
        release();/*回收资源*/
    }

    private void initView() {
        if (horizontalBoolean) {/*设置横竖屏*/
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.recorder_activity_new_api);
        mSurfaceView = (QHVCSurfaceView) findViewById(R.id.surfaceView1);
        finishButton = (ImageView) findViewById(R.id.record_finish);
        finishButton.setOnClickListener(this);
        recordTimeTextView = (TextView) findViewById(R.id.record_time);
        mirrorButton = (ImageView) findViewById(R.id.new_api_mirror_button);
        mirrorButton.setOnClickListener(this);
        muteButton = (ImageView) findViewById(R.id.new_api_mute_button);
        muteButton.setOnClickListener(this);
        changeCameraButton = (ImageView) findViewById(R.id.new_api_change_camera_button);
        changeCameraButton.setOnClickListener(this);
        startButton = (ImageView) findViewById(R.id.new_api_start_button);
        startButton.setOnClickListener(this);
        beautyButton = (ImageView) findViewById(R.id.new_api_beauty_button);
        beautyButton.setOnClickListener(this);
        faceUButton = (ImageView) findViewById(R.id.new_api_faceU_button);
        faceUButton.setOnClickListener(this);
        cameraEnableButton = (ImageView) findViewById(R.id.new_api_enable_camera_button);
        cameraEnableButton.setOnClickListener(this);
        controlerLayout = (LinearLayout) findViewById(R.id.recorder_controler_layout);
        messageLayout = (ScrollView) findViewById(R.id.record_message_layout);
        messageLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });
        mGestureDetector = new GestureDetector(this, learnGestureListener);
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });

        sdkVersionTextView = (TextView) findViewById(R.id.record_message_sdk_version);
        rtmpTextView = (TextView) findViewById(R.id.record_message_rtmp);
        encoderTypeTextView = (TextView) findViewById(R.id.record_message_encoder_type);
        encodeMethodTextView = (TextView) findViewById(R.id.record_message_encode_method);
        resolutionRatioTextView = (TextView) findViewById(R.id.record_message_resolution_ratio);
        codeRateTextView = (TextView) findViewById(R.id.record_message_code_rate);
        fpsTextView = (TextView) findViewById(R.id.record_message_fps);
        videoStoreTextView = (TextView) findViewById(R.id.record_message_video_store);
        videoLostTextView = (TextView) findViewById(R.id.record_message_video_lost);
        netTypeTextView = (TextView) findViewById(R.id.record_message_net_type);
        transportSpeedTextView = (TextView) findViewById(R.id.record_message_transport_speed);
        if (DRAWFACAPOINTS) {/*测试代码*/
            mOverlap = (SurfaceView) findViewById(R.id.surfaceViewOverlap);
            mOverlap.setVisibility(View.VISIBLE);
            mOverlap.setZOrderOnTop(true);
            mOverlap.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            mOverlap.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceChanged(SurfaceHolder holder, int format,
                                           int width, int height) {
                    if (matrix == null) {
                        matrix = new Matrix();
                        matrix.postScale(-1, 1); //测试前置摄像头镜像水平翻转
                    }
                    matrix.setScale(width / 90, height / 160);
                    RecorderLogger.i(TAG, "LiveCloud-----width:" + width + "----height:" + height);
                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                }

            });
        }
    }

    private void initData() {
        Intent dataIntent = getIntent();
        String businessId = dataIntent.getStringExtra(RecorderConstants.BUSINESS_ID);
        channelId = dataIntent.getStringExtra(RecorderConstants.CHANNEL_ID);
        URL = dataIntent.getStringExtra(RecorderConstants.URL);
        title = dataIntent.getStringExtra(RecorderConstants.TITLE);
        if (!TextUtils.isEmpty(businessId)) {
            QHVCSdk.getInstance().getConfig().setBusinessId(businessId);
        }
        sharedPreferences = QHVCSharedPreferences.getInstence();
        horizontalBoolean = sharedPreferences.getBoolean(RecorderConstants.CHOICE_HORIZONTAL, false);
        mPublishStatsTool = new PublishStatsTool();
    }

    private void initAPI() {
        onlyVoiceBoolean = sharedPreferences.getBoolean(RecorderConstants.CHOICE_ONLY_VOICE, false);
        boolean saveVideoBoolean = sharedPreferences.getBoolean(RecorderConstants.SAVE_VIDEO_FILE, false);
        resolutionRatioSp = sharedPreferences.getString(RecorderConstants.RESOLUTION_RATIO, "360P");
        encodeTypeSp = sharedPreferences.getString(RecorderConstants.ENCODE_TYPE, "H.264");
        String videoFpsSp = sharedPreferences.getString(RecorderConstants.VIDEO_FPS, "15Hz");
        int codeRateSp = sharedPreferences.getInt(RecorderConstants.CODE_RATE, 200);
        boolean autoAdjustCodeRateSp = sharedPreferences.getBoolean(RecorderConstants.AUTO_ADJUST_CODE_RATE, false);
        String audioCodeRateSp = sharedPreferences.getString(RecorderConstants.AUDIO_CODE_RATE, "32kbps");
        String audioSampleSp = sharedPreferences.getString(RecorderConstants.AUDIO_SAMPLE, "22.05KHz");

        mQhvcLiveKitAdvanced = QHVCLiveKitAdvanced.getInstance(this.getApplicationContext());
        mQhvcLiveKitAdvanced.setChannelId(channelId);/*设置渠道Id*/
        QHVCMediaSettings.Builder mQHVCMediaSettingsBuilder = new QHVCMediaSettings.Builder();
        if (onlyVoiceBoolean) {/*纯音频*/
            mQHVCMediaSettingsBuilder.setInputVideoFormat(BaseSettings.EVideoCodecID.V_CODEC_ID_NO_VIDEO);
            mQHVCMediaSettingsBuilder.setOutputVideoFormat(MediaSettings.EVideoCodecID.V_CODEC_ID_H264);
            mQhvcLiveKitAdvanced.setEncodeMethod(QHVCConstants.RecorderConstants.ENCODE_SOFTWARE);/*设置编码方式（硬编或者软编）*/
        } else {
            if (mQhvcLiveKitAdvanced != null) {
                if (mQhvcLiveKitAdvanced.isSupportHardwareCoding()) {/*支持硬编*/
                    mQhvcLiveKitAdvanced.setEncodeMethod(QHVCConstants.RecorderConstants.ENCODE_HARDWARE);/*设置编码方式（硬编或者软编）*/
                    if ("H.264".equals(encodeTypeSp)) {/*H.264*/
                        mQHVCMediaSettingsBuilder.setInputVideoFormat(BaseSettings.EVideoCodecID.V_CODEC_ID_H264)/*硬编h264*/
                                .setOutputVideoFormat(BaseSettings.EVideoCodecID.V_CODEC_ID_H264);
                    } else if ("H.265".equals(encodeTypeSp)) {/*H.265*/
                        mQHVCMediaSettingsBuilder.setInputVideoFormat(BaseSettings.EVideoCodecID.V_CODEC_ID_I420)/*硬编美颜h265*/
                                .setOutputVideoFormat(BaseSettings.EVideoCodecID.V_CODEC_ID_HEVC);
                    }
                } else {/*软编*/
                    mQhvcLiveKitAdvanced.setEncodeMethod(QHVCConstants.RecorderConstants.ENCODE_SOFTWARE);/*设置编码方式（硬编或者软编）*/
                    if ("H.264".equals(encodeTypeSp)) {/*H.264*/
                        mQHVCMediaSettingsBuilder.setInputVideoFormat(BaseSettings.EVideoCodecID.V_CODEC_ID_NV21)/*硬编h264*/
                                .setOutputVideoFormat(BaseSettings.EVideoCodecID.V_CODEC_ID_H264);
                    } else if ("H.265".equals(encodeTypeSp)) {/*H.265*/
                        mQHVCMediaSettingsBuilder.setInputVideoFormat(BaseSettings.EVideoCodecID.V_CODEC_ID_NV21)/*硬编美颜h265*/
                                .setOutputVideoFormat(BaseSettings.EVideoCodecID.V_CODEC_ID_HEVC);
                    }
                    if ("360P".equals(resolutionRatioSp)) {/*软编设置编码尺寸*/
                        mQHVCMediaSettingsBuilder.setCodecWidth(360);
                        mQHVCMediaSettingsBuilder.setCodecHeight(640);
                    } else if ("480P".equals(resolutionRatioSp)) {
                        mQHVCMediaSettingsBuilder.setCodecWidth(480);
                        mQHVCMediaSettingsBuilder.setCodecHeight(854);
                    } else if ("640P".equals(resolutionRatioSp)) {
                        mQHVCMediaSettingsBuilder.setCodecWidth(640);
                        mQHVCMediaSettingsBuilder.setCodecHeight(1136);
                    } else if ("720P".equals(resolutionRatioSp)) {
                        mQHVCMediaSettingsBuilder.setCodecWidth(720);
                        mQHVCMediaSettingsBuilder.setCodecHeight(1280);
                    }
                }
            }
        }

        if ("15Hz".equals(videoFpsSp)) {/*设置帧率*/
            mQHVCMediaSettingsBuilder.setFps(15);
        } else {
            mQHVCMediaSettingsBuilder.setFps(30);
        }
        mQhvcLiveKitAdvanced.setBitrate(codeRateSp * 1024);/*设置码率*/

        if (autoAdjustCodeRateSp) {/*码率自适应*/
            mQhvcLiveKitAdvanced.setAutoAdjustState(1);
        } else {
            mQhvcLiveKitAdvanced.setAutoAdjustState(0);
        }

        if ("32kbps".equals(audioCodeRateSp)) {/*音频码率*/
            mQHVCMediaSettingsBuilder.setTargetBitrate(32000);
        } else if ("48kbps".equals(audioCodeRateSp)) {
            mQHVCMediaSettingsBuilder.setTargetBitrate(48000);
        } else if ("64kbps".equals(audioCodeRateSp)) {
            mQHVCMediaSettingsBuilder.setTargetBitrate(64000);
        } else if ("128kbps".equals(audioCodeRateSp)) {
            mQHVCMediaSettingsBuilder.setTargetBitrate(128000);
        }

        if ("22.05KHz".equals(audioSampleSp)) {/*音频采样率*/
            mQHVCMediaSettingsBuilder.setSampleRate(22050);
        } else if ("44.1KHz".equals(audioSampleSp)) {
            mQHVCMediaSettingsBuilder.setSampleRate(44100);
        } else if ("48KHz".equals(audioSampleSp)) {
            mQHVCMediaSettingsBuilder.setSampleRate(48000);
        }

        mQhvcLiveKitAdvanced.setMediaSettings(mQHVCMediaSettingsBuilder.build());
        QHVCPublishSettings.Builder mQhvcPublishSettingsBuild = new QHVCPublishSettings.Builder();
        mQhvcPublishSettingsBuild.setOnlyToFile(0);
        if (saveVideoBoolean) {/*推流时是否保存文件到本地*/
            mQhvcPublishSettingsBuild.setMp4FileName(SAVEVIDEOFILEPATH);
        }
        mQhvcLiveKitAdvanced.setPublishSettings(mQhvcPublishSettingsBuild.build());
        if (TextUtils.isEmpty(URL)) {/*户输入的推流地址为空则用内置的*/
            URL = RTMP;
        }
        mQhvcLiveKitAdvanced.setRtmpPushAddr(URL);
        if ("360P".equals(resolutionRatioSp)) {/*硬编设置编码尺寸*/
            mQhvcLiveKitAdvanced.setHardEncodeSize(QHVCConstants.HardEncoderSize.ENCODER_360X640);
        } else if ("480P".equals(resolutionRatioSp)) {
            mQhvcLiveKitAdvanced.setHardEncodeSize(QHVCConstants.HardEncoderSize.ENCODER_480X854);
        } else if ("640P".equals(resolutionRatioSp)) {
            mQhvcLiveKitAdvanced.setHardEncodeSize(QHVCConstants.HardEncoderSize.ENCODER_640X1136);
        } else if ("720P".equals(resolutionRatioSp)) {
            mQhvcLiveKitAdvanced.setHardEncodeSize(QHVCConstants.HardEncoderSize.ENCODER_720X1280);
        }
        mQhvcLiveKitAdvanced.setDisplayPreview(mSurfaceView);
        mQhvcLiveKitAdvanced.setCameraFacing(QHVCConstants.Camera.FACING_FRONT);/*设置使用前置或者后置摄像头*/
        if (horizontalBoolean) {
            mQhvcLiveKitAdvanced.setOrientation(Configuration.ORIENTATION_LANDSCAPE, this);/*设置预览方向*/
        } else {
            mQhvcLiveKitAdvanced.setOrientation(Configuration.ORIENTATION_PORTRAIT, this);/*设置预览方向*/
        }
        mQhvcLiveKitAdvanced.setStateCallback(new QHVCRecorderCallBack() {/*设置状态回调*/
            @Override
            public void onState(int sessionId, int pubEvent, int msg1, final String msg2) {
                RecorderLogger.i(TAG, "LiveCloud-------pubEvent:" + pubEvent);
                switch (pubEvent) {
                    case QHVCRecorderCallBack.EVENT_CODE.EVENT_UNKNOWN:
                        break;
                    case QHVCRecorderCallBack.EVENT_CODE.EVENT_CONNECTED:
                        break;
                    case QHVCRecorderCallBack.EVENT_CODE.EVENT_CONNECT_FAILED:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RecorderActivityNewAPI.this, "连接失败", Toast.LENGTH_SHORT).show();
                                muteButton.setImageResource(R.drawable.recorder_mute_enable);/*暂                                                                 停推流重置静音状态*/
                                stopClick();/*暂停计时*/
                                mQhvcLiveKitAdvanced.pausePublish();/*暂停传输*/
                            }
                        });
                        break;
                    case INFO_CODE.INFO_CAMERA_OPEN_FAILED:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RecorderActivityNewAPI.this, "请检查相机权限", Toast.LENGTH_LONG).show();
                            }
                        });
                        break;

                }
            }

            @Override
            public void onEncodedMessage(int sessionId, int type, ByteBuffer buffer, int length, long time) {

            }
        });
    }

    /**
     * 资源释放
     */
    private void release() {
        RecorderLogger.i(TAG, "LiveCloud--------release");

        workThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (mPublishStatsTool != null) {/*停止推流打点*/
                    mPublishStatsTool.stop();
                    mPublishStatsTool.release();
                }
            }
        });
        if (mQhvcLiveKitAdvanced != null) {
            mQhvcLiveKitAdvanced.release();/*释放资源*/
        }
        if (workThreadPoolExecutor != null) {
            workThreadPoolExecutor.shutdown();
            try {
                workThreadPoolExecutor.awaitTermination(Long.MAX_VALUE,
                        TimeUnit.DAYS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            workThreadPoolExecutor = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode != KeyEvent.KEYCODE_BACK) {

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record_finish:
                exitDialog();
                break;
            case R.id.new_api_mirror_button:
                if (mQhvcLiveKitAdvanced != null) {
                    if (mQhvcLiveKitAdvanced.isMirro()) {
                        mQhvcLiveKitAdvanced.setMirro(false);
                    } else {
                        mQhvcLiveKitAdvanced.setMirro(true);
                    }
                }
                break;
            case R.id.new_api_mute_button:
                if (mQhvcLiveKitAdvanced != null) {
                    if (mQhvcLiveKitAdvanced.isPublishState()) {/*是否是推流状态*/
                        if (mQhvcLiveKitAdvanced.isMute()) {
                            mQhvcLiveKitAdvanced.setMute(false);
                            muteButton.setImageResource(R.drawable.recorder_mute_enable);
                        } else {
                            mQhvcLiveKitAdvanced.setMute(true);
                            muteButton.setImageResource(R.drawable.recorder_mute_unable);
                        }
                    } else {/*不是推流状态*/

                    }
                }
                break;
            case R.id.new_api_change_camera_button:
                if (mQhvcLiveKitAdvanced != null) {
                    mQhvcLiveKitAdvanced.switchCameraFacing();
                }
                break;
            case R.id.new_api_start_button:
                if (isFirstStart) {
                    mQhvcLiveKitAdvanced.startPublish();/*开始推流*/
                    workThreadPoolExecutor.execute(new Runnable() {/*开始传输时打点*/
                        @Override
                        public void run() {
                            mPublishStatsTool.start(QHVCSdk.getInstance().getConfig().getBusinessId(), channelId, URL, title);
                        }
                    });
                    isFirstStart = false;
                    startButton.setImageResource(R.drawable.recorder_pause);
                    startButton.setTag(R.drawable.recorder_pause);
                    startClick();/*开始推流计时*/
                } else if ((int) startButton.getTag() == R.drawable.recorder_pause) {
                    muteButton.setImageResource(R.drawable.recorder_mute_enable);/*暂停推流重置静音状态*/
                    stopClick();/*暂停计时*/
                    mQhvcLiveKitAdvanced.pausePublish();/*暂停传输*/
                } else if ((int) startButton.getTag() == R.drawable.recorder_start) {
                    startClick();/*开始推流计时*/
                    mQhvcLiveKitAdvanced.resumePublish();/*恢复传输*/
                }
                break;
            case R.id.new_api_beauty_button:
                showBeautyPopWindow();
                break;
            case R.id.new_api_faceU_button:
                showFaceUPopWindow();
                break;
            case R.id.new_api_enable_camera_button:
                if (mQhvcLiveKitAdvanced != null) {
                    if (mQhvcLiveKitAdvanced.isVideoEnable()) {/*视频是否是开启状态*/
                        mQhvcLiveKitAdvanced.setEnableVideo(false);
                        cameraEnableButton.setImageResource(R.drawable.recorder_camera_unable);
                    } else {/*视频是关闭状态*/
                        mQhvcLiveKitAdvanced.setEnableVideo(true);
                        cameraEnableButton.setImageResource(R.drawable.recorder_camera_enable);
                    }
                }
                break;
        }

        if (v == mExitCancel) {
            mExitDialog.dismiss();
        }
        if (v == mExitConfirm) {
            mExitDialog.dismiss();
            finish();
        }

    }

    /**
     * 显示美颜PopWindow
     */
    private void showBeautyPopWindow() {
        mQhvcLiveKitAdvanced.openBeauty();/*开启美颜功能*/
        if (mBeautyPopWindow == null) {
            mBeautyPopWindow = new BeautyPopWindow(this);
            mBeautyPopWindow.showAtLocation(mSurfaceView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            controlerLayout.setVisibility(View.INVISIBLE);
            mBeautyPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    controlerLayout.setVisibility(View.VISIBLE);
                }
            });
            mBeautyPopWindow.setSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float fl = (float) (progress * 0.01);
                        switch ((int) seekBar.getTag()) {
                            case R.id.record_popwindow_beauty:
                                mQhvcLiveKitAdvanced.setBeautyRatio(fl);
                                break;
                            case R.id.record_popwindow_white:
                                mQhvcLiveKitAdvanced.setWhiteRatio(fl);
                                break;
                            case R.id.record_popwindow_sharpface:
                                mQhvcLiveKitAdvanced.setSharpFaceRatio(fl);
                                break;
                            case R.id.record_popwindow_bigeye:
                                mQhvcLiveKitAdvanced.setBigEyeRatio(fl);
                                break;
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        } else {
            if (!mBeautyPopWindow.isShowing()) {
                mBeautyPopWindow.showAtLocation(mSurfaceView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                controlerLayout.setVisibility(View.INVISIBLE);
            } else {
                mBeautyPopWindow.dismiss();
            }
        }
    }


    private void showFaceUPopWindow() {
        if (horizontalBoolean) {
            Toast.makeText(this, "暂不支持", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mFaceUPopWindow == null) {
            mFaceUPopWindow = new FaceUPopWindow(this);
            mFaceUPopWindow.showAtLocation(mSurfaceView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            controlerLayout.setVisibility(View.INVISIBLE);
            mFaceUPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    controlerLayout.setVisibility(View.VISIBLE);
                }
            });
            mFaceUPopWindow.setOnItemClickListener(new FaceUPopWindow.MyItemClickListener() {
                @Override
                public void onItemClick(View view, int postion, String faceUPath) {
                    if (mQhvcLiveKitAdvanced != null) {
                        if (postion == 0) {
                            mQhvcLiveKitAdvanced.stopFaceU();
                        } else {
                            mQhvcLiveKitAdvanced.showFaceU(faceUPath, -1, new QHVCFaceUCallBack() {
                                @Override
                                public void onFaceUBack(String sourcePath, String faceUInfo) {

                                }
                            });
                        }
                    }
                }
            });
        } else {
            if (!mFaceUPopWindow.isShowing()) {
                mFaceUPopWindow.showAtLocation(mSurfaceView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                controlerLayout.setVisibility(View.INVISIBLE);
            } else {
                mFaceUPopWindow.dismiss();
            }
        }
    }

    /*开始计时*/
    private void startClick() {
        startButton.setImageResource(R.drawable.recorder_pause);
        startButton.setTag(R.drawable.recorder_pause);
        timeHandler.postDelayed(timeClickRunnable, 1000);
    }

    /*停止计时*/
    private void stopClick() {
        startButton.setImageResource(R.drawable.recorder_start);
        startButton.setTag(R.drawable.recorder_start);
        timeHandler.removeCallbacks(timeClickRunnable);
        resetRecordMessage();/*重置推流信息*/
    }

    private String getStringTime(int cnt) {
        int hour = cnt / 3600;
        int min = cnt % 3600 / 60;
        int second = cnt % 60;
        return String.format(Locale.CHINA, "%02d:%02d:%02d", hour, min, second);
    }

    Handler timeHandler = new Handler();
    Runnable timeClickRunnable = new Runnable() {
        @Override
        public void run() {
            recordTimeCount++;
            recordTimeTextView.setText(getStringTime(recordTimeCount));
            showRecordMessage();/*显示*/
            timeHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("recordTimeCount", recordTimeCount);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        recordTimeCount = savedInstanceState.getInt("recordTimeCount");
    }

    GestureDetector.OnGestureListener learnGestureListener = new GestureDetector.OnGestureListener() {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                                float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            //向左滑
            RecorderLogger.i(TAG, "LiveCloud--------onFling");
            if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE
                    && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                showMessageLayout();
            }
            //向右滑
            if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
                    && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                dismissMessageLayout();
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    };

    private void showMessageLayout() {
        if (!messageLayout.isShown()) {
            messageLayout.setVisibility(View.VISIBLE);
        }
    }

    private void dismissMessageLayout() {
        if (messageLayout.isShown()) {
            messageLayout.setVisibility(View.GONE);
        }
    }

    private void showRecordMessage() {
        sdkVersionTextView.setText("SDK版本号：" + QHVCLiveKit.getVersion());
        rtmpTextView.setText("推流地址：" + RTMP);
        encoderTypeTextView.setText("视屏编码格式：" + encodeTypeSp);
        if (mQhvcLiveKitAdvanced.isSupportHardwareCoding() && !onlyVoiceBoolean) {
            encodeMethodTextView.setText("软硬解编码格式：" + "硬编");
        } else {
            encodeMethodTextView.setText("软硬解编码格式：" + "软编");
        }
        resolutionRatioTextView.setText("实时分辨率：" + resolutionRatioSp);
        netTypeTextView.setText("网络类型：" + QHVCSdk.getInstance().getConfig().getNetworkType());
        if (mQhvcLiveKitAdvanced != null) {
            HashMap<String, String> infoMap = (HashMap<String, String>) mQhvcLiveKitAdvanced.getTansportInfo().clone();
            String speed = infoMap.get(QHVCConstants.RecordMessage.SPEED);
            String codeSpeed = infoMap.get(QHVCConstants.RecordMessage.CODESPEED);
            String fpsSpeed = infoMap.get(QHVCConstants.RecordMessage.FPSSPEED);
            String dropFps = infoMap.get(QHVCConstants.RecordMessage.DROPFPS);
            String storeLength = infoMap.get(QHVCConstants.RecordMessage.STORELENGTH);
            if (!TextUtils.isEmpty(speed)) {
                transportSpeedTextView.setText("当前推流速度：" + speed + " KB/S");
            }
            if (!TextUtils.isEmpty(codeSpeed)) {
                codeRateTextView.setText("实时码率：" + codeSpeed + " kbps");
            }
            if (!TextUtils.isEmpty(fpsSpeed)) {
                fpsTextView.setText("实时帧率：" + fpsSpeed + " fps");
            }
            if (!TextUtils.isEmpty(dropFps)) {
                videoLostTextView.setText("视频丢帧：" + dropFps);
            }
            if (!TextUtils.isEmpty(storeLength)) {
                videoStoreTextView.setText("视频堆积：" + storeLength);
            }
        }
    }

    private void resetRecordMessage() {
        transportSpeedTextView.setText("当前推流速度：" + 0 + " KB/S");
        codeRateTextView.setText("实时码率：" + 0 + " kbps");
        fpsTextView.setText("实时帧率：" + 0 + " fps");
    }

    private void exitDialog() {
        mExitCancel = new TextView(this);
        mExitConfirm = new TextView(this);
        mExitCancel.setText("取消");
        mExitConfirm.setText("确定");
        mExitCancel.setOnClickListener(this);
        mExitConfirm.setOnClickListener(this);
        mExitDialog = new BaseDialog(this);
        mExitDialog.show();
        mExitDialog
                .setChooseDialog(new TextView[]{
                        mExitCancel, mExitConfirm
                });
        mExitDialog.hasTitle(false);
        mExitDialog.setMsgText("确定要退出吗？");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        try {
            super.onConfigurationChanged(newConfig);
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                RecorderLogger.i(TAG, "LiveCloud---------onConfigurationChanged---ORIENTATION_LANDSCAPE");
            } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                RecorderLogger.i(TAG, "LiveCloud---------onConfigurationChanged---ORIENTATION_PORTRAIT");
            }
        } catch (Exception ex) {
        }
    }

    /**
     * 绘制人脸框 测试用
     */
    private void drawFacePoints() {
        thread = new Thread() {
            @Override
            public void run() {
                while (!killed) {
                    Canvas canvas = mOverlap.getHolder().lockCanvas();
                    if (canvas == null)
                        continue;
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    if (mQhvcLiveKitAdvanced.getQHVCDrawEff2().stack_faces.size() > 0) {
                        if (mQhvcLiveKitAdvanced.getQHVCDrawEff2().stack_faces.size() > 0) {
                            PointF[] points = mQhvcLiveKitAdvanced.getQHVCDrawEff2().stack_faces.peek();
                            boolean frontCamera = false;

                            if (matrix != null) {
                                canvas.setMatrix(matrix);
                            }

                            int viewWidth = 90;
                            int viewHeight = 160;
                            if (mPointFace == null) {
                                mPointFace = new PointF[95];
                                for (int i = 0; i < 95; ++i) {
                                    mPointFace[i] = new PointF();
                                }
                            }
                            if (QhFaceApi.bUseAlignment) {
                                if (mQhvcLiveKitAdvanced != null) {
                                    if (mQhvcLiveKitAdvanced.isMirro()) {
                                        for (int i = 0; i < points.length; ++i) {
                                            mPointFace[i].x = viewWidth - points[i].x;
                                            mPointFace[i].y = points[i].y;
                                        }
                                        drawPoints(canvas, mPointFace, viewWidth, viewHeight, frontCamera);
                                    } else {
                                        drawPoints(canvas, points, viewWidth, viewHeight, frontCamera);
                                    }
                                }
                            } else {
                                QhUtils.drawFaceRect(canvas, new Rect(), viewWidth, viewHeight, frontCamera);
                            }
                        }
                    }
                    mOverlap.getHolder().unlockCanvasAndPost(canvas);
                }
            }
        };
        thread.start();
    }

    public static void drawPoints(Canvas canvas, PointF[] points, int width, int height, boolean frontCamera) {
        if (canvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.rgb(255, 255, 255));
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(0.5F);
            PointF[] var9 = points;
            int var8 = points.length;

            for (int var7 = 0; var7 < var8; ++var7) {
                PointF point = var9[var7];
                PointF p = new PointF();
                p.set(point);
                if (frontCamera) {
                    p.x = (float) width - p.x;
                }

                canvas.drawPoint(p.x, p.y, paint);
            }

        }
    }

    /**
     * 按钮-监听电话
     */
    public void createPhoneListener() {
        TelephonyManager telephony = (TelephonyManager) getSystemService(
                Context.TELEPHONY_SERVICE);
        telephony.listen(new OnePhoneStateListener(),
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * 电话状态监听.
     *
     * @author stephen
     */
    class OnePhoneStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    RecorderLogger.i(TAG, "[Listener]等待接电话:" + incomingNumber);
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    RecorderLogger.i(TAG, "[Listener]电话挂断:" + incomingNumber);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    RecorderLogger.i(TAG, "[Listener]通话中:" + incomingNumber);
                    if (mQhvcLiveKitAdvanced != null && mQhvcLiveKitAdvanced.isPublishState()) {/*推流状态*/
                        muteButton.setImageResource(R.drawable.recorder_mute_enable);/*暂停推流重置静音状态*/
                        stopClick();/*暂停计时*/
                        mQhvcLiveKitAdvanced.pausePublish();/*暂停传输*/
                    }
                    break;
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    }
}
