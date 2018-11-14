
package com.qihoo.videocloud.player;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by guohailiang on 2017/6/21.
 */

public interface PlayConstant {
    // video 显示方式
    int SHOW_MODEL_PORT_SMALL = 0;// 竖屏-半屏显示
    int SHOW_MODEL_PORT = 1; // 竖屏-全屏显示
    int SHOW_MODEL_LAND = 2; // 横屏显示

    @IntDef({
            SHOW_MODEL_PORT_SMALL,
            SHOW_MODEL_PORT,
            SHOW_MODEL_LAND
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface ShowModel {
    }
}
