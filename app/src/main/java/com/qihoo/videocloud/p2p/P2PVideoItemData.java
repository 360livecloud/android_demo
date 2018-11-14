
package com.qihoo.videocloud.p2p;

import java.io.Serializable;

/**
 * Created by guohailiang on 2017/9/22.
 */

public class P2PVideoItemData implements Serializable {

    private String title;
    //cover page url
    private String image;
    // resource url
    private String url;
    // resource id
    private String rid;
    private String watchCount;
    private String duration;

    public String getWatchCount() {
        return watchCount;
    }

    public void setWatchCount(String watchCount) {
        this.watchCount = watchCount;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }
}
