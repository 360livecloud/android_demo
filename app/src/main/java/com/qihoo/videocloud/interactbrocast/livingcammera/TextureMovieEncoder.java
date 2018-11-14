/*
 * Copyright 2013 Google Inc. All rights reserved.
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.qihoo.livecloud.hc.HCGPUImageFilterQhSoften;
import com.qihoo.livecloud.interact.api.QHVCInteractiveConstant;
import com.qihoo.livecloud.interact.api.QHVCInteractiveLocalVideoRenderCallback;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.makeup.gpu.QhMakeUpApi;
import com.qihoo.videocloud.VideoCloudApplication;
import com.qihoo.videocloud.interactbrocast.faceu.DrawEff2;
import com.qihoo.videocloud.interactbrocast.faceu.EffectManager;
import com.qihoo.videocloud.interactbrocast.main.InteractConstant;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Encode a movie from frames rendered from an external texture image.
 * <p>
 * The object wraps an encoder running on a dedicated thread.  The various control messages
 * may be sent from arbitrary threads (typically the app UI thread).  The encoder thread
 * manages both sides of the encoder (feeding and draining); the only external input is
 * the GL texture.
 * <p>
 * The design is complicated slightly by the need to create an EGL context that shares state
 * with a view that gets restarted if (say) the device orientation changes.  When the view
 * in question is a GLSurfaceView, we don't have full control over the EGL context creation
 * on that side, so we have to bend a bit backwards here.
 * <p>
 * To use:
 * <ul>
 * <li>create TextureMovieEncoder object
 * <li>create an EncoderConfig
 * <li>call TextureMovieEncoder#startRecording() with the config
 * <li>call TextureMovieEncoder#setTextureId() with the texture object that receives frames
 * <li>for each frame, after latching it with SurfaceTexture#updateTexImage(),
 * call TextureMovieEncoder#frameAvailable().
 * </ul>
 * <p>
 * TODO: tweak the API (esp. textureId) so it's less awkward for simple use cases.
 */
public class TextureMovieEncoder implements Runnable {
    public static final boolean PRINT_FPS = false;

    private static final String TAG = "TextureMovieEncoder";
    private static final boolean VERBOSE = false;

    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_UPDATE_FACEU_ID = 5;
    private static final int MSG_SET_MIRRO = 6;

    private static final int MSG_QUIT = 10;

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private FullFrameRect mFullScreen;
    private int mTextureId;
    private int mFrameNum;
    private VideoEncoderCore mVideoEncoder;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;

    private Object mReadyFence = new Object(); // guards ready/running
    private boolean mReady;
    private boolean mRunning;

    AtomicInteger m_atom_meiyan_index = new AtomicInteger(0);
    float m_f_meiyan_index_inner = 0.0f;

    boolean m_b_mirro = true;
    int m_n_mirro = 0;

    //For FaceU
    DrawEff2 m_draw_eff;
    EffectManager effectManager = new EffectManager();
    //MaskManager mask_manager = new MaskManager();
    private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.RECTANGLE);
    private final Sprite2d mRect = new Sprite2d(mRectDrawable);
    Texture2dProgram mTextureProgram;

    //private RtmpStream rts;
    //    private WKTVLivingCamera mLivingCamera;
    private InteractRecorderController mCameraController;

    private EncoderConfig mConfig;

    /**
     * Encoder configuration.
     * <p>
     * Object is immutable, which means we can safely pass it between threads without
     * explicit synchronization (and don't need to worry about it getting tweaked out from
     * under us).
     * <p>
     * TODO: make frame rate and iframe interval configurable?  Maybe use builder pattern
     * with reasonable defaults for those and bit rate.
     */
    public static class EncoderConfig {
        final InteractRecorderController mCameraController;
        final EGLContext mEglContext;
        int mWidth;
        int mHeight;

        final int m_render_width;
        final int m_render_heigth;
        final int m_render_scale;

        int mEncodeWidth;
        int mEncodeHeight;

        boolean m_b_useqhface;

        public EncoderConfig(InteractRecorderController controller, EGLContext sharedEglContext, int width, int height, int r_width, int r_heigth, int r_scale, int encode_w,
                int encode_h, boolean b_useqhface) {
            mCameraController = controller;
            mEglContext = sharedEglContext;
            mWidth = width;
            mHeight = height;

            m_render_width = r_width;
            m_render_heigth = r_heigth;
            m_render_scale = r_scale;

            mEncodeWidth = encode_w;
            mEncodeHeight = encode_h;

            m_b_useqhface = b_useqhface;

            if (mEncodeWidth <= mEncodeHeight) { //竖屏
                if (mEncodeWidth != mWidth) {
                    double rate = (double) mHeight / (double) mWidth;
                    mWidth = mEncodeWidth;
                    mHeight = (int) (mWidth * rate);
                    mHeight = (mHeight % 2) == 1 ? (mHeight + 1) : mHeight;
                }
            } else { //横屏
                //                if (mEncodeHeight != mHeight) {
                //                    double rate = (double) mWidth / (double) mHeight;
                //                    mHeight = mEncodeHeight;
                //                    mWidth = (int)(mHeight * rate);
                //                    mWidth = (mWidth % 2) == 1 ? (mWidth + 1) : mWidth;
                //                }
                int temp = mWidth;
                mWidth = mHeight;
                mHeight = temp;
            }
        }

        @Override
        public String toString() {
            if (mCameraController != null) {
                return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mCameraController.getBitrate() +
                        " ' ctxt=" + mEglContext;
            }
            return "";
        }
    }

    private void handleSetMirro(int n_mirro) {
        m_b_mirro = (n_mirro == 1);
    }

    public void setMeiyanIndex(float value) {
        if (mHandler == null)
            return;
        // Float pass_value = new Float(value);
        m_atom_meiyan_index.set((int) (value * 100));
    }

    public void setDraEff(DrawEff2 draw_eff) {
        m_draw_eff = draw_eff;
    }

    /**
     * Tells the video recorder to start recording.  (Call from non-encoder thread.)
     * <p>
     * Creates a new thread, which will create an encoder using the provided configuration.
     * <p>
     * Returns after the recorder thread has started and is ready to accept Messages.  The
     * encoder may not yet be fully configured.
     */
    public void startRecording(EncoderConfig config) {
        Log.d(TAG, "Encoder: startRecording()");
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "TextureMovieEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }

    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     * <p>
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     * <p>
     * TODO: have the encoder thread invoke a callback on the UI thread just before it shuts down
     * so we can provide reasonable status UI (and let the caller know that movie encoding
     * has completed).
     */
    public void stopRecording() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
            mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
        }
        // We don't know when these will actually finish (or even start).  We don't want to
        // delay the UI thread though, so we return immediately.
    }

    /**
     * Returns true if recording has been started.
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     * <p>
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     * <p>
     * TODO: either block here until the texture has been rendered onto the encoder surface,
     * or have a separate "block if still busy" method that the caller can execute immediately
     * before it calls updateTexImage().  The latter is preferred because we don't want to
     * stall the caller while this thread does work.
     */
    float[] transform_matrix = new float[16];

    public void frameAvailable(SurfaceTexture st) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }

        //float[] transform = new float[16];      // TODO - avoid alloc every frame
        st.getTransformMatrix(transform_matrix);
        long timestamp = st.getTimestamp();
        if (timestamp == 0) {
            // Seeing this after device is toggled off/on with power button.  The
            // first frame back has a zero timestamp.
            //
            // MPEG4Writer thinks this is cause to abort() in native code, so it's very
            // important that we just ignore the frame.
            Log.w(TAG, "HEY: got SurfaceTexture with timestamp of zero");
            return;
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE,
                (int) (timestamp >> 32), (int) timestamp, transform_matrix));
    }

    public void setIsNeedMirro(boolean b_mirro) {
        if (mHandler == null)
            return;
        m_n_mirro = b_mirro ? 1 : 0;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_MIRRO, m_n_mirro, 0));
    }

    /**
     * Tells the video recorder what texture name to use.  This is the external texture that
     * we're receiving camera previews in.  (Call from non-encoder thread.)
     * <p>
     * TODO: do something less clumsy
     */
    public void setTextureId(int id) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null));
    }

    /**
     * Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
     * <p>
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<TextureMovieEncoder> mWeakEncoder;

        public EncoderHandler(TextureMovieEncoder encoder) {
            mWeakEncoder = new WeakReference<TextureMovieEncoder>(encoder);
        }

        public void release() {
            mWeakEncoder.clear();
        }

        @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
        @Override // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            TextureMovieEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }
            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timestamp = (((long) inputMessage.arg1) << 32) |
                            (((long) inputMessage.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable((float[]) obj, timestamp);
                    break;
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(inputMessage.arg1);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;
                case MSG_UPDATE_FACEU_ID:
                    encoder.handleFaceuID((String) inputMessage.obj);
                    break;
                case MSG_SET_MIRRO:
                    encoder.handleSetMirro(inputMessage.arg1);
                    break;
                case MSG_QUIT:
                    encoder.quit();

                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    /**
     * Starts recording.
     */
    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void handleStartRecording(EncoderConfig config) {
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ": handleStartRecording, EncoderConfig: " + config.toString());
        mFrameNum = 0;
        mConfig = config;
        prepareEncoder(config.mCameraController, config.mEglContext);
    }

    HCGPUImageFilterQhSoften m_gpubeauty;
    boolean canPrintFps = true;
    int frameNumber = 0;
    long startTime = 0L;
    long totalTime = 0L;

    /**
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     * <p>
     *
     * @param transform      The texture transform, from SurfaceTexture.
     * @param timestampNanos The frame's timestamp, from SurfaceTexture.
     */
    @SuppressLint("WrongCall")
    private void handleFrameAvailable(float[] transform, long timestampNanos) {
        if (VERBOSE)
            Log.d(TAG, "handleFrameAvailable tr=" + transform);

        mVideoEncoder.drainEncoder(false); //ian mark

        if (mCameraController != null) {
            synchronized (mCameraController.paint_lock) {

                if (!InteractConstant.USE_HUAJIAO_DRAW) {
                    //mFullScreen.drawFrameForEncode(mTextureId, transform);
                    mFullScreen.drawFrame(mTextureId, transform);
                } else {
                    m_f_meiyan_index_inner = (float) m_atom_meiyan_index.get() / 100;

                    if (m_gpubeauty == null) {
                        if (mCameraController.getContext() != null) {
                            m_gpubeauty = new HCGPUImageFilterQhSoften(mCameraController.getContext());
                        } else {
                            Logger.e(InteractConstant.TAG, InteractConstant.TAG + ",create GPUImageFilterQhSoften fail! context is null!");
                        }
                    } else {
                        if (m_f_meiyan_index_inner < 0.1f || m_f_meiyan_index_inner > 1.0f) {
                            if (m_b_mirro) {
                                m_gpubeauty._onDraw(mTextureId,
                                        mConfig.m_render_width / mConfig.m_render_scale,
                                        mConfig.m_render_heigth / mConfig.m_render_scale, 0, 0, 0,
                                        mConfig.mWidth,
                                        mConfig.mHeight,
                                        mFullScreen.getRectDrawable().getVertexArray(),
                                        mFullScreen.getRectDrawable().getTexMirroCoordArray(),
                                        QhMakeUpApi.EFFECT_NO,
                                        null,
                                        transform,
                                        0.6f);
                            } else {
                                m_gpubeauty._onDraw(mTextureId,
                                        mConfig.m_render_width / mConfig.m_render_scale,
                                        mConfig.m_render_heigth / mConfig.m_render_scale, 0, 0, 0,
                                        mConfig.mWidth,
                                        mConfig.mHeight,
                                        mFullScreen.getRectDrawable().getVertexArray(),
                                        mFullScreen.getRectDrawable().getTexCoordArray(),
                                        QhMakeUpApi.EFFECT_NO,
                                        null,
                                        transform,
                                        0.6f);
                            }

                            //                    m_gpubeauty._onDraw(mTextureId,
                            //                            mConfig.m_render_width / mConfig.m_render_scale,
                            //                            mConfig.m_render_heigth / mConfig.m_render_scale, 0, 0, 0,
                            //                            mConfig.mWidth,
                            //                            mConfig.mHeight,
                            //                            mFullScreen.getRectDrawable().getVertexArray(),
                            //                            mFullScreen.getRectDrawable().getTexMirroCoordArray(),
                            //                            QhMakeUpApi.EFFECT_SOFT,
                            //                            null,
                            //                            transform,
                            //                            0.6f);
                        } else {
                            m_gpubeauty.setSoftenLevel(m_f_meiyan_index_inner);
                            if (m_b_mirro) {
                                m_gpubeauty._onDraw(mTextureId,
                                        mConfig.m_render_width / mConfig.m_render_scale,
                                        mConfig.m_render_heigth / mConfig.m_render_scale, 0, 0, 0,
                                        mConfig.mWidth,
                                        mConfig.mHeight,
                                        mFullScreen.getRectDrawable().getVertexArray(),
                                        mFullScreen.getRectDrawable().getTexMirroCoordArray(),
                                        QhMakeUpApi.EFFECT_SOFT,
                                        null,
                                        transform,
                                        0.6f);
                            } else {
                                m_gpubeauty._onDraw(mTextureId,
                                        mConfig.m_render_width / mConfig.m_render_scale,
                                        mConfig.m_render_heigth / mConfig.m_render_scale, 0, 0, 0,
                                        mConfig.mWidth,
                                        mConfig.mHeight,
                                        mFullScreen.getRectDrawable().getVertexArray(),
                                        mFullScreen.getRectDrawable().getTexCoordArray(),
                                        QhMakeUpApi.EFFECT_SOFT,
                                        null,
                                        transform,
                                        0.6f);
                            }
                        }
                    }
                }

            }
        }

        //draw faceu
        drawFaceU();

        int width = mConfig.mWidth;
        int height = mConfig.mHeight;

        if (InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_GPU_READPIXELS) {
            byte b[] = new byte[width * height * 4];
            ByteBuffer bb = ByteBuffer.wrap(b);
            long lastTime = System.currentTimeMillis();
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);
            long time = System.currentTimeMillis() - lastTime;
            totalTime += time;
            if (Logger.LOG_ENABLE) {
                if (frameNumber > 0) {
                    Logger.d(TAG, TAG + ", readPixels time: " + time + ", averageTime:  " + totalTime / frameNumber);
                }
            }
            QHVCInteractiveLocalVideoRenderCallback localVideoRenderCallback = mCameraController.getLocalVideoRenderCallback();
            if (localVideoRenderCallback != null) {
                localVideoRenderCallback.onFrameAvailable(b, width, height);
            }

            VideoSourceListener mYuvListener = mCameraController.getVideoListener();
            int rotation = 180;
            if (mYuvListener != null) {
                mYuvListener.onFrameAvailable(b, width, height, rotation, System.currentTimeMillis(), QHVCInteractiveConstant.VideoFormat.RGBA);
            }

            printAverageFps();
        }

        /*
        byte b[] = new byte [width * height * 4];
        ByteBuffer bb = ByteBuffer.wrap(b);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);
        //做调试用，存图到SD卡
        //createAndSaveBitmap(width, height, bb);
        
        int rotation = 180;
        //        int rotation = 0;
        VideoSourceListener mYuvListener = mCameraController.getVideoListener();
        if (mYuvListener != null) {
            mYuvListener.onFrameAvailable(b, width, height, rotation, System.currentTimeMillis(), QHLiveCloudConstant.VideoFormat.RGBA);
        }
        //printFPS();
        */

        //        VideoSourceListener mYuvListener = mCameraController.getVideoListener();
        //        if (mYuvListener != null) {
        //            mYuvListener.onTextureCaptured(mTextureId, width, height, System.currentTimeMillis());
        //        }

        mInputWindowSurface.setPresentationTime(timestampNanos);
        mInputWindowSurface.swapBuffers();
    }

    private void printAverageFps() {
        if (canPrintFps == false) {
            return;
        }

        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }
        frameNumber++;
        long time = (System.currentTimeMillis() - startTime) / 1000;
        if (time > 0) {
            int fps = (int) (frameNumber / time);
            if (Logger.LOG_ENABLE) {
                Logger.d(TAG, TAG + ", averageFps : " + fps);
            }
        }
    }

    /////For faceu start
    public void updateFaceUID(final String id) {
        if (mHandler == null) {
            handleFaceuID(id);
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_FACEU_ID, id));
        }
        //        mHandler.post(new Runnable() {
        //            @Override
        //            public void run() {
        //                handleFaceuID(id);
        //            }
        //        });

    }

    private void handleFaceuID(String id) {
        effectManager.clear();
        //mask_manager.clear();
        if (DrawEff2.TypeOfRes(id) == DrawEff2.TYPE_FACEU) {
            int n_ret = 0;
            try {
                //                n_ret = effectManager.parseLocalConfig(id); //当FaceU资源在SD卡上时
                n_ret = effectManager.parseAssetsConfig(VideoCloudApplication.getInstance(), id); //当faceU资源在assets目录下
            } catch (Throwable e) {
                n_ret = -1;
            }
            if (n_ret != 0) {
                effectManager.clear();
            }
        }
        //        else if (DrawEff2.TypeOfRes(id) == DrawEff2.TYPE_MASK) //mask
        //        {
        //            int n_ret = mask_manager.ParseConfig(BaseApplication.getContext(), id);
        //            if (n_ret != 0)
        //            {
        //                mask_manager.clear();
        //            }
        //            else
        //            {
        //
        //            }
        //        }
    }

    private void drawFaceU() {
        if (m_draw_eff != null && m_draw_eff.GetUseEff()) {
            PointF[] points = null;
            PointF[] points_dup = null;
            int face_det_width = 0;
            int face_det_height = 0;
            //            synchronized (m_draw_eff.stack_lock) {
            if (m_draw_eff.stack_faces.size() > 0) {
                points = m_draw_eff.stack_faces.peek();
                points_dup = new PointF[points.length];
                for (int i = 0; i < points.length; ++i) {
                    points_dup[i] = new PointF();
                    points_dup[i].x = points[i].x;
                    points_dup[i].y = points[i].y;
                }
                face_det_width = mConfig.m_render_width / mConfig.m_render_scale;
                face_det_height = mConfig.m_render_heigth / mConfig.m_render_scale;
            }
            //            }
            if (points_dup != null && points_dup.length > 0) {
                if (mTextureProgram == null) {
                    mTextureProgram = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D);
                }
                //                if (m_draw_eff.GetUseEffMask())
                //                {
                //
                //                    m_draw_eff.drawEffectMask(points_dup, face_det_width, face_det_height,
                //                            m_config.m_render_width, m_config.m_render_heigth,
                //                            CameraSurfaceRenderer.mDisplayProjectionMatrix,
                //                            (float)mConfig.m_render_scale, (float)mConfig.m_render_scale,
                //                            mask_manager, mTextureProgram, mRect, true, m_b_mirro, mConfig.m_b_useqhface);
                //                }
                //                else
                //                {
                m_draw_eff.drawEffect(points_dup, face_det_width, face_det_height,
                        mConfig.m_render_width, mConfig.m_render_heigth,
                        CameraSurfaceRenderer.mDisplayProjectionMatrix,
                        (float) mConfig.m_render_scale, (float) mConfig.m_render_scale,
                        effectManager, mTextureProgram, mRect, true, m_b_mirro, mConfig.m_b_useqhface);
                //                }
            }
        }
    }
    /////For faceu end

    private long lastSaveBitmapTime = System.currentTimeMillis();

    private void createAndSaveBitmap(int width, int height, ByteBuffer buffer) {
        final int SAVE_BITMAP_INTERVAL = 500; //单位：ms
        if (System.currentTimeMillis() - lastSaveBitmapTime > SAVE_BITMAP_INTERVAL) {
            lastSaveBitmapTime = System.currentTimeMillis();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);

            //Utils.saveBitmapToSdcardSync(bitmap);
            bitmap.recycle();
        }
    }

    List<Long> list = new ArrayList<Long>();

    @SuppressLint("UseValueOf")
    private void printFPS() {
        list.add(new Long(System.currentTimeMillis()));
        if (list.size() > 1) {
            long first = list.get(0).longValue();
            long last = list.get(list.size() - 1).longValue();
            while (last - first > 1000) {
                list.remove(0);
                first = list.get(0).longValue();
            }

            if (PRINT_FPS) {
                //        		XLog.debug(LiveConstant.TAG, "ian, encode fps: %d", list.size());
            }
        }
    }

    /**
     * Handles a request to stop encoding.
     */
    private void handleStopRecording() {
        //mVideoEncoder.drainEncoder(true);  //ian mark
        releaseEncoder();
    }

    /**
     * Sets the texture name that SurfaceTexture will use when frames are received.
     */
    private void handleSetTexture(int id) {
        //Log.d(TAG, "handleSetTexture " + id);
        mTextureId = id;
    }

    private void quit() {
        Looper.myLooper().quit();
        if (mHandler != null) {
            mHandler.release();
        }
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ": TextureMovieEncoder quit()....");
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     * <p>
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        Log.d(TAG, "handleUpdatedSharedContext " + newSharedContext);

        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        mFullScreen.release(false);
        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);

        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        // Create new programs and such for the new context.
        mFullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void prepareEncoder(InteractRecorderController controller, EGLContext sharedContext) {
        mCameraController = controller;
        //    	mLivingCamera = new WKTVLivingCamera(activity);
        //        mCameraActivity.setWKTVLivingCamera(mLivingCamera);

        try {
            //int width, int height, int bitrate, int fps, int iFrameInterval, File outputFile
            mVideoEncoder = new VideoEncoderCore(mConfig.mWidth, mConfig.mHeight, mCameraController.getBitrate(), mCameraController.getFps(),
                    mCameraController.getIFrameInterval());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        if (InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_GPU_READPIXELS) {
            mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
            mInputWindowSurface.makeCurrent();

            mFullScreen = new FullFrameRect(
                    new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        } else if (InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_GPU) {
            VideoSourceListener mYuvListener = mCameraController.getVideoListener();
            if (mYuvListener != null) {
                SurfaceTexture surfaceTexture = mYuvListener.getSurfaceTexture(mConfig.mEncodeWidth, mConfig.mEncodeHeight);
                if (surfaceTexture != null) {
                    long time = surfaceTexture.getTimestamp();
                    surfaceTexture.setDefaultBufferSize(mConfig.mEncodeWidth, mConfig.mEncodeHeight);
                    mInputWindowSurface = new WindowSurface(mEglCore, surfaceTexture);
                    mInputWindowSurface.makeCurrent();

                    mFullScreen = new FullFrameRect(
                            new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void releaseEncoder() {
        mVideoEncoder.release();
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release(false);
            mFullScreen = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }

        if (m_gpubeauty != null) {
            m_gpubeauty.onDestroy();
            m_gpubeauty = null;
        }
    }

    /**
     * Draws a box, with position offset.
     */
    private void drawBox(int posn) {
        final int width = mInputWindowSurface.getWidth();
        int xpos = (posn * 4) % (width - 50);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(xpos, 0, 100, 100);
        GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }
}
