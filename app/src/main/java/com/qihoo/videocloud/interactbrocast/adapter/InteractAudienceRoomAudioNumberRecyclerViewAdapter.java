
package com.qihoo.videocloud.interactbrocast.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.interactbrocast.InteractAudienceActivity;
import com.qihoo.videocloud.interactbrocast.modle.InteractUserModel;

import java.util.ArrayList;
import java.util.List;

public class InteractAudienceRoomAudioNumberRecyclerViewAdapter extends RecyclerView.Adapter<InteractAudienceRoomAudioNumberRecyclerViewAdapter.ViewHolder> {

    private List<InteractUserModel> dataList;
    private MyItemClickListener mOnItemClickListener;
    private String myUserId;
    private String roomId;
    private InteractAudienceActivity mContext;

    public InteractAudienceRoomAudioNumberRecyclerViewAdapter(List<InteractUserModel> list, String myUserId, String roomId, InteractAudienceActivity context) {
        if (list == null) {
            list = new ArrayList<>();
        }
        this.dataList = list;
        this.myUserId = myUserId;
        this.roomId = roomId;
        this.mContext = context;
    }

    public void setData(List<InteractUserModel> data) {
        this.dataList.clear();
        this.dataList.addAll(data);
        notifyDataSetChanged();
    }

    public List<InteractUserModel> getData() {
        return new ArrayList<>(this.dataList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.interact_room_audio_number_recycler_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.userName.setText(dataList.get(position).getUserId());
        if (myUserId != null && myUserId.equals(dataList.get(position).getUserId())) {
            holder.kickButton.setVisibility(View.VISIBLE);
            holder.kickButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.changeToAudience();
                }
            });
        } else {
            holder.kickButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    public InteractUserModel getItem(int position) {
        if (this.dataList == null) {
            return null;
        }

        if (position < 0 || position >= this.dataList.size()) {
            return null;
        }

        return this.dataList.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userAvatar;
        TextView userName;
        ImageView kickButton;
        //        ImageView inviteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            userAvatar = (ImageView) itemView.findViewById(R.id.interact_audio_number_avatar);
            userName = (TextView) itemView.findViewById(R.id.interact_audio_number_name);
            kickButton = (ImageView) itemView.findViewById(R.id.interact_audio_number_kick);
            //            inviteButton = (ImageView) itemView.findViewById(R.id.interact_number_invite);
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
