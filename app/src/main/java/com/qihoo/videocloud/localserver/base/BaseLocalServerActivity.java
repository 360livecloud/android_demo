
package com.qihoo.videocloud.localserver.base;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.qihoo.livecloud.sdk.QHVCSdk;
import com.qihoo.livecloud.sdk.QHVCSdkConfig;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.videocloud.localserver.download.DownloadManager;
import com.qihoo.videocloud.localserver.setting.LocalServerSettingConfig;
import com.qihoo.videocloud.utils.Utils;

import net.qihoo.videocloud.LocalServer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by LeiXiaojun on 2017/10/16.
 */

public class BaseLocalServerActivity extends Activity {

    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = 0X0001;

    private static AtomicInteger sLocalServerInit = new AtomicInteger(0);
    private AtomicBoolean mLocalServerInit = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkSelfPermissionAndRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
            initLocalServer();
        }
    }

    @Override
    protected void onDestroy() {
        unInitLocalServer();
        super.onDestroy();
    }

    private boolean checkSelfPermissionAndRequest(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            permission
                    },
                    requestCode);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initLocalServer();
                } else {
                    Toast.makeText(this, "您拒绝了SD卡存储权限！", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void initLocalServer() {
        if (sLocalServerInit.getAndIncrement() == 0) {
            LocalServer.setLogLevel(LocalServer.LOG_DEBUG);

            QHVCSdkConfig qhvcSdkConfig = QHVCSdk.getInstance().getConfig();

            Map<String, Object> params = new HashMap<>();
            params.put(LocalServer.PARAM_CACHE_SIZE, LocalServerSettingConfig.CACHE_SIZE);

            boolean ret = LocalServer.initialize(this, Utils.getCacheDir(), Utils.getDeviceId(this), qhvcSdkConfig.getBusinessId(), params);
            if (ret) {
                mLocalServerInit.set(true);
                LocalServer.setCacheSize(LocalServerSettingConfig.CACHE_SIZE);
                DownloadManager.getInstance().init();
                Logger.i(LocalServerSettingConfig.TAG, "LocalServer初始化成功！");
            } else {
                Logger.e(LocalServerSettingConfig.TAG, "LocalServer初始化失败！");
                Toast.makeText(this, "LocalServer初始化失败！", Toast.LENGTH_SHORT).show();

                sLocalServerInit.decrementAndGet();
            }
        } else {
            mLocalServerInit.set(true);
        }
    }

    private void unInitLocalServer() {
        if (mLocalServerInit.get()) {
            if (sLocalServerInit.decrementAndGet() == 0) {
                DownloadManager.getInstance().unInit();
                LocalServer.destroy();
                Logger.i(LocalServerSettingConfig.TAG, "LocalServer销毁！");
            }
        }
    }

    protected boolean checkLocalServerValid() {
        if (sLocalServerInit.get() == 0) {
            Toast.makeText(this, "LocalServer尚未初始化！", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
