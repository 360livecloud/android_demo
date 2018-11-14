
package com.qihoo.videocloud.player.live;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.stats.tool.PublishStatsTool;
import com.qihoo.videocloud.view.ExpandableHeightListView;
import com.qihoo.videocloud.view.MarqueeTextView;

import java.util.ArrayList;
import java.util.List;

public class LiveConfigNoAddressFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = LiveConfigNoAddressFragment.class.getSimpleName();

    private EditText etBusinessId;
    private EditText etChannelId;
    private EditText etSn;
    private TextView tvSourceListUnfold;
    private View viewLine;
    private ExpandableHeightListView lvSource;
    private RadioGroup rgDecodedMode;
    private RadioButton rbConfigDecodedAuto;
    private RadioButton rbConfigDecodedSoft;
    private ImageView ivPlay;
    private ImageView ivArrow;
    private View layoutSnList;
    private Context context;
    private List<String> snList = new ArrayList<>();

    private boolean isUnfold = true;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_config_no_address, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etBusinessId = (EditText) view.findViewById(R.id.et_busuness_id);
        etChannelId = (EditText) view.findViewById(R.id.et_channel_id);
        etSn = (EditText) view.findViewById(R.id.et_sn);
        tvSourceListUnfold = (TextView) view.findViewById(R.id.tv_source_list_unfold);
        viewLine = view.findViewById(R.id.view_line);
        ivArrow = (ImageView) view.findViewById(R.id.iv_arrow);
        layoutSnList = view.findViewById(R.id.layout_sn_list);
        layoutSnList.setOnClickListener(this);
        lvSource = (ExpandableHeightListView) view.findViewById(R.id.lv_source);
        lvSource.setExpanded(true);
        rgDecodedMode = (RadioGroup) view.findViewById(R.id.rg_decoded_mode);
        rbConfigDecodedAuto = (RadioButton) view.findViewById(R.id.rb_config_decoded_auto);
        rbConfigDecodedSoft = (RadioButton) view.findViewById(R.id.rb_config_decoded_soft);
        ivPlay = (ImageView) view.findViewById(R.id.iv_play);
        ivPlay.setOnClickListener(this);

        requestSnList();
    }

    private void requestSnList() {
        String bid = etBusinessId.getText().toString().trim();
        Logger.e(TAG, "request sn list. bid: " + bid);
        new PublishStatsTool().getList(bid, new PublishStatsTool.RespondListener() {
            @Override
            public void onSuccess(Object result) {

                if (result != null && result instanceof List) {
                    List<String> list = (List<String>) result;
                    if (list != null && list.size() > 0) {
                        snList = list;
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Logger.e(TAG, "sn list size: " + snList.size());
                                lvSource.setAdapter(new MyAdapter(context));
                            }
                        });
                        return;
                    }
                }
                Logger.e(TAG, "sn list is empty.");
            }

            @Override
            public void onFailed(int errCode, String errMessage) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_play: {
                jump(false, etSn.getText().toString().trim());
            }
                break;
            case R.id.layout_sn_list: {
                if (isUnfold) {

                    isUnfold = false;

                    tvSourceListUnfold.setText(context.getResources().getText(R.string.str_config_source_list_unflod));
                    ivArrow.setImageDrawable(context.getResources().getDrawable(R.drawable.arrow));
                    viewLine.setVisibility(View.GONE);
                    lvSource.setVisibility(View.GONE);
                } else {
                    isUnfold = true;

                    tvSourceListUnfold.setText(context.getResources().getText(R.string.str_config_source_list_flod));
                    ivArrow.setImageDrawable(context.getResources().getDrawable(R.drawable.arrow_90));
                    viewLine.setVisibility(View.VISIBLE);
                    lvSource.setVisibility(View.VISIBLE);
                }
            }
                break;

            default:
                break;
        }
    }

    /**
     * 界面跳转
     * @param isHaveAddress 是否为有地址方式
     * @param sn 有地址时为rtmp...
     *           无地址时为 sn
     */
    private void jump(boolean isHaveAddress, String sn) {
        Intent intent = new Intent(getContext(), LiveActivity.class);
        intent.putExtra("businessId", etBusinessId.getText().toString().trim());
        intent.putExtra("channelId", etChannelId.getText().toString().trim());
        intent.putExtra("autoDecoded", rbConfigDecodedAuto.isChecked());
        if (isHaveAddress) {
            intent.putExtra("haveAddress", Boolean.TRUE);
            intent.putExtra("url", sn);
        } else {
            intent.putExtra("haveAddress", Boolean.FALSE);
            intent.putExtra("sn", sn);
        }

        startActivity(intent);
    }

    class MyAdapter extends BaseAdapter {

        Context context;

        public MyAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return snList.size();
        }

        @Override
        public Object getItem(int position) {
            return snList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            MyAdapter.ViewHolder holder = null;
            if (convertView == null) {
                convertView = View.inflate(context, R.layout.adapter_live_config_sn_list_item, null);

                holder = new MyAdapter.ViewHolder();
                holder.tvText = (MarqueeTextView) convertView.findViewById(R.id.tv_text);

                convertView.setTag(holder);
            } else {
                holder = (MyAdapter.ViewHolder) convertView.getTag();
            }

            final String text = snList.get(position);
            if (!TextUtils.isEmpty(text)) {
                holder.tvText.setText(text);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    jump(true, text);
                }
            });

            return convertView;
        }

        class ViewHolder {
            MarqueeTextView tvText;
        }
    }
}
