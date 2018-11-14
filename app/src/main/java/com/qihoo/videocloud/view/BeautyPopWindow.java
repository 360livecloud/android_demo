package com.qihoo.videocloud.view;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;

import com.qihoo.livecloudrefactor.R;


public class BeautyPopWindow extends PopupWindow implements View.OnClickListener {

    private ImageView popViewBeautyButton;
    private ImageView popViewWhiteButton;
    private ImageView popViewSharpfaceButton;
    private ImageView popViewBigeyeButton;
    private ImageView popViewClose;
    private ImageView popViewConfirm;
    private SeekBar popViewSeekBar;
    private float mBeautyRatio;
    private float mWhitRatio;
    private float mSharpFaceRatio;
    private float mBigEyeRatio;

    public BeautyPopWindow(Context context) {
        super(context);
        View popView = LayoutInflater.from(context).inflate(R.layout.beauty_popwindow_layout, null);
        initBeautyPopWindowView(popView);


        //设置SelectPicPopupWindow的View
        this.setContentView(popView);
        //设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        //设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        //设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        this.setOutsideTouchable(true);
        this.setTouchable(true);
        this.setBackgroundDrawable(new BitmapDrawable());
        this.setAnimationStyle(R.style.popupWindowAnimation);
        popViewBeautyButton.setSelected(true);/*第一次进来默认选第一个*/
    }


    private void initBeautyPopWindowView(View popView) {
        popViewBeautyButton = (ImageView) popView.findViewById(R.id.record_popwindow_beauty);
        popViewBeautyButton.setOnClickListener(this);
        popViewWhiteButton = (ImageView) popView.findViewById(R.id.record_popwindow_white);
        popViewWhiteButton.setOnClickListener(this);
        popViewSharpfaceButton = (ImageView) popView.findViewById(R.id.record_popwindow_sharpface);
        popViewSharpfaceButton.setOnClickListener(this);
        popViewBigeyeButton = (ImageView) popView.findViewById(R.id.record_popwindow_bigeye);
        popViewBigeyeButton.setOnClickListener(this);
//        if (horizontalBoolean) {
//            popViewSharpfaceButton.setVisibility(View.INVISIBLE);
//            popViewBigeyeButton.setVisibility(View.INVISIBLE);
//        }
        popViewClose = (ImageView) popView.findViewById(R.id.record_popwindow_close);
        popViewClose.setOnClickListener(this);
        popViewConfirm = (ImageView) popView.findViewById(R.id.record_popwindow_confirm);
        popViewConfirm.setOnClickListener(this);
        popViewSeekBar = (SeekBar) popView.findViewById(R.id.record_popwindow_beauty_seekbar);
    }

    public void setSeekBarChangeListener(final SeekBar.OnSeekBarChangeListener seekBarChangelistener) {
        popViewSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (popViewBeautyButton.isSelected()) {/*选中美颜*/
                        float fl = (float) (progress * 0.01);
                        mBeautyRatio = fl;
                        seekBar.setTag(R.id.record_popwindow_beauty);
                    } else if (popViewWhiteButton.isSelected()) {/*选中美白*/
                        float fl = (float) (progress * 0.01);
                        mWhitRatio = fl;
                        seekBar.setTag(R.id.record_popwindow_white);
                    } else if (popViewSharpfaceButton.isSelected()) {/*选中瘦脸*/
                        float fl = (float) (progress * 0.01);
                        mSharpFaceRatio = fl;
                        seekBar.setTag(R.id.record_popwindow_sharpface);
                    } else if (popViewBigeyeButton.isSelected()) {/*选中大眼*/
                        float fl = (float) (progress * 0.01);
                        mBigEyeRatio = fl;
                        seekBar.setTag(R.id.record_popwindow_bigeye);
                    }
                    if (seekBarChangelistener != null) {
                        seekBarChangelistener.onProgressChanged(seekBar, progress, fromUser);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarChangelistener.onStartTrackingTouch(seekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarChangelistener.onStopTrackingTouch(seekBar);
            }
        });
    }

    private void setBeautyPopWindowSelectButton(View selectView) {
        popViewBeautyButton.setSelected(false);
        popViewWhiteButton.setSelected(false);
        popViewSharpfaceButton.setSelected(false);
        popViewBigeyeButton.setSelected(false);
        selectView.setSelected(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record_popwindow_beauty:
                setBeautyPopWindowSelectButton(popViewBeautyButton);
                popViewSeekBar.setProgress((int) (mBeautyRatio * 100));
                break;
            case R.id.record_popwindow_white:
                setBeautyPopWindowSelectButton(popViewWhiteButton);
                popViewSeekBar.setProgress((int) (mWhitRatio * 100));
                break;
            case R.id.record_popwindow_sharpface:
                setBeautyPopWindowSelectButton(popViewSharpfaceButton);
                popViewSeekBar.setProgress((int) (mSharpFaceRatio * 100));
                break;
            case R.id.record_popwindow_bigeye:
                setBeautyPopWindowSelectButton(popViewBigeyeButton);
                popViewSeekBar.setProgress((int) (mBigEyeRatio * 100));
                break;
            case R.id.record_popwindow_close:
                dismiss();
                break;
            case R.id.record_popwindow_confirm:
                dismiss();
                break;
        }
    }
}
