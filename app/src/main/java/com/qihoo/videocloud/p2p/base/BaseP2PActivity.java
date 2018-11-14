
package com.qihoo.videocloud.p2p.base;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.utils.FileUtils;
import com.qihoo.videocloud.QHVCPlayer;
import com.qihoo.videocloud.p2p.setting.P2PSettingConfig;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseP2PActivity extends Activity {

    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = 0X0001;

    private static AtomicInteger sP2PInit = new AtomicInteger(0);
    private AtomicBoolean mP2PInit = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkSelfPermissionAndRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
            initP2P();
        }
    }

    @Override
    protected void onDestroy() {
        unInitP2P();
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
                    initP2P();
                } else {
                    Toast.makeText(this, "您拒绝了SD卡存储权限！", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void initP2P() {
        if (sP2PInit.getAndIncrement() == 0) {
            QHVCPlayer.setP2PCacheSize(P2PSettingConfig.CACHE_SIZE);
            mP2PInit.set(true);
        } else {
            mP2PInit.set(true);
        }
    }

    private void unInitP2P() {
        if (mP2PInit.get()) {
            if (sP2PInit.decrementAndGet() == 0) {
                Logger.i(P2PSettingConfig.TAG, "P2P销毁！");
            }
        }
    }

    protected boolean checkP2PValid() {
        if (sP2PInit.get() == 0) {
            Toast.makeText(this, "P2P尚未初始化！", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private static String getP2PCacheDir() {
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
        dir = dir + File.separator + "LiveCloud" + File.separator + "P2PCache" + File.separator;
        FileUtils.createDir(dir);
        return dir;
    }
}
