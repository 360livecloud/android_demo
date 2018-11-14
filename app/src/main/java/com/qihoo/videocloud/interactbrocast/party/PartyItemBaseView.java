
package com.qihoo.videocloud.interactbrocast.party;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.qihoo.livecloudrefactor.R;

/**
 * Created by liuyanqing on 2018/3/8.
 */

public class PartyItemBaseView extends RelativeLayout {

    public PartyItemBaseView(Context context) {
        super(context);
        initViews(context);
    }

    public PartyItemBaseView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PartyItemBaseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initViews(context);
    }

    private void initViews(Context context) {
        View rootView = LayoutInflater.from(context).inflate(R.layout.party_video_baseview, this);
        this.setBackgroundColor(0x00FFFFFF);

        videoBaseLayout = (FrameLayout) rootView.findViewById(R.id.video_base_view);

    }

    public void setVideoView(View view, int w, int h) {
        videoBaseLayout.removeAllViews();
        if (view.getParent() == null) {
            videoBaseLayout.addView(view, w, h);
            videoView = view;
        } else {
            FrameLayout frameLayout = (FrameLayout) view.getParent();
            frameLayout.removeAllViews();
            videoBaseLayout.addView(view, w, h);
            videoView = view;
        }
    }

    public View getVideoView() {
        return videoView;
    }

    private FrameLayout videoBaseLayout;
    private View videoView;

}
