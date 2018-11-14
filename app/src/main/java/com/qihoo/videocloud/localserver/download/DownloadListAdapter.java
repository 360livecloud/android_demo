
package com.qihoo.videocloud.localserver.download;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.view.ConfirmDialog;

import java.util.Locale;

/**
 * Created by Administrator on 2017/9/27.
 */
public class DownloadListAdapter extends BaseAdapter {

    private Context mContext;

    private DownloadManager mDownloadManager;

    public DownloadListAdapter(Context context) {
        mContext = context;
        mDownloadManager = DownloadManager.getInstance();
    }

    @Override
    public int getCount() {
        return mDownloadManager.getTaskList().size();
    }

    @Override
    public Object getItem(int position) {
        return mDownloadManager.getTaskList().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        final ViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.localserver_download_list_item, null);
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        DownloadTask downloadTask = (DownloadTask) getItem(position);

        viewHolder.position = position;
        viewHolder.rid.setText(downloadTask.rid);
        viewHolder.progress.setVisibility(downloadTask.state == DownloadConstant.State.STATE_DOWNLOADING ||
                downloadTask.state == DownloadConstant.State.STATE_PAUSED ? View.VISIBLE : View.GONE);
        viewHolder.progress.setProgress(downloadTask.total != 0 ? (int) ((float) downloadTask.position / downloadTask.total * 100) : 0);
        viewHolder.state.setText(getStateDesc(downloadTask.state, downloadTask.errCode, downloadTask.errMsg));
        viewHolder.speed.setVisibility(downloadTask.state == DownloadConstant.State.STATE_DOWNLOADING ? View.VISIBLE : View.GONE);
        viewHolder.speed.setText(String.format(Locale.getDefault(), "%d kb/s", (int) (downloadTask.speed / 1024)));
        viewHolder.total.setText(String.format(Locale.getDefault(), "%.2f M", (float) downloadTask.total / 1024 / 1024));
        viewHolder.resume.setVisibility(downloadTask.state == DownloadConstant.State.STATE_WAITING ||
                downloadTask.state == DownloadConstant.State.STATE_PAUSED ||
                downloadTask.state == DownloadConstant.State.STATE_FAILED ? View.VISIBLE : View.GONE);
        viewHolder.resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DownloadTask task = (DownloadTask) getItem(viewHolder.position);
                switch (task.state) {
                    case DownloadConstant.State.STATE_WAITING: {
                        mDownloadManager.cachePersistence(task);
                        break;
                    }
                    case DownloadConstant.State.STATE_PAUSED:
                    case DownloadConstant.State.STATE_FAILED: {
                        mDownloadManager.resumeCachePersistence(task);
                        break;
                    }
                }
            }
        });
        viewHolder.pause.setVisibility(downloadTask.state == DownloadConstant.State.STATE_DOWNLOADING ? View.VISIBLE : View.GONE);
        viewHolder.pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DownloadTask task = (DownloadTask) getItem(viewHolder.position);
                mDownloadManager.pauseCachePersistence(task);
            }
        });
        viewHolder.wait.setVisibility(View.GONE);
        viewHolder.wait.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO
            }
        });
        viewHolder.delete.setVisibility(View.VISIBLE);
        viewHolder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final ConfirmDialog dialog = new ConfirmDialog(mContext);
                dialog.setContent(R.string.str_download_delete_content);
                dialog.setCheckout(R.string.str_download_delete_checkout);
                dialog.setChecked(true);
                dialog.setOnOkButtonOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DownloadTask task = (DownloadTask) getItem(viewHolder.position);
                        mDownloadManager.cancelCachePersistence(task, dialog.isChecked());
                    }
                });
                dialog.show();
            }
        });

        return view;
    }

    private String getStateDesc(int state, int errCode, String errMsg) {
        String ret;
        switch (state) {
            case DownloadConstant.State.STATE_WAITING: {
                ret = "等待下载";
                break;
            }
            case DownloadConstant.State.STATE_DOWNLOADING: {
                ret = "正在下载";
                break;
            }
            case DownloadConstant.State.STATE_PAUSED: {
                ret = "暂停下载";
                break;
            }
            case DownloadConstant.State.STATE_DOWNLOADED: {
                ret = "下载成功";
                break;
            }
            case DownloadConstant.State.STATE_FAILED: {
                ret = String.format(Locale.getDefault(), "下载失败(%d): %s", errCode, errMsg);
                break;
            }
            default: {
                ret = "未知";
                break;
            }
        }

        return ret;
    }

    private class ViewHolder {
        int position;

        TextView rid;
        ProgressBar progress;
        TextView state;
        TextView speed;
        TextView total;
        ImageView resume;
        ImageView pause;
        ImageView wait;
        ImageView delete;

        ViewHolder(View view) {
            rid = (TextView) view.findViewById(R.id.rid);
            progress = (ProgressBar) view.findViewById(R.id.progress);
            state = (TextView) view.findViewById(R.id.state);
            speed = (TextView) view.findViewById(R.id.speed);
            total = (TextView) view.findViewById(R.id.total);
            resume = (ImageView) view.findViewById(R.id.resume);
            pause = (ImageView) view.findViewById(R.id.pause);
            wait = (ImageView) view.findViewById(R.id.wait);
            delete = (ImageView) view.findViewById(R.id.delete);
        }
    }
}
