
package com.qihoo.videocloud.interactbrocast.main;

import com.qihoo.livecloud.interact.api.QHVCInteractiveConstant;

/**
 * Created by liuyanqing on 2016/11/11.
 */

public class InteractConstant {
    public static final String TAG = QHVCInteractiveConstant.TAG;

    //KEY
    public static final String KEY_CHANNEL_NAME = "key_channel_name";
    public static final String KEY_STREAM_MODE = "key_stream_mode";
    public static final String KEY_VIDEO_CAPTURE = "key_video_capture";
    public static final String KEY_VIDEO_RENDER_MODE = "key_video_render_mode";

    public static final String DEVELOP_EVN = "develop_evn";/*开发环境*/
    public static final int DEVELOP_EVN_TEST = 0;/*测试环境*/
    public static final int DEVELOP_EVN_ONLINE = 1;/*线上环境*/
    public static final String ENCODE_TYPE = "interact_encode_type";/*编码类型*/
    public static final int ENCODE_TYPE_SOFT = 0;/*软解*/
    public static final int ENCODE_TYPE_HARD = 1;/*硬解*/
    public static final String BROCAST_SETTING_PROFILE_TYPE = "brocast_setting_profile_type";/*主播配置参数*/
    public static final String GUEST_SETTING_PROFILE_TYPE = "guest_setting_profile_type";/*嘉宾配置参数*/

    public static final boolean USE_HUAJIAO_DRAW = true; //TODO TEST
    public static final boolean USE_SET_SURFACE = true; //模拟setSurfece方式播放远端视频

    /**
     *  标示视频由谁来提供（采集）
     */
    public static final int VIDEO_SDK_COMMON_CAPTURE = 1; //SDK采集（普通）
    public static final int VIDEO_USER_CAPTURE = 3; //业务采集视频（可支持美颜等）

    //标识视频采集方式
    public enum VideoCapture {
        RECORD_ONPREVIEWFRAME, RECORD_GPU, RECORD_GPU_READPIXELS
    };

    public static final VideoCapture CURR_VIDEO_CAPTURE = VideoCapture.RECORD_GPU; //VideoCapture.RECORD_ONPREVIEWFRAME;

    public static final int BASE_VALUE_PERMISSION = 0X0001;
    public static final int PERMISSION_REQ_ID_RECORD_AUDIO = BASE_VALUE_PERMISSION + 1;
    public static final int PERMISSION_REQ_ID_CAMERA = BASE_VALUE_PERMISSION + 2;
    public static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = BASE_VALUE_PERMISSION + 3;

    public static final int MAX_PEER_COUNT = 3;

    public static final int DEFAULT_PROFILE_IDX = 4; // default use 360P

    public static class PrefManager {
        public static final String PREF_PROPERTY_PROFILE_IDX = "pref_profile_index";
    }

    public static int[] VIDEO_PROFILES = new int[] {
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_240P_3, // 240x240   15   140
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_240P_4, // 424x240   15   220
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_360P_3, // 360x360   15   260
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_360P_6, // 360x360   30   400
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_360P, // 640x360   15   400
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_360P_4, // 640x360   30   600 （声网） // 640x360   15   600 （即构）2
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_360P_9, // 640x360   15   800
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_480P_3, // 480x480   15   400
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_480P_6, // 480x480   30   600
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_480P_8, // 848x480   15   610
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_480P_9, // 848x480   30   930
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_720P, // 1280x720  15   1130
            QHVCInteractiveConstant.VideoProfile.VIDEO_PROFILE_720P_3 // 1280x720  30   1710
    };

    /**
     * 房间类型-主播&主播
     */
    public static final int ROOM_TYPE_ANCHOR_AND_ANCHOR = 1;
    /**
     * 房间类型-主播&嘉宾
     */
    public static final int ROOM_TYPE_ANCHOR_AND_GUEST = 2;
    /**
     * 房间类型-轰趴
     */
    public static final int ROOM_TYPE_HOME_PARTY = 3;

    /**
     * 房间生命周期类型-绑定到主播
     */
    public static final int ROOM_LIFE_TYPE_BIND_ANCHOR = 1;
    /**
     * 房间生命周期类型-绑定到房间
     */
    public static final int ROOM_LIFE_TYPE_BIND_ROOM = 2;

    /**
     * 互动方式-音视频
     */
    public static final int TALK_TYPE_ALL = 0;
    /**
     * 互动方式-仅音频
     */
    public static final int TALK_TYPE_AUDIO = 1;
    /**
     * 互动方式-仅视频
     */
    public static final int TALK_TYPE_VIDEO = 2;

    /**
     * 用户身份信息-主播
     */
    public static final int USER_IDENTITY_ANCHOR = 1;
    /**
     * 用户身份信息-嘉宾
     */
    public static final int USER_IDENTITY_GUEST = 2;
    /**
     * 用户身份信息-观众
     */
    public static final int USER_IDENTITY_AUDIENCE = 0;


    /**
     * 错误类型-服务端返回值解析异常
     */
    public static final int ERROR_SERVER_CONTENT_PARSE_FAILED = 1000;

    /**
     * Intent传递参数-房间类型，参见{@link #ROOM_TYPE_ANCHOR_AND_ANCHOR}等
     */
    public static String INTENT_EXTRA_INTERACT_TYPE = "interact_type";
    /**
     * Intent传递参数-房间类型名称
     */
    public static String INTENT_EXTRA_INTERACT_TYPE_NAME = "interact_type_name";
    /**
     * Intent传递参数-房间是音频或视频
     */
    public static String INTENT_EXTRA_INTERACT_TALK_TYPE = "interact_talk_type";
    /**
     * Intent传递参数-房间ID
     */
    public static String INTENT_EXTRA_INTERACT_ROOM_ID = "interact_room_id";
    /**
     * Intent传递参数-房间信息
     */
    public static String INTENT_EXTRA_INTERACT_ROOM_DATA = "interact_room_data";

    /**
     * Intent传递参数-用户身份
     */
    public static String INTENT_EXTRA_USER_IDENTITY = "interact_user_identity";
    /**
     * Intent传递参数-sdk鉴权是否成功
     */
    public static String INTENT_EXTRA_SDK_USIN_RIGHT = "intent_extra_sdk_usin_right";

}
