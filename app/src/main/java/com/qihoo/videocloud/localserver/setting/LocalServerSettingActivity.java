
package com.qihoo.videocloud.localserver.setting;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.localserver.base.BaseLocalServerActivity;
import com.qihoo.videocloud.localserver.download.LocalServerDownloadActivity;

import net.qihoo.videocloud.LocalServer;

/**
 * Created by LeiXiaojun on 2017/8/21.
 */

public class LocalServerSettingActivity extends BaseLocalServerActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.localserver_setting_activity);

        initView();
    }

    private void initView() {
        findViewById(R.id.header_left_icon).setOnClickListener(this);

        Switch togglePrecacheInMobile = (Switch) findViewById(R.id.toggle_precache_in_mobile);
        togglePrecacheInMobile.setChecked(LocalServer.isEnablePrecacheInMobileNetwork());
        togglePrecacheInMobile.setOnCheckedChangeListener(this);

        Switch toggleEnableCache = (Switch) findViewById(R.id.toggle_enable_cache);
        toggleEnableCache.setChecked(LocalServerSettingConfig.ENABLE_CACHE_WHEN_PAUSE);
        toggleEnableCache.setOnCheckedChangeListener(this);

        findViewById(R.id.download_manager).setOnClickListener(this);
        findViewById(R.id.clear_cache).setOnClickListener(this);

        Switch toggleEnableP2PInPlayer = (Switch) findViewById(R.id.toggle_enable_p2p_in_player);
        toggleEnableP2PInPlayer.setChecked(LocalServerSettingConfig.ENABLE_P2P_IN_PLAYER);
        toggleEnableP2PInPlayer.setOnCheckedChangeListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.header_left_icon: {
                finish();
                break;
            }
            case R.id.download_manager: {
                Intent intent = new Intent(this, LocalServerDownloadActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.clear_cache: {
                LocalServer.clearCache();
                break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        switch (compoundButton.getId()) {
            case R.id.toggle_precache_in_mobile: {
                LocalServer.enablePrecacheInMobileNetwork(isChecked);
                break;
            }
            case R.id.toggle_enable_cache: {
                LocalServerSettingConfig.ENABLE_CACHE_WHEN_PAUSE = isChecked;
                break;
            }
            case R.id.toggle_enable_p2p_in_player: {
                LocalServerSettingConfig.ENABLE_P2P_IN_PLAYER = isChecked;
                break;
            }
        }
    }
}
