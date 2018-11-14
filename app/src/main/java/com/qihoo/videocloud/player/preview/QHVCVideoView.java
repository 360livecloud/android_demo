
package com.qihoo.videocloud.player.preview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.qihoo.livecloud.play.callback.ILiveCloudDisplay;
import com.qihoo.livecloud.play.callback.PlayerCallback;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.videocloud.IQHVCPlayer;
import com.qihoo.videocloud.IQHVCPlayerAdvanced;
import com.qihoo.videocloud.QHVCPlayer;
import com.qihoo.videocloud.view.QHVCTextureView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

//import android.widget.FrameLayout;

//import android.widget.RelativeLayout;

/**
 * Created by guohailiang on 2017/7/18.
 */

public class QHVCVideoView extends RelativeLayout implements ILiveCloudDisplay {

    private static final String TAG = QHVCVideoView.class.getSimpleName();

    private QHVCTextureView playView = null;
    private QHVCPlayer qhvcPlayer;

    private Context context;

    private String playUrl;
    private int seekTo;
    private boolean isMute = false;

    private boolean isPlaying = false;
    private boolean isPause = false;

    /**
     * user use listener
     */
    private IQHVCPlayer.OnPreparedListener onPreparedListener;
    private IQHVCPlayer.OnSeekCompleteListener onSeekCompleteListener;
    private IQHVCPlayer.OnErrorListener onErrorListener;
    private IQHVCPlayer.OnCompletionListener onCompletionListener;
    private IQHVCPlayer.OnInfoListener onInfoListener;
    private IQHVCPlayer.OnVideoSizeChangedListener onVideoSizeChangedListener;
    private IQHVCPlayer.OnBufferingUpdateListener onBufferingUpdateListener;
    private IQHVCPlayer.OnBufferingEventListener onBufferingEventListener;
    private IQHVCPlayer.onProgressChangeListener onProgressChangeListener;
    private IQHVCPlayerAdvanced.OnAudioPCMListener onAudioPCMListener;
    private IQHVCPlayerAdvanced.OnPlayerNetStatsListener onPlayerNetStatsListener;

    /**
     * internal listener
     */
    private final IQHVCPlayer.OnPreparedListener preparedListener;
    private final IQHVCPlayer.OnSeekCompleteListener seekCompleteListener;
    private final IQHVCPlayer.OnErrorListener errorListener;
    private final IQHVCPlayer.OnCompletionListener completionListener;
    private final IQHVCPlayer.OnInfoListener infoListener;
    private final IQHVCPlayer.OnVideoSizeChangedListener videoSizeChangedListener;
    private final IQHVCPlayer.OnBufferingUpdateListener bufferingUpdateListener;
    private final IQHVCPlayer.OnBufferingEventListener bufferingEventListener;
    private final IQHVCPlayer.onProgressChangeListener progressChangeListener;
    private final IQHVCPlayerAdvanced.OnAudioPCMListener audioPCMListener;
    private final IQHVCPlayerAdvanced.OnPlayerNetStatsListener playerNetStatsListener;

    //----------------------------------------- 外部接口 ------------------------------------------

    public void setDataSource(/*@IQHVCPlayer.PlayType */int playType,
            @NonNull String sn,
            @NonNull String channelId,
            @NonNull String sign,
            @Nullable Map<String, Object> options) throws IOException, IllegalArgumentException, IllegalStateException {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.setDataSource(playType, sn, channelId, sign, options);
        }
    }

    public boolean snapshot(String path) {
        if (this.qhvcPlayer != null) {
            return this.qhvcPlayer.snapshot(path);
        } else {
            return false;
        }
    }

    public int getDecoderMode() {
        if (this.qhvcPlayer != null) {
            return this.qhvcPlayer.getDecoderMode();
        } else {
            return IQHVCPlayerAdvanced.LIVECLOUD_SOFT_DECODE_MODE;
        }
    }

    public void disableRender(boolean isRender) throws IllegalStateException {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.disableRender(isRender);
        }
    }

    public void stop(/*@IQHVCPlayerAdvanced.StopReason*/ int reason) throws IllegalStateException {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.stop(reason);
        }
    }

    public void addToGroup(int group) {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.addToGroup(group);
        }
    }

    public void setDataSource(/*@IQHVCPlayer.PlayType*/ int playType, @NonNull String url, @NonNull String channelId)
            throws IOException, IllegalArgumentException, IllegalStateException {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.setDataSource(playType, url, channelId);
        }
    }

    public void prepareAsync() throws IllegalStateException {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.prepareAsync();
        }
    }

    public void start() throws IllegalStateException {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.start();
        }
    }

    public boolean isPlaying() {
        if (this.qhvcPlayer != null) {
            return this.qhvcPlayer.isPlaying();
        } else {
            return false;
        }
    }

    public void pause() throws IllegalStateException {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.pause();
        }
    }

    public boolean isPaused() {
        if (this.qhvcPlayer != null) {
            return this.qhvcPlayer.isPaused();
        } else {
            return false;
        }
    }

    public void seekTo(int millis) throws IllegalStateException {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.seekTo(millis);
        }
    }

    public int getCurrentPosition() {
        if (this.qhvcPlayer != null) {
            return this.qhvcPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    public int getDuration() {
        if (this.qhvcPlayer != null) {
            return this.qhvcPlayer.getDuration();
        } else {
            return 0;
        }
    }

    public void stop() throws IllegalStateException {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.stop(IQHVCPlayerAdvanced.USER_CLOSE);
        }
    }

    public void release() {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.release();
        }
    }

    public void setMute(boolean mute) throws IllegalStateException {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.setMute(mute);
        }
    }

    public boolean isMute() {
        if (this.qhvcPlayer != null) {
            return this.qhvcPlayer.isMute();
        } else {
            return false;
        }
    }

    public void setVolume(float volume) throws IllegalStateException {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.setVolume(volume);
        }
    }

    public float getVolume() throws IllegalStateException {
        if (this.qhvcPlayer != null) {
            return this.qhvcPlayer.getVolume();
        } else {
            return 0;
        }
    }

    public void setScreenOnWhilePlaying(boolean screenOn) {
        if (this.qhvcPlayer != null) {
            this.qhvcPlayer.setScreenOnWhilePlaying(screenOn);
        }
    }

    public int getPlayerId() {
        if (this.qhvcPlayer != null) {
            return this.qhvcPlayer.getPlayerId();
        } else {
            return 0;
        }
    }

    public Map<String, Object> getMediaInformation() {

        if (this.qhvcPlayer != null) {
            return this.qhvcPlayer.getMediaInformation();
        } else {
            return null;
        }
    }

    public void setVideoRatio(float videoRatio) {
        if (playView != null && qhvcPlayer != null) {
            playView.setVideoRatio(videoRatio);
        }
    }

    //---------------------------------------------------------------------------------------------
    public void setOnPreparedListener(IQHVCPlayer.OnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
    }

    public void setOnSeekCompleteListener(IQHVCPlayer.OnSeekCompleteListener onSeekCompleteListener) {
        this.onSeekCompleteListener = onSeekCompleteListener;
    }

    public void setOnErrorListener(IQHVCPlayer.OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    public void setOnCompletionListener(IQHVCPlayer.OnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
    }

    public void setOnInfoListener(IQHVCPlayer.OnInfoListener onInfoListener) {
        this.onInfoListener = onInfoListener;
    }

    public void setOnVideoSizeChangedListener(IQHVCPlayer.OnVideoSizeChangedListener onVideoSizeChangedListener) {
        this.onVideoSizeChangedListener = onVideoSizeChangedListener;
    }

    public void setOnBufferingUpdateListener(IQHVCPlayer.OnBufferingUpdateListener onBufferingUpdateListener) {
        this.onBufferingUpdateListener = onBufferingUpdateListener;
    }

    public void setOnBufferingEventListener(IQHVCPlayer.OnBufferingEventListener onBufferingEventListener) {
        this.onBufferingEventListener = onBufferingEventListener;
    }

    public void setOnProgressChangeListener(IQHVCPlayer.onProgressChangeListener onProgressChangeListener) {
        this.onProgressChangeListener = onProgressChangeListener;
    }

    public void setOnAudioPCMListener(IQHVCPlayerAdvanced.OnAudioPCMListener onAudioPCMListener) {
        this.onAudioPCMListener = onAudioPCMListener;
    }

    public void setOnPlayerNetStatsListener(IQHVCPlayerAdvanced.OnPlayerNetStatsListener onPlayerNetStatsListener) {
        this.onPlayerNetStatsListener = onPlayerNetStatsListener;
    }

    //----------------------------------------- 内部接口 ------------------------------------------

    public QHVCVideoView(Context context) {
        this(context, null);
    }

    public QHVCVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        this.preparedListener = new IQHVCPlayer.OnPreparedListener() {
            @Override
            public void onPrepared() {
                if (QHVCVideoView.this.onPreparedListener != null) {
                    QHVCVideoView.this.onPreparedListener.onPrepared();
                }
            }
        };
        this.seekCompleteListener = new IQHVCPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(int handle) {
                if (QHVCVideoView.this.onSeekCompleteListener != null) {
                    QHVCVideoView.this.onSeekCompleteListener.onSeekComplete(handle);
                }
            }
        };
        this.errorListener = new IQHVCPlayer.OnErrorListener() {
            @Override
            public boolean onError(int handle, int what, int extra) {
                if (QHVCVideoView.this.onErrorListener != null) {
                    return QHVCVideoView.this.onErrorListener.onError(handle, what, extra);
                } else {
                    return false;
                }
            }
        };
        this.completionListener = new IQHVCPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(int handle) {
                if (QHVCVideoView.this.onCompletionListener != null) {
                    QHVCVideoView.this.onCompletionListener.onCompletion(handle);
                }
            }
        };
        this.infoListener = new IQHVCPlayer.OnInfoListener() {
            @Override
            public void onInfo(int handle, int what, int extra) {
                if (QHVCVideoView.this.onInfoListener != null) {

                    if (what == IQHVCPlayer.INFO_DEVICE_RENDER_FIRST_FRAME) {

                    } else if (what == IQHVCPlayer.INFO_DEVICE_RENDER_ERR) {

                        // err
                        if (Logger.LOG_ENABLE) {
                            Logger.e(TAG, "dvrender err");
                        }
                    } else if (what == IQHVCPlayer.INFO_DEVICE_RENDER_QUERY_SURFACE) {

                        if (playView != null) {
                            if (!isPause) {//添加判断
                                playView.render_proc(PlayerCallback.DEVICE_RENDER_QUERY_SURFACE, 0/*不使用此变量*/);
                            }
                        }
                    } else if (what == IQHVCPlayer.INFO_RENDER_RESET_SURFACE) {

                        if (playView != null) {
                            playView.pauseSurface();
                        }
                    }

                    QHVCVideoView.this.onInfoListener.onInfo(handle, what, extra);
                }
            }
        };
        this.videoSizeChangedListener = new IQHVCPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(int handle, int width, int height) {

                if (viewWidth != 0 && viewHeight != 0 && (playVideoWidth != width || playVideoHeight != height)) {
                    playVideoWidth = width;
                    playVideoHeight = height;
                    setPreviewLayoutParams();

                    if (QHVCVideoView.this.onVideoSizeChangedListener != null) {
                        QHVCVideoView.this.onVideoSizeChangedListener.onVideoSizeChanged(handle, width, height);
                    }
                }
            }
        };
        this.bufferingUpdateListener = new IQHVCPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(int handle, int percent) {
                if (QHVCVideoView.this.onBufferingUpdateListener != null) {
                    QHVCVideoView.this.onBufferingUpdateListener.onBufferingUpdate(handle, percent);
                }
            }
        };
        this.bufferingEventListener = new IQHVCPlayer.OnBufferingEventListener() {
            @Override
            public void onBufferingStart(int handle) {
                if (QHVCVideoView.this.onBufferingEventListener != null) {
                    QHVCVideoView.this.onBufferingEventListener.onBufferingStart(handle);
                }
            }

            @Override
            public void onBufferingProgress(int handle, int progress) {
                if (QHVCVideoView.this.onBufferingEventListener != null) {
                    QHVCVideoView.this.onBufferingEventListener.onBufferingProgress(handle, progress);
                }
            }

            @Override
            public void onBufferingStop(int handle) {
                if (QHVCVideoView.this.onBufferingEventListener != null) {
                    QHVCVideoView.this.onBufferingEventListener.onBufferingStop(handle);
                }
            }
        };
        this.progressChangeListener = new IQHVCPlayer.onProgressChangeListener() {
            @Override
            public void onProgressChange(int handle, int total, int progress) {
                if (QHVCVideoView.this.onProgressChangeListener != null) {
                    QHVCVideoView.this.onProgressChangeListener.onProgressChange(handle, total, progress);
                }
            }
        };
        this.audioPCMListener = new IQHVCPlayerAdvanced.OnAudioPCMListener() {
            @Override
            public void onAudioPCM(int handle, int id, ByteBuffer buffer, long timestamp, int channels, int sampleRate, int bitsPerSample) {
                if (QHVCVideoView.this.onAudioPCMListener != null) {
                    QHVCVideoView.this.onAudioPCMListener.onAudioPCM(handle, id, buffer, timestamp, channels, sampleRate, bitsPerSample);
                }
            }
        };
        this.playerNetStatsListener = new IQHVCPlayerAdvanced.OnPlayerNetStatsListener() {
            @Override
            public void onPlayerNetStats(int handle, long dvbps, long dabps, long dvfps, long dafps, long fps, long bitrate, long param1, long param2, long param3) {
                if (QHVCVideoView.this.onPlayerNetStatsListener != null) {
                    QHVCVideoView.this.onPlayerNetStatsListener.onPlayerNetStats(handle, dvbps, dabps, dvfps, dafps, fps, bitrate, param1, param2, param3);
                }
            }
        };

        viewInit();
        listenerInit();
    }

    private void viewInit() {

        this.playView = new QHVCTextureView(context);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        this.playView.setLayoutParams(layoutParams);
        this.addView(this.playView);
        playView.onPlay();

        // TODO: 2017/7/18  横竖屏处理
        //        if(this.getResources().getConfiguration().orientation == 2) {
        //            if(this.m != null) {
        //                this.m.b(false);
        //            }
        //        } else if(this.getResources().getConfiguration().orientation == 1 && this.m != null) {
        //            this.m.b(true);
        //        }
        // TODO: 2017/7/18  play view其他初始化
        this.setFocusable(true);
        this.setFocusableInTouchMode(true);
        this.requestFocus();
    }

    private void listenerInit() {
        qhvcPlayer = new QHVCPlayer(context);
        //        qhvcPlayer.setDisplay(playView);
        qhvcPlayer.setDisplay(/*this*/playView);
        qhvcPlayer.setOnPreparedListener(preparedListener);
        qhvcPlayer.setOnSeekCompleteListener(seekCompleteListener);
        qhvcPlayer.setOnErrorListener(errorListener);
        qhvcPlayer.setOnCompletionListener(completionListener);
        qhvcPlayer.setOnInfoListener(infoListener);
        qhvcPlayer.setOnVideoSizeChangedListener(videoSizeChangedListener);
        qhvcPlayer.setOnBufferingUpdateListener(bufferingUpdateListener);
        qhvcPlayer.setOnProgressChangeListener(progressChangeListener);
        qhvcPlayer.setOnAudioPCMListener(audioPCMListener);
        qhvcPlayer.setOnPlayerNetStatsListener(playerNetStatsListener);

        playView.setPlayer(qhvcPlayer);
    }

    @Override
    public void setHandle(int playerId) {
        if (Logger.LOG_ENABLE) {
            Logger.e(TAG, "set Handle playerId = " + playerId);
        }

        if (playView != null && qhvcPlayer != null) {
            playView.setHandle(qhvcPlayer.getPlayerId());
        }
    }

    @Override
    public void stopRender() {

    }

    @Override
    public void startRender() {

    }

    // for render mode
    private int viewWidth = 0; //控件宽度
    private int viewHeight = 0; //控件高度
    private int playVideoWidth = 0;
    private int playVideoHeight = 0;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (viewWidth != getWidth() || viewHeight != getHeight()) {
            viewWidth = getWidth();
            viewHeight = getHeight();
            if (playVideoWidth != 0 && playVideoHeight != 0) {
                setPreviewLayoutParams();
            }
        }
    }

    /**
     * 设置预览的大小，以便充满整个屏幕
     */
    private void setPreviewLayoutParams() {
        if (viewWidth == 0 || viewHeight == 0 || playVideoWidth == 0 || playVideoHeight == 0) {
            return;
        }
        if (Logger.LOG_ENABLE) {
            Logger.i(TAG, "scaleType: " + qhvcPlayer.getRenderMode() + " viewWidth: " + viewWidth + " viewHeight: " + viewHeight + " playVideoWidth: " + playVideoWidth
                    + " playVideoHeight: " + playVideoHeight);
        }

        int setHeight = viewHeight;
        int setWidth = viewWidth;

        float scaleX = (float) setWidth / playVideoWidth;
        float scaleY = (float) setHeight / playVideoHeight;
        float scale = 0.0f;

        final int scaleType = qhvcPlayer.getRenderMode();
        if (scaleX >= scaleY) {
            switch (scaleType) {
                case IQHVCPlayerAdvanced.RENDER_MODE_IN:
                    scale = scaleY;
                    break;
                case IQHVCPlayerAdvanced.RENDER_MODE_OUT:
                    scale = scaleX;
                    break;
                case IQHVCPlayerAdvanced.RENDER_MODE_FULL:
                    scale = 0.0f;
                    break;
                default:
                    break;
            }
        } else if (scaleX < scaleY) {
            switch (scaleType) {
                case IQHVCPlayerAdvanced.RENDER_MODE_IN:
                    scale = scaleX;
                    break;
                case IQHVCPlayerAdvanced.RENDER_MODE_OUT:
                    scale = scaleY;
                    break;
                case IQHVCPlayerAdvanced.RENDER_MODE_FULL:
                    scale = 0.0f;
                    break;
                default:
                    break;
            }
        }

        if (scale != 0.0f) {
            setWidth = (int) (playVideoWidth * scale);
            setHeight = (int) (playVideoHeight * scale);
        }
        if (Logger.LOG_ENABLE) {
            Logger.i(TAG, "set setWidth: " + setWidth + " setHeight: " + setHeight);
        }
        LayoutParams params = new LayoutParams(
                setWidth, setHeight);
        if (setWidth != viewWidth) {
            params.setMargins((viewWidth - setWidth) / 2, 0, (viewWidth - setWidth) / 2, 0);
        }
        if (setHeight != viewHeight) {
            params.setMargins(0, (viewHeight - setHeight) / 2, 0, (viewHeight - setHeight) / 2);
            //            params.setMargins( 0,viewHeight-setHeight, 0, 0);//按照产品要求，所有的画面往上推，底部对齐
        }
        if (playView != null) {
            playView.setLayoutParams(params);
        }
    }
}
