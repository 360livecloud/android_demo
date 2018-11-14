
package com.qihoo.videocloud.localserver.download;

/**
 * Created by LeiXiaojun on 2017/9/27.
 */
public interface DownloadConstant {

    interface State {

        int STATE_WAITING = 0;

        int STATE_DOWNLOADING = 1;

        int STATE_PAUSED = 2;

        int STATE_DOWNLOADED = 3;

        int STATE_FAILED = 4;
    }
}
