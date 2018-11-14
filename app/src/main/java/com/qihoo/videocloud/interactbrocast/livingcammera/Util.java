
package com.qihoo.videocloud.interactbrocast.livingcammera;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Surface;
import android.view.WindowManager;

public class Util {
    public static boolean isFroyo() {
        return Build.VERSION_CODES.FROYO == Build.VERSION.SDK_INT;
    }

    public static boolean isPortrait(Context context) {
        return (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    public static boolean isLandscape(Context context) {
        return (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    public static boolean is2012Nexus7(CameraType type) {
        if (Build.HARDWARE.equals("grouper")) {
            if (type.equals(CameraType.FRONT)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean is2016Nexus6P() {
        if (Build.MODEL.contains("Nexus 6P")) {
            return true;
        } else {
            return false;
        }
    }

    public static int getDisplayRotationValue(Activity activity) {
        int result = 0;
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        if (rotation == Surface.ROTATION_0) {
            if (Util.isPortrait(activity)) {
                result = 90;
            } else {
                result = 0;
            }
        } else if (rotation == Surface.ROTATION_90) {
            if (Util.isPortrait(activity)) {
                result = 270;
            } else {
                result = 0;
            }
        } else if (rotation == Surface.ROTATION_180) {
            if (Util.isPortrait(activity)) {
                result = 180;
            } else {
                result = 270;
            }
        } else if (rotation == Surface.ROTATION_270) {
            if (Util.isPortrait(activity)) {
                result = 90;
            } else {
                result = 180;
            }
        }

        return result;
    }

    public static int getDisplayRotationValue(Service service) {
        int result = 0;
        int rotation = ((WindowManager) service.getApplication().getSystemService(Application.WINDOW_SERVICE)).getDefaultDisplay().getRotation();

        if (rotation == Surface.ROTATION_0) {
            if (Util.isPortrait(service)) {
                result = 90;
            } else {
                result = 0;
            }
        } else if (rotation == Surface.ROTATION_90) {
            if (Util.isPortrait(service)) {
                result = 270;
            } else {
                result = 0;
            }
        } else if (rotation == Surface.ROTATION_180) {
            if (Util.isPortrait(service)) {
                result = 180;
            } else {
                result = 270;
            }
        } else if (rotation == Surface.ROTATION_270) {
            if (Util.isPortrait(service)) {
                result = 90;
            } else {
                result = 180;
            }
        }

        return result;
    }

    public static int getDisplayRotationValue(Application application) {
        int result = 0;
        int rotation = ((WindowManager) application.getSystemService(Application.WINDOW_SERVICE)).getDefaultDisplay().getRotation();

        if (rotation == Surface.ROTATION_0) {
            if (Util.isPortrait(application)) {
                result = 90;
            } else {
                result = 0;
            }
        } else if (rotation == Surface.ROTATION_90) {
            if (Util.isPortrait(application)) {
                result = 270;
            } else {
                result = 0;
            }
        } else if (rotation == Surface.ROTATION_180) {
            if (Util.isPortrait(application)) {
                result = 180;
            } else {
                result = 270;
            }
        } else if (rotation == Surface.ROTATION_270) {
            if (Util.isPortrait(application)) {
                result = 90;
            } else {
                result = 180;
            }
        }

        return result;
    }

    public static int addDegreesToRotation(int baseParam, int param) {
        int rotation = baseParam;
        rotation = (Math.abs(rotation + param));
        int absRotation = rotation % 360;
        if (absRotation == 0) {
            rotation = 0;
        } else {
            rotation = absRotation;
        }
        return rotation;
    }
}
