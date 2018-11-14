package com.qihoo.videocloud.interactbrocast.ui;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;

import com.qihoo.livecloud.interact.api.QHVCInteractiveConstant;
import com.qihoo.livecloud.interact.api.QHVCInteractiveKit;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.interactbrocast.InteractActivity;
import com.qihoo.videocloud.interactbrocast.data.InteractGlobalManager;
import com.qihoo.videocloud.interactbrocast.main.InteractConstant;

import java.util.HashMap;

import static com.qihoo.livecloud.interact.api.QHVCInteractiveConstant.RenderMode.RENDER_MODE_ADAPTIVE;
import static com.qihoo.livecloud.interact.api.QHVCInteractiveConstant.RenderMode.RENDER_MODE_HIDDEN;

/**
 * Created by liuyanqing on 2018/5/14.
 */

public class TestApiPopupWindow implements View.OnClickListener {

    private Activity mActivity;

    private PopupWindow mPopWindow;

    private QHVCInteractiveKit mInteractEngine;

    private String myUid;

    private boolean mEnableLocalVideo = true;

    private boolean mEnableAudioVolumeIndication = false;

    private boolean mMutedAll = false;

    private int mRemoteRenderMode = RENDER_MODE_HIDDEN;

    private int mRemoteVideoStream = QHVCInteractiveConstant.VIDEO_STREAM_HIGH;

    public TestApiPopupWindow(Activity activity, QHVCInteractiveKit engine) {
        this.mActivity = activity;
        mInteractEngine = engine;
        myUid = InteractGlobalManager.getInstance().getUser().getUserId();
    }


    public void showPopupWindow() {
        if (mPopWindow == null) {
            View popView = LayoutInflater.from(mActivity).inflate(R.layout.interact_inactive_interface_popwindow_layout, null);
            initView(popView);
            mPopWindow = new PopupWindow(popView, WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT, true);
            mPopWindow.setOutsideTouchable(true);
            mPopWindow.setBackgroundDrawable(new BitmapDrawable());
            mPopWindow.setFocusable(true);
            mPopWindow.setTouchable(true);
            mPopWindow.setAnimationStyle(R.style.interactpopupWindowAnimation);
            mPopWindow.showAtLocation(mActivity.findViewById(R.id.interact_room_message_root_layout), Gravity.NO_GRAVITY, 0, 100);

            mPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    release();
                }
            });

        }
    }

    private void initView(View view) {
        view.findViewById(R.id.enableVideo).setOnClickListener(this);
        view.findViewById(R.id.disableVideo).setOnClickListener(this);
        view.findViewById(R.id.enableAudio).setOnClickListener(this);
        view.findViewById(R.id.disableAudio).setOnClickListener(this);
        view.findViewById(R.id.enableLocalVideo).setOnClickListener(this);
        view.findViewById(R.id.enableAudioVolumeIndication).setOnClickListener(this);
        view.findViewById(R.id.muteAllRemoteAudioStreams).setOnClickListener(this);
        view.findViewById(R.id.setRemoteRenderMode).setOnClickListener(this);
        view.findViewById(R.id.setRemoteVideoStream).setOnClickListener(this);
        view.findViewById(R.id.clearVideoCompositingLayout).setOnClickListener(this);
    }

    public void dismissPopupWindow() {
        this.release();
    }

    private void release() {
        if (mPopWindow != null) {
            mPopWindow.dismiss();
            mPopWindow = null;
        }
        mInteractEngine = null;
        mActivity = null;
    }

    /**
     * 设置云控角色 （需在joinChannel前调用）
     */
    public static void setCloudControlRole(QHVCInteractiveKit engine) {
        if (engine != null) {
            String roleName = "test"; //TODO 角色名字请自行修改
            engine.setCloudControlRole(roleName);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.enableVideo:
                enableVideo();
                break;
            case R.id.disableVideo:
                disableVideo();
                break;
            case R.id.enableAudio:
                enableAudio();
                break;
            case R.id.disableAudio:
                disableAudio();
                break;
            case R.id.enableLocalVideo:
                enableLocalVideo(v);
                break;
            case R.id.enableAudioVolumeIndication:
                enableAudioVolumeIndication(v);
                break;
            case R.id.muteAllRemoteAudioStreams:
                muteAllRemoteAudioStreams(v);
                break;
            case R.id.setRemoteRenderMode:
                setRemoteRenderMode();
                break;
            case R.id.setRemoteVideoStream:
                setRemoteVideoStream(v);
                break;
            case R.id.clearVideoCompositingLayout:
                clearVideoCompositingLayout();
                break;
        }
    }

    private void enableVideo() {
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", clicked enableVideo...");
        if (mInteractEngine != null) {
            mInteractEngine.enableVideo();
        }
    }

    private void disableVideo() {
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", clicked disableVideo...");
        if (mInteractEngine != null) {
            mInteractEngine.disableVideo();
        }
    }

    private void enableAudio() {
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", clicked enableAudio...");
        if (mInteractEngine != null) {
            mInteractEngine.enableAudio();
        }
    }

    private void disableAudio() {
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", clicked disableAudio...");
        if (mInteractEngine != null) {
            mInteractEngine.disableAudio();
        }
    }

    private void enableLocalVideo(View view) {
        mEnableLocalVideo = !mEnableLocalVideo;
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", clicked enableLocalVideo: " + mEnableLocalVideo);
        if (mInteractEngine != null) {
            mInteractEngine.enableLocalVideo(mEnableLocalVideo);
            String state = mEnableLocalVideo? "(开)":"（关）";
            ((Button)view).setText("enableLocalVideo" + state);
        }
    }

    private void enableAudioVolumeIndication(View view) {
        mEnableAudioVolumeIndication = !mEnableAudioVolumeIndication;
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", clicked enableAudioVolumeIndication: " + mEnableAudioVolumeIndication);
        if (mInteractEngine != null) {
            if (mEnableAudioVolumeIndication) {
                mInteractEngine.enableAudioVolumeIndication(200, 3);
            } else {
                mInteractEngine.enableAudioVolumeIndication(0, 3);
            }

            String state = mEnableAudioVolumeIndication? "(开)":"（关）";
            ((Button)view).setText("enableAudioVolumeIndication" + state);
        }
    }

    private void muteAllRemoteAudioStreams(View view) {
        mMutedAll = !mMutedAll;
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", clicked muteAllRemoteAudioStreams: " + mMutedAll);
        if (mInteractEngine != null) {
            mInteractEngine.muteAllRemoteAudioStreams(mMutedAll);

            String state = mMutedAll? "(开)":"（关）";
            ((Button)view).setText("muteAllRemoteAudioStreams" + state);
        }
    }

    private void setRemoteRenderMode() { //TODO 待测试
        HashMap<String, MyVideoView> allVideo = null;
        if (mActivity != null) {
            allVideo = ((InteractActivity)mActivity).getAllUser();
        }
        if (mInteractEngine != null && allVideo != null) {

            mRemoteRenderMode++;
            mRemoteRenderMode = (mRemoteRenderMode > RENDER_MODE_ADAPTIVE) ? RENDER_MODE_HIDDEN : mRemoteRenderMode;

            Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", clicked setRemoteRenderMode: " + mRemoteRenderMode);

            for (String key : allVideo.keySet()) {
                if (!key.equals(myUid)) {
                    mInteractEngine.setRemoteRenderMode(key, mRemoteRenderMode);
                }
            }
        }
    }

    /**
     * 切换拉取大流（或小流）
     * 只有对方开启双流模式时才有效。
     * @param view
     */
    private void setRemoteVideoStream(View view) {
        HashMap<String, MyVideoView> allVideo = null;
        if (mActivity != null) {
            allVideo = ((InteractActivity)mActivity).getAllUser();
        }
        if (mInteractEngine != null && allVideo != null) {
            mRemoteVideoStream = (mRemoteVideoStream == QHVCInteractiveConstant.VIDEO_STREAM_HIGH)? QHVCInteractiveConstant.VIDEO_STREAM_LOW : QHVCInteractiveConstant.VIDEO_STREAM_HIGH;
            Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", clicked setRemoteVideoStream: " + mRemoteVideoStream);
            for (String key : allVideo.keySet()) {
                if (!key.equals(myUid)) {
                    mInteractEngine.setRemoteVideoStream(key, mRemoteVideoStream);
                }
            }

            String state = mRemoteVideoStream == QHVCInteractiveConstant.VIDEO_STREAM_HIGH ? "(大)":"（小）";
            ((Button)view).setText("setRemoteVideoStream" + state);
        }
    }

    private void clearVideoCompositingLayout() {
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", clicked clearVideoCompositingLayout...");
        if (mInteractEngine != null) {
            mInteractEngine.clearVideoCompositingLayout();
        }
    }

}
