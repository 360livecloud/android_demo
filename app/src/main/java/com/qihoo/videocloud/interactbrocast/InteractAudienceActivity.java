
package com.qihoo.videocloud.interactbrocast;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

import com.qihoo.livecloud.interact.api.QHVCInteractiveConstant;
import com.qihoo.livecloud.interact.api.QHVCInteractiveEventHandler;
import com.qihoo.livecloud.interact.api.QHVCInteractiveKit;
import com.qihoo.livecloud.interact.api.QHVCInteractiveUtils;
import com.qihoo.livecloud.livekit.api.QHVCConstants;
import com.qihoo.livecloud.livekit.api.QHVCFaceUCallBack;
import com.qihoo.livecloud.livekit.api.QHVCLiveKit;
import com.qihoo.livecloud.livekit.api.QHVCLiveKitAdvanced;
import com.qihoo.livecloud.livekit.api.QHVCMediaSettings;
import com.qihoo.livecloud.livekit.api.QHVCSurfaceView;
import com.qihoo.livecloud.tools.Constants;
import com.qihoo.livecloud.tools.LiveCloudConfig;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.tools.NetUtil;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.interactbrocast.adapter.InteractAudienceRoomAudioNumberRecyclerViewAdapter;
import com.qihoo.videocloud.interactbrocast.data.InteractGlobalManager;
import com.qihoo.videocloud.interactbrocast.livingcammera.InteractRecorderController;
import com.qihoo.videocloud.interactbrocast.main.InteractCallBackEvent;
import com.qihoo.videocloud.interactbrocast.main.InteractCallback;
import com.qihoo.videocloud.interactbrocast.main.InteractConstant;
import com.qihoo.videocloud.interactbrocast.main.WorkerThread;
import com.qihoo.videocloud.interactbrocast.modle.InteractRoomModel;
import com.qihoo.videocloud.interactbrocast.modle.InteractUserModel;
import com.qihoo.videocloud.interactbrocast.net.InteractServerApi;
import com.qihoo.videocloud.interactbrocast.ui.MyCommonButton;
import com.qihoo.videocloud.interactbrocast.ui.MyVideoView;
import com.qihoo.videocloud.utils.LibTaskController;
import com.qihoo.videocloud.utils.QHVCSharedPreferences;
import com.qihoo.videocloud.view.BeautyPopWindow;
import com.qihoo.videocloud.view.FaceUPopWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.qihoo.livecloud.tools.Constants.EMode.EMODE_LANDSCAPE;
import static com.qihoo.videocloud.interactbrocast.InteractIMManager.CMD_ANCHOR_AGREE_JOIN;
import static com.qihoo.videocloud.interactbrocast.InteractIMManager.CMD_ANCHOR_KICKOUT_GUEST;
import static com.qihoo.videocloud.interactbrocast.InteractIMManager.CMD_ANCHOR_QUIT_NOTIFY;
import static com.qihoo.videocloud.interactbrocast.InteractIMManager.CMD_ANCHOR_REFUSE_JOIN;
import static com.qihoo.videocloud.interactbrocast.InteractIMManager.CMD_GUEST_JOIN_NOTIFY;
import static com.qihoo.videocloud.interactbrocast.InteractIMManager.CMD_GUEST_QUIT_NOTIFY;
import static com.qihoo.videocloud.interactbrocast.InteractSettingActivity.fpsList;
import static com.qihoo.videocloud.interactbrocast.main.InteractConstant.INTENT_EXTRA_SDK_USIN_RIGHT;

public class InteractAudienceActivity extends BaseActivity implements View.OnClickListener, InteractCallBackEvent {

    public static final int STATE_AUDIENCE = 2; //观众模式
    public static final int STATE_GUEST = 3; //嘉宾模式

    private String myUid;
    private int mCurrState = STATE_AUDIENCE; //默认观众身份

    private InteractRoomModel mRoomModel; //房间信息

    private String roomId; //使用主播的SN做为roomName
    private LiveCloudConfig myConfig;
    private TextView mTextViewRoomName;

    private int mCurrOrientation = Constants.EMode.EMODE_PORTRAIT; //当前方向--横屏或竖屏

    private RelativeLayout mVideoLayer;
    private MyVideoView mMainVideoView;

    //嘉宾相关View
    private MyCommonButton myCommonButton;
    private Button mBtnCloseHostIn;

    private QHVCInteractiveKit mInteractEngine;
    private WorkerThread mWorker;
    private int mUserVideoCapture = InteractConstant.VIDEO_SDK_COMMON_CAPTURE;

    ///////////////For test 为了测试互动直播转推功能 ///////////////////
    private static final String pushAddr = "rtmp://ps1.live.huajiao.com/live_huajiao_v2/_LC_ps1_A01_8976003515217748781240547_OX";
    private static final String pullAddr = "http://pl1.live.huajiao.com/live_huajiao_v2/_LC_ps1_A01_8976003515217748781240547_OX.flv";
    //////////////////////////////////

    //    private UpdateRoom mUpdateRoomForGuest; //嘉宾专用
    //private String mMyHostInSn; //嘉宾连麦返回的SN

    //多人
    private final HashMap<String, MyVideoView> mAllVideoMap = new HashMap<>();

    private TextView roomNameTextView;
    private TextView roomIDTextView;
    private TextView onlineNumTextView;
    private TextView interactTimeTextView;
    private TextView roomMessageButton;
    private TextView invitingGuestsButton;
    private PopupWindow mRoomMessagePopWindow;
    private ImageView exitRoom;
    private ImageView finishFullScreen;

    private String roomName;
    private ExecutorService workThreadPoolExecutor;/*异步线程*/

    private RelativeLayout rootLayout;
    private static final int FLING_MIN_DISTANCE = 200;//  滑动最小距离
    private static final int FLING_MIN_VELOCITY = 200;// 滑动最大速度
    private int interactTimeCount = 0;/*计时*/
    private List<InteractUserModel> roomNumberList = new ArrayList<>();
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

    private InteractAudienceRoomAudioNumberRecyclerViewAdapter mRoomAudioNumberRecyclerViewAdapter;
    private RecyclerView audioNumberRecyclerView;

    //模拟业务做视频采集（支持美颜、faceU等）
//    private QHVCSurfaceView mSurfaceView;
    private QHVCLiveKitAdvanced mQhvcLiveKitAdvanced;
    private int videoEncodeWidth;
    private int videoEncodeHeight;
    private BeautyPopWindow mBeautyPopWindow;
    private FaceUPopWindow mFaceUPopWindow;

    private String resolution;

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

        if (mWorker != null) {
            mWorker.leaveChannel(roomId);
        }

        leaveRoom();
        LibTaskController.postDelayed(new Runnable() {
            @Override
            public void run() {
                exitWorker();
            }
        }, 3000);

        super.onDestroy();
    }

    private void initData() {
        mRoomModel = (InteractRoomModel) getIntent()
                .getSerializableExtra(InteractConstant.INTENT_EXTRA_INTERACT_ROOM_DATA);
        if (mRoomModel != null) {
            roomName = mRoomModel.getRoomName();
            roomId = mRoomModel.getRoomId();
            talkType = mRoomModel.getTalkType();
        }

        myUid = InteractGlobalManager.getInstance().getUser().getUserId();
        Logger.d(InteractConstant.TAG, "----userID : " + myUid);
        if (workThreadPoolExecutor == null) {
            workThreadPoolExecutor = Executors.newSingleThreadExecutor();
        }

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
            mQhvcLiveKitAdvanced.setMediaSettings(mQHVCMediaSettingsBuilder.build());
            QHVCSharedPreferences pref = QHVCSharedPreferences.getInstence();
            int prefIndex = pref.getInt(InteractConstant.GUEST_SETTING_PROFILE_TYPE,
                    InteractConstant.DEFAULT_PROFILE_IDX);
            if (prefIndex > InteractConstant.VIDEO_PROFILES.length - 1) {
                prefIndex = InteractConstant.DEFAULT_PROFILE_IDX;
            }
            mQHVCMediaSettingsBuilder.setFps(Integer.valueOf(fpsList[prefIndex]));
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

    private void showBroadcasterView() {
        if (talkType != InteractConstant.TALK_TYPE_AUDIO) {
            View view = QHVCInteractiveUtils.CreateRendererView(InteractAudienceActivity.this);
            mMainVideoView.setBgView(view, mScreenWidth, mScreenHeight);
            mMainVideoView.setAllButtonVisible(View.GONE);
            mMainVideoView.setUid(mRoomModel.getBindRoleId());

            String streamId = "";
            mInteractEngine.setupRemoteVideo(view, QHVCInteractiveConstant.RenderMode.RENDER_MODE_FIT, mRoomModel.getBindRoleId(), streamId);
        } else {
            mInteractEngine.setupRemoteVideo(null, QHVCInteractiveConstant.RenderMode.RENDER_MODE_FIT, mRoomModel.getBindRoleId(), "");
        }
    }

    private void initView() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_interact_audience);
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
        onlineNumTextView.setText("1");
        interactTimeTextView = (TextView) findViewById(R.id.interact_time);

        mVideoLayer = (RelativeLayout) findViewById(R.id.video_layer);
        mMainVideoView = (MyVideoView) findViewById(R.id.main_video_view);
        mMainVideoView.setButtonVisible(R.id.interact_close_view, View.GONE);
        final GestureDetector mGestureDetector = new GestureDetector(this, learnGestureListener);
        mVideoLayer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });
        //mAllVideoMap.put(myUid, mLargeVideoView);

        View v = findViewById(R.id.common_btn);
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
                showBeautyPopWindow();
            }

            @Override
            public void doOnClickFaceU(ImageView view) {
                showFaceUPopWindow();
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
        if (InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_ONPREVIEWFRAME) {
            myCommonButton.setViewVisible(MyCommonButton.VIEW_BEAUTY, View.GONE);
            myCommonButton.setViewVisible(MyCommonButton.VIEW_FACEU, View.GONE);
        }

        roomMessageButton = (TextView) findViewById(R.id.interact_room_message);
        roomMessageButton.setOnClickListener(this);
        invitingGuestsButton = (TextView) findViewById(R.id.interact_room_inviting_guests);
        invitingGuestsButton.setOnClickListener(this);
        exitRoom = (ImageView) findViewById(R.id.interact_close_room);
        exitRoom.setOnClickListener(this);
        finishFullScreen = (ImageView) findViewById(R.id.interact_return_messagelayout);
        finishFullScreen.setOnClickListener(this);
        rootLayout = (RelativeLayout) findViewById(R.id.interact_room_message_root_layout);

        myCommonButton.setViewVisible(R.id.btn_mute_audio, View.GONE);
        myCommonButton.setViewVisible(R.id.btn_switch_camera, View.GONE);
        myCommonButton.setViewVisible(R.id.btn_enable_video, View.GONE);

        //todo 暂时注掉美颜faceU功能按钮
//        myCommonButton.setViewVisible(R.id.btn_beauty, View.GONE);
//        myCommonButton.setViewVisible(R.id.btn_faceu, View.GONE);
        myCommonButton.setViewVisible(R.id.btn_filter, View.GONE);

        if (talkType == InteractConstant.TALK_TYPE_AUDIO) {
            showAudioNumberPopWindow(rootLayout);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mCurrState == STATE_GUEST) {
            if (mQhvcLiveKitAdvanced != null) {
                mQhvcLiveKitAdvanced.resumePreview();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCurrState == STATE_GUEST) {
            if (mQhvcLiveKitAdvanced != null) {
                mQhvcLiveKitAdvanced.pausePreview();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCurrState == STATE_AUDIENCE) {
        }
    }

    private void changeToGuest() {
        InteractServerApi.changeUserdentity(myUid, roomId, InteractConstant.USER_IDENTITY_GUEST,
                new InteractServerApi.ResultCallback<InteractRoomModel>() {

                    @Override
                    public void onSuccess(InteractRoomModel data) {
                        setClientRoleToGuest();
                    }

                    @Override
                    public void onFailed(int errCode, String errMsg) {
                        showToast(errMsg);
                    }
                });

    }

    private void setClientRoleToGuest() {
        mInteractEngine.setClientRole(QHVCInteractiveConstant.CLIENT_ROLE_BROADCASTER);
        invitingGuestsButton.setText("互动中");
        invitingGuestsButton.setClickable(false);

    }

    private void guestStartPreview() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startPreview();
            }
        });
        mCurrState = STATE_GUEST;
        notifyIMGuestJoin();
        myCommonButton.setViewVisible(R.id.btn_mute_audio, View.VISIBLE);
        if (talkType != InteractConstant.TALK_TYPE_AUDIO) {
            myCommonButton.setViewVisible(R.id.btn_switch_camera, View.VISIBLE);
            myCommonButton.setViewVisible(R.id.btn_enable_video, View.VISIBLE);
        }
    }

    /*嘉宾进入通知*/
    private void notifyIMGuestJoin() {
        InteractIMManager.getInstance().sendNotify(CMD_GUEST_JOIN_NOTIFY, "", new InteractIMManager.SendMessageCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(int errorCode) {

            }
        });
    }

    public void changeToAudience() {
        mInteractEngine.setClientRole(QHVCInteractiveConstant.CLIENT_ROLE_AUDIENCE);
        invitingGuestsButton.setText("互动申请");
        invitingGuestsButton.setClickable(true);

        mCurrState = STATE_AUDIENCE;
        cancelPreview();

        InteractServerApi.changeUserdentity(myUid, roomId, InteractConstant.USER_IDENTITY_AUDIENCE,
                new InteractServerApi.ResultCallback<InteractRoomModel>() {

                    @Override
                    public void onSuccess(InteractRoomModel data) {
                        /*嘉宾退出通知*/
                        InteractIMManager.getInstance().sendNotify(CMD_GUEST_QUIT_NOTIFY, "", new InteractIMManager.SendMessageCallback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError(int errorCode) {

                            }
                        });
                    }

                    @Override
                    public void onFailed(int errCode, String errMsg) {

                    }
                });

        myCommonButton.setViewVisible(R.id.btn_mute_audio, View.GONE);
        if (talkType != InteractConstant.TALK_TYPE_AUDIO) {
            myCommonButton.setViewVisible(R.id.btn_switch_camera, View.GONE);
            myCommonButton.setViewVisible(R.id.btn_enable_video, View.GONE);
        }
    }

    private void startPreview() {
        final MyVideoView videoView = new MyVideoView(InteractAudienceActivity.this);
        int w = 360;
        int h = 640;
        if (mCurrOrientation == EMODE_LANDSCAPE) {
            w = 640;
            h = 360;
        }

        videoView.setClickable(true);
        videoView.setOnTouchListener(InteractAudienceActivity.this);
        videoView.setUid(myUid);
        videoView.setAllButtonVisible(View.GONE);
        videoView.setButtonVisible(R.id.interact_close_view, View.VISIBLE);
        videoView.setVideoViewListener(new MyVideoView.VideoViewListener() {
            @Override
            public void changeToFullScreen(MyVideoView videoView1) {
            }

            @Override
            public void doOnClickMuteRemoteVideo(MyVideoView videoView, TextView tv) {
            }

            @Override
            public void doOnClickMuteRemoteAudio(MyVideoView videoView, TextView tv) {
            }

            @Override
            public void doOnClickfinish(final MyVideoView videoView) {
                changeToAudience();
            }
        });
        mAllVideoMap.put(myUid, videoView);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(w, h);
        Random random = new Random();
        lp.leftMargin = random.nextInt(mScreenWidth - w);
        lp.topMargin = random.nextInt(mScreenHeight - h);
        mVideoLayer.addView(videoView, -1, lp);
        View view = null;
        if (mUserVideoCapture == InteractConstant.VIDEO_USER_CAPTURE) {
            view = new QHVCSurfaceView(InteractAudienceActivity.this);
            if (view instanceof SurfaceView) {
                SurfaceView surfaceV = (SurfaceView) view;
                surfaceV.setZOrderOnTop(true);
                surfaceV.setZOrderMediaOverlay(true);
            }
            mQhvcLiveKitAdvanced.setDisplayPreview((QHVCSurfaceView) view);
            videoView.setBgView(view, w, h);
            mQhvcLiveKitAdvanced.prepare();
            mQhvcLiveKitAdvanced.startPreview();/*开始预览*/

            SurfaceTexture surfaceTexture = mWorker.getSurfaceTexture(videoEncodeWidth, videoEncodeHeight);
            if (surfaceTexture != null) {
                mQhvcLiveKitAdvanced.setSharedSurfaceTexture(surfaceTexture);
            }
            mQhvcLiveKitAdvanced.startEncode();

        } else {
            if (talkType == InteractConstant.TALK_TYPE_AUDIO) {/*纯音频*/
                mWorker.preview(true, null, myUid);
                return;
            }
            view = QHVCInteractiveUtils.CreateRendererView(InteractAudienceActivity.this);
            if (view != null) {
                if (view instanceof SurfaceView) {
                    SurfaceView surfaceV = (SurfaceView) view;
                    surfaceV.setZOrderOnTop(true);
                    surfaceV.setZOrderMediaOverlay(true);
                }
            }
            videoView.setBgView(view, w, h);
            mWorker.preview(true, view, myUid);
        }

    }

    private void cancelPreview() {
        MyVideoView videoView = mAllVideoMap.get(myUid);
        mVideoLayer.removeView(videoView);
        mAllVideoMap.remove(myUid);

        mWorker.preview(false, null, myUid);
    }

    private void exitWorker() {
        if (mWorker != null) {
            mWorker.exit();
            mWorker = null;
        }
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

    private void leaveRoom() {
        if (workThreadPoolExecutor != null) {
            workThreadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    InteractServerApi.userLeaveRoom(myUid, roomId,
                            new InteractServerApi.ResultCallback<InteractRoomModel>() {
                                @Override
                                public void onSuccess(InteractRoomModel data) {
                                    showToast("离开房间");
                                    timeHandler.removeCallbacks(timeClickRunnable);
                                }

                                @Override
                                public void onFailed(int errCode, String errMsg) {
                                    timeHandler.removeCallbacks(timeClickRunnable);
                                }
                            });
                }
            });
        }
    }

    /**
     * 开始观看（观众）
     */
    private void startWatch() {

        doConfigEngine();

        joinChannel();
    }

    /**
     * 关闭连麦
     */

    private void initWorker() {
        if (mWorker == null) {
            mWorker = new WorkerThread(mUserVideoCapture == InteractConstant.VIDEO_USER_CAPTURE);
            mWorker.setTalkType(talkType);
        }
        if (mWorker.isReady() == false) {
            mWorker.start();
            mWorker.waitForReady();
        }

        mInteractEngine = mWorker.getInteractEngine();
    }

    private void loadInteractEngine() {
        InteractCallback.getInstance().addCallBack(InteractAudienceActivity.this);

        Map<String, String> optionInfo = new HashMap<>(); //TODO
        optionInfo.put(QHVCInteractiveConstant.EngineOption.PUSH_ADDR, pushAddr);
        optionInfo.put(QHVCInteractiveConstant.EngineOption.PULL_ADDR, pullAddr);
        mWorker.loadEngine(roomId, myUid, optionInfo);
    }

    private void doConfigEngine() {
        int role = QHVCInteractiveConstant.CLIENT_ROLE_BROADCASTER;
        switch (mCurrState) {
            case STATE_AUDIENCE:
                role = QHVCInteractiveConstant.CLIENT_ROLE_AUDIENCE;
                break;
            case STATE_GUEST:
                role = QHVCInteractiveConstant.CLIENT_ROLE_BROADCASTER;
                break;
        }
        QHVCSharedPreferences pref = QHVCSharedPreferences.getInstence();
        int prefIndex = pref.getInt(InteractConstant.GUEST_SETTING_PROFILE_TYPE,
                InteractConstant.DEFAULT_PROFILE_IDX);
        if (prefIndex > InteractConstant.VIDEO_PROFILES.length - 1) {
            prefIndex = InteractConstant.DEFAULT_PROFILE_IDX;
        }
        int vProfile = InteractConstant.VIDEO_PROFILES[prefIndex];
        setVideoWidthAndHeight(vProfile);
        //int vProfile = 30;
        mWorker.configEngine(role, vProfile, mCurrOrientation);
    }

    private void setVideoWidthAndHeight(int profile) {
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

    }

    public void joinChannel() {
        mWorker.joinChannel();
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

    private void doFullScreen(ImageView btn) {
        if (rootLayout.isShown()) {
            rootLayout.setVisibility(View.GONE);
            finishFullScreen.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoadEngineSuccess(String roomId, String uid) {
        if (!TextUtils.isEmpty(roomId) && !TextUtils.isEmpty(uid)) {
            Logger.i(InteractConstant.TAG, InteractConstant.TAG + ", onLoadEngineSuccess, roomId: " + roomId + ", uid:" + uid);
            if (roomId.equals(this.roomId) && uid.equals(myUid)) {
                startWatch();
            }
        } else {
            showToast("加载互动直播引擎失败");
            Logger.e(InteractConstant.TAG, InteractConstant.TAG + " ERROR! onLoadEngineSuccess(), but uid or roomId is NULL!");
        }
    }

    @Override
    public void onJoinChannelSuccess(String channel, String uid, int elapsed) {
        InteractServerApi.joinRoom(uid, roomId, InteractConstant.USER_IDENTITY_AUDIENCE,
                new InteractServerApi.ResultCallback<InteractRoomModel>() {
                    @Override
                    public void onSuccess(InteractRoomModel data) {
                        showToast("观众加入房间成功！");
                        showBroadcasterView();
                        mWorker.getInteractEngine().setEnableSpeakerphone(true); // 设置使用外放播放声音
                        timeHandler.postDelayed(timeClickRunnable, 1000);
                        if (data.getUserIdentity() == InteractConstant.USER_IDENTITY_GUEST) {/*嘉宾异常退出的情况，需要恢复嘉宾身份*/
                            setClientRoleToGuest();
                        }
                    }

                    @Override
                    public void onFailed(int errCode, String errMsg) {
                        showToast("观众加入房间失败！errCode：" + errCode + "---errMsg:" + errMsg);
                    }
                });
    }

    @Override
    public void onAudioVolumeIndication(QHVCInteractiveEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {

    }

    @Override
    public void onUserOffline(String uid, int reason) {
    }

    @Override
    public void onLeaveChannel(QHVCInteractiveEventHandler.RtcStats stats) {
        exitWorker();
    }

    @Override
    public void onError(int errType, int errCode) {
        switch (errType) {
            case QHVCInteractiveConstant.ErrorType.JOIN_ERR:
                //加入频道失败，需退出后重新加入频道 TODO
                showToast("加入频道失败！" + errCode);
                onBackPressed();
                break;
            case QHVCInteractiveConstant.ErrorType.LOADENGINE_ERROR:
                Logger.w(InteractConstant.TAG, "LoadInteractEngine failed in InteractAudienceActivity.. errCode: " + errCode);
                showToast("加载互动直播引擎失败， errCode: " + errCode);
                if (errCode == QHVCInteractiveConstant.SDKServerErrorCode.UNAUTHORIZED_TOKEN_MISS_MATCH) {
                    Intent intent = new Intent();
                    intent.setClass(InteractAudienceActivity.this, PrepareInteractBrocastActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(INTENT_EXTRA_SDK_USIN_RIGHT, false);
                    InteractAudienceActivity.this.startActivity(intent);
                }
                onBackPressed();
                break;
            case QHVCInteractiveConstant.ErrorType.PUBLISH_ERR:
                //先不用处理
                break;
        }

    }

    @Override
    public void onRtcStats(QHVCInteractiveEventHandler.RtcStats stats) {
        //回调连麦状态信息
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
    public void onConnectionLost(int errCode) {
        //  连接丢失回调, 业务需要做UI展示
        showToast("连麦已断开！err: " + errCode);
        //        if(mCurrState == STATE_GUEST){
        //            changeToAudience();
        //        }
    }

    @Override
    public void onFirstRemoteVideoFrame(String uid, int width, int height, int elapsed) {
        if(uid.equals(mRoomModel.getBindRoleId())){
            resolution = height+"X"+width;
        }
    }

    @Override
    public void onRemoteVideoStats(final QHVCInteractiveEventHandler.RemoteVideoStats stats) {
        //远端视频 回调状态信息
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MyVideoView view = mAllVideoMap.get(stats.uid);
                if (null != view)
                    view.setInfo("Delay :" + stats.delay + "\nWidth :" + stats.width + "\nHeight :" + stats.height
                            + "\nR-Bitrate :" + stats.receivedBitrate + "\nR-Fps :" + stats.receivedFrameRate);
            }
        });
    }

    @Override
    public void onLocalVideoStats(final QHVCInteractiveEventHandler.LocalVideoStats stats) {
    }

    @Override
    public void onChangeClientRoleSuccess(int clientRole) {
        if (clientRole == QHVCInteractiveConstant.CLIENT_ROLE_BROADCASTER) {
            //观众切成嘉宾（主播）
            guestStartPreview();
        }
    }

    public void doRenderRemoteUi(final String uid) {
        Logger.d(InteractConstant.TAG, "ian, doRenderRemoteUi, uid: " + uid);
        Logger.i("CM", "CM----doRenderRemoteUi()-----:uid" + uid);
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

                if (talkType == InteractConstant.TALK_TYPE_AUDIO) {/*纯音频*/
                    mInteractEngine.setupRemoteVideo(null,
                            QHVCInteractiveConstant.RenderMode.RENDER_MODE_FIT, uid, "");
                    return;
                }
                //SurfaceView surfaceV = QHLiveCloudHostInUtils.CreateRendererView(getApplicationContext());
                View view = QHVCInteractiveUtils.CreateRendererView(InteractAudienceActivity.this);
                if (view != null) {
                    if (view instanceof SurfaceView) {
                        SurfaceView surfaceV = (SurfaceView) view;
                        surfaceV.setZOrderOnTop(true);
                        surfaceV.setZOrderMediaOverlay(true);
                    }
                    QHVCInteractiveKit hostInEngine = mWorker.getInteractEngine();
                    if (!myUid.equals(uid)) {
                        hostInEngine.setupRemoteVideo(view, QHVCInteractiveConstant.RenderMode.RENDER_MODE_HIDDEN, uid, "");
                        addRemoteVideo(view, uid);
                    }
                }
            }
        });
    }

    private void addRemoteVideo(View sView, final String uid) {
        final MyVideoView videoView = new MyVideoView(this);
        int w = 360;
        int h = 640;
        if (mCurrOrientation == EMODE_LANDSCAPE) {
            w = 640;
            h = 360;
        }
        videoView.setBgView(sView, w, h);
        videoView.setClickable(true);
        videoView.setOnTouchListener(this);
        videoView.setUid(uid);
        videoView.setAllButtonVisible(View.GONE);
        videoView.setVideoViewListener(new MyVideoView.VideoViewListener() {
            @Override
            public void changeToFullScreen(MyVideoView videoView1) {
                //changeToFullView(videoView1);
            }

            @Override
            public void doOnClickMuteRemoteVideo(MyVideoView videoView, TextView tv) {
                if (videoView.getUid() != myUid) {
                    if (mInteractEngine != null) {
                        boolean muteVideo = videoView.isMuteRemoteVideo();
                        mInteractEngine.muteRemoteVideoStream(videoView.getUid(), muteVideo);
                    }
                }
            }

            @Override
            public void doOnClickMuteRemoteAudio(MyVideoView videoView, TextView tv) {
                if (videoView.getUid() != myUid) {
                    if (mInteractEngine != null) {
                        boolean muteAudio = videoView.isMuteRemoteAudio();
                        mInteractEngine.muteRemoteAudioStream(videoView.getUid(), muteAudio);
                    }
                }
            }

            @Override
            public void doOnClickfinish(MyVideoView videoView) {

            }
        });

        mAllVideoMap.put(uid, videoView);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(w, h);
        Random random = new Random();
        lp.leftMargin = random.nextInt(mScreenWidth - w);
        lp.topMargin = random.nextInt(mScreenHeight - h);
        mVideoLayer.addView(videoView, -1, lp);
    }

    private void changeToFullView(MyVideoView videoView) {
//        if (mMainVideoView.getUid().equals(myUid) || videoView.getUid().equals(myUid)) {
//            if (mGuestRecorderController != null) {
//                mGuestRecorderController.releaseCamera();
//            }
//        }

        videoView.changeToLargeVideoView(mMainVideoView);
        if (videoView.getUid().equals(myUid)) {
            videoView.setButtonVisible(MyVideoView.VIEW_BTN_VIDEO, View.GONE);
            videoView.setButtonVisible(MyVideoView.VIEW_BTN_AUDIO, View.GONE);
        }

        mAllVideoMap.remove(mMainVideoView.getUid());
        mAllVideoMap.remove(videoView.getUid());
        mAllVideoMap.put(mMainVideoView.getUid(), mMainVideoView);
        mAllVideoMap.put(videoView.getUid(), videoView);

        if (mMainVideoView.getUid().equals(myUid) == false) {
            //拉大流
            mInteractEngine.setRemoteVideoStream(mMainVideoView.getUid(), QHVCInteractiveConstant.VIDEO_STREAM_HIGH);
            mBtnCloseHostIn.setVisibility(View.GONE);
        } else {
            mBtnCloseHostIn.setVisibility(View.VISIBLE);
        }

        if (videoView.getUid().equals(myUid) == false) {
            //换小流
            mInteractEngine.setRemoteVideoStream(videoView.getUid(), QHVCInteractiveConstant.VIDEO_STREAM_LOW);
        }

//        if (mMainVideoView.getUid().equals(myUid) || videoView.getUid().equals(myUid)) {
//            if (mGuestRecorderController != null) {
//                LibTaskController.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        mGuestRecorderController.resumeCamera();
//                    }
//                }, 300);
//            }
//        }
    }

    private void removeRemoteVideo(String uid, int reason, String streamId) {
        if (mAllVideoMap.containsKey(uid) && uid.equals(myUid) == false) {
            MyVideoView videoView = mAllVideoMap.get(uid);
            if (videoView != null) {
                if (videoView == mMainVideoView) {
                    MyVideoView localVideoView = null;
                    Set<String> set = mAllVideoMap.keySet();
                    for (String key : set) {
                        MyVideoView videoViewTmp = mAllVideoMap.get(key);
                        if (videoViewTmp != null) {
                            if (videoViewTmp.getUid().equals(myUid)) {
                                localVideoView = videoViewTmp;
                                break;
                            }
                        }
                    }
                    changeToFullView(localVideoView);
                }
                videoView = mAllVideoMap.get(uid);
                mVideoLayer.removeView(videoView);
                mAllVideoMap.remove(uid);
                mInteractEngine.removeRemoteVideo(streamId, uid);
                Logger.i("CM", "CM----removeRemoteVideo()-----:uid" + uid);
                Logger.d(InteractConstant.TAG,
                        InteractConstant.TAG + ": removeRemoteVideo success, uid: " + uid + ", reason: " + reason);
            } else {
                Logger.w(InteractConstant.TAG,
                        InteractConstant.TAG + ": videoView is null, uid: " + uid + ",removeRemoteVideo failed!");
            }
        }
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
        roomIdTextView.setText("房间号：" + mRoomModel.getRoomId());
        sdkVersionTextView.setText("SDK版本号：" + QHVCInteractiveKit.getVersion());
        if (mCurrState == STATE_AUDIENCE) {
            roleTextView.setText("当前角色：观众");
        } else {
            roleTextView.setText("当前角色：嘉宾");
        }
        resolutionRatioView.setText("分辨率："+resolution);
        codeRateView.setText("视频码率: " + codeRate + " kbps");
        fpsView.setText("视频帧率：" + fps + " fps");
        videoQualityView.setText("视频质量：" + quality);
        netTypeView.setText("网络类型： " + NetUtil.getNetWorkTypeToString(this));
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
                applyInteract();/*申请互动*/
                break;
            case R.id.interact_close_room:
                onBackPressed();
                break;
            case R.id.interact_return_messagelayout:
                rootLayout.setVisibility(View.VISIBLE);
                finishFullScreen.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * 申请连线
     */
    private void applyInteract() {
        InteractIMManager.getInstance().sendCommand(mRoomModel.getBindRoleId(), InteractIMManager.CMD_GUEST_ASK_JOIN, "", new InteractIMManager.SendMessageCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(int errorCode) {

            }
        });
    }

    @Override
    protected void doAgreeRecordAudio() {
        if (checkSelfPermissions()) {

        }
    }

    @Override
    protected void doAgreeCamera() {
        if (checkSelfPermissions()) {

        }
    }

    @Override
    protected void doRefuseRecordAudio() {
        showToast("未获得录音权限，无法连麦");
    }

    @Override
    protected void doRefuseCamera() {
        showToast("未获得摄像头权限，无法连麦");
    }

    Handler timeHandler = new Handler();
    Runnable timeClickRunnable = new Runnable() {
        @Override
        public void run() {
            interactTimeCount++;
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
                                    if (errCode == 10001 && "该房间不存在".equals(errMsg)) {/*主播被踢出*/
                                        showToast("主播掉线");
                                        onBackPressed();
                                    }
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

    private void getNumberList() {
        if (workThreadPoolExecutor != null) {
            workThreadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    InteractServerApi.getRoomUserList(myUid, roomId, new int[]{
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
                                if (user.getIdentity() == InteractConstant.USER_IDENTITY_ANCHOR) {/*嘉宾*/
                                    anchor = user;
                                }
                                if (user.getIdentity() == InteractConstant.USER_IDENTITY_AUDIENCE) {/*嘉宾*/
                                    audienceList.add(user);
                                }
                            }
                            checkNubmerhasChange(guestList, roomNumberList);
                            roomNumberList.clear();
                            roomNumberList.addAll(guestList);
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
            changeUI(newList);
        }
    }

    private void changeUI(List<InteractUserModel> newList) {
        Logger.i("CM", "CM----changeUI-----:");
        for (InteractUserModel user : newList) {/*增员*/
            if (!mAllVideoMap.containsKey(user.getUserId())) {
                doRenderRemoteUi(user.getUserId());
            }
        }

        if (mAllVideoMap != null && mAllVideoMap.size() > 0) {/*减员*/
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
                    removeRemoteVideo(uidTmp, -1, "");
                }
            }
        }

    }

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

    private void TestSendMsg() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                InteractIMManager.getInstance().sendNotify(InteractIMManager.CMD_ANCHOR_AGREE_JOIN, "", null);
            }
        });
    }

    InteractIMManager.OnReceiveCommandListener mOnReceiveCommandListener = new InteractIMManager.OnReceiveCommandListener() {

        @Override
        public void onReceiveCommand(InteractUserModel userFrom, InteractIMManager.CommandInfo command) {
            if (command != null) {
                if (command.getCmd() >= InteractIMManager.CMD_ANCHOR_INVITE_GUEST && command.getCmd() < 20000) {
                    if (mRoomModel != null) {
                        if ((mRoomModel.getRoomId().compareToIgnoreCase(command.getTarget())) != 0) {
                            return;
                        }
                    }
                }

                switch (command.getCmd()) {
                    case CMD_ANCHOR_AGREE_JOIN:/*主播同意连麦*/
                        LibTaskController.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mCurrState == STATE_AUDIENCE) {
                                    showToast("主播同意连线");
                                    //互动申请
                                    changeToGuest();

                                }
                            }
                        });
                        break;
                    case CMD_ANCHOR_REFUSE_JOIN:/*拒绝*/
                        LibTaskController.post(new Runnable() {
                            @Override
                            public void run() {
                                showToast("主播拒绝连线");
                            }
                        });
                        break;

                    case CMD_ANCHOR_KICKOUT_GUEST:/*踢出*/
                        LibTaskController.post(new Runnable() {
                            @Override
                            public void run() {
                                showToast("已被主播踢出");
                                changeToAudience();
                            }
                        });

                        break;

                    case CMD_ANCHOR_QUIT_NOTIFY:
                        LibTaskController.post(new Runnable() {
                            @Override
                            public void run() {
                                showToast("主播已退出");
                                onBackPressed();
                            }
                        });
                        break;
                    case CMD_GUEST_JOIN_NOTIFY:
                        LibTaskController.post(new Runnable() {
                            @Override
                            public void run() {
                                getNumberList();
                            }
                        });
                        break;
                    case CMD_GUEST_QUIT_NOTIFY:
                        LibTaskController.post(new Runnable() {
                            @Override
                            public void run() {
                                getNumberList();
                            }
                        });
                        break;

                }
            }
        }

        @Override
        public void onReceiveOtherCommand(InteractUserModel userFrom, String str) {

        }
    };

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

    /**
     * 显示纯音频房间人员PopWindow
     */
    private void showAudioNumberPopWindow(RelativeLayout view) {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        View popView = LayoutInflater.from(this).inflate(R.layout.interact_room_audio_number_popwindow_layout, null);
        lp.addRule(RelativeLayout.BELOW, R.id.interact_audience_button_layout);
        lp.topMargin = 20;
        popView.setLayoutParams(lp);
        initAudioNumberPopWindowView(popView);
        view.addView(popView);
        getNumberList();
    }

    private void initAudioNumberPopWindowView(View popView) {
        audioNumberRecyclerView = (RecyclerView) popView.findViewById(R.id.interact_room_audio_number_list);
        audioNumberRecyclerView.setLayoutManager(new LinearLayoutManager(InteractAudienceActivity.this));
        DividerItemDecoration decoration = new DividerItemDecoration(InteractAudienceActivity.this,
                DividerItemDecoration.VERTICAL);
        decoration.setDrawable(getResources().getDrawable(R.drawable.dash_line));
        audioNumberRecyclerView.addItemDecoration(decoration);
        if (mRoomAudioNumberRecyclerViewAdapter == null) {
            mRoomAudioNumberRecyclerViewAdapter = new InteractAudienceRoomAudioNumberRecyclerViewAdapter(roomNumberList, myUid,
                    roomId, InteractAudienceActivity.this);
            audioNumberRecyclerView.setAdapter(mRoomAudioNumberRecyclerViewAdapter);
        }
    }

    public QHVCInteractiveKit getInteractEngine() {
        return mInteractEngine;
    }

    /**
     * 显示美颜PopWindow
     */
    private void showBeautyPopWindow() {
        mQhvcLiveKitAdvanced.openBeauty();/*开启美颜功能*/
        if (mBeautyPopWindow == null) {
            mBeautyPopWindow = new BeautyPopWindow(this);
            mBeautyPopWindow.showAtLocation(mMainVideoView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
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
                mBeautyPopWindow.showAtLocation(mMainVideoView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                myCommonButton.setVisibility(View.INVISIBLE);
            } else {
                mBeautyPopWindow.dismiss();
            }
        }
    }


    private void showFaceUPopWindow() {
        if (mFaceUPopWindow == null) {
            mFaceUPopWindow = new FaceUPopWindow(this);
            mFaceUPopWindow.showAtLocation(mMainVideoView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
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
                mFaceUPopWindow.showAtLocation(mMainVideoView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                myCommonButton.setVisibility(View.INVISIBLE);
            } else {
                mFaceUPopWindow.dismiss();
            }
        }
    }
}
