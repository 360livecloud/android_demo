
package com.qihoo.videocloud.interactbrocast;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.qihoo.livecloud.tools.Logger;
import com.qihoo.videocloud.im.api.QHVCCommandMessageContent;
import com.qihoo.videocloud.im.api.QHVCConnectionStatusListener;
import com.qihoo.videocloud.im.api.QHVCIMClient;
import com.qihoo.videocloud.im.api.QHVCIMConstant;
import com.qihoo.videocloud.im.api.QHVCIMContext;
import com.qihoo.videocloud.im.api.QHVCIMMessage;
import com.qihoo.videocloud.im.api.QHVCIMUserInfo;
import com.qihoo.videocloud.im.api.QHVCMessageContent;
import com.qihoo.videocloud.im.api.QHVCOnReceiveMessageListener;
import com.qihoo.videocloud.im.api.QHVCResultCallback;
import com.qihoo.videocloud.im.api.QHVCSendMessageCallback;
import com.qihoo.videocloud.im.api.QHVCTextMessageContent;
import com.qihoo.videocloud.interactbrocast.data.InteractGlobalManager;
import com.qihoo.videocloud.interactbrocast.modle.InteractIMContext;
import com.qihoo.videocloud.interactbrocast.modle.InteractUserModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class InteractIMManager {

    /**
     1、观众发送连麦请求
     2、主播邀请观众连麦
     3、嘉宾加入成功，通知房间变更
     4、主播退出，通知房间变更
    **/

    /**
     * 主播发送邀请观众连麦
     */
    static final int CMD_ANCHOR_INVITE_GUEST = 10000;
    /**
     * 观众同意主播连麦邀请
     */
    static final int CMD_GUEST_AGREE_INVITE = 10001;
    /**
     * 观众拒绝主播连麦邀请
     */
    static final int CMD_GUEST_REFUSE_INVITE = 10002;
    /**
     * 观众发送连麦请求
     */
    static final int CMD_GUEST_ASK_JOIN = 10003;
    /**
     * 主播同意连麦请求
     */
    static final int CMD_ANCHOR_AGREE_JOIN = 10004;
    /**
     * 主播拒绝连麦请求
     */
    static final int CMD_ANCHOR_REFUSE_JOIN = 10005;

    /**
     * 主播踢出某个嘉宾
     */
    public static final int CMD_ANCHOR_KICKOUT_GUEST = 10006;

    /**
     * 嘉宾进入，发送通知
     */
    static final int CMD_GUEST_JOIN_NOTIFY = 20000;
    /**
     * 主播退出，发送通知
     */
    static final int CMD_ANCHOR_QUIT_NOTIFY = 20001;
    /**
     * 嘉宾退出，发送通知
     */
    static final int CMD_GUEST_QUIT_NOTIFY = 20002;
    /**
     * 嘉宾被踢出，发送通知
     */
    static final int CMD_GUEST_KICKOUT_NOTIFY = 20003;
    /**
     * 有观众进入，发送通知，可以用来刷新界面人数
     */

    static final int CMD_AUDIENT_JOIN_NOTIFY = 20004;
    /**
     * 有观众退出，发送通知，可以用来刷新界面人数
     */
    static final int CMD_AUDIENT_QUIT_NOTIFY = 20005;

    /**
     * 收到命令消息的回调函数
     */
    public interface OnReceiveCommandListener {
        /**
         * 收到已经知道的消息
         * @param userFrom 用户信息
         * @param command 命令结构
         */
        void onReceiveCommand(InteractUserModel userFrom, CommandInfo command);

        /**
         * 收到已经知道的消息
         * @param userFrom 用户信息
         * @param str 命令字符
        */

        void onReceiveOtherCommand(InteractUserModel userFrom, String str);
    }

    public static class CommandInfo {
        private int cmd;
        private String cmdTip;
        private String target;
        private String osType;
        private String ver;
        private String extra;
        private long sentTime;
        private long receivedTime;

        public CommandInfo(int command, String cmdTip) {
            this.cmd = command;
            this.cmdTip = cmdTip;
        }

        public int getCmd() {
            return cmd;
        }

        public String getCmdTip() {
            return cmdTip;
        }

        public String getTarget() {
            return target;
        }

        public String getOsType() {
            return osType;
        }

        public String getVer() {
            return ver;
        }

        public String getExtra() {
            return extra;
        }

        public long getSentTime() {
            return sentTime;
        }

        public long getReceivedTime() {
            return receivedTime;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public void setOsType(String osType) {
            this.osType = osType;
        }

        public void setVer(String ver) {
            this.ver = ver;
        }

        public void setExtra(String extra) {
            this.extra = extra;
        }

        public void setSentTime(long sentTime) {
            this.sentTime = sentTime;
        }

        public void setReceivedTime(long receivedTime) {
            this.receivedTime = receivedTime;
        }

        public static CommandInfo parse(String json) {

            int command = 0;
            String cmdTip = "";
            String target = "";
            String osType = "";
            String ver = "";
            String extra = "";
            try {
                JSONObject jsonObject = new JSONObject(json);
                command = jsonObject.getInt("cmd");
                cmdTip = jsonObject.getString("cmd_tip");
                target = jsonObject.getString("target");
                osType = jsonObject.getString("os_type");
                ver = jsonObject.getString("ver");
                extra = jsonObject.getString("extra");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            CommandInfo info = new CommandInfo(command, cmdTip);
            info.setTarget(target);
            info.setOsType(osType);
            info.setVer(ver);
            info.setExtra(extra);

            return info;
        }

        public static CommandInfo obtain(int cmd, String cmdTip, String target) {
            CommandInfo info = new CommandInfo(cmd, cmdTip);
            info.setOsType("android");
            info.setVer("1.0");
            info.setExtra("");
            info.setTarget(target);

            return info;
        }

        public String getCommandString() {
            JSONObject jsonObject = new JSONObject();

            String strJson = "";
            try {
                jsonObject.put("cmd", cmd);
                jsonObject.put("cmd_tip", cmdTip);
                jsonObject.put("target", target);
                jsonObject.put("os_type", osType);
                jsonObject.put("ver", ver);
                jsonObject.put("extra", target);
                strJson = jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return strJson;
        }

        public boolean isValid() {
            return cmd >= 10000;
        }
    }

    public interface ConnectCallback {

        /**
         * 回调成功
         * @param userId 回调成功返回的用户id
         */
        void onSuccess(String userId);

        /**
         * 回调失败，回调失败的错误码
         * @param errorCode
         */
        void onError(int errorCode);
    }

    public interface ChatRoomResultCallback {

        /**
         * 回调成功
         * @param roomId 回调成功房间的id
         */
        void onSuccess(String roomId);

        /**
         * 回调失败，回调失败的错误码
         * @param errorCode
         */
        void onError(int errorCode);
    }

    public interface SendMessageCallback {

        /**
         * 回调成功
         */
        void onSuccess();

        /**
         * 回调失败，回调失败的错误码
         * @param errorCode
         */
        void onError(int errorCode);
    }

    private static InteractIMManager sInst = null;

    private ArrayList<OnReceiveCommandListener> mOnReceiveCommandisteners = new ArrayList<>();

    private Object mListenserLock = new Object();
    private Context mContext;
    private AtomicBoolean mAtomicBoolean = new AtomicBoolean(false);
    private String mCurrentRoomId;

    private InteractIMManager() {

    }

    public static synchronized InteractIMManager getInstance() {
        if (sInst == null) {
            sInst = new InteractIMManager();
        }
        return sInst;
    }

    public void addReceiveCommandistener(OnReceiveCommandListener listener) {
        synchronized (mListenserLock) {
            mOnReceiveCommandisteners.add(listener);
        }
    }

    public void removeReceiveCommandistener(OnReceiveCommandListener listener) {
        synchronized (mListenserLock) {
            mOnReceiveCommandisteners.remove(listener);
        }
    }

    public void init(Context context) {
        mContext = context;

        QHVCIMClient.setOnReceiveMessageListener(new QHVCOnReceiveMessageListener() {
            @Override
            public boolean onReceived(final QHVCIMMessage message, int count) {

                ArrayList<OnReceiveCommandListener> onReceiveCommandisteners = new ArrayList<>();
                synchronized (mListenserLock) {
                    onReceiveCommandisteners.addAll(mOnReceiveCommandisteners);
                }

                String info = "";
                QHVCMessageContent content = message.content;
                if (content instanceof QHVCTextMessageContent) {
                    QHVCTextMessageContent textMessageContent = (QHVCTextMessageContent) content;
                    info = "text:" + textMessageContent.message;
                } else if (content instanceof QHVCCommandMessageContent) {
                    QHVCCommandMessageContent commandMessageContent = (QHVCCommandMessageContent) content;
                    info = commandMessageContent.info;
                    CommandInfo commandInfo = CommandInfo.parse(info);
                    if (commandInfo.isValid()) {
                        commandInfo.setSentTime(message.sentTime / 1000);
                        commandInfo.setReceivedTime(message.receivedTime / 1000);
                    }
                    InteractUserModel userModel = null;
                    if (commandMessageContent.userInfo != null) {
                        userModel = new InteractUserModel();
                        userModel.setUserId(commandMessageContent.userInfo.getUserId());
                        userModel.setNickname(commandMessageContent.userInfo.getName());
                        userModel.setPortraint(commandMessageContent.userInfo.getPortraitUri().toString());
                    }

                    for (OnReceiveCommandListener listener : onReceiveCommandisteners) {
                        if (message.conversationType == QHVCIMConstant.QHVCConversationType.CHATROOM) {
                            if (commandInfo.isValid()) {
                                listener.onReceiveCommand(userModel, commandInfo);
                            } else {
                                listener.onReceiveOtherCommand(userModel, info);
                            }
                        } else if (message.conversationType == QHVCIMConstant.QHVCConversationType.PRIVATE) {
                            if (commandInfo.isValid()) {
                                listener.onReceiveCommand(userModel, commandInfo);
                            } else {
                                listener.onReceiveOtherCommand(userModel, info);
                            }
                        }
                    }
                }

                return true;
            }
        });

        QHVCIMClient.setConnectionStatusListener(new QHVCConnectionStatusListener() {
            @Override
            public void onChanged(QHVCConnectionStatus status) {
                String info = "connect status changed " + status;
                Logger.d("test", info);

                if (status.getValue() == QHVCConnectionStatus.CONNECTED.getValue()) {
                    mAtomicBoolean.set(true);
                } else if (status.getValue() == QHVCConnectionStatus.CONNECTING.getValue()) {

                } else {
                    mAtomicBoolean.set(false);
                }
            }
        });

        QHVCIMClient.init(mContext, null, null);
    }

    public void connect(InteractUserModel user, final ConnectCallback callback) {
        String uSign = InteractGlobalManager.getInstance().getUSign();

        ArrayMap<String, String> userList = new ArrayMap<>();
        userList.put("安陵容", "http://7xs9j5.com1.z0.glb.clouddn.com/liveapp/anlingrong.jpg");
        userList.put("果郡王", "http://7xs9j5.com1.z0.glb.clouddn.com/liveapp/guojunwang.jpg");
        userList.put("华妃", "http://7xs9j5.com1.z0.glb.clouddn.com/liveapp/huafei.jpg");
        userList.put("皇后", "http://7xs9j5.com1.z0.glb.clouddn.com/liveapp/huanghou.jpg");
        userList.put("皇上", "http://7xs9j5.com1.z0.glb.clouddn.com/liveapp/huangshang.jpg");
        userList.put("沈眉庄", "http://7xs9j5.com1.z0.glb.clouddn.com/liveapp/shenmeizhuang.jpg");
        userList.put("甄嬛", "http://7xs9j5.com1.z0.glb.clouddn.com/liveapp/zhenhuan.jpg");

        long time = System.currentTimeMillis();
        Random ran = new Random(time);
        int n = ran.nextInt(userList.size());

        String nickname = user.getNickname();
        String portraint = user.getPortraint();
        if (TextUtils.isEmpty(nickname)) {
            nickname = userList.keyAt(n);
        }

        QHVCIMUserInfo imUser = new QHVCIMUserInfo(user.getUserId(), nickname, Uri.parse(portraint));

        QHVCIMContext qhvcimContext = null;
        InteractIMContext imContext = InteractGlobalManager.getInstance().getImContext();
        if (imContext != null) {
            qhvcimContext = new QHVCIMContext(imContext.getVendor(), imContext.getAppKey(), imContext.getAppSecret());
        }

        QHVCIMClient.connect(imUser, qhvcimContext, uSign, null, new QHVCResultCallback<String>() {
            @Override
            public void onSuccess(String var1) {
                if (callback != null) {
                    callback.onSuccess(var1);
                }
            }

            @Override
            public void onError(int errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }

    public void disconnect() {
        QHVCIMClient.disconnect(false);
    }

    public boolean isConnected() {
        return mAtomicBoolean.get();
    }

    public void joinChatRoom(String roomId, final ChatRoomResultCallback callback) {
        mCurrentRoomId = roomId;
        QHVCIMClient.joinChatRoom(roomId, 0, new QHVCResultCallback<String>() {
            @Override
            public void onSuccess(String var1) {
                if (callback != null) {
                    callback.onSuccess(var1);
                }
            }

            @Override
            public void onError(int errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }

    public void quitChatRoom(final ChatRoomResultCallback callback) {
        mCurrentRoomId = "";
        QHVCIMClient.quitChatRoom(new QHVCResultCallback<String>() {
            @Override
            public void onSuccess(String var1) {
                if (callback != null) {
                    callback.onSuccess(var1);
                }
            }

            @Override
            public void onError(int errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }

    private void sendCommandToChatRoom(String roomId, String command, final SendMessageCallback callback) {
        QHVCIMClient.sendCommandMessage(QHVCIMConstant.QHVCConversationType.CHATROOM, roomId, command, new QHVCSendMessageCallback() {
            @Override
            public void onSuccess(QHVCIMMessage msg) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onProgress(QHVCIMMessage msg, int progress) {

            }

            @Override
            public void onError(QHVCIMMessage msg, int errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }

    private void sendCommandToUser(String userId, String command, final SendMessageCallback callback) {
        QHVCIMClient.sendCommandMessage(QHVCIMConstant.QHVCConversationType.PRIVATE, userId, command, new QHVCSendMessageCallback() {
            @Override
            public void onSuccess(QHVCIMMessage msg) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onProgress(QHVCIMMessage msg, int progress) {

            }

            @Override
            public void onError(QHVCIMMessage msg, int errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }

    /**
     * 发送命令
     * @param userId 用户id
     * @param cmd 命令id
     * @param cmdTip 见 {@link #CMD_ANCHOR_INVITE_GUEST}
     * @param callback 回调
     */
    public void sendCommand(String userId, int cmd, String cmdTip, final SendMessageCallback callback) {
        CommandInfo info = CommandInfo.obtain(cmd, cmdTip, mCurrentRoomId);
        sendCommandToUser(userId, info.getCommandString(), callback);
    }

    /**
     * 发送通知
     * @param cmd 命令id
     * @param cmdTip 见 {@link #CMD_GUEST_JOIN_NOTIFY}
     * @param callback 回调
     */
    public void sendNotify(int cmd, String cmdTip, final SendMessageCallback callback) {
        CommandInfo info = CommandInfo.obtain(cmd, cmdTip, mCurrentRoomId);
        sendCommandToChatRoom(mCurrentRoomId, info.getCommandString(), callback);
    }

    public String getCommandNote(int command) {
        String note = "未知";

        switch (command) {
            case CMD_ANCHOR_INVITE_GUEST:
                note = "cmd:anchor_invite_guest";
                break;
            case CMD_GUEST_AGREE_INVITE:
                note = "cmd:guess_agree_invite";
                break;
            case CMD_GUEST_REFUSE_INVITE:
                note = "cmd:guess_refuse_invite";
                break;
            case CMD_GUEST_ASK_JOIN:
                note = "cmd:guest_ask_join";
                break;
            case CMD_ANCHOR_AGREE_JOIN:
                note = "cmd:anchor_agree_join";
                break;
            case CMD_ANCHOR_REFUSE_JOIN:
                note = "cmd:anchor_refuse_join";
                break;
            case CMD_ANCHOR_KICKOUT_GUEST:
                note = "cmd:anchor_kickout_guset";
                break;
            case CMD_GUEST_JOIN_NOTIFY:
                note = "cmd:guest_join_notify";
                break;
            case CMD_ANCHOR_QUIT_NOTIFY:
                note = "cmd:anchor_quit_notify";
                break;
            case CMD_GUEST_QUIT_NOTIFY:
                note = "cmd:guest_quit_notify";
                break;
            case CMD_GUEST_KICKOUT_NOTIFY:
                note = "cmd:guest_kickout_notify";
                break;
            case CMD_AUDIENT_JOIN_NOTIFY:
                note = "cmd:audient_join_notify";
                break;
            case CMD_AUDIENT_QUIT_NOTIFY:
                note = "cmd:audient_leave_notify";
                break;
        }

        return note;
    }
}
