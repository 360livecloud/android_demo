
package com.qihoo.videocloud.debug;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.qihoo.livecloud.sdk.QHVCSdk;
import com.qihoo.livecloud.sdk.QHVCSdkConfig;
import com.qihoo.livecloud.sdk.QHVCServerAddress;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.tools.NetUtil;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.player.LogAdapter;
import com.qihoo.videocloud.utils.NoDoubleClickListener;
import com.qihoo.videocloud.widget.ViewHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by guohailiang on 2017/7/10.
 */

public class SettingActivity extends Activity implements View.OnClickListener {
    private static final String TAG = SettingActivity.class.getSimpleName();

    private ViewHeader viewHeaderMine;
    private LinearLayout layoutSpinner;

    private Spinner serverAddressSpinner;
    //private ArrayAdapter serverAddressAdapter;
    private List<String> serverAddressDataList = new ArrayList<>();

    private ListView settingListView;
    private LogAdapter settingAdapter;

    private static final String CURRENT_CONFIG = "当前配置";
    private String configSelectKey = CURRENT_CONFIG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_address_setting);

        readLocalConfig();
        initView();
        initData();
    }

    private void readLocalConfig() {
        String currConfig = Setting.getCurrServerConfig();
        if (TextUtils.isEmpty(currConfig)) {
            currConfig = DebugData.DEMO_CONFIG_KEY_ONLINE;
        }
        Logger.i(TAG, TAG + ", readLocalConfig, configSelectKey : " + currConfig);
        configSelectKey = currConfig;
    }

    private void initView() {
        viewHeaderMine = (ViewHeader) findViewById(R.id.viewHeaderMine);
        layoutSpinner = (LinearLayout) findViewById(R.id.layout_spinner);
        serverAddressSpinner = (Spinner) findViewById(R.id.spinner_server_address);
        findViewById(R.id.btn_set).setOnClickListener(this);
        settingListView = (ListView) findViewById(R.id.lv_setting);

        viewHeaderMine.getLeftIcon().setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                finish();
            }
        });

        serverAddressDataList.add(DebugData.DEMO_CONFIG_KEY_ONLINE);
        serverAddressDataList.add(DebugData.DEMO_CONFIG_KEY_TEST);
        serverAddressDataList.add(DebugData.DEMO_CONFIG_KEY_DEBUG);

        //serverAddressDataList.add(CURRENT_CONFIG);
        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, serverAddressDataList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serverAddressSpinner.setAdapter(adapter);
        serverAddressSpinner.setSelection(0);
        serverAddressSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String key = serverAddressDataList.get(position);
                if (key.equals(CURRENT_CONFIG)) {

                    configSelectKey = key;
                    setSettingDetailContent(getCurrentConfig());
                } else {
                    QHVCServerAddress o = DebugData.getInstance().getDemoConfigMap().get(key);
                    if (o != null) {

                        if (Logger.LOG_ENABLE) {
                            Logger.i(TAG, "select config detail: " + o.toString());
                        }

                        configSelectKey = key;
                        setSettingDetailContent(getStringList(o));
                    } else {
                        if (Logger.LOG_ENABLE) {
                            Logger.e(TAG, "select config is null.");
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        setSettingDetailContent(getCurrentConfig());
    }

    // 配置详细信息
    private void setSettingDetailContent(List<String> list) {

        if (list != null && !list.isEmpty()) {

            if (settingAdapter == null) {

                settingAdapter = new LogAdapter(this, list, R.color.color_666666);
                settingListView.setAdapter(settingAdapter);
            } else {

                settingAdapter.setList(list);
                settingAdapter.notifyDataSetChanged();
            }
        }
    }

    private List<String> getCurrentConfig() {
        List<String> list = new ArrayList<>();

        QHVCSdkConfig o = QHVCSdk.getInstance().getConfig();
        list.add("schedule :" + o.getScheduleUrl());
        list.add("stat :" + o.getStatUrl());
        list.add("merge :" + o.getMergeUrl());
        list.add("mic :" + o.getMicUrl());
        list.add("feedback :" + o.getFeedbackUrl());
        list.add("control :" + o.getControlUrl());
        return list;
    }

    private List<String> getStringList(QHVCServerAddress o) {
        List<String> list = new ArrayList<>();
        if (o != null) {
            list.add("schedule :" + o.getSchedule());
            list.add("stat :" + o.getStat());
            list.add("merge :" + o.getMerge());
            list.add("mic :" + o.getMic());
            list.add("feedback :" + o.getFeedback());
            list.add("control :" + o.getControl());
        }
        return list;
    }

    private void initData() {

        if (!NetUtil.isConnected(this)) {
            Toast.makeText(this, "请检查网络设置", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "正在获取配置列表，请稍等...", Toast.LENGTH_SHORT).show();
        DebugData.getInstance().requestDemoConfig(new DebugData.OnRequestListener() {
            @Override
            public void onSuccess(final Map<String, QHVCServerAddress> map) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (map != null && !map.isEmpty()) {

                            //serverAddressDataList.clear();
                            //serverAddressDataList.add(CURRENT_CONFIG);

                            //serverAddressAdapter.notifyDataSetChanged();
                            serverAddressSpinner.setSelection(getSelectionIndex());
                        } else {
                            Toast.makeText(SettingActivity.this, "配置列表为空", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private int getSelectionIndex() {
        if (configSelectKey.equals(DebugData.DEMO_CONFIG_KEY_ONLINE)) {
            return 0;
        }
        if (configSelectKey.equals(DebugData.DEMO_CONFIG_KEY_TEST)) {
            return 1;
        }
        if (configSelectKey.equals(DebugData.DEMO_CONFIG_KEY_DEBUG)) {
            return 2;
        }

        return 0;
    }

    private void saveConfig() {
        if (!configSelectKey.equals(CURRENT_CONFIG)) {

            Setting.saveServerAddress(DebugData.getInstance().getDemoConfigMap().get(configSelectKey));
            Setting.saveCurrServerConfig(configSelectKey);
            Toast.makeText(this, "配置变更->" + configSelectKey + ",重启app生效!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "配置无变化", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_set: {
                saveConfig();
            }
                break;
        }
    }
}
