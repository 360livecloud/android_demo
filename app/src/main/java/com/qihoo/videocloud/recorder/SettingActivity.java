
package com.qihoo.videocloud.recorder;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.utils.QHVCSharedPreferences;

/**
 * 录制设置类
 * Created by huchengming
 */
public class SettingActivity extends Activity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener {
    private ImageView finishButton;

    private RadioGroup resolutionRatioRadioGroup;
    private RadioButton set360Button;
    private RadioButton set480Button;
    private RadioButton set640Button;
    private RadioButton set720Button;
    private RadioGroup encodeTypeRadioGroup;
    private RadioButton setH264Button;
    private RadioButton setH265Button;
    private RadioGroup videoFpsRadioGroup;
    private RadioButton set15HzButton;
    private RadioButton set30HzButton;
    private SeekBar setSeekBar;
    private TextView currentCodeRate;
    private CheckBox checkAutoAdjustCodeRate;
    private RadioGroup audioCodeRateRadioGroup;
    private RadioButton set32kbpsButton;
    private RadioButton set48kbpsButton;
    private RadioButton set64kbpsButton;
    private RadioButton set128kbpsButton;
    private RadioGroup audioSampleRadioGroup;
    private RadioButton set22_05KHzButton;
    private RadioButton set44_1KHzButton;
    private RadioButton set48KHzButton;
    private LinearLayout resetLayout;
    private LinearLayout confirmLayout;
    private QHVCSharedPreferences sharedPreferences;

    private String resolutionRatioSp;
    private String encodeTypeSp;
    private String videoFpsSp;
    private int codeRateSp;
    private boolean autoAdjustCodeRateSp;
    private String audioCodeRateSp;
    private String audioSampleSp;
    private boolean recordLocal;/*是否是本地录制*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_setting_activity_layout);
        initData();
        initView();
        bindingDate();
    }

    private void initData() {
        recordLocal = getIntent().getBooleanExtra("fromLocal", false);/*是否是本地录制视频*/

        sharedPreferences = QHVCSharedPreferences.getInstence();
        resolutionRatioSp = sharedPreferences.getString(RecorderConstants.RESOLUTION_RATIO, "360P");
        encodeTypeSp = sharedPreferences.getString(RecorderConstants.ENCODE_TYPE, "H.264");
        videoFpsSp = sharedPreferences.getString(RecorderConstants.VIDEO_FPS, "15Hz");
        if (recordLocal) {
            codeRateSp = sharedPreferences.getInt(RecorderConstants.RECORDE_LOCAL_CODE_RATE, 300);
        } else {
            codeRateSp = sharedPreferences.getInt(RecorderConstants.CODE_RATE, 100);
        }
        autoAdjustCodeRateSp = sharedPreferences.getBoolean(RecorderConstants.AUTO_ADJUST_CODE_RATE, false);
        audioCodeRateSp = sharedPreferences.getString(RecorderConstants.AUDIO_CODE_RATE, "32kbps");
        audioSampleSp = sharedPreferences.getString(RecorderConstants.AUDIO_SAMPLE, "22.05KHz");

    }

    private void initView() {
        finishButton = (ImageView) findViewById(R.id.record_setting_header_left_icon);
        finishButton.setOnClickListener(this);

        resolutionRatioRadioGroup = (RadioGroup) findViewById(R.id.record_setting_resolution_ratio);
        resolutionRatioRadioGroup.setOnCheckedChangeListener(this);
        set360Button = (RadioButton) findViewById(R.id.record_setting_360P);
        set480Button = (RadioButton) findViewById(R.id.record_setting_480P);
        set640Button = (RadioButton) findViewById(R.id.record_setting_640P);
        set720Button = (RadioButton) findViewById(R.id.record_setting_720P);

        encodeTypeRadioGroup = (RadioGroup) findViewById(R.id.record_setting_encode_type);
        encodeTypeRadioGroup.setOnCheckedChangeListener(this);
        setH264Button = (RadioButton) findViewById(R.id.record_setting_H264);
        setH265Button = (RadioButton) findViewById(R.id.record_setting_H265);

        videoFpsRadioGroup = (RadioGroup) findViewById(R.id.record_setting_video_fps);
        videoFpsRadioGroup.setOnCheckedChangeListener(this);
        set15HzButton = (RadioButton) findViewById(R.id.record_setting_15Hz);
        set30HzButton = (RadioButton) findViewById(R.id.record_setting_30Hz);

        setSeekBar = (SeekBar) findViewById(R.id.record_setting_seek_video_code_rate);
        setSeekBar.setMax(100);
        setSeekBar.setOnSeekBarChangeListener(seekBarChangeListen);
        currentCodeRate = (TextView) findViewById(R.id.record_setting_seek_current_code_rate);

        checkAutoAdjustCodeRate = (CheckBox) findViewById(R.id.record_setting_check_code_rate_auto);
        checkAutoAdjustCodeRate.setOnCheckedChangeListener(this);

        audioCodeRateRadioGroup = (RadioGroup) findViewById(R.id.record_setting_audio_code_rate);
        audioCodeRateRadioGroup.setOnCheckedChangeListener(this);
        set32kbpsButton = (RadioButton) findViewById(R.id.record_setting_32kbps);
        set48kbpsButton = (RadioButton) findViewById(R.id.record_setting_48kbps);
        set64kbpsButton = (RadioButton) findViewById(R.id.record_setting_64kbps);
        set128kbpsButton = (RadioButton) findViewById(R.id.record_setting_128kbps);

        audioSampleRadioGroup = (RadioGroup) findViewById(R.id.record_setting_sample);
        audioSampleRadioGroup.setOnCheckedChangeListener(this);
        set22_05KHzButton = (RadioButton) findViewById(R.id.record_setting_22_05KHz);
        set44_1KHzButton = (RadioButton) findViewById(R.id.record_setting_44_1KHz);
        set48KHzButton = (RadioButton) findViewById(R.id.record_setting_48KHz);

        resetLayout = (LinearLayout) findViewById(R.id.record_setting_reset_layout);
        resetLayout.setOnClickListener(this);
        confirmLayout = (LinearLayout) findViewById(R.id.record_setting_confirm_layout);
        confirmLayout.setOnClickListener(this);

        if (recordLocal) {/*本地录制 不需要码率自适应*/
            set48KHzButton.setVisibility(View.GONE);
            checkAutoAdjustCodeRate.setVisibility(View.GONE);
        }
    }

    public void bindingDate() {
        if (!TextUtils.isEmpty(resolutionRatioSp)) {/*分辨率*/
            if (resolutionRatioSp.equals(set360Button.getText())) {
                set360Button.setChecked(true);
            } else if (resolutionRatioSp.equals(set480Button.getText())) {
                set480Button.setChecked(true);
            } else if (resolutionRatioSp.equals(set640Button.getText())) {
                set640Button.setChecked(true);
            } else if (resolutionRatioSp.equals(set720Button.getText())) {
                set720Button.setChecked(true);
            }
        }

        if (!TextUtils.isEmpty(encodeTypeSp)) {/*编码类型*/
            if (encodeTypeSp.equals(setH264Button.getText())) {
                setH264Button.setChecked(true);
            } else {
                setH265Button.setChecked(true);
            }
        }

        if (!TextUtils.isEmpty(videoFpsSp)) {/*视频帧率*/
            if (videoFpsSp.equals(set15HzButton.getText())) {
                set15HzButton.setChecked(true);
            } else {
                set30HzButton.setChecked(true);
            }
        }

        setSeekBar.postDelayed(new Runnable() {/*延迟100ms*/
            @Override
            public void run() {
                if (recordLocal) {
                    currentCodeRate.setText(codeRateSp + "kbps");
                    setSeekBar.setProgress((codeRateSp - 300) / 37);/*max为100，取值是100~1800*/
                } else {
                    setSeekBar.setProgress((codeRateSp - 100) / 17);/*max为100，取值是100~1800*/
                }
            }
        }, 20);

        if (autoAdjustCodeRateSp) {/*自适应*/
            checkAutoAdjustCodeRate.setChecked(true);
        } else {
            checkAutoAdjustCodeRate.setChecked(false);
        }

        if (!TextUtils.isEmpty(audioCodeRateSp)) {
            if (audioCodeRateSp.equals(set32kbpsButton.getText())) {
                set32kbpsButton.setChecked(true);
            } else if (audioCodeRateSp.equals(set48kbpsButton.getText())) {
                set48kbpsButton.setChecked(true);
            } else if (audioCodeRateSp.equals(set64kbpsButton.getText())) {
                set64kbpsButton.setChecked(true);
            } else if (audioCodeRateSp.equals(set128kbpsButton.getText())) {
                set128kbpsButton.setChecked(true);
            }
        }

        if (!TextUtils.isEmpty(audioSampleSp)) {
            if (audioSampleSp.equals(set22_05KHzButton.getText())) {
                set22_05KHzButton.setChecked(true);
            } else if (audioSampleSp.equals(set44_1KHzButton.getText())) {
                set44_1KHzButton.setChecked(true);
            } else if (audioSampleSp.equals(set48KHzButton.getText())) {
                set48KHzButton.setChecked(true);
            }
        }

    }

    SeekBar.OnSeekBarChangeListener seekBarChangeListen = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (recordLocal) {
                codeRateSp = progress * 37 + 300;
            } else {
                codeRateSp = progress * 17 + 100;
            }
            currentCodeRate.setText(codeRateSp + "kbps");
            int quota = progress;
            int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            currentCodeRate.measure(spec, spec);
            int quotaWidth = currentCodeRate.getMeasuredWidth();

            int spec2 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            currentCodeRate.measure(spec2, spec2);
            int sbWidth = setSeekBar.getMeasuredWidth();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) currentCodeRate.getLayoutParams();
            params.leftMargin = (int) (((double) progress / setSeekBar.getMax()) * sbWidth - (double) quotaWidth * progress / setSeekBar.getMax());
            currentCodeRate.setLayoutParams(params);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record_setting_header_left_icon:
                finish();
                break;
            case R.id.record_setting_reset_layout:/*重置数据*/
                initData();
                bindingDate();
                break;
            case R.id.record_setting_confirm_layout:
                sharedPreferences.putStringValue(RecorderConstants.RESOLUTION_RATIO, resolutionRatioSp);
                sharedPreferences.putStringValue(RecorderConstants.ENCODE_TYPE, encodeTypeSp);
                sharedPreferences.putStringValue(RecorderConstants.VIDEO_FPS, videoFpsSp);
                if (recordLocal) {
                    sharedPreferences.putIntValue(RecorderConstants.RECORDE_LOCAL_CODE_RATE, codeRateSp);
                } else {
                    sharedPreferences.putIntValue(RecorderConstants.CODE_RATE, codeRateSp);
                }
                sharedPreferences.putBooleanValue(RecorderConstants.AUTO_ADJUST_CODE_RATE, autoAdjustCodeRateSp);
                sharedPreferences.putStringValue(RecorderConstants.AUDIO_CODE_RATE, audioCodeRateSp);
                sharedPreferences.putStringValue(RecorderConstants.AUDIO_SAMPLE, audioSampleSp);
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                break;
        }

    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (group.getId()) {
            case R.id.record_setting_resolution_ratio:
                resolutionRatioSp = ((RadioButton) findViewById(checkedId)).getText().toString();
                break;
            case R.id.record_setting_encode_type:
                encodeTypeSp = ((RadioButton) findViewById(checkedId)).getText().toString();
                break;
            case R.id.record_setting_video_fps:
                videoFpsSp = ((RadioButton) findViewById(checkedId)).getText().toString();
                break;
            case R.id.record_setting_audio_code_rate:
                audioCodeRateSp = ((RadioButton) findViewById(checkedId)).getText().toString();
                break;
            case R.id.record_setting_sample:
                audioSampleSp = ((RadioButton) findViewById(checkedId)).getText().toString();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.record_setting_check_code_rate_auto:
                if (isChecked) {
                    autoAdjustCodeRateSp = true;
                } else {
                    autoAdjustCodeRateSp = false;
                }
                break;
        }

    }
}
