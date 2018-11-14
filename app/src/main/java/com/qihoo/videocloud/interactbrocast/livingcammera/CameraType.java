
package com.qihoo.videocloud.interactbrocast.livingcammera;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;

public enum CameraType {
    FRONT, BACK;

    public int getCameraId() {
        if (!Util.isFroyo()) {
            switch (this) {
                case FRONT:
                    return getCameraIdByInfo(CameraInfo.CAMERA_FACING_FRONT);
                case BACK:
                    return getCameraIdByInfo(CameraInfo.CAMERA_FACING_BACK);
                default:
                    return 0;
            }
        } else {
            return 0;
        }
    }

    private int getCameraIdByInfo(int info) {
        int defaultCameraId = 0;
        if (!Util.isFroyo()) {
            CameraInfo cameraInfo = new CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == info) {
                    defaultCameraId = i;
                }
            }
        }
        return defaultCameraId;
    }
}
