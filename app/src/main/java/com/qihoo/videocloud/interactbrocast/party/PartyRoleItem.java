
package com.qihoo.videocloud.interactbrocast.party;

import android.graphics.Bitmap;
import android.view.View;

/**
 * Created by liuyanqing on 2018/3/8.
 */

public class PartyRoleItem {

    private String userId;
    private Bitmap mImage;
    private View videoView;

    public PartyRoleItem(String userId, Bitmap bitmap) {
        this.userId = userId;
        this.mImage = bitmap;
    }

    public void setImage(Bitmap bitmap) {
        this.mImage = bitmap;
    }

    public Bitmap getImage() {
        return mImage;
    }

    public String getUserId() {
        return userId;
    }

    public void setVideoView(View view) {
        this.videoView = view;
    }

    public View getVideoView() {
        return this.videoView;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PartyRoleItem) {
            PartyRoleItem role = (PartyRoleItem) obj;
            return userId.equals(role.userId);
        }
        return false;
    }

}
