
package com.qihoo.videocloud.localserver.download;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.localserver.VideoItemData;
import com.qihoo.videocloud.localserver.base.BaseLocalServerActivity;
import com.qihoo.videocloud.localserver.player.LocalServerPlayerActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LeiXiaojun on 2017/9/27.
 */
public class LocalServerDownloadActivity extends BaseLocalServerActivity implements View.OnClickListener {

    private DownloadListAdapter mDownloadListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.localserver_download_activity);

        initView();
        initData();

        DownloadManager.getInstance().addDataObserver(mDataObserver);
    }

    @Override
    protected void onDestroy() {
        DownloadManager.getInstance().removeDataObserver(mDataObserver);

        super.onDestroy();
    }

    private void initView() {
        findViewById(R.id.header_left_icon).setOnClickListener(this);

        mDownloadListAdapter = new DownloadListAdapter(this);

        final ListView downloadList = (ListView) findViewById(R.id.download_list);
        downloadList.setAdapter(mDownloadListAdapter);
        downloadList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                DownloadTask downloadTask = (DownloadTask) mDownloadListAdapter.getItem(position);
                if (downloadTask.state != DownloadConstant.State.STATE_DOWNLOADED) {
                    return;
                }

                ArrayList<VideoItemData> data = new ArrayList<>();
                int index = 0;

                List<DownloadTask> downloadTasks = DownloadManager.getInstance().getTaskList();
                for (int i = 0, count = downloadTasks.size(); i < count; i++) {
                    DownloadTask tempDownloadTask = downloadTasks.get(i);
                    if (tempDownloadTask.state == DownloadConstant.State.STATE_DOWNLOADED) {
                        VideoItemData videoItemData = new VideoItemData();
                        videoItemData.setRid(tempDownloadTask.rid);
                        videoItemData.setUrl(tempDownloadTask.file);
                        data.add(videoItemData);
                    }

                    if (tempDownloadTask == downloadTask) {
                        index = data.size() - 1;
                    }
                }

                Intent intent = new Intent(LocalServerDownloadActivity.this, LocalServerPlayerActivity.class);
                intent.putExtra("list", data);
                intent.putExtra("id", index);
                LocalServerDownloadActivity.this.startActivity(intent);
            }
        });
    }

    private void initData() {

    }

    private DownloadManager.DataObserver mDataObserver = new DownloadManager.DataObserver() {
        @Override
        public void onDataObserver() {
            if (isFinishing()) {
                return;
            }

            mDownloadListAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.header_left_icon: {
                finish();
                break;
            }
        }
    }
}
