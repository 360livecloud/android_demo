
package com.qihoo.videocloud.interactbrocast;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.qihoo.videocloud.interactbrocast.main.InteractConstant;

import java.util.HashMap;

/**
 * Created by liuyanqing on 2016/11/17.
 */

public class BaseActivity extends Activity implements View.OnTouchListener {

    protected int mScreenWidth;
    protected int mScreenHeight;
    protected float mScreenDensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initWH();
    }

    private void initWH() {
        WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(displayMetrics);

        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        mScreenDensity = displayMetrics.density;
    }

    protected boolean checkSelfPermissions() {
        return checkSelfPermissionAndRequest(Manifest.permission.RECORD_AUDIO, InteractConstant.PERMISSION_REQ_ID_RECORD_AUDIO) &&
                checkSelfPermissionAndRequest(Manifest.permission.CAMERA, InteractConstant.PERMISSION_REQ_ID_CAMERA) &&
                checkSelfPermissionAndRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE, InteractConstant.PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
    }

    public boolean checkAudioPermissionAndRequest() {
        return checkSelfPermissionAndRequest(Manifest.permission.RECORD_AUDIO, InteractConstant.PERMISSION_REQ_ID_RECORD_AUDIO);
    }

    public boolean checkCameraPermissionAndRequest() {
        return checkSelfPermissionAndRequest(Manifest.permission.CAMERA, InteractConstant.PERMISSION_REQ_ID_CAMERA);
    }

    public boolean checkCameraPermission() {
        return checkSelfPermission(Manifest.permission.CAMERA, InteractConstant.PERMISSION_REQ_ID_CAMERA);
    }

    public boolean checkAudioPermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO, InteractConstant.PERMISSION_REQ_ID_RECORD_AUDIO);
    }

    public boolean checkSelfPermissionAndRequest(String permission, int requestCode) {
        //        log.debug("checkSelfPermission " + permission + " " + requestCode);
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

    public boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String permissions[], @NonNull int[] grantResults) {
        //log.debug("onRequestPermissionsResult " + requestCode + " " + Arrays.toString(permissions) + " " + Arrays.toString(grantResults));
        switch (requestCode) {
            case InteractConstant.PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doAgreeRecordAudio();
                } else {
                    doRefuseRecordAudio();
                }
                break;
            }
            case InteractConstant.PERMISSION_REQ_ID_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doAgreeCamera();
                } else {
                    doRefuseCamera();
                }
                break;
            }
            case InteractConstant.PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //
                } else {
                    finish();
                }
                break;
            }
        }
    }

    protected void doRefuseRecordAudio() {
        //showToast("您拒绝了录音权限！");
    }

    protected void doRefuseCamera() {
        //showToast("您拒绝了摄像头权限！");
    }

    protected void doAgreeRecordAudio() {

    }

    protected void doAgreeCamera() {

    }

    /*
     * 获取状态栏高度
     * @param context
     * @return
             */
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen",
                "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 获取导航栏高度
     *
     * @return
     */
    public int getNavigationHeight() {
        int result = 0;
        int resourceId = 0;
        int rid = getResources().getIdentifier("config_showNavigationBar", "bool", "android");
        if (rid != 0) {
            resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            return getResources().getDimensionPixelSize(resourceId);
        } else
            return 0;
    }

    public void showToast(final String content) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BaseActivity.this.getApplicationContext(), content, Toast.LENGTH_LONG).show();
            }
        });
    }

    HashMap<View, int[]> touchMap = new HashMap<>();

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                int last_X = (int) event.getRawX();
                int last_Y = (int) event.getRawY();
                touchMap.put(v, new int[] {
                        last_X, last_Y
                });
                v.getParent().bringChildToFront(v);
                break;
            case MotionEvent.ACTION_MOVE:
                int[] lastPoint = touchMap.get(v);
                if (lastPoint != null) {
                    int dx = (int) event.getRawX() - lastPoint[0];
                    int dy = (int) event.getRawY() - lastPoint[1];
                    //                int left = v.getLeft() + dx;
                    //                int top = v.getTop() + dy;
                    //                int right = v.getRight() + dx;
                    //                int bottom = v.getBottom() + dy;
                    //
                    //                v.layout(left, top, right, bottom);
                    //                Logger.i(HostInConstant.TAG, "ian, position: (" + left +", " + top + ", " + right + ", " + bottom + ")");
                    int left = (int) v.getX() + dx;
                    int top = (int) v.getY() + dy;
                    v.setX(left);
                    v.setY(top);
                    lastPoint[0] = (int) event.getRawX();
                    lastPoint[1] = (int) event.getRawY();
                    touchMap.put(v, lastPoint);
                    v.getParent().requestLayout();
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return false;
    }

}
