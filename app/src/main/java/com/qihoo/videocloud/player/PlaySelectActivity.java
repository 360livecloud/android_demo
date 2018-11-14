
package com.qihoo.videocloud.player;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.player.live.LiveConfigActivity;
import com.qihoo.videocloud.player.vod.VodConfigActivity;

public class PlaySelectActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_select);

        findViewById(R.id.btn_live).setOnClickListener(this);
        findViewById(R.id.btn_vod).setOnClickListener(this);
        findViewById(R.id.headerLeftIcon).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.headerLeftIcon: {
                finish();
            }
                break;

            case R.id.btn_live: {
                startActivity(new Intent(PlaySelectActivity.this, LiveConfigActivity.class));
            }
                break;

            case R.id.btn_vod: {
                startActivity(new Intent(PlaySelectActivity.this, VodConfigActivity.class));
            }
                break;

            default:
                break;
        }
    }
}
