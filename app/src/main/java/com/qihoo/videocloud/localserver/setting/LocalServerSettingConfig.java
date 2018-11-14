
package com.qihoo.videocloud.localserver.setting;

/**
 * Created by LeiXiaojun on 2017/9/12.
 */
public class LocalServerSettingConfig {

    public static final String TAG = "LocalServer";

    /**
     * 缓存空间占用大小，单位MB
     */
    public static final int CACHE_SIZE = 50;

    /**
     * 暂停播放时是否允许继续缓存
     */
    public static boolean ENABLE_CACHE_WHEN_PAUSE = true;

    /**
     * 启用播放器中的p2p功能
     */
    public static boolean ENABLE_P2P_IN_PLAYER = false;
}
