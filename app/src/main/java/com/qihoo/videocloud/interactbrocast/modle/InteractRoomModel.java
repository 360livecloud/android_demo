
package com.qihoo.videocloud.interactbrocast.modle;

import com.qihoo.videocloud.interactbrocast.main.InteractConstant;

import java.io.Serializable;
import java.util.List;

/**
 * Created by huchengming on 2018/2/12.
 */

public class InteractRoomModel implements Serializable, Comparable {
    /**
     * 房间名称
     */
    private String roomName;

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 在线人数
     */
    private int onlineNum;

    /**
     * 绑定的主播ID
     */
    private String bindRoleId;

    /**
     * 互动方式，参见{@link InteractConstant#TALK_TYPE_ALL}等
     */
    private int talkType = InteractConstant.TALK_TYPE_ALL;

    /**
     * 最大连麦人数
     */
    private int maxNum;

    /**
     * 房间生命周期类型，参见{@link InteractConstant#ROOM_LIFE_TYPE_BIND_ANCHOR}等
     */
    private int roomLifeType = InteractConstant.ROOM_LIFE_TYPE_BIND_ANCHOR;

    /**
     * 房间创建时间
     */
    private String createTime;

    /**
     *  用户在房间的身份
     */
    private int userIdentity;
    /**
     * 用户列表
     */
    private List<InteractUserModel> userList;

    @Override
    public String toString() {
        return "roomId=" + roomId +
                ", roomName=" + roomName +
                ", onlineNum=" + onlineNum +
                ", bindRoleId=" + bindRoleId +
                ", talkType=" + talkType +
                ", maxNum=" + maxNum +
                ", roomLifeType=" + roomLifeType +
                ", createTime=" + createTime +
                ", userList=" + (userList != null ? userList.size() : -1);
    }

    @Override
    public int compareTo(Object o) {
        InteractRoomModel sdto = (InteractRoomModel) o;
        String createTime = sdto.getCreateTime();
        return this.createTime.compareTo(createTime);

    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public int getOnlineNum() {
        return onlineNum;
    }

    public void setOnlineNum(int onlineNum) {
        this.onlineNum = onlineNum;
    }

    public String getBindRoleId() {
        return bindRoleId;
    }

    public void setBindRoleId(String bindRoleId) {
        this.bindRoleId = bindRoleId;
    }

    public int getTalkType() {
        return talkType;
    }

    public void setTalkType(int talkType) {
        this.talkType = talkType;
    }

    public int getMaxNum() {
        return maxNum;
    }

    public void setMaxNum(int maxNum) {
        this.maxNum = maxNum;
    }

    public int getRoomLifeType() {
        return roomLifeType;
    }

    public void setRoomLifeType(int roomLifeType) {
        this.roomLifeType = roomLifeType;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public List<InteractUserModel> getUserList() {
        return userList;
    }

    public void setUserList(List<InteractUserModel> userList) {
        this.userList = userList;
    }

    public int getUserIdentity() {
        return userIdentity;
    }

    public void setUserIdentity(int userIdentity) {
        this.userIdentity = userIdentity;
    }
}
