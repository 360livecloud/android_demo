/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qihoo.videocloud.interactbrocast.livingcammera;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Camera-related utility functions.
 */
public class CameraUtils {

    private static final String TAG = "CameraUtils";

    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size for video.
     * <p>
     * TODO: should do a best-fit match, e.g.
     * https://github.com/commonsguy/cwac-camera/blob/master/camera/src/com/commonsware/cwac/camera/CameraUtils.java
     */
    public static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.d(TAG, "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
        }

        //for (Camera.Size size : parms.getSupportedPreviewSizes()) {
        //    Log.d(TAG, "supported: " + size.width + "x" + size.height);
        //}

        for (Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }

        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        // else use whatever the default size is
    }

    public static void setCameraPreviewSize(Camera.Parameters parms) {

        List<Size> previewSizes = parms.getSupportedPreviewSizes();
        List<Size> perfectSizes = new ArrayList<Size>();

        int w = 0;
        int h = 0;
        Double pw = (double) 16.0;
        Double ph = (double) 9.0;

        for (Size size : previewSizes) {
            if (((size.width * ph) / (size.height * pw)) == 1) {
                perfectSizes.add(size);
            }
        }

        if (perfectSizes.size() > 0) {
            // 找到合适的情况
            // 数量大于1，先排序
            if (perfectSizes.size() > 1) {
                Collections.sort(perfectSizes, new Comparator<Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        int v1 = lhs.width * lhs.height;
                        int v2 = rhs.width * rhs.height;
                        return v1 > v2 ? 1 : (v1 == v2 ? 0 : -1);
                    }
                });
            }

            int num = perfectSizes.size() - 1;
            w = perfectSizes.get(num).width;
            h = perfectSizes.get(num).height;

        } else {
            // 没有找到合适的情况
            if (previewSizes.size() > 1) {
                Collections.sort(previewSizes, new Comparator<Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        int v1 = lhs.width * lhs.height;
                        int v2 = rhs.width * rhs.height;
                        return v1 > v2 ? 1 : (v1 == v2 ? 0 : -1);
                    }
                });
            }

            w = previewSizes.get(previewSizes.size() - 1).width;
            h = previewSizes.get(previewSizes.size() - 1).height;
        }

        parms.setPreviewSize(w, h);
    }

    /**
     * Attempts to find a fixed preview frame rate that matches the desired frame rate.
     * <p>
     * It doesn't seem like there's a great deal of flexibility here.
     * <p>
     * TODO: follow the recipe from http://stackoverflow.com/questions/22639336/#22645327
     *
     * @return The expected frame rate, in thousands of frames per second.
     */
    public static int chooseFixedPreviewFps(Camera.Parameters parms, int desiredThousandFps) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            //Log.d(TAG, "entry: " + entry[0] + " - " + entry[1]);
            if ((entry[0] == entry[1]) && (entry[0] == desiredThousandFps)) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }

        int[] tmp = new int[2];
        parms.getPreviewFpsRange(tmp);
        int guess;
        if (tmp[0] == tmp[1]) {
            guess = tmp[0];
        } else {
            guess = tmp[1] / 2; // shrug
        }

        Log.d(TAG, "Couldn't find match for " + desiredThousandFps + ", using " + guess);
        return guess;
    }
}
