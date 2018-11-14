
package com.qihoo.videocloud.debug;

import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.qihoo.livecloud.sdk.QHVCServerAddress;
import com.qihoo.videocloud.VideoCloudApplication;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xutils.x;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by guohailiang on 2017/6/19.
 */

public class DebugData {
    private static final String TAG = DebugData.class.getSimpleName();

    // 云控服务端配置
    private static final String DEMO_CONFIG_URL = "http://sdk.live.360.cn/democfg.php";
    public static final String DEMO_CONFIG_KEY_ONLINE = "online";
    public static final String DEMO_CONFIG_KEY_TEST = "test";
    public static final String DEMO_CONFIG_KEY_DEBUG = "debug";
    private static final String[] DEMO_CONFIG_KEY_ARRAY = {
            DEMO_CONFIG_KEY_ONLINE,
            DEMO_CONFIG_KEY_TEST,
            DEMO_CONFIG_KEY_DEBUG
    };

    private static volatile DebugData instance;

    private Map<String, QHVCServerAddress> demoConfigMap = new HashMap<>();

    public static DebugData getInstance() {
        if (instance == null) {
            synchronized (DebugData.class) {
                if (instance == null) {
                    instance = new DebugData();
                }
            }
        }
        return instance;
    }

    public Map<String, QHVCServerAddress> getDemoConfigMap() {
        return demoConfigMap;
    }

    public void requestDemoConfig(final OnRequestListener listener) {
        x.http().get(new RequestParams(DEMO_CONFIG_URL), new Callback.CacheCallback<String>() {
            @Override
            public boolean onCache(String result) {
                return false;
            }

            @Override
            public void onSuccess(String result) {

                if (!TextUtils.isEmpty(result)) {

                    demoConfigMap.clear();
                    try {
                        JSONTokener jsonParser = new JSONTokener(result);
                        JSONObject data = (JSONObject) jsonParser.nextValue();

                        for (String key : DEMO_CONFIG_KEY_ARRAY) {
                            JSONObject obj = (JSONObject) data.get(key);
                            if (obj != null) {
                                QHVCServerAddress c = new QHVCServerAddress((String) obj.get("schedule"),
                                        (String) obj.get("stat"),
                                        (String) obj.get("merge"),
                                        (String) obj.get("mic"),
                                        (String) obj.get("feedback"),
                                        (String) obj.get("control"));
                                demoConfigMap.put(key, c);
                            }
                        }
                        Log.i(TAG, "cloud config: " + demoConfigMap.toString());
                        listener.onSuccess(demoConfigMap);

                    } catch (JSONException ex) {
                        demoConfigMap.clear();
                        Log.v(TAG, "[request cloud config] parse exception: " + ex.getMessage());
                    }
                } else {
                    Log.v(TAG, "[request cloud config] respond data is empty!");
                }
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                ex.printStackTrace();
                Log.e(TAG, "[request cloud config] onError");
                Toast.makeText(VideoCloudApplication.getInstance(), "置列表请求失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(CancelledException cex) {
                cex.printStackTrace();
                Log.v(TAG, "[request cloud config] onCancelled");
                Toast.makeText(VideoCloudApplication.getInstance(), "置列表请求取消", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinished() {
                Log.v(TAG, "[request cloud config] onFinished");
            }
        });
    }

    public interface OnRequestListener {
        void onSuccess(Map<String, QHVCServerAddress> map);
    }
}
