
package com.qihoo.videocloud.p2p;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.qihoo.livecloud.play.callback.PlayerCallback;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.IQHVCPlayer;
import com.qihoo.videocloud.IQHVCPlayerAdvanced;
import com.qihoo.videocloud.QHVCPlayer;
import com.qihoo.videocloud.p2p.player.P2PPlayerActivity;
import com.qihoo.videocloud.p2p.setting.P2PSettingConfig;
import com.qihoo.videocloud.utils.Utils;
import com.qihoo.videocloud.view.QHVCTextureView;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class P2PVideoListAdapter extends BaseAdapter {

    private static final String TAG = P2PVideoListAdapter.class.getSimpleName();
    private static final boolean ENABLE_CACHE_WHEN_PAUSE = true;
    // 控制浮层
    private static final int LAYER_PLAYING = 0;
    private static final int LAYER_PAUSE = 1;
    private static final int LAYER_NON = 2;

    private String mCid;
    private Context mContext;
    private ImageLoader mImageLoader;
    private List<P2PVideoItemData> mObjects;
    private IQHVCPlayerAdvanced mQHVCPlayer;

    // current play view
    private volatile int mCurIndex = -1;
    private volatile QHVCTextureView mCurPlayView;
    private volatile ViewHolder mCurViewHolder;
    private int mCurSeekProgress;

    //定义当前listview是否在滑动状态
    private boolean mScrollState = false;
    private int mFirstVisibleItem;
    private int mVisibleItemCount;

    public P2PVideoListAdapter(Context context, ImageLoader imageLoader, List<P2PVideoItemData> mObjects) {
        this.mContext = context;
        this.mImageLoader = imageLoader;
        this.mObjects = mObjects;
        this.mCid = context.getResources().getString(R.string.config_player_vod_cid);
    }

    public void setScrollState(boolean scrollState) {
        this.mScrollState = scrollState;
    }

    public void setVisibleItem(int firstVisibleItem, int visibleItemCount) {
        mFirstVisibleItem = firstVisibleItem;
        mVisibleItemCount = visibleItemCount;
    }

    @Override
    public int getCount() {
        return mObjects != null ? mObjects.size() : 0;
    }

    @Override
    public P2PVideoItemData getItem(int position) {
        return mObjects != null ? mObjects.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.p2p_video_list_item, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        initializeViews(position, getItem(position), holder);
        return convertView;
    }

    private void initializeViews(int position, P2PVideoItemData itemData, final ViewHolder holder) {
        if (itemData == null) {
            return;
        }

        holder.position = position;

        if (!TextUtils.isEmpty(itemData.getTitle())) {
            holder.tvTitle.setText(itemData.getTitle());
        } else {
            holder.tvTitle.setText(itemData.getRid());
        }
        if (!TextUtils.isEmpty(itemData.getWatchCount())) {
            holder.tvWatchCount.setText(itemData.getWatchCount());
        }
        if (!TextUtils.isEmpty(itemData.getDuration())) {
            holder.tvVideoDuration.setText(itemData.getDuration());
        }
        if (!TextUtils.isEmpty(itemData.getImage())) {
            holder.ivCoverPage.setImageDrawable(null);
            mImageLoader.displayImage(itemData.getImage(), holder.ivCoverPage);
        }

        holder.ivPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mQHVCPlayer != null && mQHVCPlayer.isPlaying()) {
                    mQHVCPlayer.pause();
                    setPlayLayer(LAYER_PAUSE);
                }
            }
        });

        holder.ivPlay.setTag(position);
        holder.ivPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.position != mCurIndex) {
                    P2PVideoItemData videoItemData = getItem(holder.position);
                    if (!TextUtils.isEmpty(videoItemData.getUrl())) {

                        startPlay(holder.position, videoItemData.getUrl(), videoItemData.getRid(), holder.playView);

                        mCurIndex = holder.position;
                        mCurPlayView = holder.playView;
                        mCurViewHolder = holder;

                    }
                } else {
                    if (mQHVCPlayer != null && mQHVCPlayer.isPaused()) {
                        mQHVCPlayer.start();

                        setPlayLayer(LAYER_PLAYING);
                    }
                }
            }
        });

        holder.sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCurSeekProgress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mCurSeekProgress = 0;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mQHVCPlayer != null) {
                    mQHVCPlayer.seekTo((mQHVCPlayer.getDuration() * mCurSeekProgress) / 100, true);
                }
            }
        });

        holder.playView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.d(TAG, "playview click");
                if (mQHVCPlayer != null) {
                    if (mQHVCPlayer.isPlaying()) {
                        setPlayLayer(LAYER_PLAYING);
                    } else {
                        setPlayLayer(LAYER_PAUSE);
                    }
                }
            }
        });
        holder.layoutLayerPlaying.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.d(TAG, "playing layout click");
                zoom(holder.position);
            }
        });
        holder.layoutLayerPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.d(TAG, "pause layout click");
                zoom(holder.position);
            }
        });

        holder.ivZoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoom(holder.position);
            }
        });

        if (mScrollState) {
        }
    }

    public List<P2PVideoItemData> getObjects() {
        return mObjects;
    }

    private static class ViewHolder {
        int position;

        QHVCTextureView playView;
        TextView tvTitle;
        ImageView ivCoverPage;

        /* for playing status */
        RelativeLayout layoutLayerPlaying;
        ImageView ivPause;
        TextView tvPlayTime;
        SeekBar sbProgress;
        TextView tvDuration;
        ImageView ivZoom;

        /* for pause status */
        RelativeLayout layoutLayerPause;
        ImageView ivPlay;
        TextView tvWatchCount;
        TextView tvVideoDuration;

        public ViewHolder(View v) {
            this.playView = (QHVCTextureView) v.findViewById(R.id.playView);
            this.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            this.ivCoverPage = (ImageView) v.findViewById(R.id.iv_cover_page);
            //-------------------------------------playing-------------------------------------
            this.layoutLayerPlaying = (RelativeLayout) v.findViewById(R.id.layout_layer_playing);
            this.ivPause = (ImageView) v.findViewById(R.id.iv_pause);
            this.tvPlayTime = (TextView) v.findViewById(R.id.tv_play_time);
            this.sbProgress = (SeekBar) v.findViewById(R.id.sb_progress);
            this.tvDuration = (TextView) v.findViewById(R.id.tv_duration);
            this.ivZoom = (ImageView) v.findViewById(R.id.iv_zoom);
            //------------------------------------pause--------------------------------------
            this.layoutLayerPause = (RelativeLayout) v.findViewById(R.id.layout_layer_pause);
            //            this.layoutLayerPause.setOnClickListener(this);
            this.ivPlay = (ImageView) v.findViewById(R.id.iv_play);
            this.tvWatchCount = (TextView) v.findViewById(R.id.tv_watch_count);
            this.tvVideoDuration = (TextView) v.findViewById(R.id.tv_video_duration);
        }
    }

    private void setPlayLayer(int layer) {
        if (layer == LAYER_PAUSE) {
            if (mCurViewHolder != null) {
                mCurViewHolder.layoutLayerPause.setVisibility(View.VISIBLE);
                mCurViewHolder.layoutLayerPlaying.setVisibility(View.GONE);
            }
        } else if (layer == LAYER_PLAYING) {
            if (mCurViewHolder != null) {
                mCurViewHolder.layoutLayerPause.setVisibility(View.GONE);
                mCurViewHolder.layoutLayerPlaying.setVisibility(View.VISIBLE);
            }
        } else {
            if (mCurViewHolder != null) {
                mCurViewHolder.layoutLayerPause.setVisibility(View.GONE);
                mCurViewHolder.layoutLayerPlaying.setVisibility(View.GONE);
            }
        }
    }

    public int getCurIndex() {
        return mCurIndex;
    }

    //---------------------------------------  play control --------------------------------------
    private void startPlay(int position, String url, String resId, final QHVCTextureView playView) {
        if (url.contains("?")) {
            url += "&time=" + System.currentTimeMillis();
        } else {
            url += "?time=" + System.currentTimeMillis();
        }

        if (mCurViewHolder != null && mCurViewHolder.sbProgress != null) {
            mCurViewHolder.sbProgress.setProgress(0);
            mCurViewHolder.sbProgress.setSecondaryProgress(0);
        }

        stopPlay();

        Logger.d(TAG, "startPlay mCurIndex : " + position);
        Logger.d(TAG, "startPlay url = " + url);

        QHVCPlayer.enableP2PUpload(P2PSettingConfig.ENABLE_P2P_UPLOAD);

        mQHVCPlayer = new QHVCPlayer(mContext);
        mQHVCPlayer.enableP2P(P2PSettingConfig.ENABLE_P2P);
        playView.onPlay();
        playView.setPlayer(mQHVCPlayer);

        mQHVCPlayer.setDisplay(playView);
        mQHVCPlayer.setSurfaceViewport(0, 0, playView.getWidth(), playView.getHeight());
        try {
            Map<String, Object> options = new HashMap<>();
            options.put(IQHVCPlayerAdvanced.KEY_OPTION_FORCE_P2P, false);
            mQHVCPlayer.setDataSource(IQHVCPlayer.PLAYTYPE_VOD, url, mCid, options);
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
            Toast.makeText(mContext, "数据源异常", Toast.LENGTH_SHORT).show();
            return;
        }

        mQHVCPlayer.setOnPreparedListener(new IQHVCPlayer.OnPreparedListener() {
            @Override
            public void onPrepared() {
                Logger.d(TAG, "onPrepared");
                mQHVCPlayer.start();
            }
        });

        mQHVCPlayer.setOnInfoListener(new IQHVCPlayer.OnInfoListener() {
            @Override
            public void onInfo(int handle, int what, int extra) {
                Logger.d(TAG, "onInfo handle: " + handle + " what : " + what + " extra : " + extra);

                if (what == IQHVCPlayer.INFO_LIVE_PLAY_START) {
                    if (mQHVCPlayer != null && mCurViewHolder != null) {
                        mCurViewHolder.ivCoverPage.setVisibility(View.GONE);
                        mCurViewHolder.playView.setVisibility(View.VISIBLE);

                        setPlayLayer(LAYER_PLAYING);
                    }
                } else if (what == IQHVCPlayer.INFO_PLAY_H265) {
                    Logger.d(TAG, "播放H265");
                } else if (what == IQHVCPlayer.INFO_DEVICE_RENDER_ERR) {

                    // err
                    if (Logger.LOG_ENABLE) {
                        Logger.e(TAG, "dvrender err");
                    }
                } else if (what == IQHVCPlayer.INFO_DEVICE_RENDER_QUERY_SURFACE) {

                    if (playView != null) {
                        if (mQHVCPlayer != null && !mQHVCPlayer.isPaused()) {
                            playView.render_proc(PlayerCallback.DEVICE_RENDER_QUERY_SURFACE, 0/*不使用此变量*/);
                        }
                    }
                } else if (what == IQHVCPlayer.INFO_RENDER_RESET_SURFACE) {

                    if (playView != null) {
                        playView.pauseSurface();
                    }
                }
            }
        });

        mQHVCPlayer.setOnErrorListener(new IQHVCPlayer.OnErrorListener() {
            @Override
            public boolean onError(int handle, int what, int extra) {
                Toast.makeText(mContext, "播放失败：what=" + what + ", extra=" + extra, Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        mQHVCPlayer.setOnProgressChangeListener(new IQHVCPlayer.onProgressChangeListener() {
            @Override
            public void onProgressChange(int handle, int total, int progress) {
                if (mQHVCPlayer != null && mCurViewHolder != null) {
                    int percent = (progress * 100) / total;

                    mCurViewHolder.sbProgress.setProgress(percent);

                    mCurViewHolder.tvPlayTime.setText(Utils.caluTime(progress));
                    mCurViewHolder.tvDuration.setText(Utils.caluTime(total));
                }
            }
        });

        mQHVCPlayer.setOnBufferingUpdateListener(new IQHVCPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(int handle, int percent) {
                mCurViewHolder.sbProgress.setSecondaryProgress(percent);
            }
        });

        mQHVCPlayer.setOnCompletionListener(new IQHVCPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(int handle) {
                if (mCurIndex >= 0 && mCurIndex < mObjects.size()) {

                    if (mQHVCPlayer != null) {
                        mQHVCPlayer.seekTo(0);
                    }
                }
            }
        });
        mQHVCPlayer.setOnVideoSizeChangedListener(new IQHVCPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(int handle, final int width, final int height) {
                if (mQHVCPlayer != null && mCurViewHolder != null && mCurViewHolder.playView != null) {
                    mCurViewHolder.playView.setVideoRatio((float) width / (float) height);
                    mCurViewHolder.playView.setVisibility(View.VISIBLE);
                }
            }
        });
        try {
            mQHVCPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            Logger.e(TAG, e.getMessage());
            Toast.makeText(mContext, "prepareAsync 异常", Toast.LENGTH_SHORT).show();
        }
    }

    public int stopPlay() {
        Logger.d(TAG, "stopPlay");

        int currentPos = -1;
        if (mQHVCPlayer != null) {
            if (mCurViewHolder != null) {
                mCurViewHolder.ivCoverPage.setVisibility(View.VISIBLE);
            }

            if (mCurPlayView != null) {
                mCurPlayView.setVisibility(View.GONE);
            }

            currentPos = mQHVCPlayer.getCurrentPosition();
            mQHVCPlayer.stop();
            mQHVCPlayer.release();
            mQHVCPlayer = null;
        }
        setPlayLayer(LAYER_PAUSE);

        mCurIndex = -1;
        mCurViewHolder = null;
        mCurPlayView = null;

        return currentPos;
    }

    private void zoom(int position) {
        Intent intent = new Intent(mContext, P2PPlayerActivity.class);
        intent.putExtra("list", (Serializable) getObjects());
        intent.putExtra("id", position);
        int curPlayPos = stopPlay();
        if (curPlayPos > 0) {
            intent.putExtra("curPlayPos", curPlayPos);
        }
        mContext.startActivity(intent);
    }

    private void download(int position) {
    }

    protected void onResume() {
        if (mQHVCPlayer != null) {
            try {
                mQHVCPlayer.disableRender(false);
            } catch (IllegalStateException e) {
                Logger.e(TAG, e.getMessage());
            }
        }
        if (mCurPlayView != null) {
            mCurPlayView.resumeSurface();
        }
    }

    protected void onPause() {
        if (mQHVCPlayer != null) {
            try {
                mQHVCPlayer.disableRender(true);
            } catch (IllegalStateException e) {
                Logger.e(TAG, e.getMessage());
            }
        }
        if (mCurPlayView != null) {
            mCurPlayView.pauseSurface();
        }
    }

}
