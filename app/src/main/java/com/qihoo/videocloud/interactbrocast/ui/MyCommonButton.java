
package com.qihoo.videocloud.interactbrocast.ui;

import android.view.View;
import android.widget.ImageView;

import com.qihoo.livecloudrefactor.R;

/**
 * Created by liuyanqing on 2016/12/28.
 */

public abstract class MyCommonButton implements View.OnClickListener {

    public static final int VIEW_SWITCH_CAMERA = R.id.btn_switch_camera;
    public static final int VIEW_MUTE_LOCAL_AUDIO = R.id.btn_mute_audio;
    public static final int VIEW_BEAUTY = R.id.btn_beauty;
    public static final int VIEW_FACEU = R.id.btn_faceu;

    private View view;

    private ImageView mSwitchCamearImageView;
    private ImageView mMuteImageView;
    private ImageView mEnableVideoImageView;

    private ImageView mBtnBeauty;
    private ImageView mBtnFaceU;
    private ImageView speakerButton;
    private ImageView mFilterButton;
    private ImageView fullScreenImageView;

    public MyCommonButton(View view) {
        this.view = view;
        analyzeView();
    }

    private void analyzeView() {
        speakerButton = (ImageView) view.findViewById(R.id.btn_sound_speaker);
        speakerButton.setOnClickListener(this);
        mMuteImageView = (ImageView) view.findViewById(R.id.btn_mute_audio);
        mMuteImageView.setOnClickListener(this);
        mSwitchCamearImageView = (ImageView) view.findViewById(R.id.btn_switch_camera);
        mSwitchCamearImageView.setOnClickListener(this);
        mEnableVideoImageView = (ImageView) view.findViewById(R.id.btn_enable_video);
        mEnableVideoImageView.setOnClickListener(this);
        fullScreenImageView = (ImageView) view.findViewById(R.id.btn_full_screen);
        fullScreenImageView.setOnClickListener(this);

        mBtnBeauty = (ImageView) view.findViewById(R.id.btn_beauty);
        mBtnBeauty.setOnClickListener(this);

        mBtnFaceU = (ImageView) view.findViewById(R.id.btn_faceu);
        mBtnFaceU.setOnClickListener(this);

        mFilterButton = (ImageView) view.findViewById(R.id.btn_filter);
        mFilterButton.setOnClickListener(this);
    }

    public void setViewVisible(int viewId, int visible) {
        View v = view.findViewById(viewId);
        if (v != null) {
            v.setVisibility(visible);
        }
    }

    public void setVisibility(int visibility){
        view.setVisibility(visibility);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_sound_speaker: //切换音频输出
                doOnClickSwitchAudioOutput((ImageView) v);
                break;
            case R.id.btn_mute_audio: // 本地关闭麦克风
                doOnClickMuteLocalAudio((ImageView) v);
                break;
            case R.id.btn_switch_camera: // 切换摄像头
                doOnClickSwitchCamera();
                break;
            case R.id.btn_enable_video:
                doOnClickEnableVideo((ImageView) v);
                break;
            case R.id.btn_full_screen:
                doOnClickFullScreen((ImageView) v);
                break;
            case R.id.btn_beauty:
                doOnClickBeauty((ImageView) v);
                break;
            case R.id.btn_faceu:
                doOnClickFaceU((ImageView) v);
                break;
            case R.id.btn_filter:
                break;
        }
    }

    public abstract void doOnClickSwitchCamera();

    public abstract void doOnClickMuteLocalAudio(ImageView view);

    public abstract void doOnClickSwitchAudioOutput(ImageView btn);

    public abstract void doOnClickBeauty(ImageView view);

    public abstract void doOnClickFaceU(ImageView view);

    public abstract void doOnClickEnableVideo(ImageView view);

    public abstract void doOnClickFullScreen(ImageView view);

    public abstract void doOnClickFilter(ImageView view);

}
