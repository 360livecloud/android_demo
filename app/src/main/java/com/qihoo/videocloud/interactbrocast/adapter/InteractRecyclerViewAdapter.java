
package com.qihoo.videocloud.interactbrocast.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.interactbrocast.modle.InteractRoomModel;

import java.util.ArrayList;
import java.util.List;

public class InteractRecyclerViewAdapter extends RecyclerView.Adapter<InteractRecyclerViewAdapter.ViewHolder> {

    private List<InteractRoomModel> dataList;
    private MyItemClickListener mOnItemClickListener;

    public InteractRecyclerViewAdapter(List<InteractRoomModel> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        this.dataList = list;
    }

    public void setData(List<InteractRoomModel> data) {
        this.dataList.clear();
        this.dataList.addAll(data);
        notifyDataSetChanged();
    }

    public List<InteractRoomModel> getData() {
        return new ArrayList<>(this.dataList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.interact_room_recycler_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.roomItemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(v, position);
                }
            }
        });
        holder.roomId.setText(dataList.get(position).getRoomId());
        holder.roomName.setText(dataList.get(position).getRoomName());
        holder.onlineNum.setText("在线人数：" + dataList.get(position).getOnlineNum());
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    public InteractRoomModel getItem(int position) {
        if (this.dataList == null) {
            return null;
        }

        if (position < 0 || position >= this.dataList.size()) {
            return null;
        }

        return this.dataList.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView roomName;
        TextView roomId;
        TextView onlineNum;
        LinearLayout roomItemLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            roomItemLayout = (LinearLayout) itemView.findViewById(R.id.interact_room_item);
            roomName = (TextView) itemView.findViewById(R.id.interact_room_item_name);
            roomId = (TextView) itemView.findViewById(R.id.interact_room_item_id);
            onlineNum = (TextView) itemView.findViewById(R.id.interact_room_item_online_num);
        }

    }

    //声明MyItemClickListener这个接口
    public interface MyItemClickListener {
        public void onItemClick(View view, int postion);
    }

    public void setOnItemClickListener(MyItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }
}
