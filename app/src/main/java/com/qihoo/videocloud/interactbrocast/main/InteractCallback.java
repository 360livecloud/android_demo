
package com.qihoo.videocloud.interactbrocast.main;

import com.qihoo.livecloud.interact.api.QHVCInteractiveEventHandler;
import com.qihoo.livecloud.tools.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuyanqing on 2016/11/10.
 */

public class InteractCallback extends QHVCInteractiveEventHandler {

    private List<InteractCallBackEvent> allCallBack = new ArrayList<InteractCallBackEvent>();

    private static class HostInCallbackHolder {
        private static InteractCallback instance = new InteractCallback();
    }

    public static InteractCallback getInstance() {
        return HostInCallbackHolder.instance;
    }

    public void addCallBack(InteractCallBackEvent callback) {
        if (callback != null) {
            if (!allCallBack.contains(callback)) {
                allCallBack.add(callback);
            }
        }
    }

    public void removeCallBack(InteractCallBackEvent callback) {
        if (callback != null) {
            allCallBack.remove(callback);
        }
    }

    @Override
    public void onLoadEngineSuccess(String roomId, String uid) {
        for (int i = 0; i < allCallBack.size(); i++) {
            InteractCallBackEvent event = allCallBack.get(i);
            event.onLoadEngineSuccess(roomId, uid);
        }
    }

    @Override
    public void onJoinChannelSuccess(String channel, String uid, int elapsed) {
        for (int i = 0; i < allCallBack.size(); i++) {
            InteractCallBackEvent event = allCallBack.get(i);
            event.onJoinChannelSuccess(channel, uid, elapsed);
        }
    }

    public void onAudioVolumeIndication(QHVCInteractiveEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {
        for (int i = 0; i < allCallBack.size(); i++) {
            InteractCallBackEvent event = allCallBack.get(i);
            event.onAudioVolumeIndication(speakers, totalVolume);
        }
    }

    @Override
    public void onUserOffline(String uid, int reason) {
        for (int i = 0; i < allCallBack.size(); i++) {
            InteractCallBackEvent event = allCallBack.get(i);
            event.onUserOffline(uid, reason);
        }
    }

    @Override
    public void onLeaveChannel(RtcStats stats) {
        for (int i = 0; i < allCallBack.size(); i++) {
            InteractCallBackEvent event = allCallBack.get(i);
            event.onLeaveChannel(stats);
        }
    }

    @Override
    public void onError(int errType, int errCode) {
        for (int i = 0; i < allCallBack.size(); i++) {
            InteractCallBackEvent event = allCallBack.get(i);
            event.onError(errType, errCode);
        }
    }

    @Override
    public void onWarning(int warn) {
        Logger.w(InteractConstant.TAG, InteractConstant.TAG + ": onWarning: warn: " + warn);
    }

    @Override
    public void onFirstRemoteVideoFrame(String uid, int width, int height, int elapsed) {
        for (int i = 0; i < allCallBack.size(); i++) {
            InteractCallBackEvent event = allCallBack.get(i);
            event.onFirstRemoteVideoFrame(uid, width, height, elapsed);
        }
    }

    /**
     * 统计数据回调
     * 该回调定期上报SDK的运行时的状态，每2-3秒触发一次。
     *
     * @param stats 统计信息
     */
    public void onRtcStats(RtcStats stats) {
        for (int i = 0; i < allCallBack.size(); i++) {
            InteractCallBackEvent event = allCallBack.get(i);
            event.onRtcStats(stats);
        }
    }

    /**
     * 连接丢失回调
     * 该回调方法表示 SDK 和服务器失去了网络连接，并且尝试自动重连一段时间（默认 10 秒）后仍未连上。
     *
     * @param errCode
     */
    public void onConnectionLost(int errCode) {
        for (int i = 0; i < allCallBack.size(); i++) {
            InteractCallBackEvent event = allCallBack.get(i);
            event.onConnectionLost(errCode);
        }
    }

    /**
     * 远端视频的统计数据回调
     *
     * @param stats
     */
    public void onRemoteVideoStats(RemoteVideoStats stats) {
        for (int i = 0; i < allCallBack.size(); i++) {
            InteractCallBackEvent event = allCallBack.get(i);
            event.onRemoteVideoStats(stats);
        }
    }

    @Override
    public void onLocalVideoStats(LocalVideoStats stats) {
        for (int i = 0; i < allCallBack.size(); i++) {
            InteractCallBackEvent event = allCallBack.get(i);
            event.onLocalVideoStats(stats);
        }
    }

    @Override
    public void onChangeClientRoleSuccess(int clientRole) {
        for (int i = 0; i < allCallBack.size(); i++) {
            InteractCallBackEvent event = allCallBack.get(i);
            event.onChangeClientRoleSuccess(clientRole);
        }
    }

}
