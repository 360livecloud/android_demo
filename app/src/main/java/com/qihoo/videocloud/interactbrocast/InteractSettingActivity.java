
package com.qihoo.videocloud.interactbrocast;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo.livecloud.interact.api.QHVCInteractiveKit;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.interactbrocast.main.InteractConstant;
import com.qihoo.videocloud.interactbrocast.net.InteractServerApi;
import com.qihoo.videocloud.utils.QHVCSharedPreferences;

import static com.qihoo.videocloud.interactbrocast.main.InteractConstant.DEVELOP_EVN_ONLINE;
import static com.qihoo.videocloud.interactbrocast.main.InteractConstant.DEVELOP_EVN_TEST;
import static com.qihoo.videocloud.interactbrocast.main.InteractConstant.ENCODE_TYPE_SOFT;

/**
 * 互动直播设置类
 * Created by huchengming
 */
public class InteractSettingActivity extends Activity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    private ImageView finishButton;

    private LinearLayout resetLayout;
    private LinearLayout confirmLayout;
    private QHVCSharedPreferences sharedPreferences;
    public static String[] resolutionRatioList = {
            "240x240", "424x240", "360x360", "360x360", "640x360", "640x360",
            "640x360", "480x480", "480x480", "848x480", "848x480", "1280x720", "1280x720"
    }; //分辨率数组
    public static String[] fpsList = {
            "15", "15", "15", "30", "15", "15", "15", "15", "30", "15", "30", "15", "30"
    }; //帧率数组
    private static String[] codeRate = {
            "140", "220", "260", "400", "400", "600", "800", "400", "600", "610", "930", "1130", "1710"
    }; //码率数组

    private Spinner brocastSpinner;
    private Spinner guestSpinner;
    private TextView brocastResolutionRatioTextView;
    private TextView brocastFpsTextView;
    private TextView brocastCodeRateTextView;
    private TextView guestResolutionRatioTextView;
    private TextView guestFpsTextView;
    private TextView guestCodeRateTextView;
    private RadioGroup envTypeRadio;
    private RadioGroup audienceEncoderTypeRadio;
    private int bocastSettingProfileType;
    private int guestSettingProfileType;
    private int developEvn;
    private int audienceEncoderType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.interact_setting_activity_layout);
        initData();
        initView();
        bindingDate();
    }

    private void initData() {
        if (sharedPreferences == null) {
            sharedPreferences = QHVCSharedPreferences.getInstence();
        }
        developEvn = sharedPreferences.getInt(InteractConstant.DEVELOP_EVN, DEVELOP_EVN_TEST);
        audienceEncoderType = sharedPreferences.getInt(InteractConstant.ENCODE_TYPE, ENCODE_TYPE_SOFT);
        bocastSettingProfileType = sharedPreferences.getInt(InteractConstant.BROCAST_SETTING_PROFILE_TYPE, 0);
        guestSettingProfileType = sharedPreferences.getInt(InteractConstant.GUEST_SETTING_PROFILE_TYPE, 0);
    }

    private void initView() {
        finishButton = (ImageView) findViewById(R.id.interact_brocast_setting_header_left_icon);
        finishButton.setOnClickListener(this);

        brocastSpinner = (Spinner) findViewById(R.id.interact_setting_broadcaster_profile_type);
        brocastResolutionRatioTextView = (TextView) findViewById(R.id.interact_setting_broacast_resolution_ratio);
        brocastFpsTextView = (TextView) findViewById(R.id.interact_setting_broacast_frame_ratio);
        brocastCodeRateTextView = (TextView) findViewById(R.id.interact_setting_broacast_code_rate);
        brocastSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setBrocastProfile(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        guestSpinner = (Spinner) findViewById(R.id.interact_setting_guest_profile_type);
        guestResolutionRatioTextView = (TextView) findViewById(R.id.interact_setting_guest_resolution_ratio);
        guestFpsTextView = (TextView) findViewById(R.id.interact_setting_guest_frame_ratio);
        guestCodeRateTextView = (TextView) findViewById(R.id.interact_setting_guest_code_rate);
        guestSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setGuestProfile(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        resetLayout = (LinearLayout) findViewById(R.id.interact_brocast_setting_reset_layout);
        resetLayout.setOnClickListener(this);
        confirmLayout = (LinearLayout) findViewById(R.id.interact_brocast_setting_confirm_layout);
        confirmLayout.setOnClickListener(this);
        envTypeRadio = (RadioGroup) findViewById(R.id.interact_setting_env_type);
        envTypeRadio.setOnCheckedChangeListener(this);
        audienceEncoderTypeRadio = (RadioGroup) findViewById(R.id.interact_audience_setting_encoder_type);
        audienceEncoderTypeRadio.check(R.id.interact_audience_setting_soft_encoder_type);
        audienceEncoderTypeRadio.getChildAt(1).setClickable(false);
        audienceEncoderTypeRadio.getChildAt(1).setEnabled(false);/*android暂时只支持软解*/
    }

    public void bindingDate() {
        if (developEvn == DEVELOP_EVN_TEST) {
            envTypeRadio.check(R.id.interact_brocast_env_test);
        } else {
            envTypeRadio.check(R.id.interact_setting_env_online);
        }
        brocastSpinner.setSelection(bocastSettingProfileType);
        guestSpinner.setSelection(guestSettingProfileType);

    }

    public void resetData() {
        initData();
        bindingDate();
    }

    private void setBrocastProfile(int index) {
        brocastResolutionRatioTextView.setText(resolutionRatioList[index]);
        brocastFpsTextView.setText(fpsList[index]);
        brocastCodeRateTextView.setText(codeRate[index]);
        bocastSettingProfileType = index;
    }

    private void setGuestProfile(int index) {
        guestResolutionRatioTextView.setText(resolutionRatioList[index]);
        guestFpsTextView.setText(fpsList[index]);
        guestCodeRateTextView.setText(codeRate[index]);
        guestSettingProfileType = index;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.interact_brocast_setting_header_left_icon:
                finish();
                break;
            case R.id.interact_brocast_setting_reset_layout:/*重置数据*/
                resetData();
                break;
            case R.id.interact_brocast_setting_confirm_layout:
                sharedPreferences.putIntValue(InteractConstant.DEVELOP_EVN, developEvn);
                if (developEvn == DEVELOP_EVN_TEST) {
                    InteractServerApi.setDebug(true);
                    QHVCInteractiveKit.setDebugEnv(true);
                } else {
                    InteractServerApi.setDebug(false);
                    QHVCInteractiveKit.setDebugEnv(false);
                }
                sharedPreferences.putIntValue(InteractConstant.BROCAST_SETTING_PROFILE_TYPE, bocastSettingProfileType);
                sharedPreferences.putIntValue(InteractConstant.GUEST_SETTING_PROFILE_TYPE, guestSettingProfileType);
                sharedPreferences.putIntValue(InteractConstant.ENCODE_TYPE, audienceEncoderType);
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                break;
        }

    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (group.getId()) {
            case R.id.interact_setting_env_type:
                if (checkedId == R.id.interact_setting_env_online) {
                    developEvn = DEVELOP_EVN_ONLINE;
                } else if (checkedId == R.id.interact_brocast_env_test) {
                    developEvn = DEVELOP_EVN_TEST;
                }
                break;
        }
    }

}
