
package com.qihoo.videocloud.debug;

import android.text.TextUtils;

import com.qihoo.livecloud.sdk.QHVCServerAddress;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.videocloud.utils.QHVCSharedPreferences;

/**
 * Created by guohailiang on 2017/7/10.
 */

public class Setting {
    private static final String TAG = Setting.class.getSimpleName();

    private static final String KEY_URL_SCHEDULE = "KEY_URL_SCHEDULE";
    private static final String KEY_URL_STAT = "KEY_URL_STAT";
    private static final String KEY_URL_MERGE = "KEY_URL_MERGE";
    private static final String KEY_URL_MIC = "KEY_URL_MIC";
    private static final String KEY_URL_FEEDBACK = "KEY_URL_FEEDBACK";
    private static final String KEY_URL_CONTROL = "KEY_URL_CONTROL";
    private static final String KEY_CURR_CONFIG = "KEY_CURR_CONFIG";

    public static QHVCServerAddress readServerAddress() {
        QHVCSharedPreferences sp = QHVCSharedPreferences.getInstence();
        String schedule = sp.getString(KEY_URL_SCHEDULE, null);
        String stat = sp.getString(KEY_URL_STAT, null);
        String merge = sp.getString(KEY_URL_MERGE, null);
        String mic = sp.getString(KEY_URL_MIC, null);
        String feedback = sp.getString(KEY_URL_FEEDBACK, null);
        String control = sp.getString(KEY_URL_CONTROL, null);

        if (!TextUtils.isEmpty(schedule)
                && !TextUtils.isEmpty(stat)
                && !TextUtils.isEmpty(merge)
                && !TextUtils.isEmpty(mic)
                && !TextUtils.isEmpty(feedback)
                && !TextUtils.isEmpty(control)) {

            QHVCServerAddress serverAddress = new QHVCServerAddress(schedule, stat, merge, mic, feedback, control);

            if (Logger.LOG_ENABLE) {
                Logger.i(TAG, "read serverAddress: " + serverAddress.toString());
            }

            return serverAddress;
        } else {

            if (Logger.LOG_ENABLE) {
                Logger.w(TAG, "read serverAddress. data incomplete.");
            }

            return null;
        }
    }

    public static void saveServerAddress(QHVCServerAddress serverAddress) {

        if (serverAddress == null) {
            if (Logger.LOG_ENABLE) {
                Logger.w(TAG, "save serverAddress.data is null.");
            }
            return;
        }

        if (Logger.LOG_ENABLE) {
            Logger.w(TAG, "save serverAddress: " + serverAddress.toString());
        }

        QHVCSharedPreferences sp = QHVCSharedPreferences.getInstence();
        sp.putStringValue(KEY_URL_SCHEDULE, serverAddress.getSchedule());
        sp.putStringValue(KEY_URL_STAT, serverAddress.getStat());
        sp.putStringValue(KEY_URL_MERGE, serverAddress.getMerge());
        sp.putStringValue(KEY_URL_MIC, serverAddress.getMic());
        sp.putStringValue(KEY_URL_FEEDBACK, serverAddress.getFeedback());
        sp.putStringValue(KEY_URL_CONTROL, serverAddress.getControl());
    }

    public static String getCurrServerConfig() {
        QHVCSharedPreferences sp = QHVCSharedPreferences.getInstence();
        String config = sp.getString(KEY_CURR_CONFIG, "");

        return config;
    }

    public static void saveCurrServerConfig(String currConfig) {
        QHVCSharedPreferences sp = QHVCSharedPreferences.getInstence();
        sp.putStringValue(KEY_CURR_CONFIG, currConfig);
    }

}
