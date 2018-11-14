
package com.qihoo.videocloud.interactbrocast.modle;

/**
 * Created by wangpengfei on 2018/3/21.
 */

public class InteractIMContext {
    private String vendor;
    private String appKey;
    private String appSecret;

    public InteractIMContext(String vendor, String appKey, String appSecret) {
        this.vendor = vendor;
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    public String getVendor() {
        return vendor;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }
}
