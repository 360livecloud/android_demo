
package com.qihoo.videocloud.interactbrocast;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.interactbrocast.adapter.InteractRecyclerViewAdapter;
import com.qihoo.videocloud.interactbrocast.data.InteractGlobalManager;
import com.qihoo.videocloud.interactbrocast.main.InteractConstant;
import com.qihoo.videocloud.interactbrocast.modle.InteractRoomModel;
import com.qihoo.videocloud.interactbrocast.net.InteractServerApi;

import java.util.Collections;
import java.util.List;

public class InteracatTypeHallActivity extends Activity implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private EditText mRoomSearchEditText;
    private RecyclerView mRoomRecyclerList;
    private InteractRecyclerViewAdapter mRoomRecyclerAdapter;

    private int mRoomType;

    private boolean mSearching = false;
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    private void initView() {
        setContentView(R.layout.activity_interacat_type_hall);

        findViewById(R.id.interact_brocast_type_hall_header_left_icon).setOnClickListener(this);

        TextView titleTextView = (TextView) findViewById(R.id.interact_type_title);
        titleTextView.setText(getIntent().getStringExtra(InteractConstant.INTENT_EXTRA_INTERACT_TYPE_NAME));

        mRoomSearchEditText = (EditText) findViewById(R.id.interact_type_hall_room_id);
        findViewById(R.id.interact_type_hall_go_room).setOnClickListener(this);

        mRoomRecyclerList = (RecyclerView) findViewById(R.id.interact_type_room_list);
        mRoomRecyclerList.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration decoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        decoration.setDrawable(getResources().getDrawable(R.drawable.dash_line));
        mRoomRecyclerList.addItemDecoration(decoration);

        mRoomRecyclerAdapter = new InteractRecyclerViewAdapter(null);
        mRoomRecyclerAdapter.setOnItemClickListener(mItemClickListener);
        mRoomRecyclerList.setAdapter(mRoomRecyclerAdapter);

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setProgressBackgroundColorSchemeResource(R.color.actionbar_bg_color);
        refreshLayout.setColorSchemeColors(Color.WHITE, Color.YELLOW, Color.GREEN, Color.RED, Color.CYAN, Color.DKGRAY, Color.MAGENTA, Color.BLUE, Color.BLACK, Color.LTGRAY);

        findViewById(R.id.interact_create_room).setOnClickListener(this);
    }

    private void initData() {
        mRoomType = getIntent().getIntExtra(InteractConstant.INTENT_EXTRA_INTERACT_TYPE, InteractConstant.ROOM_TYPE_ANCHOR_AND_ANCHOR);
    }

    private void refreshData() {
        InteractServerApi.getRoomList(InteractGlobalManager.getInstance().getUser().getUserId(), mRoomType,
                new InteractServerApi.ResultCallback<List<InteractRoomModel>>() {

                    @Override
                    public void onSuccess(List<InteractRoomModel> data) {
                        if (isFinishing()) {
                            return;
                        }
                        Collections.reverse(data);/*排序*/

                        // 房间容错测试代码
                        //                        final List<InteractRoomModel> interactRoomModels = new ArrayList<>();
                        //                        for (final InteractRoomModel roomModel : data) {
                        //                            if (roomModel.getBindRoleId().compareToIgnoreCase(InteractGlobalManager.getInstance().getUser().getUserId()) == 0) {
                        //                                InteractServerApi.dismissRoom(InteractGlobalManager.getInstance().getUser().getUserId(), roomModel.getRoomId(), new InteractServerApi.ResultCallback<Void>() {
                        //                                    @Override
                        //                                    public void onSuccess(Void data) {
                        //                                        Toast.makeText(InteracatTypeHallActivity.this,"解散房间" + roomModel.getRoomId(), Toast.LENGTH_SHORT).show();
                        //                                    }
                        //
                        //                                    @Override
                        //                                    public void onFailed(int errCode, String errMsg) {
                        //                                        interactRoomModels.add(roomModel);
                        //                                        mRoomRecyclerAdapter.setData(interactRoomModels);
                        //                                    }
                        //                                });
                        //                            } else {
                        //                                InteractServerApi.userLeaveRoom(InteractGlobalManager.getInstance().getUser().getUserId(), roomModel.getRoomId(), new InteractServerApi.ResultCallback<InteractRoomModel>() {
                        //                                    @Override
                        //                                    public void onSuccess(InteractRoomModel data) {
                        //                                        Toast.makeText(InteracatTypeHallActivity.this,"离开房间" + roomModel.getRoomId(), Toast.LENGTH_SHORT).show();
                        //                                    }
                        //
                        //                                    @Override
                        //                                    public void onFailed(int errCode, String errMsg) {
                        //                                        interactRoomModels.add(roomModel);
                        //                                        mRoomRecyclerAdapter.setData(interactRoomModels);
                        //                                    }
                        //                                });
                        //                            }
                        //                        }

                        mRoomRecyclerAdapter.setData(data);
                        refreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onFailed(int errCode, String errMsg) {
                        if (isFinishing()) {
                            return;
                        }

                        showToast("获取房间列表错误(" + errCode + ")");
                        refreshLayout.setRefreshing(false);
                    }
                });
    }

    InteractRecyclerViewAdapter.MyItemClickListener mItemClickListener = new InteractRecyclerViewAdapter.MyItemClickListener() {

        @Override
        public void onItemClick(View view, int postion) {
            InteractRoomModel roomModel = mRoomRecyclerAdapter.getItem(postion);
            if (roomModel == null) {
                return;
            }

            doJoinRoom(roomModel);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.interact_brocast_type_hall_header_left_icon: {
                finish();
                break;
            }
            case R.id.interact_type_hall_go_room: {
                doSearch();
                break;
            }
            case R.id.interact_create_room: {
                Intent intent = new Intent(this, CreateInteractRoomActivity.class);
                intent.putExtra(InteractConstant.INTENT_EXTRA_INTERACT_TYPE, mRoomType);
                startActivity(intent);
                break;
            }
        }
    }

    private void doSearch() {
        if (mSearching) {
            showToast("操作太频繁，请稍后重试");
            return;
        }

        String text = mRoomSearchEditText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            showToast("请输入房间ID");
            return;
        }

        mSearching = true;
        InteractServerApi.getRoomInfo(InteractGlobalManager.getInstance().getUser().getUserId(), text, new InteractServerApi.ResultCallback<InteractRoomModel>() {

            @Override
            public void onSuccess(InteractRoomModel roomModel) {
                if (isFinishing()) {
                    return;
                }

                mSearching = false;

                if (roomModel != null) {
                    doJoinRoom(roomModel);
                } else {
                    showToast("该房间不存在");
                }
            }

            @Override
            public void onFailed(int errCode, String errMsg) {
                if (isFinishing()) {
                    return;
                }

                mSearching = false;
                showToast("该房间不存在(" + errCode + ")");
            }
        });
    }

    private void doJoinRoom(InteractRoomModel roomModel) {
        if (roomModel == null) {
            return;
        }

        Intent intent = null;
        switch (mRoomType) {
            case InteractConstant.ROOM_TYPE_ANCHOR_AND_GUEST:
            case InteractConstant.ROOM_TYPE_ANCHOR_AND_ANCHOR: {
                String myUid = InteractGlobalManager.getInstance().getUser().getUserId();
                String bindRoleId = roomModel.getBindRoleId();
                if (myUid.equals(bindRoleId)) {
                    intent = new Intent(InteracatTypeHallActivity.this, InteractActivity.class);
                } else {
                    intent = new Intent(InteracatTypeHallActivity.this, InteractAudienceActivity.class);
                }
            }
                break;
            case InteractConstant.ROOM_TYPE_HOME_PARTY:
                intent = new Intent(InteracatTypeHallActivity.this, PartyMainActivity.class);
                intent.putExtra(InteractConstant.INTENT_EXTRA_USER_IDENTITY, InteractConstant.USER_IDENTITY_AUDIENCE);
                break;
        }

        intent.putExtra(InteractConstant.INTENT_EXTRA_INTERACT_ROOM_DATA, roomModel);
        startActivity(intent);
    }

    /**
     * Called when a swipe gesture triggers a refresh.
     */
    @Override
    public void onRefresh() {
        refreshLayout.setRefreshing(true);
        refreshData();
    }

    private void showToast(final String toast) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(InteracatTypeHallActivity.this.getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
