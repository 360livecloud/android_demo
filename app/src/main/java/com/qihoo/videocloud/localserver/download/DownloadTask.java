
package com.qihoo.videocloud.localserver.download;

/**
 * Created by LeiXiaojun on 2017/9/27.
 */
public class DownloadTask {

    public String rid;

    public String url;

    public String file;

    public long position;

    public long total;

    public double speed;

    /**
     * 下载状态，参见{@link DownloadConstant.State}
     */
    public int state;

    public int errCode;

    public String errMsg;

    @Override
    public String toString() {
        return "rid=" + rid +
                ", url=" + url +
                ", file=" + file +
                ", position=" + position +
                ", total=" + total +
                ", speed=" + speed +
                ", state=" + state +
                ", errCode=" + errCode +
                ", errMsg=" + errMsg;
    }
}
