
package com.qihoo.videocloud.interactbrocast.livingcammera;

import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.qihoo.livecloud.hc.HCGPUImageFilterQhSoften;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.makeup.gpu.QhMakeUpApi;
import com.qihoo.videocloud.VideoCloudApplication;
import com.qihoo.videocloud.interactbrocast.faceu.DrawEff2;
import com.qihoo.videocloud.interactbrocast.faceu.EffectManager;
import com.qihoo.videocloud.interactbrocast.main.InteractConstant;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.EGL14.eglGetCurrentContext;

/**
 * Created by liuyanqing on 2016/10/28.
 */

/**
 * Renderer object for our GLSurfaceView.
 * <p>
 * Do not call any methods here directly from another thread -- use the
 * GLSurfaceView#queueEvent() call.
 */
public class CameraSurfaceRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "WKCameraSurfaceRenderer";
    private static final boolean VERBOSE = true;

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;

    private InteractRecorderController.CameraHandler mCameraHandler;
    private TextureMovieEncoder mVideoEncoder;

    private FullFrameRect mFullScreen;

    private final float[] mSTMatrix = new float[16];
    private int mTextureId;

    private SurfaceTexture mSurfaceTexture;
    private int mFrameCount;

    // width/height of the incoming camera preview frames
    private boolean mIncomingSizeUpdated;
    private int mIncomingWidth;
    private int mIncomingHeight;

    private int mVideoWidth;
    private int mVideoHeight;

    private int m_surface_width = 0;
    private int m_surface_height = 0;

    private InteractRecorderController mController;

    private AtomicBoolean mEnabledStart = new AtomicBoolean(false);

    //美颜
    HCGPUImageFilterQhSoften m_gpubeauty;

    InteractConstant.VideoCapture mVideoCapture;

    EGL10 mEgl;
    EGLContext mEGLContext;

    float m_f_meiyan_index_inner = 0.0F;

    private boolean videoNeedMirro = true;

    ////// FaceU Start
    private DrawEff2 m_draw_eff;
    private boolean m_b_use_qhface = false;
    static public float[] mDisplayProjectionMatrix = new float[16];
    EffectManager effectManager = new EffectManager();

    private Drawable2d mRectDrawable;// = new Drawable2d(Drawable2d.Prefab.RECTANGLE);
    private Sprite2d mRect; //= new Sprite2d(mRectDrawable);
    Texture2dProgram mTextureProgram;
    ////// FaceU end

    /**
     * Constructs WKCameraSurfaceRenderer.
     * <p>
     * @param cameraHandler Handler for communicating with UI thread
     * @param movieEncoder video encoder object
     */
    public CameraSurfaceRenderer(InteractRecorderController.CameraHandler cameraHandler,
            TextureMovieEncoder movieEncoder, InteractRecorderController controller, InteractConstant.VideoCapture videoCapture) {
        mCameraHandler = cameraHandler;
        mVideoEncoder = movieEncoder;
        mVideoCapture = videoCapture;

        mController = controller;

        mTextureId = -1;

        mFrameCount = -1;

        mIncomingSizeUpdated = false;
        mIncomingWidth = 720;
        mIncomingHeight = 1280;
    }

    public void setDrawEff(DrawEff2 drawEff) {
        m_draw_eff = drawEff;
    }

    public void setUseQhface(boolean use_qhface) {
        this.m_b_use_qhface = use_qhface;
    }

    public void setMeiyanIndex(float index) {
        m_f_meiyan_index_inner = index;
    }

    public void setVideoNeedMirro(boolean mirro) {
        this.videoNeedMirro = mirro;
    }

    /**
     * Notifies the renderer thread that the activity is pausing.
     * <p>
     * For best results, call this *after* disabling Camera preview.
     */
    public void notifyPausing() {
        if (mSurfaceTexture != null) {
            Log.d(TAG, "renderer pausing -- releasing SurfaceTexture");
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release(false); // assume the GLSurfaceView EGL context is about
            mFullScreen = null; //  to be destroyed
        }
        mIncomingWidth = mIncomingHeight = -1;

        if (m_gpubeauty != null) {
            m_gpubeauty.onDestroy();
            m_gpubeauty = null;
        }

    }

    /**
     * Notifies the renderer that we want to stop or start recording.
     */
    public void changeRecordingState(boolean isRecording) {
        //       mRecordingEnabled = isRecording;
    }

    /**
     * Records the size of the incoming camera preview frames.
     * <p>
     * It's not clear whether this is guaranteed to execute before or after onSurfaceCreated(),
     * so we assume it could go either way.  (Fortunately they both run on the same thread,
     * so we at least know that they won't execute concurrently.)
     */
    public void setCameraPreviewSize(int width, int height) {
        Log.d(TAG, "setCameraPreviewSize");
        mIncomingWidth = width;
        mIncomingHeight = height;
        mIncomingSizeUpdated = true;

        //for FaceU
        android.opengl.Matrix.orthoM(mDisplayProjectionMatrix, 0, 0, mIncomingWidth, 0, mIncomingHeight, -1, 1);
    }

    public void setVideoSize(int video_w, int video_h) {
        this.mVideoWidth = video_w;
        this.mVideoHeight = video_h;
    }

    public void setEnabledStart(boolean enable) {
        mEnabledStart.set(enable);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + " : ian, onSurfaceCreated()...");
        m_b_need_updateegl = true;

        // We're starting up or coming back.  Either way we've got a new EGLContext that will
        // need to be shared with the video encoder, so figure out if a recording is already
        // in progress.

        //        mMFilter.init();

        // Set up the texture blitter that will be used for on-screen display.  This
        // is *not* applied to the recording, because that uses a separate shader.
        mFullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));

        mTextureId = mFullScreen.createTextureObject();

        // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
        // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
        // available messages will arrive on the main thread.
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        // Tell the UI thread to enable the camera preview.
        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(InteractRecorderController.CameraHandler.MSG_SET_SURFACE_TEXTURE, mSurfaceTexture));

        //for faceU
        if (mRectDrawable == null) {
            mRectDrawable = new Drawable2d(Drawable2d.Prefab.RECTANGLE);
        }
        if (mRect == null) {
            mRect = new Sprite2d(mRectDrawable);
        }
        if (mTextureProgram == null) {
            mTextureProgram = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D);
        }

        //ian add
        //        if (Constant.SELF_RECORDER_TYPE == Constant.RECORDER_TYPE1) {
        //            mEgl = (EGL10) EGLContext.getEGL();
        //            mEGLContext = mEgl.eglGetCurrentContext();
        //            mActivity.updateSharedContext(mEGLContext);
        //        }

        //        mActivity.joinChannel();  //ian mark
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        //        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(
        //                HostInRecorderController.CameraHandler.MSG_SET_SURFACE_TEXTURE, mSurfaceTexture));

        m_surface_width = width;
        m_surface_height = height;
        Logger.d(InteractConstant.TAG, InteractConstant.TAG + ":onSurfaceChanged(), m_surface_width : " + m_surface_width + ", m_surface_height:" + m_surface_height);
        //mMFilter.onOutputSizeChanged(width, height);
    }

    /*********** FaceU相关****************/

    public void ChangeFaceUID(String id) {
        m_draw_eff.ClearCache();
        effectManager.clear();

        if (DrawEff2.TypeOfRes(id) == DrawEff2.TYPE_FACEU) {
            int n_ret = 0;
            try {
                //                n_ret = effectManager.parseLocalConfig(id);  //当FaceU资源在SD卡上时
                n_ret = effectManager.parseAssetsConfig(VideoCloudApplication.getInstance(), id); //当faceU资源在assets目录下
            } catch (Throwable e) {
                n_ret = -1;
            }
            if (n_ret != 0) {
                effectManager.clear();
            } else {
                m_draw_eff.SetCacheNum(effectManager.GetPngTotalNum());
            }
            m_draw_eff.SetUseEffMask(false);
        }
    }

    /*********** ****************/

    private int m_scale_watch = 2; //2
    private int m_scale_dectect = 8;
    private boolean m_b_need_updateegl = false;
    long m_n_ctrl_encode_fps = 0;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onDrawFrame(GL10 unused) {
        // Latch the latest frame.  If there isn't anything new, we'll just re-use whatever
        // was there before.
        if (mSurfaceTexture != null) {
            if (mController != null) {
                synchronized (mController.paint_lock) {
                    mSurfaceTexture.updateTexImage();
                }
            }
        } else {
            return;
        }

        mVideoEncoder.setMeiyanIndex(m_f_meiyan_index_inner);
        if (!mVideoEncoder.isRecording()) {
            if (mEnabledStart.get() &&
                    (mVideoCapture == InteractConstant.VideoCapture.RECORD_GPU || mVideoCapture == InteractConstant.VideoCapture.RECORD_GPU_READPIXELS)) {
                if (InteractConstant.USE_HUAJIAO_DRAW) {
                    mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                            mController, eglGetCurrentContext(), mIncomingWidth / m_scale_watch, mIncomingHeight / m_scale_watch,
                            mIncomingWidth, mIncomingHeight, m_scale_dectect, mVideoWidth, mVideoHeight, m_b_use_qhface));
                } else {
                    mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                            mController, eglGetCurrentContext(), mIncomingWidth / m_scale_watch, mIncomingHeight / m_scale_watch,
                            mIncomingWidth, mIncomingHeight, m_scale_dectect, mVideoWidth, mVideoHeight, m_b_use_qhface));
                }
            }
            mVideoEncoder.setTextureId(mTextureId);
            mVideoEncoder.setIsNeedMirro(videoNeedMirro);
            m_b_need_updateegl = false;
        } else if (m_b_need_updateegl) {
            mVideoEncoder.updateSharedContext(eglGetCurrentContext());
            m_b_need_updateegl = false;
        } else {
            boolean b_escape_frame = false;
            if (m_n_ctrl_encode_fps == 0) {
                m_n_ctrl_encode_fps = System.currentTimeMillis();
            } else {
                if ((System.currentTimeMillis() - m_n_ctrl_encode_fps) < 40) //41
                {
                    b_escape_frame = true;
                } else {
                    m_n_ctrl_encode_fps = System.currentTimeMillis();
                }
            }

            if (!b_escape_frame) {
                mVideoEncoder.frameAvailable(mSurfaceTexture);
            } else {
                //Logger.w(InteractConstant.TAG, InteractConstant.TAG + ": escape_frame, got one");
            }
        }

        if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
            // Texture size isn't set yet.  This is only used for the filters, but to be
            // safe we can just skip drawing while we wait for the various races to resolve.
            // (This seems to happen if you toggle the screen off/on with power button.)
            Log.i(TAG, "Drawing before incoming texture size set; skipping");
            return;
        }

        if (mIncomingSizeUpdated) {
            mFullScreen.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
            mIncomingSizeUpdated = false;
        }

        // Draw the video frame.
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        //printFPS();
        if (!InteractConstant.USE_HUAJIAO_DRAW) {
            mFullScreen.drawFrame(mTextureId, mSTMatrix);
        } else {
            if (m_gpubeauty == null) {
                m_gpubeauty = new HCGPUImageFilterQhSoften(mController.getContext());
            }

            if (m_f_meiyan_index_inner >= 0.1F && m_f_meiyan_index_inner <= 1.0F) {
                m_gpubeauty.setSoftenLevel(m_f_meiyan_index_inner);
                m_gpubeauty._onDraw(mTextureId, mIncomingWidth / m_scale_dectect, mIncomingHeight / m_scale_dectect, 0, 0, 0,
                        m_surface_width, m_surface_height, mFullScreen.getRectDrawable().getVertexArray(), mFullScreen.getRectDrawable().getTexCoordArray(),
                        QhMakeUpApi.EFFECT_SOFT, null, mSTMatrix, 0.6F);
            } else {
                //            m_gpubeauty.onDraw(mTextureId,
                //                    mIncomingWidth / m_scale_dectect,
                //                    mIncomingHeight / m_scale_dectect,
                //                    m_surface_width,
                //                    m_surface_height,
                //                    mFullScreen.getRectDrawable().getVertexArray(),
                //                    mFullScreen.getRectDrawable().getTexCoordArray(),
                //                    QhMakeUpApi.EFFECT_NO,
                //                    null,
                //                    mSTMatrix,
                //                    0.6f);
                m_gpubeauty._onDraw(mTextureId,
                        mIncomingWidth / m_scale_dectect, mIncomingHeight / m_scale_dectect, 0, 0, 0,
                        m_surface_width, m_surface_height,
                        mFullScreen.getRectDrawable().getVertexArray(), mFullScreen.getRectDrawable().getTexCoordArray(), QhMakeUpApi.EFFECT_NO, null, mSTMatrix, 0.6F);
            }
        }

        drawEff();

        //        if (mController != null) {
        //            VideoSourceListener mYuvListener = mController.getVideoListener();
        //            if (mYuvListener != null) {
        //                mYuvListener.onTextureCaptured(mTextureId, m_surface_width, m_surface_height, System.currentTimeMillis());
        //            }
        //        }
    }

    private void drawEff() {
        // 绘制特效
        if (m_draw_eff.GetUseEff()) {
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
                face_det_width = mIncomingWidth / m_scale_dectect;
                face_det_height = mIncomingHeight / m_scale_dectect;
            }
            //            }
            if (points_dup != null && points_dup.length > 0) {
                //                if (m_draw_eff.GetUseEffMask())
                //                {
                //                    m_draw_eff.drawEffectMask(points_dup, face_det_width, face_det_height, mIncomingWidth, mIncomingHeight, mDisplayProjectionMatrix,
                //                            (float) m_scale_dectect, (float) m_scale_dectect, mask_manager, mTextureProgram, mRect, false, false, m_b_use_qhface);
                //                }
                //                else
                //                {
                m_draw_eff.drawEffect(points_dup, face_det_width, face_det_height, mIncomingWidth, mIncomingHeight, mDisplayProjectionMatrix,
                        (float) m_scale_dectect, (float) m_scale_dectect, effectManager, mTextureProgram, mRect, false, false, m_b_use_qhface);
                //                }

            }
        }
    }

}
