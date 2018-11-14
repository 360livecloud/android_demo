
package com.qihoo.videocloud.localserver;

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
import com.qihoo.videocloud.localserver.download.DownloadManager;
import com.qihoo.videocloud.localserver.download.DownloadTask;
import com.qihoo.videocloud.localserver.download.LocalServerDownloadActivity;
import com.qihoo.videocloud.localserver.player.LocalServerPlayerActivity;
import com.qihoo.videocloud.utils.Utils;
import com.qihoo.videocloud.view.QHVCTextureView;

import net.qihoo.videocloud.LocalServer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalServerVideoListAdapter extends BaseAdapter {

    private static final String TAG = LocalServerVideoListAdapter.class.getSimpleName();
    private static final boolean ENABLE_CACHE_WHEN_PAUSE = true;
    // 控制浮层
    private static final int LAYER_PLAYING = 0;
    private static final int LAYER_PAUSE = 1;
    private static final int LAYER_NON = 2;

    private String mCid;
    private Context mContext;
    private ImageLoader mImageLoader;
    private List<VideoItemData> mObjects;
    private IQHVCPlayerAdvanced mQHVCPlayer;

    private boolean mErrorOccurred = false;

    // current play view
    private volatile int mCurIndex = -1;
    private volatile QHVCTextureView mCurPlayView;
    private volatile ViewHolder mCurViewHolder;
    private int mCurSeekProgress;

    //定义当前listview是否在滑动状态
    private boolean mScrollState = false;
    private int mFirstVisibleItem;
    private int mVisibleItemCount;
    private Map<String, Integer> mPreCacheMap = new HashMap<>(32);

    public LocalServerVideoListAdapter(Context context, ImageLoader imageLoader, List<VideoItemData> mObjects) {
        this.mContext = context;
        this.mImageLoader = imageLoader;
        this.mObjects = mObjects;
        this.mCid = context.getResources().getString(R.string.config_player_vod_cid);

        preCache(0, 3);
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
    public VideoItemData getItem(int position) {
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
            convertView = View.inflate(mContext, R.layout.localserver_video_list_item, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        initializeViews(position, getItem(position), holder);
        return convertView;
    }

    private void initializeViews(int position, VideoItemData itemData, final ViewHolder holder) {
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
                    VideoItemData videoItemData = getItem(holder.position);
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
        holder.ivDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download(holder.position);
            }
        });
        holder.ivDownload2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download(holder.position);
            }
        });

        if (mScrollState) {
            cancelPrecache(mFirstVisibleItem, mVisibleItemCount);
            preCache(mFirstVisibleItem, mVisibleItemCount);
        }
    }

    public List<VideoItemData> getObjects() {
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
        ImageView ivDownload;

        /* for pause status */
        RelativeLayout layoutLayerPause;
        ImageView ivPlay;
        TextView tvWatchCount;
        TextView tvVideoDuration;
        ImageView ivDownload2;

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
            this.ivDownload = (ImageView) v.findViewById(R.id.iv_download);
            //------------------------------------pause--------------------------------------
            this.layoutLayerPause = (RelativeLayout) v.findViewById(R.id.layout_layer_pause);
            //            this.layoutLayerPause.setOnClickListener(this);
            this.ivPlay = (ImageView) v.findViewById(R.id.iv_play);
            this.tvWatchCount = (TextView) v.findViewById(R.id.tv_watch_count);
            this.tvVideoDuration = (TextView) v.findViewById(R.id.tv_video_duration);
            this.ivDownload2 = (ImageView) v.findViewById(R.id.iv_download2);
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
        if (mCurViewHolder != null && mCurViewHolder.sbProgress != null) {
            mCurViewHolder.sbProgress.setProgress(0);
            mCurViewHolder.sbProgress.setSecondaryProgress(0);
        }
        stopPlay();

        String newUrl = LocalServer.getPlayUrl(resId, url);
        Logger.d(TAG, "startPlay mCurIndex : " + position);
        Logger.d(TAG, "startPlay url = " + url);

        LocalServer.enableCache(ENABLE_CACHE_WHEN_PAUSE);

        mQHVCPlayer = new QHVCPlayer(mContext);
        playView.onPlay();
        playView.setPlayer(mQHVCPlayer);
        mQHVCPlayer.setDisplay(playView);
        mQHVCPlayer.setSurfaceViewport(0, 0, playView.getWidth(), playView.getHeight());
        try {
            Map<String, Object> options = new HashMap<>();
            mQHVCPlayer.setDataSource(IQHVCPlayer.PLAYTYPE_VOD, new String[] {
                    resId
            }, new String[] {
                    newUrl
            }, 0, mCid, "", options);
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
                mErrorOccurred = true;
                return false;
            }
        });

        mQHVCPlayer.setOnProgressChangeListener(new IQHVCPlayer.onProgressChangeListener() {
            @Override
            public void onProgressChange(int handle, int total, int progress) {
                if (mQHVCPlayer != null && mCurViewHolder != null) {
                    mCurViewHolder.sbProgress.setProgress((progress * 100) / total);

                    mCurViewHolder.tvPlayTime.setText(Utils.caluTime(progress));
                    mCurViewHolder.tvDuration.setText(Utils.caluTime(total));
                }
            }
        });

        mQHVCPlayer.setOnBufferingUpdateListener(new IQHVCPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(int handle, int percent) {
                if (mQHVCPlayer != null && mCurViewHolder != null) {
                    mCurViewHolder.sbProgress.setSecondaryProgress(percent);
                }
            }
        });

        mQHVCPlayer.setOnCompletionListener(new IQHVCPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(int handle) {
                if (mCurIndex >= 0 && mCurIndex < mObjects.size()) {

                    if (mQHVCPlayer != null && !mErrorOccurred) {
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
                mCurPlayView.stopRender();
                mCurPlayView.setPlayer(null);
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

        mErrorOccurred = false;
        return currentPos;
    }

    private void zoom(int position) {
        Intent intent = new Intent(mContext, LocalServerPlayerActivity.class);
        intent.putExtra("list", (Serializable) getObjects());
        intent.putExtra("id", position);
        int curPlayPos = stopPlay();
        if (curPlayPos > 0) {
            intent.putExtra("curPlayPos", curPlayPos);
        }
        mContext.startActivity(intent);
    }

    private void download(int position) {
        VideoItemData tempVideoItemData = getItem(position);

        String url = tempVideoItemData.getUrl();
        if (TextUtils.isEmpty(url)) {
            return;
        }

        DownloadTask downloadTask = new DownloadTask();
        downloadTask.rid = tempVideoItemData.getRid();
        downloadTask.url = url;
        downloadTask.file = Utils.getDownloadDir() + downloadTask.rid;
        boolean ret = DownloadManager.getInstance().cachePersistence(downloadTask);
        if (ret) {
            Intent intent = new Intent(mContext, LocalServerDownloadActivity.class);
            mContext.startActivity(intent);
        }

        Toast.makeText(mContext, ret ? R.string.str_download_add_success : R.string.str_download_add_failed, Toast.LENGTH_SHORT).show();
    }

    private void preCache(int firstItemIndex, int visibleCount) {
        int size = mObjects.size();
        for (int i = firstItemIndex; i <= (firstItemIndex + visibleCount) && i < size; i++) {

            VideoItemData o = mObjects.get(i);
            if (o != null && !TextUtils.isEmpty(o.getUrl()) && !TextUtils.isEmpty(o.getRid())) {

                String rid = o.getRid();
                if (i != mCurIndex && null == mPreCacheMap.get(rid)) {
                    LocalServer.doPrecache(rid, o.getUrl(), 800);
                    mPreCacheMap.put(rid, i);
                    Logger.d(TAG, "pre cache: " + i + " rid: " + rid + " url: " + o.getUrl());
                }
            }
        }
    }

    private void cancelPrecache(int firstItemIndex, int visibleCount) {

        List<String> removeKey = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : mPreCacheMap.entrySet()) {

            int value = entry.getValue();
            if (value != mCurIndex) {
                if (value < firstItemIndex || value > (firstItemIndex + visibleCount)) {
                    String key = entry.getKey();
                    Logger.d(TAG, "cancel pre cache: " + value + " rid: " + key);
                    removeKey.add(key);
                    LocalServer.cancelPrecache(key);
                }
            }
        }
        for (String key : removeKey) {
            mPreCacheMap.remove(key);
        }
    }

    public void cancelPrecacheAll() {
        for (Map.Entry<String, Integer> entry : mPreCacheMap.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            Logger.d(TAG, "cancel cache: " + value + " rid: " + key);
            LocalServer.cancelPrecache(key);
        }
    }
}
