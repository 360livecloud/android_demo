
package com.qihoo.videocloud.interactbrocast.main;

import com.qihoo.livecloud.interact.api.QHVCInteractiveEventHandler;

/**
 * Created by liuyanqing on 2016/11/17.
 */

public interface InteractCallBackEvent {

    /**
     *  加载互动直播引擎数据成功回调
     该回调方法表示SDK加载引擎数据成功。该回调成功后，业务可以进行一系列参数的设置，之后调用joinChannel以及后续操作。
    
     * @param roomId 房间Id
     * @param uid 用户 ID
     */
    public void onLoadEngineSuccess(String roomId, String uid);

    /**
     * 加入频道回调
     * 表示客户端已经登入服务器.
     *
     * @param channel 频道名
     * @param uid     用户 ID。
     * @param elapsed 从joinChannel 开始到该事件产生的延迟（毫秒）
     */
    public void onJoinChannelSuccess(String channel, String uid, int elapsed);

    /**
     * 说话声音音量提示回调
     *  提示谁在说话及其音量，默认禁用。可以通过 enableAudioVolumeIndication 方法设置。
     * @param speakers 说话者（数组）。
     * @param totalVolume （混音后的）总音量（ 0~255）
     */
    public void onAudioVolumeIndication(QHVCInteractiveEventHandler.AudioVolumeInfo[] speakers, int totalVolume);

    /**
     * 其他用户离开当前频道回调
     * 提示有用户离开了频道（或掉线）。
     *
     * @param uid    用户 ID
     * @param reason 离线原因：
     *               UserOfflineReason.USER_OFFLINE_QUIT：用户主动离开
     *               UserOfflineReason.USER_OFFLINE_DROPPED：因过长时间收不到对方数据包，超时掉线。
     */
    public void onUserOffline(String uid, int reason);

    /**
     * 离开频道回调
     *
     * @param stats 相关的统计信息。
     */
    public void onLeaveChannel(QHVCInteractiveEventHandler.RtcStats stats);

    /**
     * 发生错误回调
     * 表示 SDK 运行时出现了（网络或媒体相关的）错误。通常情况下， SDK 上报的错误意味着
     * SDK 无法自动恢复，需要 APP 干预或提示用户。
     *
     * @param errType 错误类型
     *                ErrorType.JOIN_ERR(3601): 加入频道时错误
     *                ErrorType.PUBLISH_ERR(3602): 连麦中错误
     */
    public void onError(int errType, int errCode);

    /**
     * 统计数据回调
     * 该回调定期上报SDK的运行时的状态，每2-3秒触发一次。
     *
     * @param stats 统计信息
     */
    public void onRtcStats(QHVCInteractiveEventHandler.RtcStats stats);

    /**
     * 连接丢失回调
     * 该回调方法表示 SDK 和服务器失去了网络连接，并且尝试自动重连一段时间（默认 10 秒）后仍未连上。
     *
     * @param errCode
     */
    public void onConnectionLost(int errCode);

    /**
     * 远端视频显示回调
     *
     * @param uid     用户 ID，指定是哪个用户的视频流
     * @param width   视频流宽(像素)
     * @param height
     * @param elapsed 加入频道开始到该回调触发的延迟（毫秒）
     */
    public void onFirstRemoteVideoFrame(String uid, int width, int height, int elapsed);

    /**
     * 远端视频的统计数据回调
     *
     * @param stats
     */
    public void onRemoteVideoStats(QHVCInteractiveEventHandler.RemoteVideoStats stats);

    public void onLocalVideoStats(QHVCInteractiveEventHandler.LocalVideoStats stats);

    /**
     * 角色身份切换成功回调。
     * 当调用setClientRole()方法切换角色身份时，切换成功后回调此方法。如果切换身份失败，会回调onError()。
     * 注：只有加入频道成功之后切换身份才会有此回调。
     *
     * @param clientRole 改变后的角色类型
     */
    public void onChangeClientRoleSuccess(int clientRole);
}
