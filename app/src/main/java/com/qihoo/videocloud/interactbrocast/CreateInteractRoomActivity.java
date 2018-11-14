
package com.qihoo.videocloud.interactbrocast;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.interactbrocast.data.InteractGlobalManager;
import com.qihoo.videocloud.interactbrocast.main.InteractConstant;
import com.qihoo.videocloud.interactbrocast.modle.InteractRoomModel;
import com.qihoo.videocloud.interactbrocast.net.InteractServerApi;

public class CreateInteractRoomActivity extends Activity implements View.OnClickListener {

    private EditText mRoomNameEditText;
    private Spinner mRoomMaxNumSpinner;
    private Switch mRoomTalkTypeSwitch;

    private int mRoomType;
    private boolean mCreating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
    }

    private void initView() {
        setContentView(R.layout.activity_create_interact_room);

        findViewById(R.id.interact_brocast_create_room_header_left_icon).setOnClickListener(this);
        findViewById(R.id.interact_create_room_confirm_layout).setOnClickListener(this);

        mRoomNameEditText = (EditText) findViewById(R.id.interact_create_room_name);
        mRoomMaxNumSpinner = (Spinner) findViewById(R.id.interact_create_max_num);
        mRoomMaxNumSpinner.setSelection(3);
        mRoomTalkTypeSwitch = (Switch) findViewById(R.id.interact_create_room_talk_type);
    }

    private void initData() {
        mRoomType = getIntent().getIntExtra(InteractConstant.INTENT_EXTRA_INTERACT_TYPE, InteractConstant.ROOM_TYPE_ANCHOR_AND_GUEST);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.interact_brocast_create_room_header_left_icon: {
                finish();
                break;
            }
            case R.id.interact_create_room_confirm_layout: {
                doCreate();
                break;
            }
        }
    }

    private void doCreate() {
        if (mCreating) {
            showToast("操作太频繁，请稍后重试");
            return;
        }

        String roomName = mRoomNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(roomName)) {
            showToast("请输入房间名称");
            return;
        }
        if (roomName.length() > 16) {
            showToast("房间名称不能大于16个字符");
            return;
        }

        final int talkType = mRoomTalkTypeSwitch.isChecked() ? InteractConstant.TALK_TYPE_AUDIO : InteractConstant.TALK_TYPE_ALL;
        int roomLifeType = 0;
        switch (mRoomType) {
            case InteractConstant.ROOM_TYPE_ANCHOR_AND_ANCHOR:
            case InteractConstant.ROOM_TYPE_ANCHOR_AND_GUEST: {
                roomLifeType = InteractConstant.ROOM_LIFE_TYPE_BIND_ANCHOR;
                break;
            }
            case InteractConstant.ROOM_TYPE_HOME_PARTY: {
                roomLifeType = InteractConstant.ROOM_LIFE_TYPE_BIND_ROOM;
                break;
            }
        }
        int maxNum = Integer.valueOf(mRoomMaxNumSpinner.getSelectedItem().toString());

        mCreating = true;
        InteractServerApi.createRoom(InteractGlobalManager.getInstance().getUser().getUserId(), roomName, mRoomType, talkType,
                roomLifeType, maxNum, new InteractServerApi.ResultCallback<InteractRoomModel>() {

                    @Override
                    public void onSuccess(InteractRoomModel roomModel) {
                        if (isFinishing()) {
                            return;
                        }

                        mCreating = false;

                        if (roomModel != null) {
                            Intent intent = null;
                            switch (mRoomType) {
                                case InteractConstant.ROOM_TYPE_ANCHOR_AND_ANCHOR:
                                case InteractConstant.ROOM_TYPE_ANCHOR_AND_GUEST:
                                    intent = new Intent(CreateInteractRoomActivity.this, InteractActivity.class);
                                    intent.putExtra(InteractConstant.INTENT_EXTRA_INTERACT_TALK_TYPE, talkType);
                                    break;
                                case InteractConstant.ROOM_TYPE_HOME_PARTY:
                                    intent = new Intent(CreateInteractRoomActivity.this, PartyMainActivity.class);
                                    intent.putExtra(InteractConstant.INTENT_EXTRA_USER_IDENTITY, InteractConstant.USER_IDENTITY_ANCHOR);
                                    break;
                            }
                            intent.putExtra(InteractConstant.INTENT_EXTRA_INTERACT_ROOM_DATA, roomModel);
                            startActivity(intent);
                            finish();
                        } else {
                            showToast("创建房间失败");
                        }
                    }

                    @Override
                    public void onFailed(int errCode, String errMsg) {
                        if (isFinishing()) {
                            return;
                        }

                        mCreating = false;
                        showToast("创建房间失败(" + errCode + ")");
                    }
                });
    }

    private void showToast(final String toast) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CreateInteractRoomActivity.this.getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
