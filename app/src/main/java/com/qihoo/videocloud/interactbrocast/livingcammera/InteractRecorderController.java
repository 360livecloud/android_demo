
package com.qihoo.videocloud.interactbrocast.livingcammera;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.view.TextureView;
import android.view.View;

import com.huajiao.jni.LibYuv;
import com.qihoo.faceapi.util.QhFaceInfo;
import com.qihoo.livecloud.interact.api.QHVCInteractiveConstant;
import com.qihoo.livecloud.interact.api.QHVCInteractiveLocalVideoRenderCallback;
import com.qihoo.livecloud.tools.Constants;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.videocloud.interactbrocast.faceu.DrawEff2;
import com.qihoo.videocloud.interactbrocast.main.InteractConstant;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by liuyanqing on 2016/11/17.
 */

public class InteractRecorderController implements SurfaceTexture.OnFrameAvailableListener, TextureView.SurfaceTextureListener, Camera.PreviewCallback {

    private Activity mActivity;

    private TextureView mTextureView;

    private GLSurfaceView mGLView;
    private CameraSurfaceRenderer mRenderer;
    private Camera mCamera;
    private CameraType mCurrCameraType;
    private CameraHandler mCameraHandler;

    private SurfaceTexture mCurrentSurfaceTexture;
    private TextureMovieEncoder mVideoEncoder;

    private int m_camera_preview_width = 720;
    private int m_camera_preview_height = 1280;
    private int rotation = 0; //摄像头旋转角度

    private int mCameraPreviewWidth, mCameraPreviewHeight;

    private static final int FRAME_BUFFER_NUM = 4;
    private LinkedBlockingQueue<byte[]> mDataQueue;// 正在处理的队列
    private ExecutorService mVideoEncodeExecutor;
    private boolean isEncoding = false;

    private int mBitrate; // 码流
    private int mFps; //fps
    private int mIframeInterval; //关键帧间隔

    private int mCurrOrientation = Constants.EMode.EMODE_PORTRAIT; //当前屏幕方向

    private int mVideoWidth = 360;
    private int mVideoHeight = 640;

    private int mScreenWidth;
    private int mScreenHeight;

    private boolean mIsFirstOpen;

    final public Object paint_lock = new Object(); //绘制锁

    //Faceu相关
    DrawEff2 m_draw_eff = new DrawEff2();
    boolean m_b_use_qhfacedetected = true;
    DrawEff2.QhTracker m_qh_tracker = null;
    long n_frame_ctrl = 0;
    AtomicInteger m_thread_exec_counter = new AtomicInteger(0);
    byte[] data_inner;
    ArrayBlockingQueue<byte[]> m_yuv_buff = new ArrayBlockingQueue<>(1);
    ExecutorService face_dectect_thread_exec = Executors.newSingleThreadExecutor();
    byte[] data_rotate_scale = null;

    private int m_n_face_detected_scale = 8;
    private int m_n_watch_scale = 2;

    /////////////////////////////////

    private VideoSourceListener mVideoListener;

    private QHVCInteractiveLocalVideoRenderCallback mLocalVideoRenderCallback; //为狼人杀，本地视频吐给业务

    public InteractRecorderController(Activity activity) {
        this.mActivity = activity;
    }

    public Activity getContext() {
        return mActivity;
    }

    public void initGLSurfaceView(View view) {
        if (view instanceof GLSurfaceView) {
            this.mGLView = (GLSurfaceView) view;
            mCameraHandler = new CameraHandler(this);
            mVideoEncoder = new TextureMovieEncoder();
            mRenderer = new CameraSurfaceRenderer(mCameraHandler, mVideoEncoder, this, InteractConstant.CURR_VIDEO_CAPTURE);
            mGLView.setRenderer(mRenderer);
            mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            //add for FaceU
            mRenderer.setDrawEff(m_draw_eff);
            mRenderer.setUseQhface(m_b_use_qhfacedetected);

            mVideoEncoder.setDraEff(m_draw_eff);
        } else {
            mTextureView = (TextureView) view;
            mTextureView.setSurfaceTextureListener(this);
        }

        mCurrCameraType = CameraType.FRONT;
        mIsFirstOpen = true;
    }

    public void setScreenWH(int w, int h) {
        mScreenWidth = w;
        mScreenHeight = h;
        setRecordParameters();
    }

    public void setVideoSize(int videoWidth, int videoHeight) {
        this.mVideoWidth = videoWidth;
        this.mVideoHeight = videoHeight;

        if (mGLView != null) {
            mGLView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    mRenderer.setCameraPreviewSize(m_camera_preview_width, m_camera_preview_height);
                    mRenderer.setVideoSize(mVideoWidth, mVideoHeight);

                }
            });
        }
    }

    public void setOrientation(int orientation) {
        this.mCurrOrientation = orientation;
    }

    public void setVideoSourceListener(VideoSourceListener yuvListener) {
        this.mVideoListener = yuvListener;
    }

    public VideoSourceListener getVideoListener() {
        return this.mVideoListener;
    }

    public void setLocalVideoRenderCallback(QHVCInteractiveLocalVideoRenderCallback videoRenderCallback) {
        mLocalVideoRenderCallback = videoRenderCallback;
    }

    public QHVCInteractiveLocalVideoRenderCallback getLocalVideoRenderCallback() {
        return this.mLocalVideoRenderCallback;
    }

    public void resumeCamera() {
        try {
            openCamera(); // updates mCameraPreviewWidth/Height

            if (mIsFirstOpen) {
                mIsFirstOpen = false;
                if (mGLView != null) {
                    mGLView.onResume();
                    mGLView.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            Logger.d(InteractConstant.TAG,
                                    InteractConstant.TAG + ": mCameraPreviewWidth: " + mCameraPreviewWidth + ", mCameraPreviewHeight: " + mCameraPreviewHeight);
                            if (InteractConstant.USE_HUAJIAO_DRAW) {
                                mRenderer.setCameraPreviewSize(m_camera_preview_width, m_camera_preview_height);
                                mRenderer.setVideoSize(mVideoWidth, mVideoHeight);
                            } else {
                                //                            mRenderer.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);
                                mRenderer.setCameraPreviewSize(m_camera_preview_width, m_camera_preview_height);
                                mRenderer.setVideoSize(mVideoWidth, mVideoHeight);
                            }
                        }
                    });
                }

                //setFaceU();

            } else {
                handleTextureAndStartPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String faceu_id = "30004_1";

    public void setFaceU(boolean showFaceU) {
        if (showFaceU) {
            if (Logger.LOG_ENABLE) {
                Logger.d(InteractConstant.TAG, InteractConstant.TAG + ", setFaceu id:  " + faceu_id);
            }
            if (mGLView != null) {
                mGLView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mRenderer.ChangeFaceUID(faceu_id);
                    }
                });
            }
            m_draw_eff.SetUseEff(showFaceU);

            if (mVideoEncoder != null) {
                mVideoEncoder.updateFaceUID(faceu_id);
            }
        } else {
            m_draw_eff.SetUseEff(false);
        }
    }

    public void setMeiyanIndex(float index) {
        if (mRenderer != null) {
            mRenderer.setMeiyanIndex(index);
        }
    }

    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     * <p>
     * Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the preview.
     */
    private void openCamera() {

        releaseCamera();

        if (Util.isFroyo()) {
            mCamera = Camera.open();
        } else {
            mCamera = Camera.open(mCurrCameraType.getCameraId());
        }

        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();

        //CameraUtils.choosePreviewSize(parms, mCameraDesiredWidth, mCameraDesiredHeight);
        CameraUtils.setCameraPreviewSize(parms);

        if (mCurrCameraType == CameraType.BACK) {
            setFocusMode(parms, Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else {
            setFocusMode(parms, Camera.Parameters.FOCUS_MODE_AUTO);
        }

        if (Util.is2016Nexus6P() && mCurrCameraType == CameraType.FRONT) {
            setCameraDisplayOrientation(Util.addDegreesToRotation(Util.getDisplayRotationValue(mActivity), 180));
        } else {
            rotation = Util.getDisplayRotationValue(mActivity);
            setCameraDisplayOrientation(rotation);
            //Logger.d(InteractConstant.TAG, InteractConstant.TAG + ": rotation: " + rotation);
        }

        if (mCurrCameraType == CameraType.FRONT && mCurrOrientation == Constants.EMode.EMODE_PORTRAIT) {
            mCodeOrientation = (rotation + 180) % 360;
        } else {
            mCodeOrientation = rotation;
        }

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.

        parms.setPreviewSize(m_camera_preview_height, m_camera_preview_width);
        parms.setPreviewFormat(ImageFormat.NV21);
        //先注掉setRecordingHint， 打开此函数时，会造成部分手机拉升变形，发现的手机有(OPPO A33, OPPO A37, MI 2, LG部分手机，华为荣耀部分手机)。
        //parms.setRecordingHint(true);

        // leave the frame rate set to default
        mCamera.setParameters(parms);

        int[] fpsRange = new int[2];
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        parms.getPreviewFpsRange(fpsRange);
        String previewFacts = mCameraPreviewSize.width + "x" + mCameraPreviewSize.height;
        if (fpsRange[0] == fpsRange[1]) {
            previewFacts += " @" + (fpsRange[0] / 1000.0) + "fps";
        } else {
            previewFacts += " @[" + (fpsRange[0] / 1000.0) + " - " + (fpsRange[1] / 1000.0) + "] fps";
        }

        if (InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_ONPREVIEWFRAME) {
            if (mDataQueue != null) {
                mDataQueue.clear();
            } else {
                mDataQueue = new LinkedBlockingQueue<byte[]>(FRAME_BUFFER_NUM);
            }
            for (int i = 0; i < FRAME_BUFFER_NUM; i++) {
                byte[] data = new byte[mCameraPreviewSize.width * mCameraPreviewSize.height * 3 / 2];
                mCamera.addCallbackBuffer(data);
            }
        }
        mCameraPreviewWidth = mCameraPreviewSize.width;
        mCameraPreviewHeight = mCameraPreviewSize.height;
    }

    public int getFps() {
        return this.mFps;
    }

    public int getBitrate() {
        return this.mBitrate;
    }

    public int getIFrameInterval() {
        return this.mIframeInterval;
    }

    private void setFocusMode(Camera.Parameters cameraParams, String mode) {
        if (cameraParams == null) {
            return;
        }

        List<String> supported = cameraParams.getSupportedFocusModes();
        if (supported.contains(mode)) {
            cameraParams.setFocusMode(mode);
        }
    }

    private void setCameraDisplayOrientation(int rotation) {
        if (mCamera != null) {
            mCamera.setDisplayOrientation(rotation);
        }
    }

    private void setRecordParameters() {
        mBitrate = 1000000;
        mFps = 20;
        mIframeInterval = 3;
        //        if (mScreenWidth > mScreenHeight) { //横屏
        //            mRecordHeight = 360;
        //            mRecordWidth = mRecordHeight * mScreenWidth / mScreenHeight;
        //        } else {
        //            mRecordWidth = 360;
        //            mRecordHeight = mRecordWidth * mScreenHeight / mScreenWidth;
        //        }
        //
        //        mRecordWidth = (mRecordWidth % 2) != 0 ? (mRecordWidth + 1) : mRecordWidth;
        //        mRecordHeight = (mRecordHeight % 2) != 0 ? (mRecordHeight + 1) : mRecordHeight;
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    public void releaseCamera() {
        unSetPreViewCallback();
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            try {
                mCamera.release();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            mCamera = null;
        }

        if (mVideoEncodeExecutor != null) {
            mVideoEncodeExecutor.shutdown();
            try {
                mVideoEncodeExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mVideoEncodeExecutor = null;
        }

        if (mDataQueue != null) {
            mDataQueue.clear();
            //mDataQueue = null;
        }
    }

    private AtomicBoolean captch = new AtomicBoolean(false);

    public void switchCamera() {
        //TODO test
        /////////
        // captch.set(true);
        /////////////
        // FaceUTest faceUTest = new FaceUTest();

        ////change faceu test
        //changeFaceuId();

        if (mCurrCameraType == CameraType.FRONT) {
            mCurrCameraType = CameraType.BACK;
        } else {
            mCurrCameraType = CameraType.FRONT;
        }

        openCamera();

        handleTextureAndStartPreview();

        boolean needMirro = (mCurrCameraType == CameraType.FRONT);
        if (mVideoEncoder != null) {
            mVideoEncoder.setIsNeedMirro(needMirro);
        }
        if (mRenderer != null) {
            mRenderer.setVideoNeedMirro(needMirro);
        }
    }

    private int index = 0;

    private void changeFaceuId() {
        String[] allRes = {
                "300001_1", "30002_1", "30009_1", "30012_1", "30014_1", "31035_1", "80001_1", "90014_5", "90017_4", "800248_1",
                "800281_1", "800288_1", "800290_1", "800290_1", "800291_1", "800324_1", "800341_1", "800410_1", "800427_1", "800465_1", "800484_1"
        };
        index++;
        if (index >= allRes.length) {
            index = 0;
        }
        faceu_id = allRes[index];
        setFaceU(true);
    }

    public void destroy() {
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ": do destroy() in HostInRecorderController..");
        isEncoding = false;
        releaseCamera();
        if (mGLView != null) {
            mGLView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    // Tell the renderer that it's about to be paused so it can clean up.
                    mRenderer.notifyPausing();
                }
            });
        }
        if (mRenderer != null) {
            mRenderer.setEnabledStart(false);
        }
        if (mCameraHandler != null) {
            mCameraHandler.invalidateHandler(); // paranoia
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.stopRecording();
            mVideoEncoder = null;
        }
        if (mTextureView != null) {
            mTextureView.setSurfaceTextureListener(null);
            mTextureView = null;
        }
    }

    private void handleTextureAndStartPreview() {
        if (mCamera == null) {
            return;
        }
        if (mCurrentSurfaceTexture != null) {
            if (InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_GPU ||
                    InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_GPU_READPIXELS) {
                mCurrentSurfaceTexture.setOnFrameAvailableListener(this);
                setPreViewCallbackWithBuffs();
            }
            try {
                mCamera.setPreviewTexture(mCurrentSurfaceTexture);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        if (InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_ONPREVIEWFRAME) {
            try {
                mCamera.setPreviewCallbackWithBuffer(this);
                if (mVideoEncodeExecutor == null) {
                    //预览时判断，如果是空的则处理
                    mVideoEncodeExecutor = Executors.newSingleThreadExecutor();
                }
            } catch (Throwable e) {
                mCamera.release();
                mCamera = null;
                e.printStackTrace();
            }
        }

        mCamera.startPreview();
    }

    /**
     * Connects the SurfaceTexture to the Camera preview output, and starts the preview.
     */
    public void handleSetSurfaceTexture(SurfaceTexture st) {
        mCurrentSurfaceTexture = st;
        resumeCamera();
        handleTextureAndStartPreview();
    }

    public void startCameraRecorder() {
        isEncoding = true;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //handleTextureAndStartPreview();
                if (mGLView != null) {
                    mGLView.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            // notify the renderer that we want to change the encoder's state
                            // mRenderer.changeRecordingState(mRecordingEnabled);

                            mRenderer.setEnabledStart(true); //TODO
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (mGLView != null) {
            mGLView.requestRender();
        }
    }

    private byte[] targetData = null;
    public int videoWidth = 720;//视频尺寸
    public int videoHeight = 1280;//视频尺寸
    private int mCodeOrientation = 0;//编码角度

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (data == null || mCamera == null)
            return;

        if (InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_GPU ||
                InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_GPU_READPIXELS) {
            catchPoints(data);
            //            catchPoints2(data);
            return;
        }

        onPreviewFrameCapture(data);
    }

    private void onPreviewFrameCapture(byte[] data) {
        if (isEncoding && mDataQueue != null) {
            if (mDataQueue.size() < FRAME_BUFFER_NUM - 1) {
                mDataQueue.offer(data);//压入
                if (mVideoEncodeExecutor != null && !mVideoEncodeExecutor.isShutdown()) {
                    mVideoEncodeExecutor.execute(new Runnable() {
                        public void run() {
                            if (isEncoding) {
                                byte[] dataTmp = mDataQueue.poll();// 取出缓存

                                if (targetData == null || targetData.length != videoWidth * videoHeight * 3 / 2) {
                                    targetData = new byte[videoWidth * videoHeight * 3 / 2];
                                }
                                //
                                LibYuv.nv21ScaleRotationI420(dataTmp, mCameraPreviewWidth, mCameraPreviewHeight, targetData, videoWidth, videoHeight, mCodeOrientation);
                                if (mVideoListener != null) {
                                    int rotation = 0;
                                    mVideoListener.onFrameAvailable(targetData, videoWidth, videoHeight, rotation, System.currentTimeMillis(),
                                            QHVCInteractiveConstant.VideoFormat.I420);
                                }

                                //                                if (mVideoListener != null) {
                                //                                    int rotation = mCodeOrientation;
                                //                                    mVideoListener.onFrameAvailable(dataTmp, mCameraPreviewWidth, mCameraPreviewHeight, rotation, System.currentTimeMillis(), QHLiveCloudConstant.VideoFormat.NV21);
                                //                                }

                                if (mCamera != null) {
                                    //TODO 尝试修复华为 PE-TL10手机偶现的Crash
                                    if (dataTmp != null) {
                                        mCamera.addCallbackBuffer(dataTmp);
                                    }
                                }
                            }
                        }
                    });
                }
            } else {
                mCamera.addCallbackBuffer(data);
            }
        } else {
            mCamera.addCallbackBuffer(data);
        }
    }

    private void setPreViewCallbackWithBuffs() {
        if (mCamera == null) {
            return;
        }
        mCamera.setPreviewCallback(this);
    }

    private void unSetPreViewCallback() {
        if (mCamera == null)
            return;
        mCamera.setPreviewCallback(null);
        mCamera.setPreviewCallbackWithBuffer(null);
        //isFaceFind.set(false);//如果停止预览，当然人脸识别失败了
    }

    private void catchPoints(final byte[] data) {
        if (!m_draw_eff.GetUseEff()) {
            return;
        }
        if (m_b_use_qhfacedetected && m_qh_tracker == null) {
            m_qh_tracker = m_draw_eff.GetQhTracker();
        }

        if (++n_frame_ctrl % 1 == 0) {
            int n_counter = m_thread_exec_counter.get();

            if (data_inner == null) {
                data_inner = new byte[data.length];
            }
            // final byte[] data_inner = new byte[data.length];
            System.arraycopy(data, 0, data_inner, 0, data.length);

            if (!m_yuv_buff.offer(data_inner)) {
                m_yuv_buff.clear();
                m_yuv_buff.offer(data_inner);
            }

            n_counter = m_thread_exec_counter.incrementAndGet();

            Logger.e("tread_exec_count", "" + n_counter);
            face_dectect_thread_exec.execute(new Runnable() {
                @Override
                public void run() {
                    byte[] tmp_inner = null;
                    try {
                        tmp_inner = m_yuv_buff.poll(0, TimeUnit.MILLISECONDS);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    m_thread_exec_counter.decrementAndGet();
                    if (tmp_inner == null) {
                        return;
                    }

                    if (data_rotate_scale == null) {
                        data_rotate_scale = new byte[(m_camera_preview_width / m_n_face_detected_scale * m_camera_preview_height / m_n_face_detected_scale) * 3 / 2];
                    }
                    long time_beg = System.currentTimeMillis();
                    int rotate = mCodeOrientation;
                    int n_mirro = 0;
                    // 如果是前置摄像头，需要将预览图反转180度
                    if (mCurrCameraType == CameraType.FRONT) {
                        n_mirro = 1;
                    }

                    LibYuv.turnAndRotation(tmp_inner, m_camera_preview_height,
                            m_camera_preview_width, data_rotate_scale,
                            m_camera_preview_width / m_n_face_detected_scale,
                            m_camera_preview_height / m_n_face_detected_scale,
                            rotate, 0, 1);

                    //                    if (data_rotate_scale != null) {
                    //                        for (int i = 0; i < data_rotate_scale.length; i++) {
                    //                            Logger.d(Logger.TAG, "data_rotate_scale: " + i + ":" + data_rotate_scale[i]);
                    //                        }
                    //                    }

                    saveData(data_rotate_scale);

                    if (m_b_use_qhfacedetected) {
                        Logger.e("qhface_det", "trans time is: " + (System.currentTimeMillis() - time_beg));
                        time_beg = System.currentTimeMillis();
                        QhFaceInfo face = m_qh_tracker.DetectedFace(
                                data_rotate_scale, m_camera_preview_width / m_n_face_detected_scale,
                                m_camera_preview_height / m_n_face_detected_scale);
                        Logger.e("qhface_det", "faceu time is: " + (System.currentTimeMillis() - time_beg));
                        //drawFacePoint(face);

                        //                        synchronized (m_draw_eff.stack_lock) {
                        if (face == null) {
                            m_draw_eff.stack_faces.clear();

                            //								if (m_b_is_faceugift) {
                            //									// 构造假数据
                            //									PointF[] points_fake = new PointF[95];
                            //									for (int i = 0; i < 95; ++i) {
                            //										points_fake[i] = new PointF();
                            //										if (i == 39) {
                            //											points_fake[i].x = 56.663044f;
                            //											points_fake[i].y = 74.875465f;
                            //										} else if (i == 45) {
                            //											points_fake[i].x = 49.538357f;
                            //											points_fake[i].y = 74.72132f;
                            //										} else if (i == 51) {
                            //											points_fake[i].x = 38.892017f;
                            //											points_fake[i].y = 74.61875f;
                            //										} else if (i == 57) {
                            //											points_fake[i].x = 31.77602f;
                            //											points_fake[i].y = 74.484604f;
                            //										} else if (i == 66) {
                            //											points_fake[i].x = 49.886257f;
                            //											points_fake[i].y = 84.35812f;
                            //										} else if (i == 71) {
                            //											points_fake[i].x = 38.576107f;
                            //											points_fake[i].y = 84.14751f;
                            //										} else if (i == 78) {
                            //											points_fake[i].x = 44.18642f;
                            //											points_fake[i].y = 91.90639f;
                            //										}
                            //									}
                            //									m_draw_eff.stack_faces.push(points_fake);
                            //								}
                        } else {
                            if (m_draw_eff.stack_faces.size() > 10) {
                                m_draw_eff.stack_faces.clear();
                            }
                            PointF[] points_tmp = face.getPointsArray();
                            if (n_mirro == 1) {
                                for (int i = 0; i < points_tmp.length; ++i) {
                                    points_tmp[i].x = m_camera_preview_width
                                            / m_n_face_detected_scale
                                            - points_tmp[i].x;
                                }
                            }

                            // PointF[] points = new
                            // PointF[points_tmp.length];
                            //
                            // for (int i = 0; i < points.length; ++i)
                            // {
                            // points[i] = new PointF();
                            // points[i].x = points_tmp[i].x;
                            // points[i].y = points_tmp[i].y;
                            // }
                            m_draw_eff.stack_faces.push(points_tmp);
                            Logger.e("faceu_point", "39:"
                                    + points_tmp[39].x + ","
                                    + points_tmp[39].y + " 45:"
                                    + points_tmp[45].x + ","
                                    + points_tmp[45].y + " 57:"
                                    + points_tmp[57].x + ","
                                    + points_tmp[57].y + " 51:"
                                    + points_tmp[51].x + ","
                                    + points_tmp[51].y + " 78:"
                                    + points_tmp[78].x + ","
                                    + points_tmp[78].y + " 66:"
                                    + points_tmp[66].x + ","
                                    + points_tmp[66].y + " 71:"
                                    + points_tmp[71].x + ","
                                    + points_tmp[71].y);
                        }
                        //                        }
                    }
                }
            });
        }
    }

    private void saveData(final byte[] data) {
        if (data == null) {
            return;
        }

        if (captch.get()) {
            captch.set(false);
            new Thread() {
                @Override
                public void run() {
                    FaceUTest.createFileWithByte(data);
                }
            }.start();
            Logger.d(Logger.TAG, Logger.TAG + ", saveData success!");
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        handleSetSurfaceTexture(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    /**
     * Handles camera operation requests from other threads.  Necessary because the Camera
     * must only be accessed from one thread.
     * <p>
     * The object is created on the UI thread, and all handlers run there.  Messages are
     * sent from other threads, using sendMessage().
     */
    public static class CameraHandler extends Handler {
        public static final int MSG_SET_SURFACE_TEXTURE = 0;

        // Weak reference to the Activity; only access this from the UI thread.
        private WeakReference<InteractRecorderController> mWeakController;

        public CameraHandler(InteractRecorderController controller) {
            mWeakController = new WeakReference<InteractRecorderController>(controller);
        }

        /**
         * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
         * attempts to access a stale Activity through a handler are caught.
         */
        public void invalidateHandler() {
            mWeakController.clear();
        }

        @Override // runs on UI thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Logger.d(InteractConstant.TAG, "CameraHandler [" + this + "]: what=" + what);

            InteractRecorderController controller = mWeakController.get();
            if (controller == null) {
                Logger.w(InteractConstant.TAG, "CameraHandler.handleMessage: activity is null");
                return;
            }

            switch (what) {
                case MSG_SET_SURFACE_TEXTURE:
                    controller.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }
    }

}
