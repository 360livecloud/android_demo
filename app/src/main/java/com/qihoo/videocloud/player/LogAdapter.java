
package com.qihoo.videocloud.player;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.qihoo.livecloudrefactor.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by guohailiang on 2017/6/16.
 */

public class LogAdapter extends BaseAdapter {

    private Context context;
    private List<String> list = new ArrayList<>();
    private int textColorResId = R.color.color_666666;

    public LogAdapter(Context context, List<String> list, int textColorResId) {
        this.context = context;
        this.list = list;
        this.textColorResId = textColorResId;
    }

    public void setTextColorResId(int textColorResId) {
        this.textColorResId = textColorResId;
    }

    public void setList(List<String> list) {
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = View.inflate(context, R.layout.adapter_log_item, null);
        }
        TextView textView = (TextView) convertView;
        textView.setText(list.get(position));
        textView.setTextColor(context.getResources().getColor(textColorResId));
        textView.setClickable(false);
        return convertView;
    }
}
