
package com.qihoo.videocloud.interactbrocast.data;

import com.qihoo.videocloud.interactbrocast.modle.InteractIMContext;
import com.qihoo.videocloud.interactbrocast.modle.InteractUserModel;

/**
 * Created by LeiXiaojun on 2018/3/5.
 */

public class InteractGlobalManager {

    private static InteractGlobalManager sInst = null;

    private InteractGlobalManager() {

    }

    public static synchronized InteractGlobalManager getInstance() {
        if (sInst == null) {
            sInst = new InteractGlobalManager();
        }
        return sInst;
    }

    private InteractUserModel mUser;

    public InteractUserModel getUser() {
        return mUser;
    }

    public void setUser(InteractUserModel user) {
        this.mUser = user;
    }

    /**
     * 渠道ID
     */
    private String mChannelId;

    public String getBusinessId() {
        return mChannelId;
    }

    public void setChannelId(String channelId) {
        this.mChannelId = channelId;
    }

    public String getChannelId() {
        return this.mChannelId;
    }

    /**
     * 会话ID
     */
    private String mSessionId;

    public String getSessionId() {
        return mSessionId;
    }

    public void setSessionId(String sessionId) {
        this.mSessionId = sessionId;
    }

    /**
     * 业务签名，用于互动直播SDK鉴权
     */
    private String mUSign;

    public String getUSign() {
        return mUSign;
    }

    public void setUSign(String uSign) {
        this.mUSign = uSign;
    }

    /**
     * 业务标识AppKey，用于互动直播SDK鉴权
     */
    private String mAppKey;

    public void setAppKey(String appKey) {
        this.mAppKey = appKey;
    }

    public String getAppKey() {
        return this.mAppKey;
    }

    /**
     * 业务Secret KEY，必须放在业务服务端，切勿放在客户端。此处放在Demo中仅用于演示
     */
    private String mSecretKey;

    public String getSecretKey() {
        return mSecretKey;
    }

    public void setSecretKey(String secretKey) {
        this.mSecretKey = secretKey;
    }

    /**
     * IM上下文信息
     */

    private InteractIMContext imContext;

    public InteractIMContext getImContext() {
        return imContext;
    }

    public void setImContext(InteractIMContext imContext) {
        this.imContext = imContext;
    }

}
