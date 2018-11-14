
package com.qihoo.videocloud.p2p;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.p2p.base.BaseP2PActivity;
import com.qihoo.videocloud.p2p.data.P2PVideoList;
import com.qihoo.videocloud.p2p.setting.P2PSettingActivity;
import com.qihoo.videocloud.p2p.setting.P2PSettingConfig;

import java.io.Serializable;

public class P2PActivity extends BaseP2PActivity implements View.OnClickListener {

    protected ImageLoader mImageLoader = ImageLoader.getInstance();
    private boolean mInstanceStateSaved;

    private P2PVideoListAdapter mP2PVideoListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.p2p_activity);

        findViewById(R.id.header_left_icon).setOnClickListener(this);
        findViewById(R.id.header_right_icon).setOnClickListener(this);

        mP2PVideoListAdapter = new P2PVideoListAdapter(this, mImageLoader, P2PVideoList.getList());
        mP2PVideoListAdapter.notifyDataSetChanged();

        ListView mListView = (ListView) findViewById(R.id.vod_list);
        mListView.setAdapter(mP2PVideoListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!checkP2PValid()) {
                    return;
                }

                int curPlayPos = -1;
                if (mP2PVideoListAdapter != null) {
                    curPlayPos = mP2PVideoListAdapter.stopPlay();
                }
                Intent intent = new Intent(P2PActivity.this, P2PActivity.class);
                intent.putExtra("list", (Serializable) mP2PVideoListAdapter.getObjects());
                intent.putExtra("id", position);
                if (curPlayPos > 0) {
                    intent.putExtra("curPlayPos", curPlayPos);
                }
                P2PActivity.this.startActivity(intent);
            }
        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch (scrollState) {
                    //停止滚动
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE: {
                        mP2PVideoListAdapter.setScrollState(false);

                    }
                        break;
                    //滚动做出了抛的动作
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING: {
                        mP2PVideoListAdapter.setScrollState(true);

                    }
                        break;
                    //正在滚动
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL: {
                        mP2PVideoListAdapter.setScrollState(true);

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
                    mP2PVideoListAdapter.setVisibleItem(firstVisibleItem, visibleItemCount);
                }
                onScrollCheck(view, totalItemCount, mP2PVideoListAdapter.getCount());
            }

            protected void onScrollCheck(AbsListView view, int totalItemCount, int dataCount) {

                int headCount = ((ListView) view).getHeaderViewsCount();
                int footerCount = ((ListView) view).getFooterViewsCount();

                if (mP2PVideoListAdapter != null && totalItemCount > (headCount + footerCount)) {
                    int position = mP2PVideoListAdapter.getCurIndex();
                    if (position != -1) {
                        int startIndex = view.getFirstVisiblePosition() - headCount;
                        int endIndex = view.getLastVisiblePosition() - headCount;
                        if (view.getLastVisiblePosition() > dataCount + headCount - 1) {
                            endIndex = dataCount - 1;
                        }
                        if (position < startIndex || position > endIndex) {
                            mP2PVideoListAdapter.stopPlay();
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
                Intent intent = new Intent(this, P2PSettingActivity.class);
                startActivity(intent);
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        Logger.d(P2PSettingConfig.TAG, "onBackPressed()");
        playerStop();
        super.onBackPressed();
    }

    private void playerStop() {
        if (mP2PVideoListAdapter != null) {
            mP2PVideoListAdapter.stopPlay();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mP2PVideoListAdapter != null) {
            mP2PVideoListAdapter.onResume();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mP2PVideoListAdapter != null) {
            mP2PVideoListAdapter.onPause();
        }
    }

}
