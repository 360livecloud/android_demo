
package com.qihoo.videocloud.p2p.setting;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.QHVCPlayer;
import com.qihoo.videocloud.p2p.base.BaseP2PActivity;

public class P2PSettingActivity extends BaseP2PActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.p2p_setting_activity);

        initView();
    }

    private void initView() {
        findViewById(R.id.header_left_icon).setOnClickListener(this);

        Switch togglePrecacheInMobile = (Switch) findViewById(R.id.toggle_enable_p2p_download);
        togglePrecacheInMobile.setChecked(P2PSettingConfig.ENABLE_P2P);
        togglePrecacheInMobile.setOnCheckedChangeListener(this);

        Switch toggleEnableCache = (Switch) findViewById(R.id.toggle_enable_p2p_upload);
        toggleEnableCache.setChecked(P2PSettingConfig.ENABLE_P2P_UPLOAD);
        toggleEnableCache.setOnCheckedChangeListener(this);

        findViewById(R.id.clear_cache).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.header_left_icon: {
                finish();
                break;
            }
            case R.id.clear_cache: {
                QHVCPlayer.clearP2PCache();
                Toast.makeText(this, "清楚缓冲完成！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        switch (compoundButton.getId()) {
            case R.id.toggle_enable_p2p_download: {
                P2PSettingConfig.ENABLE_P2P = isChecked;
                break;
            }
            case R.id.toggle_enable_p2p_upload: {
                P2PSettingConfig.ENABLE_P2P_UPLOAD = isChecked;
                break;
            }
        }
    }
}
