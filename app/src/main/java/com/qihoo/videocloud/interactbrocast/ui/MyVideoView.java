
package com.qihoo.videocloud.interactbrocast.ui;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qihoo.livecloudrefactor.R;

/**
 * Created by liuyanqing on 2016/12/23.
 */

public class MyVideoView extends RelativeLayout implements View.OnClickListener {

    public static final int VIEW_FULLSCREEN = R.id.btn_fullscreen;
    public static final int VIEW_BTN_VIDEO = R.id.btn_mute_remote_video;
    public static final int VIEW_BTN_AUDIO = R.id.btn_mute_remote_audio;

    private Resources mResources;

    private View mRootView;

    private String uid;

    private FrameLayout videoLayout;

    private TextView mTextUid;

    private TextView mTextFullScreen;
    private TextView mBtnMuteRemoteVideo;
    private TextView mBtnMuteRemoteAudio;
    private TextView statusInfoView;

    private boolean muteRemoteVideo;
    private boolean muteRemoteAudio;

    private View bgView;
    private int bgViewWidth;
    private int bgViewHeight;

    private VideoViewListener mListener;

    private ImageView finshButton;

    public MyVideoView(Context context) {
        super(context);
        initViews(context);
    }

    public MyVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initViews(context);
    }

    private void initViews(Context context) {
        mResources = context.getResources();

        mRootView = LayoutInflater.from(context).inflate(R.layout.video_view_live, this);
        this.setBackgroundColor(0x00FFFFFF);

        videoLayout = (FrameLayout) findViewById(R.id.video_view);

        mTextUid = (TextView) findViewById(R.id.txt_uid);
        mTextFullScreen = (TextView) findViewById(R.id.btn_fullscreen);
        mTextFullScreen.setOnClickListener(this);
        mBtnMuteRemoteVideo = (TextView) findViewById(R.id.btn_mute_remote_video);
        mBtnMuteRemoteVideo.setOnClickListener(this);
        mBtnMuteRemoteAudio = (TextView) findViewById(R.id.btn_mute_remote_audio);
        mBtnMuteRemoteAudio.setOnClickListener(this);

        statusInfoView = (TextView) findViewById(R.id.status_info);

        finshButton = (ImageView) findViewById(R.id.interact_close_view);
        finshButton.setOnClickListener(this);

        muteRemoteVideo = false;
        muteRemoteAudio = false;
    }

    public void setInfo(String info) {
        if (null != statusInfoView)
            statusInfoView.setText(info);
    }

    public void setMuteRemoteVideo(boolean muteVideo) {
        this.muteRemoteVideo = muteVideo;
    }

    public boolean isMuteRemoteVideo() {
        return this.muteRemoteVideo;
    }

    public void setMuteRemoteAudio(boolean muteAudio) {
        this.muteRemoteAudio = muteAudio;
    }

    public boolean isMuteRemoteAudio() {
        return this.muteRemoteAudio;
    }

    public void setUid(String uid) {
        this.uid = uid;
        mTextUid.setText(String.valueOf(uid));
    }

    public String getUid() {
        return this.uid;
    }

    public void setAllButtonVisible(int showState) {
        mTextFullScreen.setVisibility(showState);
        mBtnMuteRemoteVideo.setVisibility(showState);
        mBtnMuteRemoteAudio.setVisibility(showState);
        finshButton.setVisibility(showState);
    }

    public void setButtonVisible(int viewId, int visiblity) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setVisibility(visiblity);
        }
    }

    public void setBgView(View view, int w, int h) {
        this.bgView = view;
        bgViewWidth = w;
        bgViewHeight = h;
        videoLayout.addView(bgView, w, h);
    }

    public int getBgViewWidth() {
        return this.bgViewWidth;
    }

    public int getBgViewHeight() {
        return this.bgViewHeight;
    }

    public View getBgView() {
        return bgView;
    }

    public void removeBgView() {
        videoLayout.removeView(bgView);
    }

    public void setVideoViewListener(VideoViewListener listener) {
        this.mListener = listener;
    }

    public void changeToLargeVideoView(MyVideoView largeVideoView) {
        String newUid = largeVideoView.getUid();
        boolean newMuteVideo = largeVideoView.isMuteRemoteVideo();
        boolean newMuteAudio = largeVideoView.isMuteRemoteAudio();
        View oldlargeBgView = largeVideoView.getBgView();
        int largeViewWidth = largeVideoView.getBgViewWidth();
        int largeViewHeight = largeVideoView.getBgViewHeight();

        largeVideoView.removeBgView();
        this.removeBgView();
        if (bgView instanceof GLSurfaceView) {
            GLSurfaceView newLargeGLView = (GLSurfaceView) bgView;
            newLargeGLView.setZOrderOnTop(false);
            newLargeGLView.setZOrderMediaOverlay(false);
        }
        if (oldlargeBgView instanceof GLSurfaceView) {
            GLSurfaceView oldLargeGLView = (GLSurfaceView) oldlargeBgView;
            oldLargeGLView.setZOrderOnTop(true);
            oldLargeGLView.setZOrderMediaOverlay(true);
        }

        largeVideoView.setUid(uid);
        largeVideoView.setMuteRemoteVideo(muteRemoteVideo);
        largeVideoView.setMuteRemoteAudio(muteRemoteAudio);
        largeVideoView.setAllButtonVisible(View.GONE);
        largeVideoView.setBgView(bgView, largeViewWidth, largeViewHeight);

        this.setUid(newUid);
        this.setMuteRemoteVideo(newMuteVideo);
        this.setMuteRemoteAudio(newMuteAudio);
        this.setAllButtonVisible(VISIBLE);
        this.setBgView(oldlargeBgView, bgViewWidth, bgViewHeight);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_fullscreen:
                if (mListener != null) {
                    mListener.changeToFullScreen(this);
                }
                break;
            case R.id.btn_mute_remote_video:
                if (mListener != null) {
                    muteRemoteVideo = !muteRemoteVideo;
                    int color = muteRemoteVideo ? getResources().getColor(R.color.common_red) : getResources().getColor(R.color.white);
                    ((TextView) v).setTextColor(color);
                    mListener.doOnClickMuteRemoteVideo(this, (TextView) v);
                }
                break;
            case R.id.btn_mute_remote_audio:
                if (mListener != null) {
                    muteRemoteAudio = !muteRemoteAudio;
                    int color = muteRemoteAudio ? getResources().getColor(R.color.common_red) : getResources().getColor(R.color.white);
                    ((TextView) v).setTextColor(color);
                    mListener.doOnClickMuteRemoteAudio(this, (TextView) v);
                }
                break;
            case R.id.interact_close_view:
                if (mListener != null) {
                    mListener.doOnClickfinish(this);
                }
                break;
        }
    }

    public static interface VideoViewListener {
        void changeToFullScreen(MyVideoView videoView);

        void doOnClickMuteRemoteVideo(MyVideoView videoView, TextView tv);

        void doOnClickMuteRemoteAudio(MyVideoView videoView, TextView tv);

        void doOnClickfinish(MyVideoView videoView);
    }

}
