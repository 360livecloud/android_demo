
package com.qihoo.videocloud.p2p.player;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.qihoo.livecloud.play.callback.PlayerCallback;
import com.qihoo.livecloud.sdk.QHVCSdk;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.tools.NetUtil;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.IQHVCPlayer;
import com.qihoo.videocloud.IQHVCPlayerAdvanced;
import com.qihoo.videocloud.QHVCPlayer;
import com.qihoo.videocloud.p2p.P2PVideoItemData;
import com.qihoo.videocloud.p2p.base.BaseP2PActivity;
import com.qihoo.videocloud.p2p.base.VerticalViewPager;
import com.qihoo.videocloud.p2p.setting.P2PSettingConfig;
import com.qihoo.videocloud.player.LogAdapter;
import com.qihoo.videocloud.utils.AndroidUtil;
import com.qihoo.videocloud.utils.NoDoubleClickListener;
import com.qihoo.videocloud.utils.QHVCSharedPreferences;
import com.qihoo.videocloud.view.QHVCTextureView;
import com.qihoo.videocloud.widget.ViewHeader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class P2PPlayerActivity extends BaseP2PActivity {

    public static final String TAG = "P2PPlayerActivity";

    protected ImageLoader mImageLoader = ImageLoader.getInstance();
    private boolean mInstanceStateSaved;

    private IQHVCPlayerAdvanced mLiveCloudPlayer;
    private QHVCTextureView mTextureView;

    private ArrayList<P2PVideoItemData> mPlayList;
    private int mCurIndex;
    private int mInitPlayPos = 0;

    private ViewHeader mHeaderView;
    private VerticalViewPager mViewPager;
    private ImageView[] mImageViews;
    private TextView mCurrTimeTextView;
    private TextView mTotalTimeTextView;
    private SeekBar mSeekBar;
    private ImageView mPlayView;
    private ImageView mPauseView;
    private View mHelpView;
    private Handler mHandler;
    private ListView mLogListView;
    private LogAdapter logAdapter;
    private List<String> logList = new ArrayList<>();

    private int videoWidth;
    private int videoHeight;
    private long downloadBitratePerSecond;//下行码率
    private long videoBitratePerSecond;// 视频码率
    private long videoFrameRatePerSecond;//视频帧率
    private String[] mResids = new String[2];
    private String[] mPlayerUrls = new String[2];
    private int mCurrentRes = 0;

    private Vibrator mVibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.p2p_player_activity);
        Logger.d(TAG, "onCreate");

        initData(getIntent().getExtras());
        initView();

        mHandler = new Handler(Looper.getMainLooper());

        startPlay(mCurIndex, mInitPlayPos);
    }

    private void initData(Bundle bundle) {
        List<P2PVideoItemData> playList = (List<P2PVideoItemData>) bundle.getSerializable("list");
        if (playList == null || playList.isEmpty()) {
            throw new IllegalArgumentException("play list is empty");
        }

        mPlayList = new ArrayList<>();
        mPlayList.addAll(playList);
        mCurIndex = bundle.getInt("id");
        mInitPlayPos = bundle.getInt("curPlayPos", 0);
    }

    private void initView() {
        mHeaderView = (ViewHeader) findViewById(R.id.header);
        mHeaderView.getLeftIcon().setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                finish();
            }
        });

        mTextureView = (QHVCTextureView) findViewById(R.id.gl2_video_view);

        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

        mCurrTimeTextView = (TextView) findViewById(R.id.curr_time);
        mTotalTimeTextView = (TextView) findViewById(R.id.total_time);

        mPlayView = (ImageView) findViewById(R.id.play);
        mPlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLiveCloudPlayer != null && mLiveCloudPlayer.isPaused()) {
                    mLiveCloudPlayer.start();

                    if (mLiveCloudPlayer.isPlaying()) {
                        mPlayView.setVisibility(View.INVISIBLE);
                        mPauseView.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        mPauseView = (ImageView) findViewById(R.id.pause);
        mPauseView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLiveCloudPlayer != null && mLiveCloudPlayer.isPlaying()) {
                    mLiveCloudPlayer.pause();

                    mPlayView.setVisibility(View.VISIBLE);
                    mPauseView.setVisibility(View.INVISIBLE);

                }
            }
        });

        mImageViews = new ImageView[mPlayList.size()];
        for (int i = 0; i < mPlayList.size(); i++) {
            ImageView imageView = new ImageView(this);
            mImageViews[i] = imageView;

            mImageLoader.displayImage(mPlayList.get(i).getImage(), imageView);
        }

        RelativeLayout mRootView = (RelativeLayout) findViewById(R.id.video_layout);
        mViewPager = new VerticalViewPager(this);
        mViewPager.setAdapter(new MyAdapter());
        mRootView.addView(mViewPager);

        mViewPager.setCurrentItem(mCurIndex);
        mViewPager.setOnPageChangeListener(mOnPageChangeListener);

        mHelpView = findViewById(R.id.help_layout);
        mHelpView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHelpView.setVisibility(View.GONE);
            }
        });
        if (QHVCSharedPreferences.getInstence().getBoolean("p2p_player_scroll_help_show", false)) {
            mHelpView.setVisibility(View.GONE);
        } else {
            QHVCSharedPreferences.getInstence().putBooleanValue("p2p_player_scroll_help_show", true);
        }

        mLogListView = (ListView) findViewById(R.id.lv_log);

        logAdapter = new LogAdapter(this, logList, R.color.white);
        mLogListView.setAdapter(logAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLiveCloudPlayer != null) {
            try {
                mLiveCloudPlayer.disableRender(false);
            } catch (IllegalStateException e) {
                Logger.e(TAG, e.getMessage());
            }
        }
        if (mTextureView != null) {
            mTextureView.resumeSurface();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLiveCloudPlayer != null) {
            try {
                mLiveCloudPlayer.disableRender(true);
            } catch (IllegalStateException e) {
                Logger.e(TAG, e.getMessage());
            }
        }
        if (mTextureView != null) {
            mTextureView.pauseSurface();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "onDestroy");

        if (!mInstanceStateSaved) {
            mImageLoader.stop();
        }

        if (mVibrator != null) {
            mVibrator.cancel();
            mVibrator = null;
        }

        stopPlay();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mInstanceStateSaved = true;
    }

    private void startPlay(int id) {
        startPlay(id, 0);
    }

    private void startPlay(int id, final int initPlayPos) {
        stopPlay();
        if (mSeekBar != null) {
            mSeekBar.setProgress(0);
            mSeekBar.setSecondaryProgress(0);
        }

        P2PVideoItemData o = mPlayList.get(id);
        String url = o.getUrl();
        if (url.contains("?")) {
            url += "&time=" + System.currentTimeMillis();
        } else {
            url += "?time=" + System.currentTimeMillis();
        }

        mPlayerUrls[0] = url;
        mPlayerUrls[1] = "http://video.mp.sj.360.cn/vod_zhushou/vod-shouzhu-bj/1f212d18f71c15a07414de5ae49acb22.mp4";
        mResids[0] = o.getRid();
        mResids[1] = "ecb3cf59e32aa08b9f2be02682dedc48";

        Logger.d(TAG, "startPlay index=" + mCurIndex + ", url=" + url + ", player_url=" + mPlayerUrls);

        QHVCPlayer.enableP2PUpload(P2PSettingConfig.ENABLE_P2P_UPLOAD);
        mLiveCloudPlayer = new QHVCPlayer(this);
        mLiveCloudPlayer.enableP2P(P2PSettingConfig.ENABLE_P2P);
        mTextureView.onPlay();
        mTextureView.setPlayer(mLiveCloudPlayer);
        mLiveCloudPlayer.setDisplay(mTextureView);

        try {
            //TODO
            //QHVCSdkConfig qhvcSdkConfig = QHVCSdk.getInstance().getConfig();
            Map<String, Object> options = new HashMap<>();
            options.put(IQHVCPlayerAdvanced.KEY_OPTION_POSITION, initPlayPos);
            options.put(IQHVCPlayerAdvanced.KEY_OPTION_FORCE_P2P, false);

            mLiveCloudPlayer.setDataSource(IQHVCPlayer.PLAYTYPE_VOD,
                    mResids,
                    mPlayerUrls,
                    0,
                    getResources().getString(R.string.config_player_vod_cid),
                    "",
                    options);
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
            Toast.makeText(this, "数据源异常", Toast.LENGTH_SHORT).show();
            return;
        }

        mLiveCloudPlayer.setOnPreparedListener(new IQHVCPlayer.OnPreparedListener() {
            @Override
            public void onPrepared() {
                Logger.d(TAG, "onPrepared");
                mLiveCloudPlayer.start();
            }
        });

        mLiveCloudPlayer.setOnInfoListener(new IQHVCPlayer.OnInfoListener() {
            @Override
            public void onInfo(int handle, int what, int extra) {
                Logger.d(TAG, "onInfo handle: " + handle + " what : " + what + " extra : " + extra);

                if (what == IQHVCPlayer.INFO_LIVE_PLAY_START) {

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mImageViews[mViewPager.getCurrentItem() % mImageViews.length] != null) {
                                mImageViews[mViewPager.getCurrentItem() % mImageViews.length].setVisibility(View.GONE);
                            }
                        }
                    }, 50);

                    String title = mPlayList.get(mViewPager.getCurrentItem() % mImageViews.length).getTitle();
                    if (!TextUtils.isEmpty(title)) {
                        mHeaderView.setLeftText(title);
                    } else {
                        String rid = mPlayList.get(mViewPager.getCurrentItem() % mImageViews.length).getRid();
                        mHeaderView.setLeftText(rid);
                    }

                    mPlayView.setVisibility(View.GONE);
                    mPauseView.setVisibility(View.VISIBLE);
                } else if (what == IQHVCPlayer.INFO_DEVICE_RENDER_ERR) {

                    // err
                    if (Logger.LOG_ENABLE) {
                        Logger.e(TAG, "dvrender err");
                    }
                } else if (what == IQHVCPlayer.INFO_DEVICE_RENDER_QUERY_SURFACE) {

                    if (mTextureView != null) {
                        if (mLiveCloudPlayer != null && !mLiveCloudPlayer.isPaused()) {
                            mTextureView.render_proc(PlayerCallback.DEVICE_RENDER_QUERY_SURFACE, 0/*不使用此变量*/);
                        }
                    }
                } else if (what == IQHVCPlayer.INFO_RENDER_RESET_SURFACE) {

                    if (mTextureView != null) {
                        mTextureView.pauseSurface();
                    }
                } else if (what == IQHVCPlayer.INFO_PLAY_H265) {
                    String title = mPlayList.get(mViewPager.getCurrentItem() % mImageViews.length).getTitle();
                    if (!TextUtils.isEmpty(title)) {
                        mHeaderView.setLeftText(title + "(H265)");
                    } else {
                        String rid = mPlayList.get(mViewPager.getCurrentItem() % mImageViews.length).getRid();
                        mHeaderView.setLeftText(rid + "(H265)");
                        Logger.d(TAG, "播放H265");
                    }
                }
            }
        });

        mLiveCloudPlayer.setOnErrorListener(new IQHVCPlayer.OnErrorListener() {
            @Override
            public boolean onError(int handle, int what, int extra) {
                Toast.makeText(P2PPlayerActivity.this, "播放失败：what=" + what + ", extra=" + extra, Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        mLiveCloudPlayer.setOnProgressChangeListener(new IQHVCPlayer.onProgressChangeListener() {
            @Override
            public void onProgressChange(int handle, int total, int progress) {
                int percent = (progress * 100) / total;
                mSeekBar.setProgress(percent);
                mCurrTimeTextView.setText(AndroidUtil.getTimeString(progress));
                mTotalTimeTextView.setText(AndroidUtil.getTimeString(total));
                showLog();
            }
        });

        mLiveCloudPlayer.setOnBufferingUpdateListener(new IQHVCPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(int handle, int percent) {
                mSeekBar.setSecondaryProgress(percent);
            }
        });

        mLiveCloudPlayer.setOnCompletionListener(new IQHVCPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(int handle) {
                if (mLiveCloudPlayer != null) {
                    mLiveCloudPlayer.seekTo(0);
                }
            }
        });
        mLiveCloudPlayer.setOnVideoSizeChangedListener(new IQHVCPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(int handle, int width, int height) {
                videoWidth = width;
                videoHeight = height;
                if (mTextureView != null) {
                    mTextureView.setVideoRatio((float) width / (float) height);
                }
            }
        });

        mLiveCloudPlayer.setOnPlayerNetStatsListener(new IQHVCPlayerAdvanced.OnPlayerNetStatsListener() {
            @Override
            public void onPlayerNetStats(int handle, long dvbps, long dabps, long dvfps, long dafps, long fps, long bitrate, long param1, long param2, long param3) {
                downloadBitratePerSecond = dvbps + dabps;
                videoBitratePerSecond = bitrate;
                videoFrameRatePerSecond = fps;
            }
        });

        try {
            mLiveCloudPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            Logger.e(TAG, e.getMessage());
            Toast.makeText(this, "prepareAsync 异常", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mCurIndex == 0) {
            mHandler.postDelayed(mSwtichResolution, 20000);
        }
    }

    private void stopPlay() {
        Logger.d(TAG, "stopPlay");
        if (mLiveCloudPlayer != null) {
            for (int i = 0; i < mPlayList.size(); i++) {
                if (mImageViews[i] != null) {
                    mImageViews[i].setVisibility(View.VISIBLE);
                }
            }

            if (mTextureView != null) {
                mTextureView.stopRender();
            }
            mLiveCloudPlayer.stop();
            mLiveCloudPlayer.release();
            mLiveCloudPlayer = null;

            mHandler.removeCallbacks(mSwtichResolution);
        }
    }

    private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {

        private int direction = 0;// 方向
        private int oldOffset;
        private boolean isScrolling = false;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (isScrolling) {
                if (oldOffset > positionOffsetPixels) {
                    // 右
                    direction = 1;
                } else if (oldOffset < positionOffsetPixels) {
                    // 左
                    direction = 2;
                } else {
                    direction = 0;
                }
                oldOffset = positionOffsetPixels;
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (direction == 2) {
                if (++mCurIndex > mImageViews.length - 1) {
                    mCurIndex = 0;
                }
            } else if (direction == 1) {
                if (--mCurIndex < 0) {
                    mCurIndex = mImageViews.length - 1;
                }
            } else {
                return;
            }

            startPlay(mCurIndex);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            switch (state) {
                case ViewPager.SCROLL_STATE_DRAGGING:
                    isScrolling = true;
                    break;
                case ViewPager.SCROLL_STATE_IDLE:
                    isScrolling = false;
                    break;
                case ViewPager.SCROLL_STATE_SETTLING:
                    isScrolling = false;
                    break;
            }
        }
    };

    private void showLog() {
        Map<String, Object> mediaInformationMap = mLiveCloudPlayer != null ? mLiveCloudPlayer.getMediaInformation() : null;

        logList.clear();

        if (mVibrator == null) {
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        logList.add("版本号: " + QHVCSdk.getInstance().getVersion());
        String url = mPlayerUrls[mCurrentRes];
        if (mediaInformationMap.containsKey(QHVCPlayer.KEY_MEDIA_INFO_REAL_URL_STRING)) {
            url = (String) mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_REAL_URL_STRING);
        }

        logList.add("播放url: " + url);
        logList.add("分辨率: " + videoWidth + "*" + videoHeight);
        logList.add("码率: " + videoBitratePerSecond / 1024 + "k");
        logList.add("帧率: " + videoFrameRatePerSecond);
        if (mediaInformationMap != null && !mediaInformationMap.isEmpty()) {
            logList.add("音频格式: " + mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_AUDIO_FORMAT_STRING));
            logList.add("音频采样率: " + mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_AUDIO_SAMPLE_RATE_INT));
            logList.add("音频轨道: " + mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_AUDIO_CHANNEL_INT));
            logList.add("视频编码格式: " + mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_VIDEO_FORMAT_STRING));
        } else {
            logList.add("音频格式: ");
            logList.add("音频采样率: ");
            logList.add("音频轨道: ");
            logList.add("视频编码格式: ");
        }
        logList.add("Player下行流量: " + downloadBitratePerSecond / 1024 + "k");

        if (mediaInformationMap.containsKey(QHVCPlayer.KEY_MEDIA_INFO_P2P_DOWNLOAD_SPEED_LONG)) {
            long p2pDownSpeed = (long) mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_P2P_DOWNLOAD_SPEED_LONG) / 1024;
            logList.add("P2P下载数据: " + p2pDownSpeed + "k");
            if (p2pDownSpeed > 0) {
                long[] pattern = {
                        500, 1000, 500, 1000
                }; // 停止 开启 停止 开启
                mVibrator.vibrate(pattern, -1);
            } else {
                mVibrator.cancel();
            }
        }

        if (mediaInformationMap.containsKey(QHVCPlayer.KEY_MEDIA_INFO_CDN_DOWNLOAD_SPEED_LONG)) {
            long cdnDownSpeed = (long) mediaInformationMap.get(QHVCPlayer.KEY_MEDIA_INFO_CDN_DOWNLOAD_SPEED_LONG) / 1024;
            logList.add("CDN下载数据: " + cdnDownSpeed + "k");

        }

        logList.add("网络类型: " + NetUtil.getNetWorkTypeToString(this));

        logAdapter.setList(logList);
        logAdapter.notifyDataSetChanged();
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        private int mProgress = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            Logger.v(TAG, "onProgressChanged mSeekBar=" + seekBar + " progress=" + progress + " fromUser=" + fromUser);
            mProgress = progress;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Logger.v(TAG, "onStartTrackingTouch mSeekBar=" + seekBar);
            mProgress = 0;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Logger.v(TAG, "onStopTrackingTouch mSeekBar=" + seekBar + " mProgress=" + mProgress);
            if (mLiveCloudPlayer != null) {
                mLiveCloudPlayer.seekTo(mLiveCloudPlayer.getDuration() * mProgress / 100);
            }
        }
    };

    private class MyAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            if (mImageViews == null) {
                return 0;
            }
            return mImageViews.length;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            Logger.v(TAG, "destroyItem position : " + position % mImageViews.length);
            ((VerticalViewPager) container).removeView(mImageViews[position % mImageViews.length]);
        }

        @Override
        public Object instantiateItem(View container, int position) {
            Logger.v(TAG, "instantiateItem position : " + position % mImageViews.length);
            ((VerticalViewPager) container).addView(mImageViews[position % mImageViews.length], 0);
            ImageView imageView = mImageViews[position % mImageViews.length];
            imageView.setBackgroundColor(0xFF000000);

            return imageView;
        }
    }

    Runnable mSwtichResolution = new Runnable() {
        @Override
        public void run() {
            mLiveCloudPlayer.switchResolution(1, new IQHVCPlayerAdvanced.QHVCSwitchResolutionListener() {
                @Override
                public void onPrepare() {
                }

                @Override
                public void onStart() {
                }

                @Override
                public void onSuccess(final int index, final String url) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCurrentRes = index;
                            Toast.makeText(P2PPlayerActivity.this, "切换分辨率成功", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(final int errorCode, final String errorMsg) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(P2PPlayerActivity.this, "切换分辨率失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    };
}
