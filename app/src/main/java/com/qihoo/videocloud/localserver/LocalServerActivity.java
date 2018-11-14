
package com.qihoo.videocloud.localserver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.localserver.base.BaseLocalServerActivity;
import com.qihoo.videocloud.localserver.data.LocalServerVideoList;
import com.qihoo.videocloud.localserver.player.LocalServerPlayerActivity;
import com.qihoo.videocloud.localserver.setting.LocalServerSettingActivity;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by LeiXiaojun on 2017/9/21.
 */
public class LocalServerActivity extends BaseLocalServerActivity implements View.OnClickListener {

    protected ImageLoader mImageLoader = ImageLoader.getInstance();
    private boolean mInstanceStateSaved;

    private AtomicBoolean mInitLocalServer = new AtomicBoolean(false);
    private LocalServerVideoListAdapter mLocalServerVideoListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.localserver_activity);

        findViewById(R.id.header_left_icon).setOnClickListener(this);
        findViewById(R.id.header_right_icon).setOnClickListener(this);

        mLocalServerVideoListAdapter = new LocalServerVideoListAdapter(this, mImageLoader, LocalServerVideoList.getList());
        mLocalServerVideoListAdapter.notifyDataSetChanged();

        ListView mListView = (ListView) findViewById(R.id.vod_list);
        mListView.setAdapter(mLocalServerVideoListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!checkLocalServerValid()) {
                    return;
                }

                int curPlayPos = -1;
                if (mLocalServerVideoListAdapter != null) {
                    curPlayPos = mLocalServerVideoListAdapter.stopPlay();
                }
                Intent intent = new Intent(LocalServerActivity.this, LocalServerPlayerActivity.class);
                intent.putExtra("list", (Serializable) mLocalServerVideoListAdapter.getObjects());
                intent.putExtra("id", position);
                if (curPlayPos > 0) {
                    intent.putExtra("curPlayPos", curPlayPos);
                }
                LocalServerActivity.this.startActivity(intent);
            }
        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch (scrollState) {
                    //停止滚动
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE: {
                        mLocalServerVideoListAdapter.setScrollState(false);

                    }
                        break;
                    //滚动做出了抛的动作
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING: {
                        mLocalServerVideoListAdapter.setScrollState(true);

                    }
                        break;
                    //正在滚动
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL: {
                        mLocalServerVideoListAdapter.setScrollState(true);

                    }
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (totalItemCount <= 1) {
                    return;
                }
                if (visibleItemCount > 0) {
                    mLocalServerVideoListAdapter.setVisibleItem(firstVisibleItem, visibleItemCount);
                }
                onScrollCheck(view, totalItemCount, mLocalServerVideoListAdapter.getCount());
            }

            protected void onScrollCheck(AbsListView view, int totalItemCount, int dataCount) {

                int headCount = ((ListView) view).getHeaderViewsCount();
                int footerCount = ((ListView) view).getFooterViewsCount();

                if (mLocalServerVideoListAdapter != null && totalItemCount > (headCount + footerCount)) {
                    int position = mLocalServerVideoListAdapter.getCurIndex();
                    if (position != -1) {
                        int startIndex = view.getFirstVisiblePosition() - headCount;
                        int endIndex = view.getLastVisiblePosition() - headCount;
                        if (view.getLastVisiblePosition() > dataCount + headCount - 1) {
                            endIndex = dataCount - 1;
                        }
                        if (position < startIndex || position > endIndex) {
                            mLocalServerVideoListAdapter.stopPlay();
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mInstanceStateSaved) {
            mImageLoader.stop();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mInstanceStateSaved = true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.header_left_icon: {
                playerStop();
                finish();
                break;
            }
            case R.id.header_right_icon: {
                Intent intent = new Intent(this, LocalServerSettingActivity.class);
                startActivity(intent);
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        Logger.d("LocalServer", "onBackPressed()");
        playerStop();
        super.onBackPressed();
    }

    private void playerStop() {
        if (mLocalServerVideoListAdapter != null) {
            mLocalServerVideoListAdapter.stopPlay();
            mLocalServerVideoListAdapter.cancelPrecacheAll();
        }
    }
}
