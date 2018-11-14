
package com.qihoo.videocloud.interactbrocast.livingcammera;

import android.graphics.SurfaceTexture;

/**
 * Created by liuyanqing on 2017/1/19.
 */

public interface VideoSourceListener {

    public void onFrameAvailable(byte[] bytes, int width, int height, int rotation, long ts, int format);

    public SurfaceTexture getSurfaceTexture(int previewWidth, int previewHeight);

}
