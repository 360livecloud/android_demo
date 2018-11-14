
package com.qihoo.videocloud.interactbrocast;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo.livecloud.interact.api.QHVCInteractiveConstant;
import com.qihoo.livecloud.interact.api.QHVCInteractiveEventHandler;
import com.qihoo.livecloud.interact.api.QHVCInteractiveKit;
import com.qihoo.livecloud.interact.api.QHVCInteractiveUtils;
import com.qihoo.livecloud.tools.Constants;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.interactbrocast.adapter.InteractPartyRecyclerViewAdapter;
import com.qihoo.videocloud.interactbrocast.data.InteractGlobalManager;
import com.qihoo.videocloud.interactbrocast.main.InteractCallBackEvent;
import com.qihoo.videocloud.interactbrocast.main.InteractCallback;
import com.qihoo.videocloud.interactbrocast.main.InteractConstant;
import com.qihoo.videocloud.interactbrocast.main.WorkerThread;
import com.qihoo.videocloud.interactbrocast.modle.InteractRoomModel;
import com.qihoo.videocloud.interactbrocast.modle.InteractUserModel;
import com.qihoo.videocloud.interactbrocast.net.InteractServerApi;
import com.qihoo.videocloud.interactbrocast.party.PartyRoleItem;
import com.qihoo.videocloud.interactbrocast.ui.MyCommonButton;
import com.qihoo.videocloud.utils.LibTaskController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.qihoo.videocloud.interactbrocast.main.InteractConstant.USER_IDENTITY_ANCHOR;

/**
 * Created by liuyanqing on 2018/3/7.
 */

public class PartyMainActivity extends BaseActivity implements InteractCallBackEvent, View.OnClickListener {

    public static final int MODE_AUDIENCE = 1; //观众模式
    public static final int MODE_PLAYER = 2; //参与轰趴模式

    private RecyclerView mPartyGridView;
    private InteractPartyRecyclerViewAdapter mGridAdapter;

    private ArrayList<PartyRoleItem> mAllShowData = new ArrayList<>();
    //    private Hashtable<String, PartyRoleItem> mCurrUserList = new Hashtable<>(); //存放当前房间内参与轰趴的主播
    private List<InteractUserModel> mCurrUserList = new ArrayList<>();

    private int currPlayMode; //当前模式(参与模式 或 观众模式)

    private PartyRoleItem mSelfRoleItem; //自己的Item
    private View selfView; //自己的预览View
    private Bitmap mDefaultImage;

    private InteractUserModel mSelfUserModel;
    private String myUid;

    private InteractRoomModel iteractPartyRoom;
    private String roomName;
    private String roomId;
    private int onlineNum;
    private TextView roomNameTextView;
    private TextView roomIDTextView;
    private TextView onlineNumTextView;
    private MyCommonButton myCommonButton;

    //Test Todo
    TextView joinBtn;
    TextView leaveBtn;
    private ImageView exitRoom;
    private int talkType;/*音频或者视频房间*/

    //连麦
    private WorkerThread mWorker;
    private QHVCInteractiveKit mInteractEngine;

    private int interactTimeCount = 0;/*计时*/
    private ExecutorService workThreadPoolExecutor = Executors.newSingleThreadExecutor();;/*异步线程*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        initView();
        initWorker();
        loadInteractEngine();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        leaveChannel();
        leaveRoom();
        super.onDestroy();
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

    private void initView() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_party_main_layout);
        mPartyGridView = (RecyclerView) findViewById(R.id.party_gridView);
        GridLayoutManager mgr = new GridLayoutManager(PartyMainActivity.this, 2);
        mPartyGridView.setLayoutManager(mgr);
        mGridAdapter = new InteractPartyRecyclerViewAdapter(mAllShowData);
        int itemw = mScreenWidth / 2;
        int itemh = (mScreenHeight - getStatusBarHeight() - getNavigationHeight()) / 3;
        mGridAdapter.setItemWidth(itemw);
        mGridAdapter.setItemHeight(itemh);
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", mGridAdapter，width:" + itemw + ", height:" + itemh + ", screenw: " + mScreenWidth + ", screenH: " + mScreenHeight +
                ",statusBarHeight" + getStatusBarHeight() + ", NavigationHeight: " + getNavigationHeight());
        mPartyGridView.setAdapter(mGridAdapter);

        //TODO for test
        joinBtn = (TextView) findViewById(R.id.button_join);
        joinBtn.setOnClickListener(this);
        leaveBtn = (TextView) findViewById(R.id.button_leave);
        leaveBtn.setOnClickListener(this);
        switch (currPlayMode) {
            case MODE_AUDIENCE:
                leaveBtn.setVisibility(View.GONE);
                break;
            case MODE_PLAYER:
                joinBtn.setVisibility(View.GONE);
                break;
        }

        exitRoom = (ImageView) findViewById(R.id.interact_close_room);
        exitRoom.setOnClickListener(this);
        roomNameTextView = (TextView) findViewById(R.id.interact_room_name);
        roomNameTextView.setText(roomName);
        roomIDTextView = (TextView) findViewById(R.id.interact_room_id);
        roomIDTextView.setText(roomId);
        onlineNumTextView = (TextView) findViewById(R.id.interact_room_online_num);

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

            }

            @Override
            public void doOnClickFaceU(ImageView view) {

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
                //                doFullScreen(view);
            }

            @Override
            public void doOnClickFilter(ImageView view) {

            }

        };

        if (currPlayMode == MODE_AUDIENCE) {
            myCommonButton.setViewVisible(R.id.btn_mute_audio, View.GONE);
            myCommonButton.setViewVisible(R.id.btn_switch_camera, View.GONE);
            myCommonButton.setViewVisible(R.id.btn_enable_video, View.GONE);
        }

        //todo 暂时注掉美颜faceU功能按钮
        myCommonButton.setViewVisible(R.id.btn_beauty, View.GONE);
        myCommonButton.setViewVisible(R.id.btn_faceu, View.GONE);
        myCommonButton.setViewVisible(R.id.btn_filter, View.GONE);
        myCommonButton.setViewVisible(R.id.btn_full_screen, View.GONE);

    }

    private void initData() {
        iteractPartyRoom = (InteractRoomModel) getIntent()
                .getSerializableExtra(InteractConstant.INTENT_EXTRA_INTERACT_ROOM_DATA);
        if (iteractPartyRoom != null) {
            roomName = iteractPartyRoom.getRoomName();
            roomId = iteractPartyRoom.getRoomId();
            onlineNum = iteractPartyRoom.getOnlineNum();
            talkType = iteractPartyRoom.getTalkType();
        }

        int playMode = getIntent().getIntExtra(InteractConstant.INTENT_EXTRA_USER_IDENTITY, InteractConstant.USER_IDENTITY_AUDIENCE);
        switch (playMode) {
            case InteractConstant.USER_IDENTITY_AUDIENCE:
                this.currPlayMode = MODE_AUDIENCE;
                break;
            case USER_IDENTITY_ANCHOR:
                this.currPlayMode = MODE_PLAYER;
                break;
        }

        TypedArray imgs = getResources().obtainTypedArray(R.array.image_ids);
        int id = imgs.getResourceId(0, -1);
        mDefaultImage = BitmapFactory.decodeResource(getResources(), id);

        mSelfUserModel = InteractGlobalManager.getInstance().getUser();
        if (mSelfUserModel != null) {
            myUid = mSelfUserModel.getUserId();
        }
    }

    private void startParty() {
        if (currPlayMode == MODE_PLAYER) {
            doConfigEngine(QHVCInteractiveConstant.CLIENT_ROLE_BROADCASTER);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    createMyself();
                }
            });
        } else {
            doConfigEngine(QHVCInteractiveConstant.CLIENT_ROLE_AUDIENCE);
        }
        InteractCallback.getInstance().addCallBack(PartyMainActivity.this);
        //设置小流参数
        if (mInteractEngine != null) {
            mInteractEngine.enableDualStreamMode(true); //开启双流模式（即大小流）
            mInteractEngine.setLowStreamVideoProfile(180, 320, 15, 180);
        }
        joinChannel();
    }

    private void createMyself() {
        if (mSelfRoleItem == null) {
            mSelfRoleItem = new PartyRoleItem(myUid, mDefaultImage);
            if (selfView == null) {
                selfView = QHVCInteractiveUtils.CreateRendererView(PartyMainActivity.this);
                if (selfView != null) {
                    if (selfView instanceof SurfaceView) {
                        SurfaceView surfaceV = (SurfaceView) selfView;
                        surfaceV.setZOrderOnTop(true);
                        surfaceV.setZOrderMediaOverlay(true);
                    }
                }
            }

            mSelfRoleItem.setVideoView(selfView);
            if (!mCurrUserList.contains(mSelfUserModel)) {
                if (currPlayMode == MODE_PLAYER) {
                    mSelfUserModel.setIdentity(USER_IDENTITY_ANCHOR);
                }
                mCurrUserList.add(mSelfUserModel);
            }
        }
        mAllShowData.add(mSelfRoleItem);
        mGridAdapter.notifyDataSetChanged();

    }

    private void stopPreview() {
        if (mSelfRoleItem != null) {
            mCurrUserList.remove(mSelfUserModel);
            mAllShowData.remove(mSelfRoleItem);
            mGridAdapter.notifyDataSetChanged();
            if (mWorker != null) {
                mWorker.preview(false, mSelfRoleItem.getVideoView(), myUid);
            }
            mSelfRoleItem = null;
        }
    }

    private void startPreview() {
        if (mWorker != null) {
            mWorker.preview(true, mSelfRoleItem.getVideoView(), myUid);
        }
    }

    private void initWorker() {
        if (mWorker == null) {
            //TODO 暂时不考虑美颜
            mWorker = new WorkerThread(false);
        }

        mWorker.start();
        mWorker.waitForReady();
        mInteractEngine = mWorker.getInteractEngine();
    }

    private void loadInteractEngine() {
        InteractCallback.getInstance().addCallBack(PartyMainActivity.this);

        Map<String, String> optionInfo = new HashMap<>(); //TODO
        mWorker.loadEngine(roomId, myUid, optionInfo);
    }

    private void doConfigEngine(int cRole) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int prefIndex = pref.getInt(InteractConstant.PrefManager.PREF_PROPERTY_PROFILE_IDX,
                InteractConstant.DEFAULT_PROFILE_IDX);
        if (prefIndex > InteractConstant.VIDEO_PROFILES.length - 1) {
            prefIndex = InteractConstant.DEFAULT_PROFILE_IDX;
        }
        int vProfile = InteractConstant.VIDEO_PROFILES[prefIndex];

        //int vProfile = 30;
        mWorker.configEngine(cRole, vProfile, Constants.EMode.EMODE_PORTRAIT);
    }

    private void joinChannel() {
        if (mWorker != null) {
            mWorker.joinChannel();
        } else {
            Logger.w(InteractConstant.TAG, "Error! joinChannel failed, mWorker is null!");
            showToast("Error! mWorker is null!");
        }
    }

    private void getUserListFromServer() {
        int[] userIdentitys = new int[] {
                USER_IDENTITY_ANCHOR,
                InteractConstant.USER_IDENTITY_GUEST
        };

        InteractServerApi.getRoomUserList(myUid, roomId, userIdentitys,
                new InteractServerApi.ResultCallback<List<InteractUserModel>>() {

                    @Override
                    public void onSuccess(final List<InteractUserModel> data) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (data != null) {
                                    onlineNumTextView.setText(data.size()+"");
                                }
                            }
                        });
                        checkNubmerhasChange(data);
                    }

                    @Override
                    public void onFailed(int errCode, String errMsg) {
                        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", getRoomUserList failed.. errCode: " + errCode + ", errMsg: " + errMsg);
                    }
                });
    }

    private void checkNubmerhasChange(List<InteractUserModel> data) {
        boolean isChange = true;
        if (mCurrUserList.size() == data.size() && mCurrUserList.containsAll(data)) {
            isChange = false;

        }
        /*IM发送通知，房间成员已经改变*/
        if (isChange) {
            showToast("房间人员变化");
            changeUI(data);
        }
    }

    private void changeUI(List<InteractUserModel> newList) {
        for (InteractUserModel user : newList) {/*增员*/
            if (!mCurrUserList.contains(user)) {
                addRemoteUser(user);
            }
        }
        if (mCurrUserList != null) {/*减员*/
            for (InteractUserModel userModel : mCurrUserList) {
                if (!newList.contains(userModel)) {
                    removeRemoteUser(userModel);
                    break;
                }
            }
        }
    }

    private void addRemoteUser(final InteractUserModel user) {/*增员*/
        if (user.getUserId().equals(myUid)) {/*自己不再此做处理*/
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PartyRoleItem roleItem = new PartyRoleItem(user.getUserId(), mDefaultImage);
                View videoView = QHVCInteractiveUtils.CreateRendererView(PartyMainActivity.this);
                if (videoView instanceof SurfaceView) {
                    SurfaceView surfaceV = (SurfaceView) videoView;
                    surfaceV.setZOrderOnTop(true);
                    surfaceV.setZOrderMediaOverlay(true);
                }
                roleItem.setVideoView(videoView);
                mCurrUserList.add(user);
                mAllShowData.add(roleItem);
                mInteractEngine.setupRemoteVideo(videoView, QHVCInteractiveConstant.RenderMode.RENDER_MODE_HIDDEN,
                        user.getUserId(), "");
                if (mGridAdapter != null) {
                    mGridAdapter.notifyDataSetChanged();
                }
            }
        });

    }

    private void removeRemoteUser(final InteractUserModel user) {
        if (user.getUserId().equals(myUid)) {/*自己不再此做处理*/
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCurrUserList.remove(user);
                PartyRoleItem removeRoleItem = null;
                for (PartyRoleItem roleItem : mAllShowData) {
                    if (roleItem.getUserId().equals(user.getUserId())) {
                        removeRoleItem = roleItem;
                        break;
                    }
                }
                if (removeRoleItem != null) {
                    if (mInteractEngine != null) {
                        mInteractEngine.removeRemoteVideo("", removeRoleItem.getUserId());
                    }
                    mAllShowData.remove(removeRoleItem);
                }
                if (mGridAdapter != null) {
                    mGridAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void leaveChannel() {
        if (mWorker != null) {
            mWorker.leaveChannel(roomId);
        }
        LibTaskController.postDelayed(new Runnable() {
            @Override
            public void run() {
                exitWorker();
            }
        }, 3000);

    }

    private void exitWorker() {
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
        if (mWorker != null) {
            mWorker.exit();
        }
        mWorker = null;
        InteractCallback.getInstance().removeCallBack(this);

        finish();
    }

    private void joinRoomToServer() {
        InteractServerApi.joinRoom(myUid, roomId, InteractConstant.USER_IDENTITY_AUDIENCE,
                new InteractServerApi.ResultCallback<InteractRoomModel>() {
                    @Override
                    public void onSuccess(InteractRoomModel data) {
                        Toast.makeText(PartyMainActivity.this, "加入频道成功！", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(int errCode, String errMsg) {
                        showToast("joinRoomToServer fail!, errcode:" + errCode + ", " + errMsg);
                    }
                });
    }

    private void changeUserdentity(int userIdentity) {
        InteractServerApi.changeUserdentity(myUid, roomId, userIdentity, new InteractServerApi.ResultCallback<InteractRoomModel>() {

            @Override
            public void onSuccess(InteractRoomModel data) {
                //                getUserListFromServer();
            }

            @Override
            public void onFailed(int errCode, String errMsg) {
                Logger.e(InteractConstant.TAG, InteractConstant.TAG + ", changeUserdentity failed, errCode : " + errCode + ", errMsg:" + errMsg);
                showToast("changeUserdentity fail!, errcode:" + errCode + ", " + errMsg);
            }
        });
    }

    @Override
    public void onLoadEngineSuccess(String roomId, String uid) {
        startParty();
    }

    @Override
    public void onJoinChannelSuccess(String channel, String uid, int elapsed) {
        mInteractEngine.setEnableSpeakerphone(false);
        joinRoomToServer();
        if (currPlayMode == MODE_PLAYER) {
            startPreview();
        } else {
            getUserListFromServer();/*主动请求一次*/
        }
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", onJoinChannelSuccess(), channel: " + channel + ", uid: " + uid);
        startClick();
    }

    @Override
    public void onAudioVolumeIndication(QHVCInteractiveEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {

    }

    /*开始心态*/
    private void startClick() {
        timeHandler.postDelayed(timeClickRunnable, 1000);
    }

    /*停止心跳*/
    private void stopClick() {
        timeHandler.removeCallbacks(timeClickRunnable);
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

                                }
                            });
                        }
                    });

                }

            }
            //            /*定时刷成员列表，监听人员变化，三秒一次*/
            if (interactTimeCount % 3 == 0) {
                getUserListFromServer();
            }
        }
    };

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
                leaveChannel();
                break;
            case QHVCInteractiveConstant.ErrorType.LOADENGINE_ERROR:
                Logger.w(InteractConstant.TAG, "LoadInteractEngine failed in PartyMainActivity.. errCode: " + errCode);
                showToast("加载互动直播引擎失败， errCode: " + errCode);
                leaveChannel();
                break;
            case QHVCInteractiveConstant.ErrorType.PUBLISH_ERR:
                //先不用处理
                break;
        }
    }

    @Override
    public void onRtcStats(QHVCInteractiveEventHandler.RtcStats stats) {

    }

    @Override
    public void onConnectionLost(int errCode) {

    }

    @Override
    public void onFirstRemoteVideoFrame(String uid, int width, int height, int elapsed) {

    }

    @Override
    public void onRemoteVideoStats(QHVCInteractiveEventHandler.RemoteVideoStats stats) {

    }

    @Override
    public void onLocalVideoStats(QHVCInteractiveEventHandler.LocalVideoStats stats) {

    }

    @Override
    public void onChangeClientRoleSuccess(int clientRole) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_join:
                joinParty();
                break;
            case R.id.button_leave:
                leaveParty();
                break;
            case R.id.interact_close_room:
                onBackPressed();
                break;
        }

    }

    private void leaveParty() {
        changeUserdentity(InteractConstant.USER_IDENTITY_AUDIENCE);
        doConfigEngine(QHVCInteractiveConstant.CLIENT_ROLE_AUDIENCE);
        stopPreview();
        joinBtn.setVisibility(View.VISIBLE);
        leaveBtn.setVisibility(View.GONE);
        myCommonButton.setViewVisible(R.id.btn_mute_audio, View.GONE);
        if (talkType != InteractConstant.TALK_TYPE_AUDIO) {
            myCommonButton.setViewVisible(R.id.btn_switch_camera, View.GONE);
            myCommonButton.setViewVisible(R.id.btn_enable_video, View.GONE);
        }
    }

    private void joinParty() {
        //加入轰趴
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doConfigEngine(QHVCInteractiveConstant.CLIENT_ROLE_BROADCASTER);
                currPlayMode = MODE_PLAYER;
                createMyself();
                startPreview();
                changeUserdentity(USER_IDENTITY_ANCHOR);
            }
        });
        joinBtn.setVisibility(View.GONE);
        leaveBtn.setVisibility(View.VISIBLE);
        myCommonButton.setViewVisible(R.id.btn_mute_audio, View.VISIBLE);
        if (talkType != InteractConstant.TALK_TYPE_AUDIO) {
            myCommonButton.setViewVisible(R.id.btn_switch_camera, View.VISIBLE);
            myCommonButton.setViewVisible(R.id.btn_enable_video, View.VISIBLE);
        }
    }

    // 切换摄像头
    private void doSwitchCamera() {
        if (mInteractEngine != null) {
            mInteractEngine.switchCamera();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
