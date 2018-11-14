
package com.qihoo.videocloud.recorder;

/**
 * Created by huchengming on 2017/7/6.
 */

public class RecorderConstants {
    public static final String CHOICE_HORIZONTAL = "choice_horizontal";/*是否横屏*/
    public static final String CHOICE_ONLY_VOICE = "choice_only_voice";/*是否纯音频*/
    public static final String SAVE_VIDEO_FILE = "save_video_file";/*是否保存视频文件*/

    public static final String RECORDERLOCAL_CHOICE_HORIZONTAL = "recorderlocal_choice_horizontal";/*是否横屏*/
    public static final String RECORDERLOCAL_CHOICE_ONLY_VOICE = "recorderlocal_choice_only_voice";/*是否纯音频*/

    public static final String RESOLUTION_RATIO = "resolution_ratio";/*分辨率*/
    public static final String ENCODE_TYPE = "encode_type";/*编码类型*/
    public static final String VIDEO_FPS = "video_fps";/*视频帧率*/
    public static final String CODE_RATE = "code_rate";/*码率*/
    public static final String RECORDE_LOCAL_CODE_RATE = "recorde_local_code_rate";/*本地录制码率*/
    public static final String AUTO_ADJUST_CODE_RATE = "auto_adjust_code_rat";/*码率自适应*/
    public static final String AUDIO_CODE_RATE = "audio_code_rate";/*音频码率*/
    public static final String AUDIO_SAMPLE = "audio_sample";/*音频采样率*/

    /*prepareRecordActivity*/
    public static final String BUSINESS_ID = "businessId";/*businessId*/
    public static final String CHANNEL_ID = "channelId";/*channelId*/
    public static final String URL = "url";/*推流地址*/
    public static final String TITLE = "title";/*推流标题*/
    /*权限申请相关*/
    public static final int BASE_VALUE_PERMISSION = 0X0001;
    public static final int PERMISSION_REQ_ID_RECORD_AUDIO = BASE_VALUE_PERMISSION + 1;
    public static final int PERMISSION_REQ_ID_CAMERA = BASE_VALUE_PERMISSION + 2;
    public static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = BASE_VALUE_PERMISSION + 3;

}
