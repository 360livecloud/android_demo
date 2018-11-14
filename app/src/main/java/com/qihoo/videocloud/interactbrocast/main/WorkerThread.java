
package com.qihoo.videocloud.interactbrocast.main;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.qihoo.livecloud.interact.api.QHVCInteractiveConstant;
import com.qihoo.livecloud.interact.api.QHVCInteractiveKit;
import com.qihoo.livecloud.interact.api.QHVCInteractiveVideoSourceEvent;
import com.qihoo.livecloud.tools.Constants;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.tools.MD5;
import com.qihoo.videocloud.VideoCloudApplication;
import com.qihoo.videocloud.interactbrocast.data.InteractGlobalManager;
import com.qihoo.videocloud.interactbrocast.livingcammera.VideoSourceListener;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

/**
 * Created by liuyanqing on 2016/11/10.
 */

public class WorkerThread extends Thread implements VideoSourceListener {

    private final Context mContext;

    private static final int ACTION_WORKER_THREAD_QUIT = 0X1010; // quit this thread

    private static final int ACTION_WORKER_LOAD_ENGINE = 0X1011; //load engine

    private static final int ACTION_WORKER_JOIN_CHANNEL = 0X2010; //join channel

    private static final int ACTION_WORKER_LEAVE_CHANNEL = 0X2011; //leave channel

    private static final int ACTION_WORKER_CONFIG_ENGINE = 0X2012; // config engine

    private static final int ACTION_WORKER_PREVIEW = 0X2014; // preview

    private static Handler sHandler = new Handler(Looper.getMainLooper());

    private WorkerThreadHandler mWorkerHandler;

    private boolean mReady;

    private boolean mUserVideoCapture; //标识是否业务端自采集视频

    private static QHVCInteractiveKit mInteractEngine;

    private QHVCInteractiveVideoSourceEvent mVideoSource;

    private WorkerEvent mWorkerEvent;

    private int talkType;/*初始化时，指定是音频类型或是视频类型*/

    public WorkerThread(boolean recordVideo) {
        this.mUserVideoCapture = recordVideo;
        this.mContext = VideoCloudApplication.getInstance();
    }

    public void setWorkerEvent(WorkerEvent event) {
        this.mWorkerEvent = event;
    }

    public final void waitForReady() {
        while (!mReady) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Logger.d(InteractConstant.TAG, "wait for " + WorkerThread.class.getSimpleName());
        }
    }

    public boolean isReady() {
        return mReady;
    }

    public void setTalkType(int talkType) {
        this.talkType = talkType;
    }

    @Override
    public synchronized void start() {
        if (!isReady()) {

            super.start();
        }
    }

    @Override
    public void run() {
        Looper.prepare();

        mWorkerHandler = new WorkerThreadHandler(this);

        create();

        mReady = true;

        Looper.loop();
    }

    private String getLogPath() {
        String logPath = Environment.getExternalStorageDirectory() + File.separator + "hostinLog";
        String fileName = "rtc.log";
        // 目录
        File path = new File(logPath);
        // 文件
        File f = new File(logPath + File.separator + fileName);
        // 如果目录文件不存在就创建目录
        if (!path.exists()) {
            path.mkdirs();
        }
        // 如果文件不存在就创建文件
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return logPath + File.separator + fileName;
    }

    private void setVideoCapture() {
        if (mUserVideoCapture && mInteractEngine != null) {
            mVideoSource = mInteractEngine.getVideoSourceEvent();
            if (mVideoSource != null) {
                mVideoSource.attach();
                if (InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_GPU) {
                    mVideoSource.setVideoTransMode(QHVCInteractiveVideoSourceEvent.VideoTransMode.SURFACE_TEXTURE);
                } else if (InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_GPU_READPIXELS) {
                    mVideoSource.setVideoTransMode(QHVCInteractiveVideoSourceEvent.VideoTransMode.VIDEO_FRAME_DATA);
                } else {
                    mVideoSource.setVideoTransMode(QHVCInteractiveVideoSourceEvent.VideoTransMode.VIDEO_FRAME_DATA);
                }
            }
        }
    }

    private void create() {
        if (mInteractEngine == null) {
            mInteractEngine = QHVCInteractiveKit.getInstance();
        }
    }

    public QHVCInteractiveKit getInteractEngine() {
        return mInteractEngine;
    }

    public final void loadEngine(String roomId, String userId, Map<String, String> optionInfo) {
        if (Thread.currentThread() != this) {
            Logger.w(InteractConstant.TAG, InteractConstant.TAG + ", worker thread loadEngine " + roomId + " " + userId);
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_LOAD_ENGINE;
            envelop.obj = new Object[] {
                    roomId, userId, optionInfo
            };
            mWorkerHandler.sendMessage(envelop);
            return;
        }

        //TODO 注意：secretkey必须放在业务服务端，切勿放在客户端。此处uSign和appKey放在Demo中仅用于演示。
        String cid = InteractGlobalManager.getInstance().getChannelId();
        String ak = InteractGlobalManager.getInstance().getAppKey();
        String sk = InteractGlobalManager.getInstance().getSecretKey();
        String uSign = MD5.encryptMD5("sname__" + cid + "room_id__" + roomId + "uid__" + userId + sk);
        InteractGlobalManager.getInstance().setUSign(uSign);
        QHVCInteractiveKit.getInstance().setPublicServiceInfo(cid, ak, uSign);

        //开启业务做视频数据采集
        if (mUserVideoCapture) {
            mInteractEngine.openCollectingData();
        }

        int result = mInteractEngine.loadEngine(roomId, userId, caluSessionForTest(), optionInfo, InteractCallback.getInstance());
        if (result < 0) {
            showToast("loadEngine failed, err: " + result);
        }
    }

    /**
     * 模拟创建会话ID
     *
     * @return 会话ID
     */
    private String caluSessionForTest() {
        String sessionId = MD5.encryptMD5(String.valueOf(System.currentTimeMillis()) + String.valueOf(new Random().nextInt()));
        return sessionId;
    }

    public final void configEngine(int cRole, int vProfile, int orientation) {
        if (Thread.currentThread() != this) {
            Logger.w(InteractConstant.TAG, "configEngine() - worker thread asynchronously " + cRole + " " + vProfile);
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_CONFIG_ENGINE;
            envelop.obj = new Object[] {
                    cRole, vProfile, orientation
            };
            mWorkerHandler.sendMessage(envelop);
            return;
        }

        mInteractEngine.setChannelProfile(QHVCInteractiveConstant.CHANNEL_PROFILE_LIVE_BROADCASTING);
        setVideoCapture();

        if (talkType != InteractConstant.TALK_TYPE_AUDIO) {
            mInteractEngine.enableVideo();
        }
        if (Logger.LOG_ENABLE) {
            mInteractEngine.setLogFile(getLogPath());
        }

        //设置是否开启双流模式
        //mInteractEngine.enableDualStreamMode(true);
        //设置是否使用硬编码
        if (InteractConstant.CURR_VIDEO_CAPTURE == InteractConstant.VideoCapture.RECORD_GPU) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                mInteractEngine.useHardwareEncoder(true);
            }
        }

        boolean swapWidthAndHeight = (orientation == Constants.EMode.EMODE_PORTRAIT) ? true : false;
        mInteractEngine.setVideoProfile(vProfile, swapWidthAndHeight);
        mInteractEngine.setClientRole(cRole);
    }

    public final void preview(boolean start, View view, String uid) {
        if (Thread.currentThread() != this) {
            //log.warn("preview() - worker thread asynchronously " + start + " " + view + " " + (uid & 0XFFFFFFFFL));
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_PREVIEW;
            envelop.obj = new Object[] {
                    start, view, uid
            };
            mWorkerHandler.sendMessage(envelop);
            return;
        }

        if (start) {
            if (talkType != InteractConstant.TALK_TYPE_AUDIO) {
                mInteractEngine.setupLocalVideo(view, QHVCInteractiveConstant.RenderMode.RENDER_MODE_HIDDEN, uid);
            }
            int result = mInteractEngine.startPreview();
        } else {
            mInteractEngine.stopPreview();
        }
    }

    public final void joinChannel() {
        if (Thread.currentThread() != this) {
            Logger.w(InteractConstant.TAG, "joinChannel() - worker thread asynchronously.");
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_JOIN_CHANNEL;
            //envelop.obj = new Object[]{channel, uid, streamMode};
            mWorkerHandler.sendMessage(envelop);
            return;
        }

        int result = mInteractEngine.joinChannel();
        if (result < 0) {
            showToast("加入频道失败， result: " + result);
        }
    }

    public final void leaveChannel(String channel) {
        //        if (mWorkerHandler != null) {
        //            mWorkerHandler.removeCallbacksAndMessages(null);
        //        }
        if (Thread.currentThread() != this) {
            Logger.w(InteractConstant.TAG, InteractConstant.TAG + ":  leaveChannel() - worker thread asynchronously " + channel);
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_LEAVE_CHANNEL;
            envelop.obj = channel;
            mWorkerHandler.sendMessage(envelop);
            return;
        }

        if (mInteractEngine != null) {
            mInteractEngine.leaveChannel();
        }

        Logger.w(InteractConstant.TAG, InteractConstant.TAG + ": ian, WorkerThread leaveChannel() , channel: " + channel);
    }

    /**
     * call this method to exit
     * should ONLY call this method when this thread is running
     */
    public final void exit() {
        if (Thread.currentThread() != this) {
            if (mWorkerHandler != null) {
                Logger.w(InteractConstant.TAG, InteractConstant.TAG + ":  exit() - worker thread asynchronously...");
                mWorkerHandler.sendEmptyMessage(ACTION_WORKER_THREAD_QUIT);
            }
            return;
        }

        mReady = false;

        if (mVideoSource != null) {
            mVideoSource.detach();
            mVideoSource = null;
        }
        if (mInteractEngine != null) {
            mInteractEngine.destroy();
            mInteractEngine = null;
        }
        // TODO should remove all pending(read) messages

        // exit thread looper
        Looper.myLooper().quit();

        if (mWorkerHandler != null) {
            mWorkerHandler.release();
        }
        if (mWorkerEvent != null) {
            Logger.d(InteractConstant.TAG, InteractConstant.TAG + ": start mWorkerEvent.onExit()...");
            mWorkerEvent.onExit();
        }
        Logger.w(InteractConstant.TAG, InteractConstant.TAG + ": ian, WorkerThead exit()...");
    }

    public void showToast(final String content) {
        if (!TextUtils.isEmpty(content)) {
            sHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, content, Toast.LENGTH_SHORT).show();

                }
            });
        }
    }

    @Override
    public void onFrameAvailable(byte[] bytes, int width, int height, int rotation, long ts, int format) {
        if (mVideoSource != null) {
            mVideoSource.deliverFrame(bytes, width, height, 0, 0, 0, 0, rotation, ts, format);
        } else {
            Logger.e(InteractConstant.TAG, InteractConstant.TAG + ", onFrameAvailable failed, QHLiveCloudVideoSourceEvent is null....");
        }
    }

    @Override
    public SurfaceTexture getSurfaceTexture(int previewWidth, int previewHeight) {
        if (mVideoSource != null) {
            return mVideoSource.getSurfaceTexture(previewWidth, previewHeight);
        }
        return null;
    }

    private static final class WorkerThreadHandler extends Handler {

        private WorkerThread mWorkerThread;

        WorkerThreadHandler(WorkerThread thread) {
            this.mWorkerThread = thread;
        }

        public void release() {
            mWorkerThread = null;
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.mWorkerThread == null) {
                Logger.w(InteractConstant.TAG, "handler is already released! " + msg.what);
                return;
            }

            switch (msg.what) {
                case ACTION_WORKER_LOAD_ENGINE:
                    Object[] data = (Object[]) msg.obj;
                    mWorkerThread.loadEngine((String) data[0], (String) data[1], (Map<String, String>) data[2]);
                    break;
                case ACTION_WORKER_THREAD_QUIT:
                    mWorkerThread.exit();
                    break;
                case ACTION_WORKER_JOIN_CHANNEL:
                    mWorkerThread.joinChannel();
                    break;
                case ACTION_WORKER_LEAVE_CHANNEL:
                    String channel = (String) msg.obj;
                    mWorkerThread.leaveChannel(channel);
                    break;
                case ACTION_WORKER_CONFIG_ENGINE:
                    Object[] configData = (Object[]) msg.obj;
                    mWorkerThread.configEngine((int) configData[0], (int) configData[1], (int) configData[2]);
                    break;
                case ACTION_WORKER_PREVIEW:
                    Object[] previewData = (Object[]) msg.obj;
                    mWorkerThread.preview((boolean) previewData[0], (View) previewData[1], (String) previewData[2]);
                    break;
            }
        }
    }

    public interface WorkerEvent {
        void onExit();
    }

}
