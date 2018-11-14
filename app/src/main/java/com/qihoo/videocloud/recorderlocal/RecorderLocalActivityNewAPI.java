
package com.qihoo.videocloud.recorderlocal;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo.faceapi.QhFaceApi;
import com.qihoo.faceapi.util.QhUtils;
import com.qihoo.livecloud.livekit.api.QHVCConstants;
import com.qihoo.livecloud.livekit.api.QHVCFaceUCallBack;
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
import com.qihoo.videocloud.recorder.RecorderConstants;
import com.qihoo.videocloud.recorder.adapter.FaceURecylerViewAdapter;
import com.qihoo.videocloud.utils.AndroidUtil;
import com.qihoo.videocloud.utils.QHVCSharedPreferences;
import com.qihoo.videocloud.view.BaseDialog;
import com.qihoo.videocloud.view.BeautyPopWindow;
import com.qihoo.videocloud.view.FaceUPopWindow;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

/**
 * 拍摄
 * Created by huchengming
 */
public class RecorderLocalActivityNewAPI extends Activity implements View.OnClickListener {
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
    private final String SAVEVIDEOFILEPATH = "/sdcard/LiveCloud/RecorderLocal/123.mp4";
    private final String SAVEAUDIOFILEPATH = "/sdcard/LiveCloud/RecorderLocal/123.mp3";
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
        setContentView(R.layout.recorder_local_activity_new_api);
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
        if (!TextUtils.isEmpty(businessId)) {
            QHVCSdk.getInstance().getConfig().setBusinessId(businessId);
        }
        sharedPreferences = QHVCSharedPreferences.getInstence();
        horizontalBoolean = sharedPreferences.getBoolean(RecorderConstants.RECORDERLOCAL_CHOICE_HORIZONTAL, false);
    }

    private void initAPI() {
        onlyVoiceBoolean = sharedPreferences.getBoolean(RecorderConstants.RECORDERLOCAL_CHOICE_ONLY_VOICE, false);
        resolutionRatioSp = sharedPreferences.getString(RecorderConstants.RESOLUTION_RATIO, "360P");
        encodeTypeSp = sharedPreferences.getString(RecorderConstants.ENCODE_TYPE, "H.264");
        String videoFpsSp = sharedPreferences.getString(RecorderConstants.VIDEO_FPS, "15Hz");
        int codeRateSp = sharedPreferences.getInt(RecorderConstants.RECORDE_LOCAL_CODE_RATE, 300);
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
        mQhvcPublishSettingsBuild.setOnlyToFile(1);/*拍摄功能设置为1：只拍摄 不推流*/
        if (onlyVoiceBoolean) {
            mQhvcPublishSettingsBuild.setMp4FileName(SAVEAUDIOFILEPATH);
        } else {
            mQhvcPublishSettingsBuild.setMp4FileName(SAVEVIDEOFILEPATH);
        }

        mQhvcLiveKitAdvanced.setPublishSettings(mQhvcPublishSettingsBuild.build());
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
                    case EVENT_CODE.EVENT_UNKNOWN:
                        break;
                    case EVENT_CODE.EVENT_CONNECTED:
                        break;
                    case EVENT_CODE.EVENT_CONNECT_FAILED:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RecorderLocalActivityNewAPI.this, "连接失败", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(RecorderLocalActivityNewAPI.this, "请检查相机权限", Toast.LENGTH_LONG).show();
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

        if (mQhvcLiveKitAdvanced != null) {
            mQhvcLiveKitAdvanced.release();/*释放资源*/
        }
        updateVideo(SAVEVIDEOFILEPATH);
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
                RecorderLogger.i(TAG, "LiveCloud--------stop");
                exitDialog();
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
                        switch ((int)seekBar.getTag()){
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
                .setChooseDialog(new TextView[] {
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
     * @author stephen
     *
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

    /**
     * 将视频插入图库
     * @param url 视频路径地址
     */
    public void updateVideo(String url) {
        File file = new File(url);
        //获取ContentResolve对象，来操作插入视频
        ContentResolver localContentResolver = this.getContentResolver();
        //ContentValues：用于储存一些基本类型的键值对
        ContentValues localContentValues = getVideoContentValues(this, file, System.currentTimeMillis());
        //insert语句负责插入一条新的纪录，如果插入成功则会返回这条记录的id，如果插入失败会返回-1。
        Uri localUri = localContentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, localContentValues);
    }

    //再往数据库中插入数据的时候将，将要插入的值都放到一个ContentValues的实例当中
    public static ContentValues getVideoContentValues(Context paramContext, File paramFile, long paramLong) {
        ContentValues localContentValues = new ContentValues();
        localContentValues.put("title", paramFile.getName());
        localContentValues.put("_display_name", paramFile.getName());
        localContentValues.put("mime_type", "video/3gp");
        localContentValues.put("datetaken", Long.valueOf(paramLong));
        localContentValues.put("date_modified", Long.valueOf(paramLong));
        localContentValues.put("date_added", Long.valueOf(paramLong));
        localContentValues.put("_data", paramFile.getAbsolutePath());
        localContentValues.put("_size", Long.valueOf(paramFile.length()));
        return localContentValues;
    }
}
