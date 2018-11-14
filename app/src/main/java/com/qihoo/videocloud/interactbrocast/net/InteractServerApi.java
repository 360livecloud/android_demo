
package com.qihoo.videocloud.interactbrocast.net;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.qihoo.livecloud.network.HttpCallBack;
import com.qihoo.livecloud.network.LCHttpGet;
import com.qihoo.livecloud.network.LCHttpPost;
import com.qihoo.livecloud.network.LiveCloudHttpParam;
import com.qihoo.livecloud.sdk.QHVCSdk;
import com.qihoo.livecloud.sdk.QHVCSdkConfig;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.tools.Stats;
import com.qihoo.livecloud.tools.UrlSafeEncode;
import com.qihoo.videocloud.interactbrocast.data.InteractGlobalManager;
import com.qihoo.videocloud.interactbrocast.main.InteractConstant;
import com.qihoo.videocloud.interactbrocast.modle.InteractIMContext;
import com.qihoo.videocloud.interactbrocast.modle.InteractRoomModel;
import com.qihoo.videocloud.interactbrocast.modle.InteractUserModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by LeiXiaojun on 2018/3/2.
 */

public class InteractServerApi {

    /**
     * 服务端API回调
     */
    public interface ResultCallback<T> {

        void onSuccess(T data);

        void onFailed(int errCode, String errMsg);
    }

    /**
     * 是否测试环境
     */
    private static boolean DEBUG = false;

    private static final String TAG = "InteractServerApi";

    private static final String RELEASE_ENV_URL = "http://livedemo.vcloud.360.cn/api";

    private static Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * 设置是否测试环境，默认为线上环境
     *
     * @param debug true 测试环境，false 线上环境
     */
    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    private static String getServerUrl() {
        return RELEASE_ENV_URL;
    }

    private static Map<String, String> getServerCommonParams() {
        QHVCSdkConfig qhvcSdkConfig = QHVCSdk.getInstance().getConfig();

        Map<String, String> params = new HashMap<>();
        params.put("channelId", InteractGlobalManager.getInstance().getBusinessId());
        params.put("deviceId", qhvcSdkConfig.getMachineId());
        params.put("ts", String.valueOf(System.currentTimeMillis()));
        params.put("sessionId", InteractGlobalManager.getInstance().getSessionId());
        params.put("ostype", "android");
        params.put("modelName", Build.MODEL);
        params.put("appVersion", qhvcSdkConfig.getAppVersion());
        return params;
    }

    private static String getServerAuthorization(Map<String, String> mapParams) {
        String strParams;
        if (mapParams == null || mapParams.isEmpty()) {
            strParams = "";
        } else {
            StringBuilder sbParams = new StringBuilder();
            for (Map.Entry<String, String> entry : mapParams.entrySet()) {
                sbParams.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
            strParams = sbParams.substring(1);
        }

        String sign = "";
        try {
            sign = Stats.getSign(strParams);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return sign;
    }

    /**
     * 通用Get请求
     *
     * @param url URL
     * @param getParams Get参数列表，无需包含公共参数
     * @param callback 结果回调
     */
    private static void commonHttpGet(final String url, HashMap<String, String> getParams, final HttpCallBack callback) {
        if (getParams == null) {
            getParams = new HashMap<>();
        }
        getParams.putAll(getServerCommonParams());

        HashMap<String, String> headerParams = new HashMap<>();
        headerParams.put("Authorization", getServerAuthorization(getParams));

        for (Map.Entry<String, String> entry : getParams.entrySet()) {
            getParams.put(entry.getKey(), UrlSafeEncode.encode(entry.getValue()));
        }

        LiveCloudHttpParam httpParam = new LiveCloudHttpParam();
        httpParam.setParameter(getParams);
        httpParam.setRequestProperty(headerParams);

        new LCHttpGet(url, httpParam, new HttpCallBack() {

            @Override
            public void onSuccess(final String result) {
                Logger.d(TAG, url + " data=" + result);
                if (callback == null) {
                    return;
                }

                int errCode = 0;
                String errMsg = null;
                try {
                    JSONObject json = new JSONObject(result);
                    errCode = json.optInt("errno", 0);
                    errMsg = json.optString("errmsg");
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                if (errCode == 0) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(result);
                        }
                    });
                } else {
                    final int finalErrCode = errCode;
                    final String finalErrMsg = errMsg;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailed(finalErrCode, finalErrMsg);
                        }
                    });
                }
            }

            @Override
            public void onFailed(int errCode, String errMsg) {
                try {
                    JSONObject json = new JSONObject(errMsg);
                    if (json.has("errno")) {
                        errCode = json.optInt("errno");
                    }
                    if (json.has("errmsg")) {
                        errMsg = json.optString("errmsg");
                    }
                } catch (Throwable ignore) {

                }

                final int finalErrCode = errCode;
                final String finalErrMsg = errMsg;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Logger.d(TAG, url + " errCode=" + finalErrCode + ", errMsg=" + finalErrMsg);
                        if (callback != null) {
                            callback.onFailed(finalErrCode, finalErrMsg);
                        }
                    }
                });
            }

            @Override
            public void onProgressAdd(final int add) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onProgressAdd(add);
                        }
                    }
                });
            }
        }).get();
    }

    /**
     * 通用Post请求
     *
     * @param url URL
     * @param postParams Post参数列表，无需包含公共参数
     * @param callback 结果回调
     */
    private static void commonHttpPost(final String url, HashMap<String, String> postParams, final HttpCallBack callback) {
        if (postParams == null) {
            postParams = new HashMap<>();
        }
        postParams.putAll(getServerCommonParams());

        HashMap<String, String> headerParams = new HashMap<>();
        headerParams.put("Authorization", getServerAuthorization(postParams));

        LiveCloudHttpParam httpParam = new LiveCloudHttpParam();
        httpParam.setPostParameter(postParams);
        httpParam.setRequestProperty(headerParams);

        new LCHttpPost(url, httpParam, new HttpCallBack() {

            @Override
            public void onSuccess(final String result) {
                Logger.d(TAG, url + " data=" + result);
                if (callback == null) {
                    return;
                }

                int errCode = 0;
                String errMsg = null;
                try {
                    JSONObject json = new JSONObject(result);
                    errCode = json.optInt("errno", 0);
                    errMsg = json.optString("errmsg");
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                if (errCode == 0) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(result);
                        }
                    });
                } else {
                    final int finalErrCode = errCode;
                    final String finalErrMsg = errMsg;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailed(finalErrCode, finalErrMsg);
                        }
                    });
                }
            }

            @Override
            public void onFailed(int errCode, String errMsg) {
                try {
                    JSONObject json = new JSONObject(errMsg);
                    if (json.has("errno")) {
                        errCode = json.optInt("errno");
                    }
                    if (json.has("errmsg")) {
                        errMsg = json.optString("errmsg");
                    }
                } catch (Throwable ignore) {

                }

                final int finalErrCode = errCode;
                final String finalErrMsg = errMsg;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Logger.d(TAG, url + " errCode=" + finalErrCode + ", errMsg=" + finalErrMsg);
                        if (callback != null) {
                            callback.onFailed(finalErrCode, finalErrMsg);
                        }
                    }
                });
            }

            @Override
            public void onProgressAdd(final int add) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onProgressAdd(add);
                        }
                    }
                });
            }
        }).post();
    }

    /**
     * 用户注册
     *
     * @param callback 结果回调
     */
    public static void userLogin(final ResultCallback<InteractUserModel> callback) {
        String url = getServerUrl() + "/userLogin";

        commonHttpPost(url, null, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback == null) {
                    return;
                }

                try {
                    JSONObject data = new JSONObject(result).optJSONObject("data");
                    try {
                        InteractGlobalManager.getInstance().setImContext(parseImContext(data));
                    } catch (JSONException ignore) {
                    }

                    callback.onSuccess(parseUserModel(data));
                } catch (Throwable ignore) {
                    callback.onFailed(InteractConstant.ERROR_SERVER_CONTENT_PARSE_FAILED, null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    /**
     * 获取房间列表
     *
     * @param userId 用户ID
     * @param roomType 房间类型，参见{@link InteractConstant#ROOM_TYPE_ANCHOR_AND_ANCHOR}等
     * @param callback 结果回调
     */
    public static void getRoomList(String userId, int roomType, final ResultCallback<List<InteractRoomModel>> callback) {
        String url = getServerUrl() + "/getRoomList";

        HashMap<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roomType", String.valueOf(roomType));

        commonHttpPost(url, params, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback == null) {
                    return;
                }

                try {
                    JSONArray json = new JSONObject(result).optJSONArray("data");

                    List<InteractRoomModel> roomModels = new ArrayList<>();
                    for (int i = 0, length = json.length(); i < length; i++) {
                        roomModels.add(parseRoomModel(json.getJSONObject(i)));
                    }

                    callback.onSuccess(roomModels);
                } catch (Throwable ignore) {
                    callback.onFailed(InteractConstant.ERROR_SERVER_CONTENT_PARSE_FAILED, null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    /**
     * 获取房间信息
     *
     * @param userId 用户ID
     * @param roomId 房间ID
     * @param callback 结果回调
     */
    public static void getRoomInfo(String userId, String roomId, final ResultCallback<InteractRoomModel> callback) {
        String url = getServerUrl() + "/getRoomInfo";

        HashMap<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roomId", roomId);

        commonHttpPost(url, params, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback == null) {
                    return;
                }

                try {
                    JSONObject json = new JSONObject(result).optJSONObject("data");
                    callback.onSuccess(parseRoomModel(json));
                } catch (Throwable ignore) {
                    callback.onFailed(InteractConstant.ERROR_SERVER_CONTENT_PARSE_FAILED, null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    /**
     * 创建房间
     *
     * @param userId 用户ID
     * @param roomName 房间名称
     * @param roomType 房间类型，参见{@link InteractConstant#ROOM_TYPE_ANCHOR_AND_ANCHOR}等
     * @param talkType 通话类型，参见{@link InteractConstant#TALK_TYPE_ALL}等
     * @param roomLifeType 房间生命周期，参见{@link InteractConstant#ROOM_LIFE_TYPE_BIND_ANCHOR}等
     * @param maxNum 最大连麦人数
     * @param callback 结果回调
     */
    public static void createRoom(String userId, String roomName, int roomType, int talkType, int roomLifeType, int maxNum, final ResultCallback<InteractRoomModel> callback) {
        String url = getServerUrl() + "/createRoom";

        HashMap<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roomName", roomName);
        params.put("roomType", String.valueOf(roomType));
        params.put("talkType", String.valueOf(talkType));
        params.put("roomLifeType", String.valueOf(roomLifeType));
        params.put("maxNum", String.valueOf(maxNum));

        commonHttpPost(url, params, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback == null) {
                    return;
                }

                try {
                    JSONObject json = new JSONObject(result).optJSONObject("data");
                    callback.onSuccess(parseRoomModel(json));
                } catch (Throwable ignore) {
                    callback.onFailed(InteractConstant.ERROR_SERVER_CONTENT_PARSE_FAILED, null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    /**
     * 加入房间
     *
     * @param userId 用户ID
     * @param roomId 房间ID
     * @param userIdentity 用户身份信息，参见{@link InteractConstant#USER_IDENTITY_ANCHOR}等
     * @param callback 结果回调
     */
    public static void joinRoom(String userId, String roomId, int userIdentity, final ResultCallback<InteractRoomModel> callback) {
        String url = getServerUrl() + "/joinRoom";

        HashMap<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roomId", roomId);
        params.put("identity", String.valueOf(userIdentity));

        commonHttpPost(url, params, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback == null) {
                    return;
                }

                try {
                    JSONObject json = new JSONObject(result).optJSONObject("data");
                    callback.onSuccess(parseRoomModel(json));
                } catch (Throwable ignore) {
                    callback.onFailed(InteractConstant.ERROR_SERVER_CONTENT_PARSE_FAILED, null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    /**
     * 用户离开房间
     *
     * @param userId 用户ID
     * @param roomId 房间ID
     * @param callback 结果回调
     */
    public static void userLeaveRoom(String userId, String roomId, final ResultCallback<InteractRoomModel> callback) {
        String url = getServerUrl() + "/userLeaveRoom";

        HashMap<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roomId", roomId);

        commonHttpPost(url, params, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback == null) {
                    return;
                }

                try {
                    JSONObject json = new JSONObject(result).optJSONObject("data");
                    callback.onSuccess(parseRoomModel(json));
                } catch (Throwable ignore) {
                    callback.onFailed(InteractConstant.ERROR_SERVER_CONTENT_PARSE_FAILED, null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    /**
     * 获取房间用户列表
     *
     * @param userId 用户ID
     * @param roomId 房间ID
     * @param userIdentitys 用户身份信息，参见{@link InteractConstant#USER_IDENTITY_ANCHOR}等
     * @param callback 结果回调
     */
    public static void getRoomUserList(final String userId, String roomId, int[] userIdentitys, final ResultCallback<List<InteractUserModel>> callback) {
        String url = getServerUrl() + "/getRoomUserList";

        HashMap<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roomId", roomId);
        StringBuilder sbUserIdentity = new StringBuilder();
        for (int identity : userIdentitys) {
            sbUserIdentity.append(String.valueOf(identity)).append(",");
        }
        sbUserIdentity.setLength(sbUserIdentity.length() - 1);
        params.put("identity", sbUserIdentity.toString());

        commonHttpPost(url, params, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback == null) {
                    return;
                }

                try {
                    JSONArray json = new JSONObject(result).optJSONArray("data");

                    List<InteractUserModel> userModels = new ArrayList<>();
                    for (int i = 0, length = json.length(); i < length; i++) {
                        userModels.add(parseUserModel(json.getJSONObject(i)));
                    }

                    callback.onSuccess(userModels);
                } catch (Throwable ignore) {
                    callback.onFailed(InteractConstant.ERROR_SERVER_CONTENT_PARSE_FAILED, null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    /**
     * 改变嘉宾/观众身份标识
     *
     * @param userId 用户ID
     * @param roomId 房间ID
     * @param userIdentity 要变更为的用户身份信息，参见{@link InteractConstant#USER_IDENTITY_ANCHOR}等
     * @param callback 结果回调
     */
    public static void changeUserdentity(String userId, String roomId, int userIdentity, final ResultCallback<InteractRoomModel> callback) {
        String url = getServerUrl() + "/changeUserIdentity";

        HashMap<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roomId", roomId);
        params.put("identity", String.valueOf(userIdentity));

        commonHttpPost(url, params, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback == null) {
                    return;
                }

                try {
                    JSONObject json = new JSONObject(result).optJSONObject("data");
                    callback.onSuccess(parseRoomModel(json));
                } catch (Throwable ignore) {
                    callback.onFailed(InteractConstant.ERROR_SERVER_CONTENT_PARSE_FAILED, null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    /**
     * 踢出嘉宾
     * @deprecated
     *
     * @param userId 用户ID
     * @param roomId 房间ID
     * @param guestId 嘉宾ID
     * @param callback 结果回调
     */
    public static void kickGuest(String userId, String roomId, String guestId, final ResultCallback<InteractRoomModel> callback) {
        String url = getServerUrl() + "/kickGuest";

        final HashMap<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roomId", roomId);
        params.put("guestId", guestId);

        commonHttpPost(url, params, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback == null) {
                    return;
                }

                try {
                    JSONObject json = new JSONObject(result).optJSONObject("data");
                    callback.onSuccess(parseRoomModel(json));
                } catch (Throwable ignore) {
                    callback.onFailed(InteractConstant.ERROR_SERVER_CONTENT_PARSE_FAILED, null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    /**
     * 嘉宾离开房间
     * @deprecated
     *
     * @param userId 用户ID
     * @param roomId 房间ID
     * @param callback 结果回调
     */
    public static void guestLeaveRoom(String userId, String roomId, final ResultCallback<InteractRoomModel> callback) {
        String url = getServerUrl() + "/guestLeaveRoom";

        HashMap<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roomId", roomId);

        commonHttpPost(url, params, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback == null) {
                    return;
                }

                try {
                    JSONObject json = new JSONObject(result).optJSONObject("data");
                    callback.onSuccess(parseRoomModel(json));
                } catch (Throwable ignore) {
                    callback.onFailed(InteractConstant.ERROR_SERVER_CONTENT_PARSE_FAILED, null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    /**
     * 观众离开房间
     * @deprecated
     *
     * @param userId 用户ID
     * @param roomId 房间ID
     * @param callback 结果回调
     */
    public static void audienceLeaveRoom(String userId, String roomId, final ResultCallback<InteractRoomModel> callback) {
        String url = getServerUrl() + "/audienceLeaveRoom";

        HashMap<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roomId", roomId);

        commonHttpPost(url, params, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback == null) {
                    return;
                }

                try {
                    JSONObject json = new JSONObject(result).optJSONObject("data");
                    callback.onSuccess(parseRoomModel(json));
                } catch (Throwable ignore) {
                    callback.onFailed(InteractConstant.ERROR_SERVER_CONTENT_PARSE_FAILED, null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    /**
     * 主播解散房间
     *
     * @param userId 用户ID
     * @param roomId 房间ID
     * @param callback 结果回调
     */
    public static void dismissRoom(String userId, String roomId, final ResultCallback<Void> callback) {
        String url = getServerUrl() + "/dismissRoom";

        HashMap<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roomId", roomId);

        commonHttpPost(url, params, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    /**
     * 用户心跳
     *
     * @param userId 用户ID
     * @param roomId 房间ID
     * @param callback 结果回调
     */
    public static void userHeart(String userId, String roomId, final ResultCallback<Void> callback) {
        String url = getServerUrl() + "/userHeart";

        HashMap<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roomId", roomId);

        commonHttpPost(url, params, new HttpCallBack() {

            @Override
            public void onSuccess(String result) {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (callback != null) {
                    callback.onFailed(errCode, errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        });
    }

    private static InteractRoomModel parseRoomModel(JSONObject jsonObj) throws JSONException {
        InteractRoomModel roomModel = new InteractRoomModel();
        roomModel.setRoomId(jsonObj.optString("roomId"));
        roomModel.setRoomName(UrlSafeEncode.decode(jsonObj.optString("roomName")));
        roomModel.setBindRoleId(jsonObj.optString("bindRoleId"));
        roomModel.setOnlineNum(jsonObj.optInt("num"));
        roomModel.setTalkType(jsonObj.optInt("talkType"));
        roomModel.setMaxNum(jsonObj.optInt("maxNum"));
        roomModel.setRoomLifeType(jsonObj.optInt("roomLifeType"));
        roomModel.setCreateTime(jsonObj.optString("createTime"));
        roomModel.setUserIdentity(jsonObj.optInt("identity"));

        JSONArray jsonArray = jsonObj.optJSONArray("list");
        if (jsonArray != null && jsonArray.length() > 0) {
            List<InteractUserModel> userModels = new ArrayList<>();
            for (int i = 0, length = jsonArray.length(); i < length; i++) {
                userModels.add(parseUserModel(jsonArray.getJSONObject(i)));
            }
            roomModel.setUserList(userModels);
        }

        return roomModel;
    }

    private static InteractUserModel parseUserModel(JSONObject jsonObj) throws JSONException {
        InteractUserModel userModel = new InteractUserModel();

        userModel.setUserId(jsonObj.optString("userId"));
        userModel.setNickname(jsonObj.optString("nickname"));
        userModel.setPortraint(jsonObj.optString("portraint"));
        userModel.setIdentity(jsonObj.optInt("identity"));
        userModel.setCreateTime(jsonObj.optString("createTime"));

        return userModel;
    }

    private static InteractIMContext parseImContext(JSONObject jsonObj) throws JSONException {
        if (jsonObj != null) {
            JSONObject jsonIM = jsonObj.optJSONObject("imContext");
            if (jsonIM != null) {
                return new InteractIMContext(jsonIM.optString("vendor"),
                        jsonIM.optString("appKey"), jsonIM.optString("appSecret"));
            }
        }
        return null;
    }
}
