
package com.qihoo.videocloud.stats.tool;

import android.text.TextUtils;

import com.qihoo.livecloud.network.HttpCallBack;
import com.qihoo.livecloud.network.LCHttpGet;
import com.qihoo.livecloud.network.LiveCloudHttpParam;
import com.qihoo.livecloud.recorder.logUtil.RecorderLogger;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.tools.MD5;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by huchengming on 2017/8/21.
 */

public class PublishStatsTool {
    private String TAG = "PublishStatsTool";
    private final String baseURL = "http://sdk.test.live.360.cn/live.php";
    private String privateKey = "1f43a1f1b125fdb376fabee2bdf4d1f6";
    private String liveid;/*服务器返回的liveid*/
    private int mStatTimerCount;/*控住打点速率的计数器*/
    private ExecutorService workThreadPoolExecutor;/*异步线程*/
    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private boolean startPublish = false;/*是否开始推流*/

    public PublishStatsTool() {
        if (workThreadPoolExecutor == null) {
            workThreadPoolExecutor = Executors.newSingleThreadExecutor();
        }
    }

    //    http://sdk.test.live.360.cn/live.php?action=start&bid=xxx&cid=xxx&sn=xxx&title=xxx&tm=xxx&_sign=xxx
    //    http://sdk.test.live.360.cn/live.php?action=start&bid=demo&cid=live_demo_q1&sn=sn&title=title&tm=1503298857&_sign=1247c9d7b25db974b9888ea246aba804
    /**
     * 开始推流打点
     * @param bid
     * @param cid
     * @param sn
     * @param title
     */
    public void start(final String bid, final String cid, final String sn, final String title) {
        workThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                String url = baseURL;
                LiveCloudHttpParam httpParam = new LiveCloudHttpParam();
                LinkedHashMap<String, String> param = new LinkedHashMap<>();
                param.put("action", "start");
                param.put("bid", bid);
                param.put("cid", cid);
                param.put("sn", sn);
                param.put("title", title);
                long tm = System.currentTimeMillis() / 1000;
                param.put("tm", tm + "");
                //                String sign = MD5.encryptMD5(privateKey+"list"+bid+tm);
                String sign = MD5.encryptMD5(privateKey + "start" + bid + cid + sn + title + tm);
                param.put("_sign", sign);
                RecorderLogger.i(TAG, "LiveCloud  --PublishStatsTool-- start--- MD5String =" + privateKey + "start"
                        + bid + cid + sn + title + tm);
                httpParam.setParameter(param);
                HttpCallBack callBack = new HttpCallBack() {
                    @Override
                    public void onSuccess(String result) {
                        startPublish = true;
                        initTimer();/*开始推流心跳打点*/
                        RecorderLogger.i(TAG, "LiveCloud --PublishStatsTool--start" + ", start result: " + result);
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                JSONObject json = new JSONObject(result);
                                if (json != null) {
                                    int encode = getJsonInt(json, "errno", -1000);
                                    String resultMessage = getJsonString(json, "errmsg");
                                    liveid = getJsonString(json, "liveid");
                                }
                            } catch (JSONException e) {
                                RecorderLogger.e(TAG, TAG + "," + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailed(int errCode, String errMessage) {
                        RecorderLogger.i(TAG,
                                TAG + ", PublishStatsTool failed, errCode: " + errCode + ", errMessage: " + errMessage);
                    }

                    @Override
                    public void onProgressAdd(int add) {

                    }
                };

                LCHttpGet httpGet = new LCHttpGet(url, httpParam, callBack);
                httpGet.get();

            }
        });

    }

    //http://sdk.test.live.360.cn/live.php?action=update&liveid=xxx&tm=xxx&_sign=xxx
    /**
     * 推流过程中心跳
     */
    public void update() {
        if (!startPublish) {/*没有推流，不打点*/
            return;
        }
        String url = baseURL;
        LiveCloudHttpParam httpParam = new LiveCloudHttpParam();
        LinkedHashMap<String, String> param = new LinkedHashMap<>();
        param.put("action", "update");
        param.put("liveid", liveid);
        long tm = System.currentTimeMillis() / 1000;
        param.put("tm", tm + "");
        String sign = MD5
                .encryptMD5(privateKey + "update" + liveid + tm);
        param.put("_sign", sign);
        httpParam.setParameter(param);
        HttpCallBack callBack = new HttpCallBack() {
            @Override
            public void onSuccess(String result) {
                if (Logger.LOG_ENABLE) {
                    RecorderLogger.d(TAG, "LiveCloud --PublishStatsTool--update" + ", start result: " + result);
                }
                if (!TextUtils.isEmpty(result)) {
                    try {
                        JSONObject json = new JSONObject(result);
                        if (json != null) {
                            int encode = getJsonInt(json, "errno", 1);
                            String resultMessage = getJsonString(json, "errmsg");
                        }
                    } catch (JSONException e) {
                        if (Logger.LOG_ENABLE) {
                            RecorderLogger.e(TAG, TAG + "," + e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void onFailed(int errCode, String errMessage) {
                if (Logger.LOG_ENABLE) {
                    RecorderLogger.d(TAG, TAG + ", PublishStatsTool update failed, errCode: " + errCode
                            + ", errMessage: " + errMessage);
                }
            }

            @Override
            public void onProgressAdd(int add) {

            }
        };
        LCHttpGet httpGet = new LCHttpGet(url, httpParam, callBack);
        httpGet.get();
    }

    //    http://sdk.test.live.360.cn/live.php?action=stop&liveid=xxx&tm=xxx&_sign=xxx
    /**
     * 推流过程中心跳
     */
    public void stop() {
        if (!startPublish) {/*没有推流，不打点*/
            return;
        }
        RecorderLogger.i(TAG, "LiveCloud --PublishStatsTool--stop()");
        workThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                String url = baseURL;
                LiveCloudHttpParam httpParam = new LiveCloudHttpParam();
                LinkedHashMap<String, String> param = new LinkedHashMap<>();
                param.put("action", "stop");
                param.put("liveid", liveid);
                long tm = System.currentTimeMillis() / 1000;
                param.put("tm", tm + "");
                String sign = MD5
                        .encryptMD5(privateKey + "stop" + liveid + tm);
                param.put("_sign", sign);
                RecorderLogger.i(TAG,
                        "LiveCloud  --PublishStatsTool-- stop--- privateKey =" + privateKey + "------privateStr:" +
                                "action=stop&liveid=" + liveid + "&tm=" + tm + "&_sign" + sign);
                httpParam.setParameter(param);
                HttpCallBack callBack = new HttpCallBack() {
                    @Override
                    public void onSuccess(String result) {
                        startPublish = false;
                        RecorderLogger.i(TAG, "LiveCloud --PublishStatsTool--stop" + ", start result: " + result);
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                JSONObject json = new JSONObject(result);
                                if (json != null) {
                                    int encode = getJsonInt(json, "errno", 1);
                                    String resultMessage = getJsonString(json, "errmsg");
                                }
                            } catch (JSONException e) {
                                if (Logger.LOG_ENABLE) {
                                    Logger.e(TAG, TAG + "," + e.getMessage());
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailed(int errCode, String errMessage) {
                        RecorderLogger.i(TAG, TAG + ", PublishStatsTool update failed, errCode: " + errCode
                                + ", errMessage: " + errMessage);
                    }

                    @Override
                    public void onProgressAdd(int add) {

                    }
                };
                LCHttpGet httpGet = new LCHttpGet(url, httpParam, callBack);
                httpGet.get();
            }
        });
    }

    //    http://sdk.test.live.360.cn/live.php?action=list&bid=xxx&tm=xxx&_sign=xxx
    /**
     * 推流过程中心跳
     */
    public void getList(final String bid, final RespondListener listener) {
        RecorderLogger.i(TAG, "LiveCloud --PublishStatsTool--getList()");
        workThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                String url = baseURL;
                LiveCloudHttpParam httpParam = new LiveCloudHttpParam();
                LinkedHashMap<String, String> param = new LinkedHashMap<>();
                param.put("action", "list");
                param.put("bid", bid);
                long tm = System.currentTimeMillis() / 1000;
                param.put("tm", tm + "");
                String sign = MD5
                        .encryptMD5(privateKey + "list" + bid + tm);
                param.put("_sign", sign);
                RecorderLogger.i(TAG,
                        "LiveCloud  --PublishStatsTool-- getList--- privateKey =" + privateKey + "------privateStr:" +
                                "action=list&bid=" + bid + "&tm=" + tm + "&_sign" + sign);
                httpParam.setParameter(param);
                HttpCallBack callBack = new HttpCallBack() {
                    @Override
                    public void onSuccess(String result) {
                        RecorderLogger.i(TAG, "LiveCloud --PublishStatsTool--getList()" + ", start result: " + result);
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                JSONObject json = new JSONObject(result);
                                if (json != null) {
                                    int encode = getJsonInt(json, "errno", 1);
                                    String errmsg = getJsonString(json, "errmsg");
                                    if (encode == 0 && "ok".equalsIgnoreCase(errmsg)) {
                                        JSONArray jsonArray = json.getJSONArray("id");
                                        List<String> list = getSnList(jsonArray);
                                        if (listener != null) {
                                            listener.onSuccess(list);
                                            return;
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                RecorderLogger.e(TAG, TAG + "," + e.getMessage());
                            }

                            if (listener != null) {
                                listener.onFailed(-1, "no sn list data!");
                            }
                        }
                    }

                    @Override
                    public void onFailed(int errCode, String errMessage) {
                        RecorderLogger.i(TAG, TAG + ", PublishStatsTool update failed, errCode: " + errCode
                                + ", errMessage: " + errMessage);
                        if (listener != null) {
                            listener.onFailed(errCode, errMessage);
                        }
                    }

                    @Override
                    public void onProgressAdd(int add) {

                    }
                };
                LCHttpGet httpGet = new LCHttpGet(url, httpParam, callBack);
                httpGet.get();
            }
        });
    }

    private List<String> getSnList(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();

        if (jsonArray != null && jsonArray.length() > 0) {

            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject o = jsonArray.getJSONObject(i);
                    if (o != null) {
                        String title = getJsonString(o, "");
                        String value = getJsonString(o, "sn");
                        if (!TextUtils.isEmpty(value)) {
                            list.add(value);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return list;
    }

    private int getJsonInt(JSONObject json, String key, int defaultValue) {
        if (json != null && !TextUtils.isEmpty(key)) {
            if (json.has(key)) {
                try {
                    int value = json.getInt(key);
                    return value;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return defaultValue;
    }

    private String getJsonString(JSONObject json, String key) {
        String value = null;
        if (json != null && !TextUtils.isEmpty(key)) {
            if (json.has(key)) {
                try {
                    value = json.getString(key);
                    return value;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }

    /**
     * 推流中的打点
     */
    private void updataStats() {
        mStatTimerCount++;
        workThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                update();
            }
        });
    }

    public void release() {
        unInitTimer();
        if (workThreadPoolExecutor != null) {
            workThreadPoolExecutor.shutdown();
            try {
                workThreadPoolExecutor.awaitTermination(Long.MAX_VALUE,
                        TimeUnit.DAYS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            workThreadPoolExecutor = null;
        }
        RecorderLogger.i(TAG, TAG + ", PublishStatsTool release() ");
    }

    private void initTimer() {
        if (mTimer == null) {
            RecorderLogger.i(TAG, TAG + ", PublishStatsTool initTimer() ");
            mTimer = new Timer();
        }
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                public void run() {
                    if (workThreadPoolExecutor != null) {
                        workThreadPoolExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                updataStats();
                            }
                        });
                    }
                }
            };
            mTimer.scheduleAtFixedRate(mTimerTask, 0, 6000);
        }

    }

    private void unInitTimer() {
        if (Logger.LOG_ENABLE) {
            Logger.w(TAG, TAG + ", PublishStatsTool unInitTimer() ");
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }

    public interface RespondListener {
        void onSuccess(Object result);

        void onFailed(int errCode, String errMessage);
    }
}
