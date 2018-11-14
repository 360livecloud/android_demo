
package com.qihoo.videocloud.interactbrocast;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.qihoo.livecloud.interact.api.QHVCInteractiveKit;
import com.qihoo.livecloud.tools.MD5;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.interactbrocast.data.InteractGlobalManager;
import com.qihoo.videocloud.interactbrocast.main.InteractConstant;
import com.qihoo.videocloud.interactbrocast.modle.InteractUserModel;
import com.qihoo.videocloud.interactbrocast.net.InteractServerApi;
import com.qihoo.videocloud.utils.QHVCSharedPreferences;

import java.util.ArrayList;
import java.util.Random;

import static com.qihoo.videocloud.interactbrocast.main.InteractConstant.DEVELOP_EVN_TEST;
import static com.qihoo.videocloud.interactbrocast.main.InteractConstant.INTENT_EXTRA_SDK_USIN_RIGHT;

/**
 * Created by huchengming
 */
public class PrepareInteractBrocastActivity extends Activity implements View.OnClickListener {

    private QHVCSharedPreferences sharedPreferences;

    private EditText busunessIdEditText;
    private EditText channelIdEditText;
    private EditText appKeyEditText;
    private EditText secretKeyEditText;
    private EditText userIdEditText;
    private ViewGroup loginLayout;

    private boolean mLogining = false;

    private RelativeLayout businessIdLayout;
    private RelativeLayout channelIdLayout;
    private RelativeLayout appKeyLayout;
    private RelativeLayout screatKeyLayout;
    private RelativeLayout userIdLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        checkSelfPermissions();/*动态权限申请*/
    }

    private void initView() {
        setContentView(R.layout.interact_brocast_prepare_activty_layout);

        findViewById(R.id.interact_brocast_prepare_header_left_icon).setOnClickListener(this);
        findViewById(R.id.interact_brocast_prepare_set).setOnClickListener(this);
        busunessIdEditText = (EditText) findViewById(R.id.interact_brocast_prepare_busuness_id);
        channelIdEditText = (EditText) findViewById(R.id.interact_brocast_prepare_channel_id);
        appKeyEditText = (EditText) findViewById(R.id.interact_brocast_prepare_key);
        secretKeyEditText = (EditText) findViewById(R.id.interact_brocast_prepare_secret_key);
        userIdEditText = (EditText) findViewById(R.id.interact_brocast_prepare_uid);

        loginLayout = (ViewGroup) findViewById(R.id.interact_layout_login);
        findViewById(R.id.interact_login).setOnClickListener(this);

        findViewById(R.id.interact_brocast_prepare_type1).setOnClickListener(this);
        findViewById(R.id.interact_brocast_prepare_type2).setOnClickListener(this);
        findViewById(R.id.interact_brocast_prepare_type3).setOnClickListener(this);
        businessIdLayout = (RelativeLayout) findViewById(R.id.interact_business_id_layout);
        channelIdLayout = (RelativeLayout) findViewById(R.id.interact_channel_id_layout);
        appKeyLayout = (RelativeLayout) findViewById(R.id.interact_appkey_layout);
        screatKeyLayout = (RelativeLayout) findViewById(R.id.interact_screatkey_layout);
        userIdLayout = (RelativeLayout) findViewById(R.id.interact_userid_layout);
        resetToLoginUI();
    }

    private void resetToLoginUI() {
        businessIdLayout.setVisibility(View.VISIBLE);
        channelIdLayout.setVisibility(View.VISIBLE);
        appKeyLayout.setVisibility(View.VISIBLE);
        screatKeyLayout.setVisibility(View.VISIBLE);
        loginLayout.setVisibility(View.VISIBLE);
        userIdLayout.setVisibility(View.GONE);
    }

    private void changeToLoginSuccessUI() {
        businessIdLayout.setVisibility(View.GONE);
        channelIdLayout.setVisibility(View.GONE);
        appKeyLayout.setVisibility(View.GONE);
        screatKeyLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.GONE);
        userIdLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean isUsinRight = intent.getBooleanExtra(INTENT_EXTRA_SDK_USIN_RIGHT, true);
        if (!isUsinRight) {
            resetToLoginUI();
            appKeyEditText.setText(R.string.config_interact_ak);
            secretKeyEditText.setText(R.string.config_interact_sk);
        }
    }

    private void initData() {
        if (sharedPreferences == null) {
            sharedPreferences = QHVCSharedPreferences.getInstence();
        }
        int developEvn = sharedPreferences.getInt(InteractConstant.DEVELOP_EVN, DEVELOP_EVN_TEST);
        if (developEvn == DEVELOP_EVN_TEST) {/*配置环境*/
            InteractServerApi.setDebug(true);
            QHVCInteractiveKit.setDebugEnv(true);
        } else {
            InteractServerApi.setDebug(false);
            QHVCInteractiveKit.setDebugEnv(false);
        }

        InteractUserModel userModel = InteractGlobalManager.getInstance().getUser();
        if (userModel != null && !TextUtils.isEmpty(userModel.getUserId())) {
            changeToLoginSuccessUI();
            appKeyEditText.setText(InteractGlobalManager.getInstance().getAppKey());
            userIdEditText.setText(userModel.getUserId());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.interact_brocast_prepare_header_left_icon: {
                finish();
                break;
            }
            case R.id.interact_brocast_prepare_set: {
                Intent mIntent = new Intent(this, InteractSettingActivity.class);
                startActivity(mIntent);
                break;
            }
            case R.id.interact_login: {
                doLogin();
                break;
            }
            case R.id.interact_brocast_prepare_type1: {
                Intent mIntent = new Intent(this, InteracatTypeHallActivity.class);
                mIntent.putExtra(InteractConstant.INTENT_EXTRA_INTERACT_TYPE, InteractConstant.ROOM_TYPE_ANCHOR_AND_GUEST);
                mIntent.putExtra(InteractConstant.INTENT_EXTRA_INTERACT_TYPE_NAME, "主播&嘉宾");
                startActivity(mIntent);
                break;
            }
            case R.id.interact_brocast_prepare_type2: {
                Intent mIntent = new Intent(this, InteracatTypeHallActivity.class);
                mIntent.putExtra(InteractConstant.INTENT_EXTRA_INTERACT_TYPE, InteractConstant.ROOM_TYPE_ANCHOR_AND_ANCHOR);
                mIntent.putExtra(InteractConstant.INTENT_EXTRA_INTERACT_TYPE_NAME, "主播&主播");
                startActivity(mIntent);
                break;
            }
            case R.id.interact_brocast_prepare_type3: {
                Intent mIntent = new Intent(this, InteracatTypeHallActivity.class);
                mIntent.putExtra(InteractConstant.INTENT_EXTRA_INTERACT_TYPE, InteractConstant.ROOM_TYPE_HOME_PARTY);
                mIntent.putExtra(InteractConstant.INTENT_EXTRA_INTERACT_TYPE_NAME, "开趴大厅");
                startActivity(mIntent);
                break;
            }
        }
    }

    private void checkSelfPermissions() {
        checkSelfPermissionAndRequest(new String[] {
                Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
    }

    private void checkSelfPermissionAndRequest(String[] permissionList) {
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

    private boolean checkRecordPermission() {
        boolean canStartRecord = true;
        if (!selfPermissionGranted(this, Manifest.permission.RECORD_AUDIO)) {
            showToast("未获取录音权限");
            canStartRecord = false;
        }
        if (!selfPermissionGranted(this, Manifest.permission.CAMERA)) {
            showToast("未获取相机权限");
            canStartRecord = false;
        }
        if (!selfPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showToast("未获取写本地文件权限");
            canStartRecord = false;
        }
        return canStartRecord;
    }

    private boolean selfPermissionGranted(Context mContext, String permission) {
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

    private void doLogin() {
        if (mLogining) {
            return;
        }

        final String cid = channelIdEditText.getText().toString();
        final String appKey = appKeyEditText.getText().toString();
        final String secretKey = secretKeyEditText.getText().toString();
        if (TextUtils.isEmpty(cid)) {
            showToast("ChannelId不能为空, 这个参数需要到官网上申请，地址：https://live.360.cn --用户中心。");
            return;
        }
        if (TextUtils.isEmpty(appKey)) {
            showToast("AppKey不能为空, 这个参数需要到官网上申请，地址：https://live.360.cn --用户中心。");
            return;
        }
        if (TextUtils.isEmpty(secretKey)) {
            showToast("SecretKey不能为空, 这个参数需要到官网上申请，地址：https://live.360.cn --用户中心。");
            return;
        }

        InteractGlobalManager.getInstance().setChannelId(cid);
        InteractGlobalManager.getInstance().setAppKey(appKey);
        InteractGlobalManager.getInstance().setSecretKey(secretKey);
        InteractGlobalManager.getInstance().setSessionId(MD5.encryptMD5(String.valueOf(System.currentTimeMillis()) + String.valueOf(new Random().nextInt())));

        mLogining = true;
        InteractServerApi.userLogin(new InteractServerApi.ResultCallback<InteractUserModel>() {

            @Override
            public void onSuccess(InteractUserModel data) {
                if (isFinishing()) {
                    return;
                }

                mLogining = false;

                if (data != null && !TextUtils.isEmpty(data.getUserId())) {
                    InteractGlobalManager.getInstance().setUser(data);

                    changeToLoginSuccessUI();
                    userIdEditText.setText(data.getUserId());

                    initIM();
                } else {
                    showToast("登录失败");
                }
            }

            @Override
            public void onFailed(int errCode, String errMsg) {
                if (isFinishing()) {
                    return;
                }

                mLogining = false;
                showToast("登录失败(" + errCode + ")");
            }
        });
    }

    public void initIM() {

        InteractIMManager.getInstance().init(getApplicationContext());
        //        InteractIMManager.getInstance().addReceiveCommandistener(mOnReceiveCommandListener);

        InteractUserModel userModel = InteractGlobalManager.getInstance().getUser();

        InteractIMManager.getInstance().connect(userModel, new InteractIMManager.ConnectCallback() {
            @Override
            public void onSuccess(String userId) {
                showToast(userId + ":connect success!");
            }

            @Override
            public void onError(int errorCode) {
                showToast("connect failed!");
            }
        });
    }

    //    InteractIMManager.OnReceiveCommandListener mOnReceiveCommandListener = new InteractIMManager.OnReceiveCommandListener() {
    //
    //        @Override
    //        public void onReceiveCommand(InteractUserModel userFrom, InteractIMManager.CommandInfo command) {
    //            if (command != null) {
    //                showToast(InteractIMManager.getInstance().getCommandNote(command.getCmd()));
    //            }
    //        }
    //
    //        @Override
    //        public void onReceiveOtherCommand(InteractUserModel userFrom, String str) {
    //
    //        }
    //
    //    };

    Handler mHandler = new Handler(Looper.getMainLooper());

    private void showToast(final String toast) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PrepareInteractBrocastActivity.this.getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        //        InteractIMManager.getInstance().removeReceiveCommandistener(mOnReceiveCommandListener);
        super.onDestroy();
    }

}
