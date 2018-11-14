
package com.qihoo.videocloud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.debug.SettingActivity;
import com.qihoo.videocloud.interactbrocast.PrepareInteractBrocastActivity;
import com.qihoo.videocloud.localserver.LocalServerActivity;
import com.qihoo.videocloud.p2p.P2PActivity;
import com.qihoo.videocloud.player.PlaySelectActivity;
import com.qihoo.videocloud.recorder.PrepareRecordActivity;
import com.qihoo.videocloud.recorderlocal.PrepareRecordLocalActivity;
import com.qihoo.videocloud.upload.UploadActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavigationActivity extends Activity {

    private static final String TAG = NavigationActivity.class.getSimpleName();

    private ListView lvFunc;
    private List<Map<String, Object>> funcList = new ArrayList<>();
    private static final String KEY_ICON_RES_ID = "KEY_ICON_RES_ID";
    private static final String KEY_ICON_TEXT = "KEY_ICON_TEXT";
    private static final String KEY_ICON_JUMP = "KEY_ICON_JUMP";

    private final int[] resIdArray = {
            R.drawable.icon_push_data,
            R.drawable.icon_play,
            R.drawable.icon_upload,
            R.drawable.icon_localserver,
            //            R.drawable.icon_mic,
            //            R.drawable.icon_clip,
            R.drawable.icon_take_photo,
            R.drawable.icon_p2p,
            R.drawable.interact_icon,
    };
    private final String[] textArray = {
            "推流",
            "播放",
            "上传",
            "本地缓存",
            //            "连麦",
            //            "裁剪",
            "拍摄",
            "P2P",
            "互动直播",
    };
    private final Class<?>[] jumpArray = {
            PrepareRecordActivity.class,
            /*TextureViewActivity.class*/PlaySelectActivity.class,
            UploadActivity.class,
            LocalServerActivity.class,
            //            null,
            //            null,
            PrepareRecordLocalActivity.class,
            P2PActivity.class,
            PrepareInteractBrocastActivity.class,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        hideSystemNavigationBar();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        initView();
        initData();
    }

    private void initView() {
        findViewById(R.id.iv_logo).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(new Intent(NavigationActivity.this, SettingActivity.class));
                return false;
            }
        });

        lvFunc = (ListView) findViewById(R.id.lv_func);
    }

    private void initData() {
        // todo 确保resIdArray、 textArray、jumpArray长度相等
        for (int i = 0; i < resIdArray.length; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put(KEY_ICON_RES_ID, resIdArray[i]);
            map.put(KEY_ICON_TEXT, textArray[i]);
            map.put(KEY_ICON_JUMP, jumpArray[i]);

            funcList.add(map);
        }

        lvFunc.setAdapter(new FuncAdapter(this));
    }

    class FuncAdapter extends BaseAdapter {

        Context context;

        public FuncAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return funcList.size();
        }

        @Override
        public Object getItem(int position) {
            return funcList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;
            if (convertView == null) {
                convertView = View.inflate(context, R.layout.adapter_navigation_func_item, null);

                holder = new ViewHolder();
                holder.ivIcon = (ImageView) convertView.findViewById(R.id.iv_icon);
                holder.tvText = (TextView) convertView.findViewById(R.id.tv_text);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final Map<String, Object> item = funcList.get(position);
            Integer iconResId = (Integer) item.get(KEY_ICON_RES_ID);
            if (iconResId != null) {
                holder.ivIcon.setImageDrawable(context.getResources().getDrawable(iconResId));
            }

            String text = (String) item.get(KEY_ICON_TEXT);
            if (!TextUtils.isEmpty(text)) {
                holder.tvText.setText(text);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Class<?> jump = (Class<?>) item.get(KEY_ICON_JUMP);
                    if (jump != null) {
                        Intent intent = new Intent(context, jump);
                        context.startActivity(intent);
                    }
                }
            });

            return convertView;
        }

        class ViewHolder {
            ImageView ivIcon;
            TextView tvText;
        }
    }

    private void hideSystemNavigationBar() {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            View view = this.getWindow().getDecorView();
            view.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            //            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            //                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        VideoCloudApplication.getInstance().applicationExit();
    }
}
