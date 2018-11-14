
package com.qihoo.videocloud.interactbrocast.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.interactbrocast.party.PartyItemBaseView;
import com.qihoo.videocloud.interactbrocast.party.PartyRoleItem;

import java.util.ArrayList;
import java.util.List;

public class InteractPartyRecyclerViewAdapter extends RecyclerView.Adapter<InteractPartyRecyclerViewAdapter.ViewHolder> {

    private List<PartyRoleItem> dataList;
    private MyItemClickListener mOnItemClickListener;
    private int itemWidth;
    private int itemHeight;

    public InteractPartyRecyclerViewAdapter(List<PartyRoleItem> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        this.dataList = list;
    }

    public void setData(List<PartyRoleItem> data) {
        this.dataList.clear();
        this.dataList.addAll(data);
        notifyDataSetChanged();
    }

    public List<PartyRoleItem> getData() {
        return new ArrayList<>(this.dataList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.party_gridview_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        PartyRoleItem roleItem = dataList.get(position);
        holder.baseView.setVideoView(roleItem.getVideoView(), itemWidth, itemHeight);
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    public PartyRoleItem getItem(int position) {
        if (this.dataList == null) {
            return null;
        }

        if (position < 0 || position >= this.dataList.size()) {
            return null;
        }

        return this.dataList.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView defImage;
        PartyItemBaseView baseView;

        public ViewHolder(View itemView) {
            super(itemView);
            baseView = (PartyItemBaseView) itemView.findViewById(R.id.party_grid_video_baseview);
            defImage = (ImageView) itemView.findViewById(R.id.default_image);
        }

    }

    //声明MyItemClickListener这个接口
    public interface MyItemClickListener {
        public void onItemClick(View view, int postion);
    }

    public void setOnItemClickListener(MyItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public void setItemWidth(int width) {
        this.itemWidth = width;
    }

    public void setItemHeight(int height) {
        this.itemHeight = height;
    }
}
