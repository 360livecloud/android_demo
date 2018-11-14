
package com.qihoo.videocloud.interactbrocast;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.qihoo.livecloud.interact.api.QHVCInteractiveAudioFrameCallback;
import com.qihoo.livecloud.interact.api.QHVCInteractiveConstant;
import com.qihoo.livecloud.interact.api.QHVCInteractiveEventHandler;
import com.qihoo.livecloud.interact.api.QHVCInteractiveKit;
import com.qihoo.livecloud.interact.api.QHVCInteractiveMixStreamConfig;
import com.qihoo.livecloud.interact.api.QHVCInteractiveMixStreamRegion;
import com.qihoo.livecloud.interact.api.QHVCInteractiveUtils;
import com.qihoo.livecloud.livekit.api.QHVCConstants;
import com.qihoo.livecloud.livekit.api.QHVCFaceUCallBack;
import com.qihoo.livecloud.livekit.api.QHVCLiveKit;
import com.qihoo.livecloud.livekit.api.QHVCLiveKitAdvanced;
import com.qihoo.livecloud.livekit.api.QHVCMediaSettings;
import com.qihoo.livecloud.livekit.api.QHVCSurfaceView;
import com.qihoo.livecloud.tools.Constants;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.tools.NetUtil;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.interactbrocast.adapter.InteractRoomAudioNumberRecyclerViewAdapter;
import com.qihoo.videocloud.interactbrocast.adapter.InteractRoomNumberRecyclerViewAdapter;
import com.qihoo.videocloud.interactbrocast.data.InteractGlobalManager;
import com.qihoo.videocloud.interactbrocast.main.InteractCallBackEvent;
import com.qihoo.videocloud.interactbrocast.main.InteractCallback;
import com.qihoo.videocloud.interactbrocast.main.InteractConstant;
import com.qihoo.videocloud.interactbrocast.main.WorkerThread;
import com.qihoo.videocloud.interactbrocast.modle.InteractRoomModel;
import com.qihoo.videocloud.interactbrocast.modle.InteractUserModel;
import com.qihoo.videocloud.interactbrocast.net.InteractServerApi;
import com.qihoo.videocloud.interactbrocast.ui.MyCommonButton;
import com.qihoo.videocloud.interactbrocast.ui.MyVideoView;
import com.qihoo.videocloud.interactbrocast.ui.TestApiPopupWindow;
import com.qihoo.videocloud.utils.AndroidUtil;
import com.qihoo.videocloud.utils.LibTaskController;
import com.qihoo.videocloud.utils.QHVCSharedPreferences;
import com.qihoo.videocloud.view.BaseDialog;
import com.qihoo.videocloud.view.BeautyPopWindow;
import com.qihoo.videocloud.view.FaceUPopWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.qihoo.videocloud.interactbrocast.InteractSettingActivity.fpsList;

public class InteractActivity extends BaseActivity implements InteractCallBackEvent, View.OnClickListener {

    private RelativeLayout mVideoLayer;

    private MyCommonButton myCommonButton;

    private int mCurrOrientation = Constants.EMode.EMODE_PORTRAIT; //当前方向--横屏或竖屏

    ///////////////For test 为了测试互动直播转推功能 ///////////////////
    private String pushAddr;
    private String pullAddr;
    ///////////////////////////////////////////////////////////////

    ///////////////// For test为了测试合流 //////////////////////////
    public static boolean OPEN_MERGE_STREAM = true;
    public static String mergeRtmp;
    ///////////////////////////////////////////////////////////////

    private String myUid;
    private WorkerThread mWorker;
    private QHVCInteractiveKit mInteractEngine;
    private int mCurrRole; //指定是主播还是观众（或嘉宾）

    private int mUserVideoCapture = InteractConstant.VIDEO_USER_CAPTURE; //是否是业务自采集视频

    //模拟业务做视频采集（支持美颜、faceU等）
    private QHVCSurfaceView mSurfaceView;
    private QHVCLiveKitAdvanced mQhvcLiveKitAdvanced;
    private int videoEncodeWidth;
    private int videoEncodeHeight;
    private boolean isFirstIn = true;/*是否是第一次进入*/

    //FaceU
    private boolean mOpenFaceU = false;

    private Vector<QHVCInteractiveMixStreamRegion> allMixStreamRegion = new Vector<>();

    //多人
    private final HashMap<String, MyVideoView> mAllVideoMap = new HashMap<>(); //存放uid和View的对应关系
    //private final HashMap<String, String> mAllStream = new HashMap<>(); //存放uid和StreamId的对应关系

    private MyVideoView mLargeVideoView;
    private int interactTimeCount = 0;/*计时*/
    private TextView roomNameTextView;
    private TextView roomIDTextView;
    private TextView onlineNumTextView;
    private TextView interactTimeTextView;
    private String roomName;
    private String roomId;
    private String onlineNum;
    private PopupWindow mRoomMessagePopWindow;
    private PopupWindow mRoomNumberPopWindow;
    private TextView roomMessageButton;
    private TextView invitingGuestsButton;
    private ImageView exitRoom;
    private ImageView finishFullScreen;
    private InteractRoomModel iteractRoom;
    private ExecutorService workThreadPoolExecutor;/*异步线程*/
    private RecyclerView numberRecyclerView;
    private RecyclerView audioNumberRecyclerView;
    private InteractRoomNumberRecyclerViewAdapter mRoomNumberRecyclerViewAdapter;
    private InteractRoomAudioNumberRecyclerViewAdapter mRoomAudioNumberRecyclerViewAdapter;
    private List<InteractUserModel> roomNumberList = new ArrayList<>();
    private RelativeLayout rootLayout;
    private static final int FLING_MIN_DISTANCE = 200;//  滑动最小距离
    private static final int FLING_MIN_VELOCITY = 200;// 滑动最大速度

    private TextView roomIdTextView;
    private TextView sdkVersionTextView;
    private TextView roleTextView;
    private TextView resolutionRatioView;
    private TextView codeRateView;
    private TextView fpsView;
    private TextView audioTypeView;
    private TextView videoQualityView;
    private TextView netTypeView;
    private InteractUserModel anchor;/*主播*/
    private int talkType;/*音频或者视频房间*/
    private int codeRate;
    private int fps;
    private int quality;
    private ArrayList<String> applyList = new ArrayList<>();

    private BeautyPopWindow mBeautyPopWindow;
    private FaceUPopWindow mFaceUPopWindow;

    private String resolution;

    private QHVCInteractiveAudioFrameCallback mHostinAudioFrameCallback = new QHVCInteractiveAudioFrameCallback() {

        @Override
        public void onAudioFrame(byte[] audioData, int sampleRate, int numOfChannels, int bitDepth) {
            //            Logger.d(InteractConstant.TAG, InteractConstant.TAG + "，onAudioFrame, audioData.length: " + audioData.length + ", sampleRate: " + sampleRate + ", numOfChannels: " + numOfChannels +
            //                    ", bitDepth: " + bitDepth);
        }

        @Override
        public byte[] onLocalAudioFrame(byte[] audioData, int sampleRate, int numOfChannels, int bitDepth) {
            //            Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", onLocalAudioFrame audioData.length: " + audioData.length + ", sampleRate: " + sampleRate + ", numOfChannels: " + numOfChannels +
            //            ", bitDepth: " + bitDepth);
            return audioData;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        initView();

        initWorker();
        loadInteractEngine();

        // 注册IM回调
        InteractIMManager.getInstance().addReceiveCommandistener(mOnReceiveCommandListener);
        joinIMRoom();
    }

    private void loadInteractEngine() {
        InteractCallback.getInstance().addCallBack(InteractActivity.this);

        Map<String, String> optionInfo = new HashMap<>();

        optionInfo.put(QHVCInteractiveConstant.EngineOption.PUSH_ADDR, pushAddr);
        optionInfo.put(QHVCInteractiveConstant.EngineOption.PULL_ADDR, pullAddr);
        mWorker.loadEngine(roomId, myUid, optionInfo);
    }

    private void startInteract() {
        //For test
        mInteractEngine.setAudioFrameCallback(mHostinAudioFrameCallback);

        doConfigEngine(mCurrRole);
        if (mInteractEngine != null) {
            mInteractEngine.enableDualStreamMode(false); //默认不开启双流模式
            /*
            if (mCurrOrientation == Constants.EMode.EMODE_PORTRAIT) {
                mInteractEngine.setLowStreamVideoProfile(180, 320, 15, 180);
            } else {
                mInteractEngine.setLowStreamVideoProfile(320, 180, 15, 180);
            }
            */
        }
        if (OPEN_MERGE_STREAM) {
            setMixStreamInfo();
        }

        //为了测试setCloudControlRole()接口，请在测试此云控时打开
        //TestApiPopupWindow.setCloudControlRole(mInteractEngine);

        joinChannel();
    }

    private void initData() {
        iteractRoom = (InteractRoomModel) getIntent()
                .getSerializableExtra(InteractConstant.INTENT_EXTRA_INTERACT_ROOM_DATA);
        if (iteractRoom != null) {
            roomName = iteractRoom.getRoomName();
            roomId = iteractRoom.getRoomId();
            onlineNum = iteractRoom.getOnlineNum() + "";
            talkType = iteractRoom.getTalkType();
        }
        myUid = InteractGlobalManager.getInstance().getUser().getUserId();
        Logger.d(InteractConstant.TAG, "----userID : " + myUid);
        mCurrRole = QHVCInteractiveConstant.CLIENT_ROLE_BROADCASTER;/*主播身份*/
        if (workThreadPoolExecutor == null) {
            workThreadPoolExecutor = Executors.newSingleThreadExecutor();
        }
        InteractServerApi.getRoomUserList(myUid, roomId, new int[] {
                InteractConstant.USER_IDENTITY_GUEST,
                InteractConstant.USER_IDENTITY_AUDIENCE
                },
                new InteractServerApi.ResultCallback<List<InteractUserModel>>() {
                    @Override
                    public void onSuccess(List<InteractUserModel> data) {
                    }

                    @Override
                    public void onFailed(int errCode, String errMsg) {

                    }
                });

        String streamId = InteractGlobalManager.getInstance().getBusinessId() + "_" +
                    InteractGlobalManager.getInstance().getChannelId() + "_" +
                    roomId + "_" +
                    myUid + "_" + System.currentTimeMillis();
        pushAddr = "rtmp://ps1.live.huajiao.com/live_huajiao_v2/" + streamId;
        pullAddr = "http://pl1.live.huajiao.com/live_huajiao_v2/" + streamId + ".flv";

        mergeRtmp = "rtmp://ps1.live.huajiao.com/live_huajiao_v2/" + streamId + "_1";

        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", push_rtmp: " + pushAddr + ", \n flv: " + pullAddr + ", \n merge: " + mergeRtmp);

        //5.0以上并且支持硬编码的机型使用业务做视频采集（支持美颜等）
        if (QHVCLiveKit.getInstance(this).isSupportHardwareCoding()) {
            mUserVideoCapture = InteractConstant.VIDEO_USER_CAPTURE;
            initUserCapture();
        } else {
            mUserVideoCapture = InteractConstant.VIDEO_SDK_COMMON_CAPTURE;
        }
    }

    private void initUserCapture() {
        mQhvcLiveKitAdvanced = QHVCLiveKitAdvanced.getInstance(this.getApplicationContext());
        String cid = InteractGlobalManager.getInstance().getChannelId();
        mQhvcLiveKitAdvanced.setChannelId(cid);/*设置渠道Id*/
        if (mQhvcLiveKitAdvanced.isSupportHardwareCoding()) { /*支持硬编*/
            mQhvcLiveKitAdvanced.setEncodeMethod(QHVCConstants.RecorderConstants.ENCODE_HARDWARE);/*设置编码方式（硬编或者软编）*/
            QHVCMediaSettings.Builder mQHVCMediaSettingsBuilder = new QHVCMediaSettings.Builder();
            QHVCSharedPreferences pref = QHVCSharedPreferences.getInstence();
            int prefIndex = pref.getInt(InteractConstant.BROCAST_SETTING_PROFILE_TYPE,
                    InteractConstant.DEFAULT_PROFILE_IDX);
            if (prefIndex > InteractConstant.VIDEO_PROFILES.length - 1) {
                prefIndex = InteractConstant.DEFAULT_PROFILE_IDX;
            }
            mQHVCMediaSettingsBuilder.setFps(Integer.valueOf(fpsList[prefIndex]));
            mQhvcLiveKitAdvanced.setMediaSettings(mQHVCMediaSettingsBuilder.build());

            mQhvcLiveKitAdvanced.setHardEncodeSize(QHVCConstants.HardEncoderSize.ENCODER_360X640); //TODO 一会看 编码的宽高

            mQhvcLiveKitAdvanced.setEnableAudio(false); //此处借用推流采集模块，必须禁掉音频，因为互动直播本身已做音频采集。

            mQhvcLiveKitAdvanced.setCameraFacing(QHVCConstants.Camera.FACING_FRONT);/*设置使用前置或者后置摄像头*/
            if (mCurrOrientation == Constants.EMode.EMODE_LANDSCAPE) {
                mQhvcLiveKitAdvanced.setOrientation(Configuration.ORIENTATION_LANDSCAPE, this);/*设置预览方向*/
            } else {
                mQhvcLiveKitAdvanced.setOrientation(Configuration.ORIENTATION_PORTRAIT, this);/*设置预览方向*/
            }
        }

    }

    private void initView() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_interact);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mCurrOrientation = Constants.EMode.EMODE_LANDSCAPE;
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mCurrOrientation = Constants.EMode.EMODE_PORTRAIT;
        }

        roomNameTextView = (TextView) findViewById(R.id.interact_room_name);
        roomNameTextView.setText(roomName);
        roomIDTextView = (TextView) findViewById(R.id.interact_room_id);
        roomIDTextView.setText(roomId);
        onlineNumTextView = (TextView) findViewById(R.id.interact_room_online_num);
        onlineNumTextView.setText(onlineNum);
        interactTimeTextView = (TextView) findViewById(R.id.interact_time);
        interactTimeTextView.setText("00:00:00");

        mVideoLayer = (RelativeLayout) findViewById(R.id.video_layer);
        final GestureDetector mGestureDetector = new GestureDetector(this, learnGestureListener);
        mVideoLayer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });


        MyVideoView bigVideoView = (MyVideoView) findViewById(R.id.big_video);

        if (mUserVideoCapture == InteractConstant.VIDEO_USER_CAPTURE) {
            if (mSurfaceView == null) {
                mSurfaceView = new QHVCSurfaceView(InteractActivity.this);
            }
            mQhvcLiveKitAdvanced.setDisplayPreview(mSurfaceView);
            bigVideoView.setBgView(mSurfaceView, mScreenWidth, mScreenHeight);

            mQhvcLiveKitAdvanced.prepare();
            mQhvcLiveKitAdvanced.startPreview();/*开始预览*/

        }
        mLargeVideoView = bigVideoView;
        mLargeVideoView.setUid(myUid);
        mLargeVideoView.setAllButtonVisible(View.GONE);
        mAllVideoMap.put(myUid, mLargeVideoView);

        final View v = findViewById(R.id.common_btn);
        myCommonButton = new MyCommonButton(v) {
            @Override
            public void doOnClickSwitchCamera() {
                doSwitchCamera();
            }

            @Override
            public void doOnClickMuteLocalAudio(ImageView view) {
                doMuteLocalAudio(view);
            }

            @Override
            public void doOnClickSwitchAudioOutput(ImageView btn) {
                doSwitchAudioOutput(btn);
            }

            @Override
            public void doOnClickBeauty(ImageView view) {
                doBeauty(view);
            }

            @Override
            public void doOnClickFaceU(ImageView view) {
                doFaceU(view);
            }

            @Override
            public void doOnClickEnableVideo(ImageView view) {
                if (talkType == InteractConstant.TALK_TYPE_AUDIO) {
                    showToast("该房间为音频房间");
                    return;
                }
                doSwitchToSendLocalVideo(view);
            }

            @Override
            public void doOnClickFullScreen(ImageView view) {
                doFullScreen(view);
            }

            @Override
            public void doOnClickFilter(ImageView view) {

            }

        };
        roomMessageButton = (TextView) findViewById(R.id.interact_room_message);
        roomMessageButton.setOnClickListener(this);
        invitingGuestsButton = (TextView) findViewById(R.id.interact_room_inviting_guests);
        invitingGuestsButton.setOnClickListener(this);
        exitRoom = (ImageView) findViewById(R.id.interact_close_room);
        exitRoom.setOnClickListener(this);
        finishFullScreen = (ImageView) findViewById(R.id.interact_return_messagelayout);
        finishFullScreen.setOnClickListener(this);
        rootLayout = (RelativeLayout) findViewById(R.id.interact_room_message_root_layout);

        if (talkType == InteractConstant.TALK_TYPE_AUDIO) {
            myCommonButton.setViewVisible(R.id.btn_switch_camera, View.GONE);
            myCommonButton.setViewVisible(R.id.btn_enable_video, View.GONE);
        }

        //todo 暂时注掉美颜faceU功能按钮
//        myCommonButton.setViewVisible(R.id.btn_beauty, View.GONE);
//        myCommonButton.setViewVisible(R.id.btn_faceu, View.GONE);
//        myCommonButton.setViewVisible(R.id.btn_filter, View.GONE);

        if (talkType == InteractConstant.TALK_TYPE_AUDIO) {
            showAudioNumberPopWindow(rootLayout);
        }
    }

    /*开始计时*/
    private void startClick() {
        timeHandler.postDelayed(timeClickRunnable, 1000);
    }

    /*停止计时*/
    private void stopClick() {
        timeHandler.removeCallbacks(timeClickRunnable);
    }

    Handler timeHandler = new Handler();
    Runnable timeClickRunnable = new Runnable() {
        @Override
        public void run() {
            interactTimeCount++;
            interactTimeTextView.setText(getStringTime(interactTimeCount));
            timeHandler.postDelayed(this, 1000);
            if (interactTimeCount % 6 == 0) {
                /*心跳*/
                if (workThreadPoolExecutor != null) {
                    workThreadPoolExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            InteractServerApi.userHeart(myUid, roomId, new InteractServerApi.ResultCallback<Void>() {
                                @Override
                                public void onSuccess(Void data) {

                                }

                                @Override
                                public void onFailed(int errCode, String errMsg) {

                                }
                            });
                        }
                    });

                }

            }
            /*定时刷成员列表，监听人员变化，三秒一次*/
            if (interactTimeCount % 3 == 0) {
                getNumberList();
            }
        }
    };

    private String getStringTime(int cnt) {
        int hour = cnt / 3600;
        int min = cnt % 3600 / 60;
        int second = cnt % 60;
        return String.format(Locale.CHINA, "%02d:%02d:%02d", hour, min, second);
    }

    private void startPreview() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Logger.d(InteractConstant.TAG, InteractConstant.TAG + " : 调度成功，开始 直播！");
                if (mUserVideoCapture == InteractConstant.VIDEO_USER_CAPTURE) {
                    SurfaceTexture surfaceTexture = mWorker.getSurfaceTexture(videoEncodeWidth, videoEncodeHeight);
                    if (surfaceTexture != null) {
                        mQhvcLiveKitAdvanced.setSharedSurfaceTexture(surfaceTexture);
                    }
                    mQhvcLiveKitAdvanced.startEncode();
                } else {
                    View view = null;
                    if (talkType == InteractConstant.TALK_TYPE_AUDIO) {/*纯音频*/
                        mWorker.preview(true, null, myUid);
                        return;
                    }
                    if (mUserVideoCapture == InteractConstant.VIDEO_SDK_COMMON_CAPTURE) {
                        view = QHVCInteractiveUtils.CreateRendererView(InteractActivity.this);
                    }
                    /* //TODO 暂不支持
                    else if (mUserVideoCapture == InteractConstant.VIDEO_SDK_BEAUTY_CAPTURE) {
                        if (mInteractEngine != null) {
                            mInteractEngine.enableBeautyCapture(true);
                        }
                        view = QHLiveCloudInteractUtils.CreateRendererView(InteractActivity.this, true);
                    } */
                    if (view != null) {
                        mLargeVideoView.setBgView(view, mScreenWidth, mScreenHeight);
                        mLargeVideoView.setUid(myUid);
                        mLargeVideoView.setAllButtonVisible(View.GONE);

                        mWorker.preview(true, mLargeVideoView.getBgView(), myUid);
                    } else {
                        Logger.e(InteractConstant.TAG, InteractConstant.TAG + ", startPreview failed!! view is null......");
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ": onResume() in InteractActivity...");

        if (!isFirstIn) {
            if(mQhvcLiveKitAdvanced != null) {
                mQhvcLiveKitAdvanced.resumePreview();
            }
        }
        isFirstIn = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ": onPause() in InteractActivity...");
        if (mQhvcLiveKitAdvanced != null) {
            mQhvcLiveKitAdvanced.pausePreview();
        }
    }

    private void dismissRoom() {
        if (workThreadPoolExecutor != null) {
            workThreadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    //                    InteractServerApi.userLeaveRoom(myUid, roomId,
                    //                            new InteractServerApi.ResultCallback<InteractRoomModel>() {
                    //                                @Override
                    //                                public void onSuccess(InteractRoomModel data) {
                    //                                    Toast.makeText(InteractActivity.this, "离开房间", Toast.LENGTH_SHORT).show();
                    //                                }
                    //
                    //                                @Override
                    //                                public void onFailed(int errCode, String errMsg) {
                    //
                    //                                }
                    //                            });
                    InteractServerApi.dismissRoom(myUid, roomId, new InteractServerApi.ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            stopClick();
                        }

                        @Override
                        public void onFailed(int errCode, String errMsg) {
                            showToast(errCode + errMsg);
                            stopClick();
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        // 取消IM回调
        InteractIMManager.getInstance().removeReceiveCommandistener(mOnReceiveCommandListener);
        leaveIMRoom();
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ": onDestroy() in InteractActivity...");

        if (OPEN_MERGE_STREAM) {
            if (mInteractEngine != null) {
                mInteractEngine.clearVideoCompositingLayout();
            }
        }

        if (mWorker != null) {
            mWorker.leaveChannel(roomName);
        }
        dismissRoom();
        InteractIMManager.getInstance().sendNotify(InteractIMManager.CMD_ANCHOR_QUIT_NOTIFY, "", new InteractIMManager.SendMessageCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(int errorCode) {

            }
        });
        //释放业务美颜采集相关资源
        if (mQhvcLiveKitAdvanced != null) {
            mQhvcLiveKitAdvanced.release();/*释放资源*/
            mQhvcLiveKitAdvanced = null;
        }

        LibTaskController.postDelayed(new Runnable() {
            @Override
            public void run() {
                exitWorker();
            }
        }, 3000);

        super.onDestroy();
    }

    private void initWorker() {
        if (mWorker == null) {
            mWorker = new WorkerThread(mUserVideoCapture == InteractConstant.VIDEO_USER_CAPTURE);
        }

        mWorker.setTalkType(talkType);
        mWorker.start();
        mWorker.waitForReady();

        mInteractEngine = mWorker.getInteractEngine();
    }

    private void doConfigEngine(int cRole) {
        QHVCSharedPreferences pref = QHVCSharedPreferences.getInstence();
        int prefIndex = pref.getInt(InteractConstant.BROCAST_SETTING_PROFILE_TYPE,
                InteractConstant.DEFAULT_PROFILE_IDX);
        if (prefIndex > InteractConstant.VIDEO_PROFILES.length - 1) {
            prefIndex = InteractConstant.DEFAULT_PROFILE_IDX;
        }
        int vProfile = InteractConstant.VIDEO_PROFILES[prefIndex];

        resolution = InteractSettingActivity.resolutionRatioList[prefIndex];

        setVideoWidthAndHeight(vProfile);
        //int vProfile = 30;
        mWorker.configEngine(cRole, vProfile, mCurrOrientation);
    }

    private void setVideoWidthAndHeight(int profile) {
///        if (mRecorderController != null) {
            int w = 360;
            int h = 640;
            switch (profile) {
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_240P_3: //240x240
                    w = 240;
                    h = 240;
                    break;
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_240P_4: // 424x240
                    w = 240;
                    h = 424;
                    break;
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_360P: // 640x360
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_360P_4: // 640x360
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_360P_9: // 640x360
                    w = 360;
                    h = 640;
                    break;
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_360P_3: // 360x360
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_360P_6: // 360x360
                    w = 360;
                    h = 360;
                    break;
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_480P_3: // 480x480
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_480P_6: // 480x480
                    w = 480;
                    h = 480;
                    break;
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_480P_8: // 848x480
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_480P_9: // 848x480
                    w = 480;
                    h = 848;
                    break;
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_720P: // 1280x720  15   1130
                case QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_720P_3: // 1280x720  30   1710
                    w = 720;
                    h = 1280;
                    break;
                default:
                    break;
            }
            if (mCurrOrientation == Constants.EMode.EMODE_LANDSCAPE) {
                int tempW = w;
                w = h;
                h = tempW;
            }
        videoEncodeWidth = w;
        videoEncodeHeight = h;
//            mRecorderController.setVideoSize(w, h);
//        }

    }

    public void setMixStreamInfo() {
        if (mInteractEngine != null) {
            QHVCInteractiveMixStreamConfig mixStreamConfig = new QHVCInteractiveMixStreamConfig(360, 640, mergeRtmp);
            mixStreamConfig.setIframeInterval(2);
            mixStreamConfig.setVideoBitrate(800); //800kbps

            mInteractEngine.setMixStreamInfo(mixStreamConfig, QHVCInteractiveConstant.StreamLifeCycle.BIND_ROOM); //默认绑定房间

            QHVCInteractiveMixStreamRegion mixStreamRegion = new QHVCInteractiveMixStreamRegion();
            mixStreamRegion.setX(0);
            mixStreamRegion.setY(0);
            mixStreamRegion.setWidth(360);
            mixStreamRegion.setHeight(640);
            mixStreamRegion.setUserID(myUid);
            mixStreamRegion.setzOrder(0);
            allMixStreamRegion.add(mixStreamRegion);

            updateMixStream();
        }
    }

    /**
     * 更新合流布局
     */
    private void updateMixStream() {
        if (mInteractEngine != null) {
            QHVCInteractiveMixStreamRegion mixStreamInfoArray[] = new QHVCInteractiveMixStreamRegion[allMixStreamRegion.size()];
            allMixStreamRegion.toArray(mixStreamInfoArray);

            mInteractEngine.setVideoCompositingLayout(mixStreamInfoArray);
        }
    }

    public void joinChannel() {
        if (mWorker != null) {
            mWorker.joinChannel();
        } else {
            Logger.w(InteractConstant.TAG, "Error! joinChannel failed, mWorker is null!");
            showToast("Error! mWorker is null!");
        }
    }

    // 切换摄像头
    private void doSwitchCamera() {
        if (mUserVideoCapture == InteractConstant.VIDEO_USER_CAPTURE) {
            if (mQhvcLiveKitAdvanced != null) {
                mQhvcLiveKitAdvanced.switchCameraFacing();
            }
        } else {
            if (mInteractEngine != null) {
                mInteractEngine.switchCamera();
            }
        }
    }

    // 本地关闭麦克风
    private void doMuteLocalAudio(View v) {
        Object tag = v.getTag();
        if (tag == null) {
            tag = false;
        }
        if (mWorker != null) {
            mWorker.getInteractEngine().muteLocalAudioStream(!(boolean) tag);
            ImageView button = (ImageView) v;
            button.setTag(!(boolean) tag);
            if (!(boolean) tag) {
                //                button.setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.MULTIPLY);
                button.setImageResource(R.drawable.recorder_mute_unable);
            } else {
                //                button.clearColorFilter();
                button.setImageResource(R.drawable.interact_mute);
            }
        }
    }

    //切换音频输出
    private void doSwitchAudioOutput(ImageView btn) {
        if (mInteractEngine != null) {
            if (mInteractEngine.isSpeakerphoneEnabled()) {
                mInteractEngine.setEnableSpeakerphone(false);
                btn.setImageResource(R.drawable.interact_earphone);
                //                btn.clearColorFilter();
            } else {
                btn.setImageResource(R.drawable.speaker);
                mInteractEngine.setEnableSpeakerphone(true);
                //                btn.setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.MULTIPLY);
            }
        } else {
            showToast("还未开始直播，请稍后再试");
        }
    }

    private void doBeauty(ImageView view) {
        if (mQhvcLiveKitAdvanced != null) {
            showBeautyPopWindow();
        }
    }

    private void doFaceU(ImageView view) {
        if (mQhvcLiveKitAdvanced != null) {
            showFaceUPopWindow();
        }
    }

    private void doEnableLoaclVideo(ImageView btn) {
        if (mInteractEngine != null) {
            if (btn.getTag() != null && (boolean) btn.getTag()) {
                mInteractEngine.muteLocalVideoStream(true);
                btn.setTag(false);
                btn.setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.MULTIPLY);
            } else {
                mInteractEngine.muteLocalVideoStream(false);
                btn.setTag(true);
                btn.clearColorFilter();
            }
        }
    }

    private void doFullScreen(ImageView btn) {
        if (rootLayout.isShown()) {
            rootLayout.setVisibility(View.GONE);
            finishFullScreen.setVisibility(View.VISIBLE);
        }
    }

    // 是否发送本地视频
    private void doSwitchToSendLocalVideo(ImageView btn) {
        if (mInteractEngine != null) {
            if (btn.getTag() != null && (boolean) btn.getTag()) {
                btn.setTag(false);
                mInteractEngine.muteLocalVideoStream(false);
                btn.setImageResource(R.drawable.interact_enable_video);
                Logger.d(InteractConstant.TAG, InteractConstant.TAG + " : 恢复发送本地视频流!");
            } else {
                btn.setTag(true);
                btn.setImageResource(R.drawable.interact_close_video);
                mInteractEngine.muteLocalVideoStream(true);
                Logger.d(InteractConstant.TAG, InteractConstant.TAG + " : 暂停发送本地视频流!");
            }
        } else {
            showToast("还未开始直播，请稍后再试");
        }
    }

    private void doExit() {
        onBackPressed();
    }

    private void doSwitchFaceU(Button button) {
        /*
        if (mRecorderController != null) {
            mOpenFaceU = !mOpenFaceU;
            if (mOpenFaceU) {
                button.setText("萌颜\n开");
            } else {
                button.setText("萌颜\n关");
            }
            mRecorderController.setFaceU(mOpenFaceU);
        }
        */
    }

    public void doRenderRemoteUi(final String uid) {
        Logger.d(InteractConstant.TAG, "ian, doRenderRemoteUi, uid: " + uid);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                if (mWorker == null) {
                    Logger.w(InteractConstant.TAG, "Warning!  mWorker is null in doRenderRemoteUi~");
                    return;
                }
                if (talkType == InteractConstant.TALK_TYPE_AUDIO) { /*纯音频*/
                    mInteractEngine.setupRemoteVideo(null,
                            QHVCInteractiveConstant.RenderMode.RENDER_MODE_FIT, uid, "");
                    return;
                }

                View view = QHVCInteractiveUtils.CreateRendererView(InteractActivity.this);
                if (view instanceof SurfaceView) {
                    SurfaceView surfaceV = (SurfaceView) view;
                    surfaceV.setZOrderOnTop(true);
                    surfaceV.setZOrderMediaOverlay(true);
                }
                if (myUid.equals(uid) == false) {
                    if (InteractConstant.USE_SET_SURFACE) {
                        // 模拟业务渲染播放远端视频（setSurfece方式），使用SDK播放的业务请忽略。
                        TextureView textureView = new TextureView(InteractActivity.this);
                        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                            @Override
                            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                                SurfaceTexture surfaceTexture1 = surfaceTexture;
                                Surface surface = new Surface(surfaceTexture1);
                                mInteractEngine.setupRemoteVideo(surface,
                                        QHVCInteractiveConstant.RenderMode.RENDER_MODE_HIDDEN, uid, "");
                            }

                            @Override
                            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

                            }

                            @Override
                            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                                return false;
                            }

                            @Override
                            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

                            }
                        });
                        view = textureView;
                        //////////////////////////////
                    } else {
                        mInteractEngine.setupRemoteVideo(view, QHVCInteractiveConstant.RenderMode.RENDER_MODE_HIDDEN, uid, "");
                    }
                }
                addRemoteVideo(view, uid);
            }
        });
    }

    private void addRemoteVideo(View sView, final String uid) {
        final MyVideoView videoView = new MyVideoView(this);
        int w = 360;
        //        int h = 360;
        int h = 640;
        if (mCurrOrientation == Constants.EMode.EMODE_LANDSCAPE) {
            //            w = 640;
            //            h = 360;
            w = 400;
            h = 400;
        }
        videoView.setBgView(sView, w, h);
        videoView.setClickable(true);
        videoView.setOnTouchListener(this);
        videoView.setUid(uid);
        videoView.setAllButtonVisible(View.GONE);
        videoView.setButtonVisible(R.id.interact_close_view, View.VISIBLE);
        videoView.setVideoViewListener(new MyVideoView.VideoViewListener() {
            @Override
            public void changeToFullScreen(MyVideoView videoView1) {

            }

            @Override
            public void doOnClickMuteRemoteVideo(MyVideoView videoView, TextView tv) {
                if (videoView.getUid().equals(myUid) == false) {
                    if (mInteractEngine != null) {
                        boolean muteVideo = videoView.isMuteRemoteVideo();
                        mInteractEngine.muteRemoteVideoStream(videoView.getUid(), muteVideo);
                    }
                }
            }

            @Override
            public void doOnClickMuteRemoteAudio(MyVideoView videoView, TextView tv) {
                if (videoView.getUid().equals(myUid) == false) {
                    if (mInteractEngine != null) {
                        boolean muteAudio = videoView.isMuteRemoteAudio();
                        mInteractEngine.muteRemoteAudioStream(videoView.getUid(), muteAudio);
                    }
                }
            }

            @Override
            public void doOnClickfinish(final MyVideoView videoView) {
                InteractIMManager.getInstance().sendCommand(videoView.getUid(), InteractIMManager.CMD_ANCHOR_KICKOUT_GUEST, "", new InteractIMManager.SendMessageCallback() {
                    @Override
                    public void onSuccess() {
                        mVideoLayer.removeView(videoView);
                        mAllVideoMap.remove(videoView.getUid());
                        mInteractEngine.removeRemoteVideo("", videoView.getUid());
                    }

                    @Override
                    public void onError(int errorCode) {

                    }
                });
            }
        });

        mAllVideoMap.put(uid, videoView);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(w, h);
        Random random = new Random();
        lp.leftMargin = random.nextInt(mScreenWidth - w);
        lp.topMargin = random.nextInt(mScreenHeight - h);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mVideoLayer.addView(videoView, -1, lp);
        addMixStream(uid);
        updateMixStream();
    }

    private void removeRemoteVideo(String uid, int reason) {
        if (mAllVideoMap.containsKey(uid) && !uid.equals(myUid)) {
            MyVideoView videoView = mAllVideoMap.get(uid);
            if (videoView != null) {
                videoView = mAllVideoMap.get(uid);
                mVideoLayer.removeView(videoView);
                mAllVideoMap.remove(uid);
                mInteractEngine.removeRemoteVideo("", uid);
                Logger.d(InteractConstant.TAG,
                        InteractConstant.TAG + ": removeRemoteVideo success, uid: " + uid + ", reason: " + reason);
            } else {
                Logger.w(InteractConstant.TAG,
                        InteractConstant.TAG + ": videoView is null, uid: " + uid + ",removeRemoteVideo failed!");
            }
        }
    }

    @Override
    public void onLoadEngineSuccess(String roomId, String uid) {
        if (!TextUtils.isEmpty(roomId) && !TextUtils.isEmpty(uid)) {
            Logger.i(InteractConstant.TAG, InteractConstant.TAG + ", onLoadEngineSuccess, roomId: " + roomId + ", uid:" + uid);
            if (roomId.equals(this.roomId) && uid.equals(myUid)) {
                startInteract();
            }
        } else {
            Logger.e(InteractConstant.TAG, InteractConstant.TAG + " ERROR! onLoadEngineSuccess(), but uid or roomId is NULL!");
        }
    }

    @Override
    public void onJoinChannelSuccess(final String channel, final String uid, final int elapsed) {
        InteractServerApi.joinRoom(uid, roomId, InteractConstant.USER_IDENTITY_AUDIENCE,
                new InteractServerApi.ResultCallback<InteractRoomModel>() {
                    @Override
                    public void onSuccess(InteractRoomModel data) {
                    }

                    @Override
                    public void onFailed(int errCode, String errMsg) {
                    }
                });
        startPreview();
        startClick();
        mWorker.getInteractEngine().setEnableSpeakerphone(true); // 设置使用外放播放声音
    }

    @Override
    public void onAudioVolumeIndication(QHVCInteractiveEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {
        StringBuffer stringBuffer = new StringBuffer();
        for (QHVCInteractiveEventHandler.AudioVolumeInfo info: speakers) {
            stringBuffer.append(info.toString());
        }
        Logger.i(InteractConstant.TAG, "onAudioVolumeIndication, totalVolume: " + totalVolume + stringBuffer.toString());
    }

    @Override
    public void onUserOffline(String uid, int reason) {
        if (reason == QHVCInteractiveConstant.UserOfflineReason.USER_OFFLINE_QUIT) {
            //用户主动离开 do Nothing

        } else {
            //用户掉线，需要重新拉取视频
            //TODO
            //            Toast.makeText(this, uid + "已断开连接，请重新拉取视频", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLeaveChannel(QHVCInteractiveEventHandler.RtcStats stats) {
        Logger.d(InteractConstant.TAG, "------onLeaveChannel----- ");
        exitWorker();
    }

    @Override
    public void onError(int errType, int errCode) {
        switch (errType) {
            case QHVCInteractiveConstant.ErrorType.JOIN_ERR:
                //加入频道失败，需退出后重新加入频道 TODO
                showToast("加入频道失败！err: " + errCode);
                finish();
                break;
            case QHVCInteractiveConstant.ErrorType.LOADENGINE_ERROR:
                Logger.w(InteractConstant.TAG, "LoadInteractEngine failed! errCode: " + errCode);
                showToast("加载互动直播引擎失败， errCode: " + errCode);
                if (errCode == QHVCInteractiveConstant.SDKServerErrorCode.UNAUTHORIZED_TOKEN_MISS_MATCH) {
                    Intent intent = new Intent();
                    intent.setClass(InteractActivity.this, PrepareInteractBrocastActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(InteractConstant.INTENT_EXTRA_SDK_USIN_RIGHT, false);
                    InteractActivity.this.startActivity(intent);
                }
                finish();
                break;
            case QHVCInteractiveConstant.ErrorType.PUBLISH_ERR:
                //先不用处理
                break;

        }
    }

    @Override
    public void onRtcStats(QHVCInteractiveEventHandler.RtcStats stats) {
        //回调 状态信息
        if (stats != null) {
            codeRate = stats.txKBitRate;
            fps = stats.fps;
            quality = stats.quality;
            if (mRoomMessagePopWindow != null && mRoomMessagePopWindow.isShowing()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showRoomMessage();
                    }
                });
            }
        }
    }

    @Override
    public void onLocalVideoStats(final QHVCInteractiveEventHandler.LocalVideoStats stats) {

    }

    @Override
    public void onChangeClientRoleSuccess(int clientRole) {
        //do nothing
    }

    @Override
    public void onRemoteVideoStats(final QHVCInteractiveEventHandler.RemoteVideoStats stats) {
        //远端视频 回调状态信息
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MyVideoView view = mAllVideoMap.get(stats.uid);
                if (view != null) {
                    if (stats != null) {
                        view.setInfo("Delay :" + stats.delay + "\nWidth :" + stats.width + "\nHeight :" + stats.height
                                + "\nR-Bitrate :" + stats.receivedBitrate + "\nR-Fps :"
                                + stats.receivedFrameRate);
                    }
                }
            }
        });
    }

    @Override
    public void onConnectionLost(int errCode) {
        //  连接丢失回调, 业务需要做UI展示
        showToast("已断开！err: " + errCode);
        finish();
    }

    @Override
    public void onFirstRemoteVideoFrame(String uid, int width, int height, int elapsed) {
        Log.i("HCM","----onFirstRemoteVideoFrame----");
        //        addMixStream(uid);
    }

    //TODO 需考虑纯音频连麦情况
    private void addMixStream(String uid) {
        int size = allMixStreamRegion.size();
        if (size > 0) {

            QHVCInteractiveMixStreamRegion mixStreamRegion = null;
            for (int i = 0; i < allMixStreamRegion.size(); i++) {
                QHVCInteractiveMixStreamRegion mixStreamInfo = allMixStreamRegion.elementAt(i);
                if (mixStreamInfo != null && mixStreamInfo.getUserID().equals(uid)) {
                    mixStreamRegion = mixStreamInfo;
                    break;
                }
            }
            if (mixStreamRegion == null) {
                mixStreamRegion = new QHVCInteractiveMixStreamRegion();
                allMixStreamRegion.add(mixStreamRegion);
            }
            mixStreamRegion.setX(120 * (size - 1));
            mixStreamRegion.setY(240);
            mixStreamRegion.setWidth(120);
            mixStreamRegion.setHeight(212);
            mixStreamRegion.setUserID(uid);
            mixStreamRegion.setzOrder(size);
        }
    }

    private void removeMixStream(String uid) {
        if (TextUtils.isEmpty(uid) == false) {
            for (int i = 0; i < allMixStreamRegion.size(); i++) {
                QHVCInteractiveMixStreamRegion mixStreamInfo = allMixStreamRegion.elementAt(i);
                if (mixStreamInfo != null && mixStreamInfo.getUserID().equals(uid)) {
                    allMixStreamRegion.remove(mixStreamInfo);
                    break;
                }
            }
        }

        updateMixStream();
    }

    /*
    private void updateMixStream() {
        if (!TextUtils.isEmpty(rtmpAddr)) {
            if (mHostInEngine != null) {
                QHLiveCloudHostInEngine.QHMixStreamRegion mixStreamInfoArray[] = new QHLiveCloudHostInEngine.QHMixStreamRegion[allMixStreamRegion.size()];
                allMixStreamRegion.toArray(mixStreamInfoArray);
    
                mHostInEngine.updateMixStreamConfig(mixStreamInfoArray);
            }
        }
    }
    */

    private void exitWorker() {
        if (mWorker != null) {
            mWorker.exit();
        }
        mWorker = null;
        InteractCallback.getInstance().removeCallBack(this);
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
        finish();
    }

    /**
     * 显示房间信息PopWindow
     */
    private void showRoomMessagePopWindow() {
        if (mRoomMessagePopWindow == null) {
            View popView = LayoutInflater.from(this).inflate(R.layout.interact_room_message_popwindow_layout, null);
            initRoomMessageWindowView(popView);
            mRoomMessagePopWindow = new PopupWindow(popView, WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT, true);
            mRoomMessagePopWindow.setOutsideTouchable(true);
            mRoomMessagePopWindow.setBackgroundDrawable(new BitmapDrawable());
            mRoomMessagePopWindow.setFocusable(true);
            mRoomMessagePopWindow.setTouchable(true);
            mRoomMessagePopWindow.setAnimationStyle(R.style.interactpopupWindowAnimation);
            mRoomMessagePopWindow.showAsDropDown(roomMessageButton, 0, 30);

            mRoomMessagePopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                }
            });
            showRoomMessage();
        } else {
            if (!mRoomMessagePopWindow.isShowing()) {
                mRoomMessagePopWindow.showAsDropDown(roomMessageButton, 0, 30);
                showRoomMessage();
            } else {
                mRoomMessagePopWindow.dismiss();
            }
        }
    }

    private void initRoomMessageWindowView(View popView) {
        roomIdTextView = (TextView) popView.findViewById(R.id.interact_room_message_room_id);
        sdkVersionTextView = (TextView) popView.findViewById(R.id.interact_room_message_sdk_version);
        roleTextView = (TextView) popView.findViewById(R.id.interact_room_message_role);
        resolutionRatioView = (TextView) popView.findViewById(R.id.interact_room_message_resolution_ratio);
        codeRateView = (TextView) popView.findViewById(R.id.interact_room_message_code_rate);
        fpsView = (TextView) popView.findViewById(R.id.interact_room_message_fps);
        audioTypeView = (TextView) popView.findViewById(R.id.interact_room_message_audio_type);
        videoQualityView = (TextView) popView.findViewById(R.id.interact_room_message_video_quality);
        netTypeView = (TextView) popView.findViewById(R.id.interact_room_message_net_type);

    }

    private void showRoomMessage() {
        roomIdTextView.setText("房间号：" + iteractRoom.getRoomId());
        sdkVersionTextView.setText("SDK版本号：" + QHVCInteractiveKit.getVersion());
        roleTextView.setText("当前角色：主播");
        resolutionRatioView.setText("分辨率："+ resolution);
        codeRateView.setText("视频码率: " + codeRate + " kbps");
        fpsView.setText("视频帧率：" + fps + " fps");
        videoQualityView.setText("视频质量：" + quality);
        netTypeView.setText("网络类型： " + NetUtil.getNetWorkTypeToString(this));
    }

    /**
     * 显示房间观众PopWindow
     */
    private void showRoomNumberPopWindow() {
        if (mRoomNumberPopWindow == null) {
            View popView = LayoutInflater.from(this).inflate(R.layout.interact_room_number_popwindow_layout, null);
            initNumberPopWindowView(popView);
            mRoomNumberPopWindow = new PopupWindow(popView, WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT, true);
            mRoomNumberPopWindow.setBackgroundDrawable(new BitmapDrawable());
            mRoomNumberPopWindow.setOutsideTouchable(true);
            mRoomNumberPopWindow.setFocusable(true);
            mRoomNumberPopWindow.setTouchable(true);
            mRoomNumberPopWindow.setAnimationStyle(R.style.interactpopupWindowAnimation);
            mRoomNumberPopWindow.showAsDropDown(invitingGuestsButton, 0, 30);
            mRoomNumberPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                }
            });
            getNumberList();
        } else {
            if (!mRoomNumberPopWindow.isShowing()) {
                getNumberList();
                mRoomNumberPopWindow.showAsDropDown(invitingGuestsButton, 0, 30);
            } else {
                mRoomNumberPopWindow.dismiss();
            }
        }
    }

    private void getNumberList() {
        if (workThreadPoolExecutor != null) {
            workThreadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    InteractServerApi.getRoomUserList(myUid, roomId, new int[] {
                            InteractConstant.USER_IDENTITY_ANCHOR, InteractConstant.USER_IDENTITY_GUEST, InteractConstant.USER_IDENTITY_AUDIENCE
                    }, new InteractServerApi.ResultCallback<List<InteractUserModel>>() {
                        @Override
                        public void onSuccess(List<InteractUserModel> data) {
                            ArrayList<InteractUserModel> guestList = new ArrayList<>();
                            ArrayList<InteractUserModel> audienceList = new ArrayList<>();
                            for (InteractUserModel user : data) {
                                if (user.getIdentity() == InteractConstant.USER_IDENTITY_GUEST) {/*嘉宾*/
                                    guestList.add(user);
                                }
                                if (user.getIdentity() == InteractConstant.USER_IDENTITY_ANCHOR) {/*主播*/
                                    anchor = user;
                                }
                                if (user.getIdentity() == InteractConstant.USER_IDENTITY_AUDIENCE) {/*观众*/
                                    audienceList.add(user);
                                }
                            }
                            checkNubmerhasChange(guestList, roomNumberList);
                            roomNumberList.clear();
                            roomNumberList.addAll(guestList);
                            if (mRoomNumberRecyclerViewAdapter != null) {
                                mRoomNumberRecyclerViewAdapter.notifyDataSetChanged();
                            }
                            if (mRoomAudioNumberRecyclerViewAdapter != null) {
                                mRoomAudioNumberRecyclerViewAdapter.notifyDataSetChanged();
                            }
                            onlineNumTextView.setText(1 + guestList.size() + audienceList.size() + "");
                        }

                        @Override
                        public void onFailed(int errCode, String errMsg) {

                        }
                    });
                }
            });
        }
    }

    private void checkNubmerhasChange(List<InteractUserModel> newList, List<InteractUserModel> oldList) {
        boolean isChange = true;
        if (newList.size() == oldList.size() && oldList.containsAll(newList)) {
            isChange = false;
        }
        /*IM发送通知，房间成员已经改变*/
        if (isChange) {
            showToast("房间人员变化");
            //            InteractIMManager.getInstance().sendQuestJoinNotify("",null);
            changeUI(newList);
        }
    }

    private void changeUI(List<InteractUserModel> newList) {
        for (InteractUserModel user : newList) {/*增员*/
            if (!mAllVideoMap.containsKey(user.getUserId())) {
                doRenderRemoteUi(user.getUserId());
            }
        }

        if (mAllVideoMap != null && mAllVideoMap.size() > 0) { /*减员*/
            Set<String> set = mAllVideoMap.keySet();
            for (String uidTmp : set) {
                boolean have = false;
                for (InteractUserModel userModel : newList) {
                    if (userModel.getUserId().equals(uidTmp)) {
                        have = true;
                        break;
                    }
                }
                if (!have) {
                    removeMixStream(uidTmp);
                    removeRemoteVideo(uidTmp, -1);
                }
            }
        }
    }

    private void initNumberPopWindowView(View popView) {
        numberRecyclerView = (RecyclerView) popView.findViewById(R.id.interact_room_number_list);
        numberRecyclerView.setLayoutManager(new LinearLayoutManager(InteractActivity.this));
        DividerItemDecoration decoration = new DividerItemDecoration(InteractActivity.this,
                DividerItemDecoration.VERTICAL);
        decoration.setDrawable(getResources().getDrawable(R.drawable.dash_line));
        numberRecyclerView.addItemDecoration(decoration);
        if (mRoomNumberRecyclerViewAdapter == null) {
            mRoomNumberRecyclerViewAdapter = new InteractRoomNumberRecyclerViewAdapter(roomNumberList, myUid,
                    roomId, InteractActivity.this);
            numberRecyclerView.setAdapter(mRoomNumberRecyclerViewAdapter);
        }
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.interact_room_message:
                showRoomMessagePopWindow();
                break;
            case R.id.interact_room_inviting_guests:
                showRoomNumberPopWindow();
                break;
            case R.id.interact_close_room:
                finish();
                break;
            case R.id.interact_return_messagelayout:
                rootLayout.setVisibility(View.VISIBLE);
                finishFullScreen.setVisibility(View.GONE);
                break;

        }
    }

    InteractIMManager.OnReceiveCommandListener mOnReceiveCommandListener = new InteractIMManager.OnReceiveCommandListener() {
        @Override
        public void onReceiveCommand(final InteractUserModel userFrom, InteractIMManager.CommandInfo command) {
            if (command.getCmd() >= InteractIMManager.CMD_ANCHOR_INVITE_GUEST && command.getCmd() < 20000) {
                if (roomId != null) {
                    if (roomId.compareToIgnoreCase(command.getTarget()) != 0) {
                        return;
                    }
                }
            }

            switch (command.getCmd()) {
                case InteractIMManager.CMD_GUEST_ASK_JOIN:/*观众申请连线*/
                    LibTaskController.post(new Runnable() {
                        @Override
                        public void run() {
                            if (userFrom != null) {
                                applyDialog(userFrom.getUserId() + userFrom.getNickname(), userFrom.getUserId());
                            } else {
                                showToast("申请连线用户的信息为空");
                            }
                        }
                    });

                    break;

                case InteractIMManager.CMD_GUEST_JOIN_NOTIFY:
                    LibTaskController.post(new Runnable() {
                        @Override
                        public void run() {
                            getNumberList();
                        }
                    });
                    break;
                case InteractIMManager.CMD_GUEST_QUIT_NOTIFY:
                    LibTaskController.post(new Runnable() {
                        @Override
                        public void run() {
                            getNumberList();
                        }
                    });
                    break;
            }
        }

        @Override
        public void onReceiveOtherCommand(InteractUserModel userFrom, String str) {
            showToast("-------------");
        }
    };

    private void applyDialog(String message, final String uersId) {
        if (applyList.contains(uersId)) {
            return;
        }
        applyList.add(uersId);
        final TextView mExitCancel;
        final TextView mExitConfirm;
        final BaseDialog mExitDialog = new BaseDialog(this);
        mExitCancel = new TextView(this);
        mExitCancel.setTag(uersId);
        mExitConfirm = new TextView(this);
        mExitConfirm.setTag(uersId);
        mExitCancel.setText("拒绝");
        mExitConfirm.setText("同意");

        mExitCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = (String) mExitCancel.getTag();
                mExitDialog.dismiss();
                applyList.remove(uersId);
                InteractIMManager.getInstance().sendCommand(userId, InteractIMManager.CMD_ANCHOR_REFUSE_JOIN, "", new InteractIMManager.SendMessageCallback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(int errorCode) {

                    }
                });
            }
        });
        mExitConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = (String) mExitConfirm.getTag();
                InteractIMManager.getInstance().sendCommand(userId, InteractIMManager.CMD_ANCHOR_AGREE_JOIN, "", new InteractIMManager.SendMessageCallback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(int errorCode) {

                    }
                });
                mExitDialog.dismiss();
                applyList.remove(uersId);
            }
        });
        mExitDialog.show();
        mExitDialog
                .setChooseDialog(new TextView[] {
                        mExitCancel, mExitConfirm
        });
        mExitDialog.hasTitle(false);
        mExitDialog.setCanceledOnTouchOutside(false);
        mExitDialog.setMsgText(uersId + "申请连线");
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
            if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE
                    && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                rootLayout.setVisibility(View.VISIBLE);
                finishFullScreen.setVisibility(View.GONE);
            }
            //向右滑
            if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
                    && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                rootLayout.setVisibility(View.GONE);
                finishFullScreen.setVisibility(View.VISIBLE);
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    };

    private void joinIMRoom() {

        InteractIMManager.getInstance().joinChatRoom(roomId, new InteractIMManager.ChatRoomResultCallback() {
            @Override
            public void onSuccess(String roomId) {
                showToast("im joinChatRoom success!");
            }

            @Override
            public void onError(int errorCode) {
                showToast("im joinChatRoom error! err:" + errorCode);
            }
        });
    }

    private void leaveIMRoom() {
        InteractIMManager.getInstance().quitChatRoom(new InteractIMManager.ChatRoomResultCallback() {
            @Override
            public void onSuccess(String roomId) {
                showToast("im quitChatRoom success!");
            }

            @Override
            public void onError(int errorCode) {
                showToast("im quitChatRoom error! err:" + errorCode);
            }
        });

    }

    /**
     * 显示纯音频房间人员PopWindow
     */
    private void showAudioNumberPopWindow(RelativeLayout view) {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        View popView = LayoutInflater.from(this).inflate(R.layout.interact_room_audio_number_popwindow_layout, null);
        lp.addRule(RelativeLayout.BELOW, R.id.interact_button_layout);
        lp.topMargin = 20;
        popView.setLayoutParams(lp);
        initAudioNumberPopWindowView(popView);
        view.addView(popView);
        getNumberList();
    }

    private void initAudioNumberPopWindowView(View popView) {
        audioNumberRecyclerView = (RecyclerView) popView.findViewById(R.id.interact_room_audio_number_list);
        audioNumberRecyclerView.setLayoutManager(new LinearLayoutManager(InteractActivity.this));
        DividerItemDecoration decoration = new DividerItemDecoration(InteractActivity.this,
                DividerItemDecoration.VERTICAL);
        decoration.setDrawable(getResources().getDrawable(R.drawable.dash_line));
        audioNumberRecyclerView.addItemDecoration(decoration);
        if (mRoomAudioNumberRecyclerViewAdapter == null) {
            mRoomAudioNumberRecyclerViewAdapter = new InteractRoomAudioNumberRecyclerViewAdapter(roomNumberList, myUid,
                    roomId, InteractActivity.this);
            audioNumberRecyclerView.setAdapter(mRoomAudioNumberRecyclerViewAdapter);
        }
    }

    public QHVCInteractiveKit getInteractEngine() {
        return mInteractEngine;
    }

    public HashMap<String, MyVideoView> getAllUser() {
        return this.mAllVideoMap;
    }

    /**
     * 仅用于测试互动直播接口，业务接入时请忽略
     */
    private void showApiTestView() {
        TestApiPopupWindow apiPopupWindow = new TestApiPopupWindow(InteractActivity.this, mInteractEngine);
        apiPopupWindow.showPopupWindow();
    }

    /**
     * 显示美颜PopWindow
     */
    private void showBeautyPopWindow() {
        mQhvcLiveKitAdvanced.openBeauty();/*开启美颜功能*/
        if (mBeautyPopWindow == null) {
            mBeautyPopWindow = new BeautyPopWindow(this);
            mBeautyPopWindow.showAtLocation(mSurfaceView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            myCommonButton.setVisibility(View.INVISIBLE);
            mBeautyPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    myCommonButton.setVisibility(View.VISIBLE);
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
                myCommonButton.setVisibility(View.INVISIBLE);
            } else {
                mBeautyPopWindow.dismiss();
            }
        }
    }


    private void showFaceUPopWindow() {
        if (mFaceUPopWindow == null) {
            mFaceUPopWindow = new FaceUPopWindow(this);
            mFaceUPopWindow.showAtLocation(mSurfaceView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            myCommonButton.setVisibility(View.INVISIBLE);
            mFaceUPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    myCommonButton.setVisibility(View.VISIBLE);
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
                myCommonButton.setVisibility(View.INVISIBLE);
            } else {
                mFaceUPopWindow.dismiss();
            }
        }
    }

}
