package com.qihoo.videocloud.interactbrocast.faceu;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.opengl.GLUtils;
import android.util.Log;

import com.qihoo.faceapi.QhFaceApi;
import com.qihoo.faceapi.util.QhFaceInfo;
import com.qihoo.videocloud.VideoCloudApplication;
import com.qihoo.videocloud.interactbrocast.livingcammera.Sprite2d;
import com.qihoo.videocloud.interactbrocast.livingcammera.Texture2dProgram;

import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glTexParameterf;

/**
 * Created by j-yutianzuo on 2016/4/26.
 */
final public class DrawEff2 {
    public static final int TYPE_FACEU = 1;
    public static final int TYPE_MASK = 2;
    public static final int TYPE_NARROW_FACE = 2;

    public static int TypeOfRes(String id) {
        int ret = TYPE_FACEU;
//        if (id.startsWith(ChooseFaceLayout.MASK_PREFIX))
//        {
//            ret = TYPE_MASK;
//        }
//        else if (id.startsWith(ChooseFaceLayout.NARROW_FACE_PREFIX))
//        {
//            ret = TYPE_NARROW_FACE;
//        }
        return ret;
    }

    float deltaTime_render = 0.0f;
    float deltaTime_render_encoder = 0.0f;

    boolean m_b_use_eff = false;
    boolean m_b_use_mask = false;

    //    private CvFaceMultiTrack106 tracker = null;
    public Stack<PointF[]> stack_faces = new Stack<>();
    final public Object stack_lock = new Object();

    final private Object cache_lock = new Object();
    private Map<String, Bitmap> m_bitmap_cache = new TreeMap<>();
    private int m_n_cache_num = 40;

//    public CvFaceMultiTrack106 GetTracker()
//    {
//        if (tracker == null)
//        {
//            tracker = new CvFaceMultiTrack106();
//            tracker.setMaxDetectableFaces(1);
//        }
//        return tracker;
//    }

    static public class QhTracker {
        public QhTracker() {

        }

        public void Init() {
            String path = EffectManager.getAppDir(VideoCloudApplication.getInstance()) + EffectManager.QH_FACE_MODEL_FOLDER_NAME;
            QhFaceApi.qhFaceDetectInit(path, 1);
//        	QhFaceApi.qhFaceDetectInit(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "FaceModels", 1);
//    		QhFaceApi.qhFaceDetectInit(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "FaceModels", 5);
        }

        public void Uninit() {
            QhFaceApi.qhFaceDetectDestroy();
        }

        public QhFaceInfo DetectedFace(byte[] data, int width, int height) {
            Log.d("Ian", "ian, data.length = " + data.length + " width = " + width + " height:" + height);
            QhFaceInfo faces[] = QhFaceApi.faceDetectYUV(data, width, height, -1);
//            QhFaceInfo faces[] = QhFaceApi.FaceDetectYUV(data, width, height, 1);
            if (faces != null && faces.length > 0) {
                return faces[0];
            } else {
                return null;
            }
        }
    }

    QhTracker m_qhtracker;

    public QhTracker GetQhTracker() {
        if (m_qhtracker == null) {
            m_qhtracker = new QhTracker();
            m_qhtracker.Init();
        }
        return m_qhtracker;
    }

    public void SetUseEff(boolean b) {
        m_b_use_eff = b;
        if (!m_b_use_eff) {
            ClearCache();
        }
    }

    public boolean GetUseEff() {
        return m_b_use_eff;
    }

    public void SetUseEffMask(boolean b) {
        m_b_use_mask = b;
        if (m_b_use_mask) {
            ClearCache();
        }
    }

    public boolean GetUseEffMask() {
        return m_b_use_mask;
    }

    public void ClearCache() {
        synchronized (cache_lock) {
            m_bitmap_cache.clear();
        }
    }

    public void SetCacheNum(int num) {
        synchronized (cache_lock) {
            deltaTime_render = 0.0f;
            deltaTime_render_encoder = 0.0f;
            m_n_cache_num = num;
        }
    }

    public void Uninit() {
        if (m_b_ini_facemask) {
            QhFaceApi.qhFaceMaskDestroy();
        }

        if (m_qhtracker != null) {
            m_qhtracker.Uninit();
            m_qhtracker = null;
        }
    }

    private void Mirror(PointF[] points, int width, int height) {
        for (int i = 0; i < points.length; ++i) {
            points[i].x = width - points[i].x;
        }
    }

    public void drawEffect(PointF[] points, int face_det_width, int face_det_height, int width, int height, float[] matrix,
                           float w_ratio, float h_ratio, EffectManager effectManager,
                           Texture2dProgram mTextureProgram, Sprite2d mRect, boolean is_render_encoder, boolean b_mirror, boolean b_useqh_face) {
        if (!m_b_use_eff) {
            return;
        }

        if (effectManager == null || effectManager.getTextureNum() <= 0) {
            return;
        }

        if (mTextureProgram == null || mRect == null) {
            return;
        }

//        if (is_render_encoder)
//        {
//            return;
//        }

        int LEFT_EYE = 0;
        int RIGHT_EYE = 0;
        int NOSE = 0;
        int TOP_LIP = 0;

        if (b_useqh_face) {
            LEFT_EYE = 39;
            RIGHT_EYE = 57;
            NOSE = 69;

            //修正点坐标，兼容senstime，然后直接把计算修正后的点赋值给39,57,69点。
            points[39].x = (points[39].x + points[45].x) / 2;
            points[39].y = (points[39].y + points[45].y) / 2;

            points[57].x = (points[51].x + points[57].x) / 2;
            points[57].y = (points[51].y + points[57].y) / 2;

            points[69].x = (points[66].x + points[71].x) / 2;
            points[69].y = (points[66].y + points[71].y) / 2;

            TOP_LIP = 78;
        } else {
            LEFT_EYE = 77;
            RIGHT_EYE = 74;
            NOSE = 46;
            TOP_LIP = 87;
        }


        float mid_x = 0;
        float mid_y = 0;

        float ratio_w = w_ratio;
        float ratio_h = h_ratio;

        //PointF[] points = faces[0].getPointsArray();
        if (b_mirror) {
            Mirror(points, face_det_width, face_det_height);
        }

        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        //glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Bitmap bitmap = null;
        int textureNum = effectManager.getTextureNum();
        int[] textures = new int[textureNum];
        glGenTextures(textureNum, textures, 0);

        int maxImageNum = effectManager.getTextureFrameCount(0);
        for (int i = 0; i < textureNum; i++) {
            maxImageNum = Math.max(maxImageNum, effectManager.getTextureFrameCount(i));
        }

        float eyeDistance = (float) Math.sqrt((float) ((points[RIGHT_EYE].x - points[LEFT_EYE].x) * (points[RIGHT_EYE].x - points[LEFT_EYE].x)
                + (points[RIGHT_EYE].y - points[LEFT_EYE].y) * (points[RIGHT_EYE].y - points[LEFT_EYE].y)));

//		float angle = (float) -((float) 180.0f * Math.atan((points[0].y - points[32].y) / (points[0].x - points[32].x))
//				/ Math.PI);

        float angle = (float) -((float) 180.0f * Math.atan((points[LEFT_EYE].y - points[RIGHT_EYE].y) / (points[LEFT_EYE].x - points[RIGHT_EYE].x))
                / Math.PI); //向右转角度为负，向左为正，为了后面设置opengl旋转参数


        for (int i = 0; i < textureNum; i++) {
            float radius = 0.0f;

            int midType = effectManager.getTextureMidType(i);

            float x = (float) effectManager.getTextureX(i);
            float y = (float) effectManager.getTextureY(i);
            float w = (float) effectManager.getTextureW(i);
            float h = (float) effectManager.getTextureH(i);

            switch (midType) {
                case 0:
                    // 2.1 head type
                    mid_x = ((points[RIGHT_EYE].x + points[LEFT_EYE].x) / 2.0f) * ratio_w;
                    mid_y = height - ((points[RIGHT_EYE].y + points[LEFT_EYE].y) / 2.0f) * ratio_h;
                    break;
                case 1:
                    // 2.2 nose
                    mid_x = points[NOSE].x * ratio_w;
                    mid_y = height - (points[NOSE].y) * ratio_h;
                    break;
                case 2:
                    // 2.3 fixed type
                    mid_x = (w - effectManager.getTextureMidX(i)) / 2.0f;
                    mid_y = (h - effectManager.getTextureMidY(i)) / 2.0f;
                    break;
                case 3:
                    // 2.4 mouse type
                    mid_x = points[TOP_LIP].x * ratio_w;
                    mid_y = height - points[TOP_LIP].y * ratio_h;
                    break;
                default:
                    //if(LiveCloudRecorder.bDebug)
                    return;
            }

            float scaleRatio;
            int scaleType = effectManager.getTextureScaleType(i);
            if (scaleType == 1) {
                scaleRatio = (float) effectManager.getTextureScaleRation(i);
            } else {
                scaleRatio = eyeDistance / (float) effectManager.getTextureScaleRation(i);
            }

            if (midType == 2) {
                mid_x = mid_x * scaleRatio * 2;
                mid_y = mid_y * scaleRatio * 2;
            }

            if (midType != 2) {
                if (y == 0.0f) {
                    y = 0.0000001f;
                }
                if (x == 0.0f) {
                    x = 0.0000001f;
                }
                float x_dis_from_mid = -(w / 2 + x);
                float y_dis_from_mid = -(h / 2 + y);
                if (x_dis_from_mid == 0.0f) {
                    x_dis_from_mid = 0.0000001f;
                }
                float dis_from_point = (float) Math.sqrt(x_dis_from_mid * x_dis_from_mid + y_dis_from_mid * y_dis_from_mid);
                float angle_pic_align_hor_abs = (float) ((float) 180.0f * Math.atan(Math.abs(y_dis_from_mid / x_dis_from_mid))
                        / Math.PI);

                float angle_sum;

                float cos_angle_in_radians;
                float sin_angle_in_radians;

                float mid_y_dis;
                float mid_x_dis;

                if (x_dis_from_mid > .0f && y_dis_from_mid >= .0f) //第一象限
                {
                    angle_sum = angle_pic_align_hor_abs + (angle);
                    cos_angle_in_radians = (float) Math.cos((double) Math.abs(angle_sum) * Math.PI / 180);
                    sin_angle_in_radians = (float) Math.sin((double) Math.abs(angle_sum) * Math.PI / 180);
                    mid_y_dis = Math.abs(dis_from_point * sin_angle_in_radians);
                    mid_x_dis = Math.abs(dis_from_point * cos_angle_in_radians);

                    if (angle_sum >= .0f && angle_sum <= 90.0f) {
                        mid_x = mid_x + mid_x_dis * scaleRatio * w_ratio;
                        mid_y = mid_y + mid_y_dis * scaleRatio * w_ratio;
                    } else if (angle_sum > 90.0f) {
                        mid_x = mid_x - mid_x_dis * scaleRatio * w_ratio;
                        mid_y = mid_y + mid_y_dis * scaleRatio * w_ratio;
                    } else if (angle_sum < .0f) {
                        mid_x = mid_x + mid_x_dis * scaleRatio * w_ratio;
                        mid_y = mid_y - mid_y_dis * scaleRatio * w_ratio;
                    }
                } else if (x_dis_from_mid < .0f && y_dis_from_mid >= .0f) //第二象限
                {
                    angle_sum = (180.0f - angle_pic_align_hor_abs) + (angle);
                    cos_angle_in_radians = (float) Math.cos((double) Math.abs(angle_sum) * Math.PI / 180);
                    sin_angle_in_radians = (float) Math.sin((double) Math.abs(angle_sum) * Math.PI / 180);
                    mid_y_dis = Math.abs(dis_from_point * sin_angle_in_radians);
                    mid_x_dis = Math.abs(dis_from_point * cos_angle_in_radians);

                    if (angle_sum > 180.0f) {
                        mid_x = mid_x - mid_x_dis * scaleRatio * w_ratio;
                        mid_y = mid_y - mid_y_dis * scaleRatio * w_ratio;
                    } else if (angle_sum >= 90.0f && angle_sum <= 180.0f) {
                        mid_x = mid_x - mid_x_dis * scaleRatio * w_ratio;
                        mid_y = mid_y + mid_y_dis * scaleRatio * w_ratio;
                    } else if (angle_sum < 90.0f) {
                        mid_x = mid_x + mid_x_dis * scaleRatio * w_ratio;
                        mid_y = mid_y + mid_y_dis * scaleRatio * w_ratio;
                    }
                } else if (x_dis_from_mid < .0f && y_dis_from_mid < .0f) //第三象限
                {
                    angle_sum = 180.0f + angle_pic_align_hor_abs + (angle);
                    cos_angle_in_radians = (float) Math.cos((double) Math.abs(angle_sum) * Math.PI / 180);
                    sin_angle_in_radians = (float) Math.sin((double) Math.abs(angle_sum) * Math.PI / 180);
                    mid_y_dis = Math.abs(dis_from_point * sin_angle_in_radians);
                    mid_x_dis = Math.abs(dis_from_point * cos_angle_in_radians);

                    if (angle_sum >= 180.0f && angle_sum <= 270.0f) {
                        mid_x = mid_x - mid_x_dis * scaleRatio * w_ratio;
                        mid_y = mid_y - mid_y_dis * scaleRatio * w_ratio;
                    } else if (angle_sum > 270.0f) {
                        mid_x = mid_x + mid_x_dis * scaleRatio * w_ratio;
                        mid_y = mid_y - mid_y_dis * scaleRatio * w_ratio;
                    } else if (angle_sum < 180.0f) {
                        mid_x = mid_x - mid_x_dis * scaleRatio * w_ratio;
                        mid_y = mid_y + mid_y_dis * scaleRatio * w_ratio;
                    }
                } else if (x_dis_from_mid > .0f && y_dis_from_mid < .0f) //第四象限
                {
                    angle_sum = (360.0f - angle_pic_align_hor_abs) + (angle);
                    cos_angle_in_radians = (float) Math.cos((double) Math.abs(angle_sum) * Math.PI / 180);
                    sin_angle_in_radians = (float) Math.sin((double) Math.abs(angle_sum) * Math.PI / 180);
                    mid_y_dis = Math.abs(dis_from_point * sin_angle_in_radians);
                    mid_x_dis = Math.abs(dis_from_point * cos_angle_in_radians);

                    if (angle_sum < 270.0f) {
                        mid_x = mid_x - mid_x_dis * scaleRatio * w_ratio;
                        mid_y = mid_y - mid_y_dis * scaleRatio * w_ratio;
                    } else if (angle_sum >= 270.0f && angle_sum <= 360.0f) {
                        mid_x = mid_x + mid_x_dis * scaleRatio * w_ratio;
                        mid_y = mid_y - mid_y_dis * scaleRatio * w_ratio;
                    } else if (angle_sum > 360.0f) {
                        mid_x = mid_x + mid_x_dis * scaleRatio * w_ratio;
                        mid_y = mid_y + mid_y_dis * scaleRatio * w_ratio;
                    }
                }
            }


//            if (midType != 2)
//            {
//                if (y == 0.0f)
//                {
//                    y = 0.0000001f;
//                }
//
//                float x_dis_from_mid = -(w / 2 + x);
//                float y_dis_from_mid = -(h / 2 + y);
//                if (x_dis_from_mid == 0.0f)
//                {
//                    x_dis_from_mid = 0.0000001f;
//                }
//                float dis_from_point = (float) Math.sqrt(x_dis_from_mid * x_dis_from_mid + y_dis_from_mid * y_dis_from_mid);
//                float cos_angle_in_radians = (float)Math.cos((double) Math.abs(angle) * Math.PI / 180);
//                float sin_angle_in_radians = (float)Math.sin((double) Math.abs(angle) * Math.PI / 180);
//                float mid_y_dis = Math.abs(dis_from_point * cos_angle_in_radians); //中心点旋转以前的xy距离mid点的偏移（绝对值）
//                float mid_x_dis = Math.abs(dis_from_point * sin_angle_in_radians);
//
//                float mid_x_dis_ajust = (float)Math.abs(Math.cos((double) Math.abs(angle) * Math.PI / 180)) * Math.abs(x_dis_from_mid); //
//                float mid_y_dis_ajust = (float)Math.abs(Math.sin((double) Math.abs(angle) * Math.PI / 180)) * Math.abs(x_dis_from_mid);
//
//                float angle_ajust = ((float) Math.atan(Math.abs(x_dis_from_mid) / Math.abs(y_dis_from_mid))) * 180.0f / (float) Math.PI; //中心点，修正点夹角,绝对值
//
//                if (angle >= 0.0f) //向左转
//                {
//                    if (Math.abs(angle) >= angle_ajust) //转头角度大于修正角度，即图像中心点和修正点位于同一个象限内
//                    {
//                        if (y_dis_from_mid >= .0f && x_dis_from_mid >= .0f)
//                        {
//                            mid_x = mid_x + (mid_x_dis_ajust - mid_x_dis) * scaleRatio * w_ratio;
//                            mid_y = mid_y + (mid_y_dis + mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                        else if (y_dis_from_mid >= .0f && x_dis_from_mid < .0f)
//                        {
//                            mid_x = mid_x - (mid_x_dis + mid_x_dis_ajust) * scaleRatio * w_ratio;
//                            mid_y = mid_y + (mid_y_dis - mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                        else if (y_dis_from_mid < .0f && x_dis_from_mid >= .0f)
//                        {
//                            mid_x = mid_x + (mid_x_dis + mid_x_dis_ajust) * scaleRatio * w_ratio;
//                            mid_y = mid_y + (mid_y_dis_ajust - mid_y_dis) * scaleRatio * h_ratio;
//                        }
//                        else if (y_dis_from_mid < .0f && x_dis_from_mid < .0f)
//                        {
//                            mid_x = mid_x + (mid_x_dis - mid_x_dis_ajust) * scaleRatio * w_ratio;
//                            mid_y = mid_y - (mid_y_dis + mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                    }
//                    else //转头角度小于修正角度，即图像中心点和修正点位于不同象限内
//                    {
//                        if (y_dis_from_mid >= .0f && x_dis_from_mid >= .0f)
//                        {
//                            mid_x = mid_x + (mid_x_dis_ajust - mid_x_dis) * scaleRatio * w_ratio;
//                            mid_y = mid_y + (mid_y_dis + mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                        else if (y_dis_from_mid >= .0f && x_dis_from_mid < .0f)
//                        {
//                            mid_x = mid_x - (mid_x_dis_ajust + mid_x_dis) * scaleRatio * w_ratio;
//                            mid_y = mid_y + (mid_y_dis - mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                        else if (y_dis_from_mid < .0f && x_dis_from_mid >= .0f)
//                        {
//                            mid_x = mid_x + (mid_x_dis + mid_x_dis_ajust) * scaleRatio * w_ratio;
//                            mid_y = mid_y - (mid_y_dis - mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                        else if (y_dis_from_mid < .0f && x_dis_from_mid < .0f)
//                        {
//                            mid_x = mid_x - (mid_x_dis_ajust - mid_x_dis) * scaleRatio * w_ratio;
//                            mid_y = mid_y - (mid_y_dis + mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                    }
//                }
//                else //向右转
//                {
//                    if (Math.abs(angle) >= angle_ajust) //转头角度大于修正角度，即图像中心点和修正点位于同一个象限内
//                    {
//                        if (y_dis_from_mid >= .0f && x_dis_from_mid >= .0f)
//                        {
//                            mid_x = mid_x + (mid_x_dis + mid_x_dis_ajust) * scaleRatio * w_ratio;
//                            mid_y = mid_y + (mid_y_dis - mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                        else if (y_dis_from_mid >= .0f && x_dis_from_mid < .0f)
//                        {
//                            mid_x = mid_x + (mid_x_dis - mid_x_dis_ajust) * scaleRatio * w_ratio;
//                            mid_y = mid_y + (mid_y_dis + mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                        else if (y_dis_from_mid < .0f && x_dis_from_mid >= .0f)
//                        {
//                            mid_x = mid_x - (mid_x_dis - mid_x_dis_ajust) * scaleRatio * w_ratio;
//                            mid_y = mid_y - (mid_y_dis + mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                        else if (y_dis_from_mid < .0f && x_dis_from_mid < .0f)
//                        {
//                            mid_x = mid_x - (mid_x_dis + mid_x_dis_ajust) * scaleRatio * w_ratio;
//                            mid_y = mid_y - (mid_y_dis - mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                    }
//                    else //转头角度小于修正角度，即图像中心点和修正点位于不同象限内
//                    {
//                        if (y_dis_from_mid >= .0f && x_dis_from_mid >= .0f)
//                        {
//                            mid_x = mid_x + (mid_x_dis + mid_x_dis_ajust) * scaleRatio * w_ratio;
//                            mid_y = mid_y + (mid_y_dis - mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                        else if (y_dis_from_mid >= .0f && x_dis_from_mid < .0f)
//                        {
//                            mid_x = mid_x - (mid_x_dis_ajust - mid_x_dis) * scaleRatio * w_ratio;
//                            mid_y = mid_y + (mid_y_dis + mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                        else if (y_dis_from_mid < .0f && x_dis_from_mid >= .0f)
//                        {
//                            mid_x = mid_x - (mid_x_dis - mid_x_dis_ajust) * scaleRatio * w_ratio;
//                            mid_y = mid_y - (mid_y_dis + mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                        else if (y_dis_from_mid < .0f && x_dis_from_mid < .0f)
//                        {
//                            mid_x = mid_x - (mid_x_dis + mid_x_dis_ajust) * scaleRatio * w_ratio;
//                            mid_y = mid_y - (mid_y_dis - mid_y_dis_ajust) * scaleRatio * h_ratio;
//                        }
//                    }
//                }
//            }

            int radiusType = effectManager.getTextureRadiusType(i);

            if (radiusType == 1) {
                radius = (float) effectManager.getTextureRadius(i);
            } else {
                radius = angle;
            }

            if (is_render_encoder) {
                if (deltaTime_render_encoder >= maxImageNum) {
                    deltaTime_render_encoder = 0;
                }
            } else {
                if (deltaTime_render >= maxImageNum) {
                    deltaTime_render = 0;
                }
            }


            int chooseNum = 0;
            if (is_render_encoder) {
                chooseNum = (int) ((deltaTime_render_encoder >= (float) effectManager.getTextureFrameCount(i))
                        ? (effectManager.getTextureFrameCount(i) - 1) : deltaTime_render_encoder);
            } else {
                chooseNum = (int) ((deltaTime_render >= (float) effectManager.getTextureFrameCount(i))
                        ? (effectManager.getTextureFrameCount(i) - 1) : deltaTime_render);
            }

            long l1 = System.currentTimeMillis();
            String str_png_name = effectManager.GetPngName(i, chooseNum);
            synchronized (cache_lock) {
                if (m_bitmap_cache.containsKey(str_png_name)) {
                    bitmap = m_bitmap_cache.get(str_png_name);
                    //LivingLog.e("cachetest", "got cache " + str_png_name + "  in " + (is_render_encoder ? "encode render thread" : "render thread") + " size is " + m_bitmap_cache.size());
                } else {
                    bitmap = effectManager.GetBitmap(i, chooseNum);
                    if (bitmap != null) {
                        if (m_bitmap_cache.size() > m_n_cache_num) {
                            m_bitmap_cache.clear();
                        }
                        m_bitmap_cache.put(str_png_name, bitmap);
                        //LivingLog.e("cachetest", "put cache " + str_png_name + "  in " + (is_render_encoder ? "encode render thread" : "render thread") + " size is " + m_bitmap_cache.size());
                    } else {
                        m_bitmap_cache.clear();
                    }
                }
            }

            Log.e("facetime", "eff decode bitmap time:" + (System.currentTimeMillis() - l1));
            l1 = System.currentTimeMillis();


            glBindTexture(GL_TEXTURE_2D, textures[i]);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            if (bitmap != null) {
                GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
                if (scaleType == 1) {
                    mRect.setScale(w * scaleRatio * 2, h * scaleRatio * 2);
                } else {
                    mRect.setScale(w * scaleRatio * w_ratio, h * scaleRatio * h_ratio);
                }
                mRect.setPosition(mid_x, mid_y);
                mRect.setTexture(textures[i]);
                mRect.setRotation(radius);
                // Matrix.orthoM(mDisplayProjectionMatrix, 0, 0, mWidth, 0, mHeight,
                // -1, 1);
                glGetError();
                mRect.draw(mTextureProgram, matrix);
            }
            Log.e("facetime", "eff draw bitmap time:" + (System.currentTimeMillis() - l1));
            //bitmap.recycle();
        }
        if (is_render_encoder) {
            deltaTime_render_encoder += 1.0f;
        } else {
            deltaTime_render += 1.0f;
        }

        glDeleteTextures(textureNum, textures, 0);
        glDisable(GL_BLEND);
    }


    Bitmap m_bm_render;
    Bitmap m_bm_render_recoder;
    boolean m_b_ini_facemask = false;
/*    synchronized public void drawEffectMask(PointF[] points, int face_det_width, int face_det_height, int width, int height, float[] matrix,
                               float w_ratio, float h_ratio, MaskManager mask_manager, Texture2dProgram mTextureProgram,
                               Sprite2d mRect, boolean is_render_encoder, boolean b_mirror, boolean b_useqhface)
    {
        if (!m_b_use_eff)
        {
            return;
        }

        if (mTextureProgram == null || mRect == null)
        {
            return;
        }

        String str_maskpng_path = mask_manager.GetPngPath();
        if (mask_manager == null || TextUtils.isEmpty(str_maskpng_path)
                || !new File(str_maskpng_path).isFile())
        {
            return;
        }


        float mid_x = width / 2;
        float mid_y = height / 2;

        float ratio_w = w_ratio;
        float ratio_h = h_ratio;

        if (b_mirror)
        {
            Mirror(points, face_det_width, face_det_height);
        }

        int n_scale_points = 1;
        int n_height = face_det_height;
        while (n_height < 640)
        {
            n_scale_points *= 2;
            n_height = face_det_height * n_scale_points;
        }
        for (int i = 0; i < points.length; ++i)
        {
            points[i].x *= n_scale_points;
            points[i].y *= n_scale_points;
        }

        if (!m_b_ini_facemask)
        {
            m_b_ini_facemask = true;
            QhFaceApi.qhFaceMaskInit(face_det_width * n_scale_points, face_det_height * n_scale_points, b_useqhface);
        }

        glEnable(GL_BLEND);
        //glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);


        int[] textures = new int[1];
        glGenTextures(1, textures, 0);


        glBindTexture(GL_TEXTURE_2D, textures[0]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);


        long beg = System.currentTimeMillis();
        Bitmap bitmap = null;
        if (is_render_encoder)
        {
            if (m_bm_render_recoder == null)
            {
                m_bm_render_recoder = Bitmap.createBitmap(face_det_width * n_scale_points, face_det_height * n_scale_points, Bitmap.Config.ARGB_8888);
            }
            //synchronized (cache_lock)
            {
                bitmap = QhFaceApi.qhGetFaceMaskBitmap(mask_manager.GetPngPath(), mask_manager.GetPointPath(b_useqhface), points, face_det_width * n_scale_points, face_det_height * n_scale_points, m_bm_render_recoder);
            }
        }
        else
        {
            if (m_bm_render == null)
            {
                m_bm_render = Bitmap.createBitmap(face_det_width * n_scale_points, face_det_height * n_scale_points, Bitmap.Config.ARGB_8888);
            }
            //synchronized (cache_lock)
            {
                beg = System.currentTimeMillis();
                bitmap = QhFaceApi.qhGetFaceMaskBitmap(mask_manager.GetPngPath(), mask_manager.GetPointPath(b_useqhface), points, face_det_width * n_scale_points, face_det_height * n_scale_points, m_bm_render);
                Log.e("masktest", "GetBitmap is : " + (System.currentTimeMillis() - beg));
            }
        }

        if (bitmap != null)
        {
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
            mRect.setScale(width, height);
            mRect.setPosition(mid_x, mid_y);
            mRect.setTexture(textures[0]);
            mRect.setRotation(0);
            // Matrix.orthoM(mDisplayProjectionMatrix, 0, 0, mWidth, 0, mHeight,
            // -1, 1);
            glGetError();
            mRect.draw(mTextureProgram, matrix);
        }

        glDeleteTextures(1, textures, 0);
        glDisable(GL_BLEND);
    }
*/
}
