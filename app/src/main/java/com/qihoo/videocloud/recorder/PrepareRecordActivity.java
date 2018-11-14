
package com.qihoo.videocloud.recorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.utils.QHVCSharedPreferences;

import java.util.ArrayList;

/**
 * Created by huchengming
 */
public class PrepareRecordActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private ImageView backButton;
    private ImageView setButton;
    private ImageView startButton;
    private Switch orientationSwitch;
    private Switch onlyVoiceSwitch;
    private Switch saveVideoSwitch;
    private QHVCSharedPreferences sharedPreferences;
    private boolean orientationBoolean;/*是否横屏*/
    private boolean onlyVoiceBoolean;/*是否是纯音频*/
    private boolean saveVideoBoolean;/*是否保存视频文件*/
    private EditText busunessIdEditText;
    private EditText channelIdEditText;
    private EditText urlEditText;
    private EditText titleEditText;
    /**
     * 通知栏背景颜色
     */
    private int statusBackgroundClor = R.drawable.header_bg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        initView();
        bindingDate();
        checkSelfPermissions();/*动态权限申请*/
    }

    private void initView() {
        setContentView(R.layout.record_prepare_activty_layout);
        backButton = (ImageView) findViewById(R.id.record_prepare_header_left_icon);
        backButton.setOnClickListener(this);
        setButton = (ImageView) findViewById(R.id.record_prepare_set);
        setButton.setOnClickListener(this);
        startButton = (ImageView) findViewById(R.id.record_prepare_start);
        startButton.setOnClickListener(this);

        orientationSwitch = (Switch) findViewById(R.id.record_prepare_choice_orientation);
        orientationSwitch.setOnCheckedChangeListener(this);
        onlyVoiceSwitch = (Switch) findViewById(R.id.record_prepare_choice_only_voice);
        onlyVoiceSwitch.setOnCheckedChangeListener(this);
        saveVideoSwitch = (Switch) findViewById(R.id.record_prepare_choice_save_video_file);
        saveVideoSwitch.setOnCheckedChangeListener(this);

        busunessIdEditText = (EditText) findViewById(R.id.record_prepare_busuness_id);
        channelIdEditText = (EditText) findViewById(R.id.record_prepare_channel_id);
        urlEditText = (EditText) findViewById(R.id.record_prepare_url);
        titleEditText = (EditText) findViewById(R.id.record_prepare_title);
    }

    private void initData() {
        sharedPreferences = QHVCSharedPreferences.getInstence();
        orientationBoolean = sharedPreferences.getBoolean(RecorderConstants.CHOICE_HORIZONTAL, false);
        onlyVoiceBoolean = sharedPreferences.getBoolean(RecorderConstants.CHOICE_ONLY_VOICE, false);
        saveVideoBoolean = sharedPreferences.getBoolean(RecorderConstants.SAVE_VIDEO_FILE, false);
    }

    private void bindingDate() {
        orientationSwitch.setChecked(orientationBoolean);
        onlyVoiceSwitch.setChecked(onlyVoiceBoolean);
        saveVideoSwitch.setChecked(saveVideoBoolean);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record_prepare_header_left_icon:
                finish();
                break;
            case R.id.record_prepare_set:
                Intent mIntent = new Intent(this, SettingActivity.class);
                startActivity(mIntent);
                break;
            case R.id.record_prepare_start:
                String title = titleEditText.getText().toString().trim();
                if (TextUtils.isEmpty(title)) {
                    Toast.makeText(this, "请填写推流标题", Toast.LENGTH_SHORT).show();
                    return;
                }
                //                if(!checkRecordPermission()){/*检查权限*/
                //                    return;
                //                }
                Intent newApiIntent = new Intent(this, RecorderActivityNewAPI.class);
                newApiIntent.putExtra(RecorderConstants.BUSINESS_ID, busunessIdEditText.getText().toString().trim());
                newApiIntent.putExtra(RecorderConstants.CHANNEL_ID, channelIdEditText.getText().toString().trim());
                newApiIntent.putExtra(RecorderConstants.URL, urlEditText.getText().toString().trim());
                newApiIntent.putExtra(RecorderConstants.TITLE, title);
                startActivity(newApiIntent);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.record_prepare_choice_orientation:
                orientationSwitch.setChecked(isChecked);
                sharedPreferences.putBooleanValue(RecorderConstants.CHOICE_HORIZONTAL, isChecked);
                break;
            case R.id.record_prepare_choice_only_voice:
                onlyVoiceSwitch.setChecked(isChecked);
                sharedPreferences.putBooleanValue(RecorderConstants.CHOICE_ONLY_VOICE, isChecked);
                if (isChecked) {
                    saveVideoSwitch.setClickable(false);
                    saveVideoSwitch.setChecked(false);
                } else {
                    saveVideoSwitch.setClickable(true);
                }
                break;
            case R.id.record_prepare_choice_save_video_file:
                saveVideoSwitch.setChecked(isChecked);
                sharedPreferences.putBooleanValue(RecorderConstants.SAVE_VIDEO_FILE, isChecked);
                break;
        }
    }

    protected void checkSelfPermissions() {
        checkSelfPermissionAndRequest(new String[] {
                Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
    }

    public void checkSelfPermissionAndRequest(String[] permissionList) {
        ArrayList<String> requestPermissionList = new ArrayList<String>();
        for (String permission : permissionList) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionList.add(permission);
            }
        }
        if (requestPermissionList.size() > 0) {
            String[] requestPermissionArr = new String[requestPermissionList.size()];
            requestPermissionList.toArray(requestPermissionArr);
            ActivityCompat.requestPermissions(this, requestPermissionArr, 0);
        }
    }

    public boolean checkRecordPermission() {
        boolean canStartRecord = true;
        if (!selfPermissionGranted(this, Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, "未获取录音权限", Toast.LENGTH_SHORT).show();
            canStartRecord = false;
        }
        if (!selfPermissionGranted(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this, "未获取相机权限", Toast.LENGTH_SHORT).show();
            canStartRecord = false;
        }
        if (!selfPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "未获取写本地文件权限", Toast.LENGTH_SHORT).show();
            canStartRecord = false;
        }
        return canStartRecord;
    }

    public boolean selfPermissionGranted(Context mContext, String permission) {
        // For Android < Android M, self permissions are always granted.
        boolean result = true;
        int targetSdkVersion = 0;
        try {
            final PackageInfo info = mContext.getPackageManager().getPackageInfo(
                    mContext.getPackageName(), 0);
            targetSdkVersion = info.applicationInfo.targetSdkVersion;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                result = mContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
            } else {
                result = PermissionChecker.checkSelfPermission(mContext, permission) == PermissionChecker.PERMISSION_GRANTED;
            }
        }
        return result;
    }

}
