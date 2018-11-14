
package com.qihoo.videocloud.player.live;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qihoo.livecloudrefactor.R;

/**
 * Created by guohailiang on 2017/6/19.
 */

public class LiveConfigActivity extends FragmentActivity implements View.OnClickListener {

    private RelativeLayout rlTab;
    private ImageView headerLeftIcon;
    private TextView tvNoPlayAddress;
    private TextView tvHavePlayAddress;
    private ViewPager viewPager;
    private MyFragmentAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_config);

        initView();
    }

    private void initView() {
        rlTab = (RelativeLayout) findViewById(R.id.rl_tab);
        headerLeftIcon = (ImageView) findViewById(R.id.headerLeftIcon);
        headerLeftIcon.setOnClickListener(this);

        tvNoPlayAddress = (TextView) findViewById(R.id.tv_no_play_address);
        tvNoPlayAddress.setOnClickListener(this);
        tvHavePlayAddress = (TextView) findViewById(R.id.tv_have_play_address);
        tvHavePlayAddress.setOnClickListener(this);
        viewPager = (ViewPager) findViewById(R.id.viewPager);

        viewPagerAdapter = new MyFragmentAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (0 == position) {
                    tvNoPlayAddress.setTextAppearance(LiveConfigActivity.this, R.style.textview_live_config_title_selected);
                    tvHavePlayAddress.setTextAppearance(LiveConfigActivity.this, R.style.textview_live_config_title_normal);
                } else if (1 == position) {
                    tvNoPlayAddress.setTextAppearance(LiveConfigActivity.this, R.style.textview_live_config_title_normal);
                    tvHavePlayAddress.setTextAppearance(LiveConfigActivity.this, R.style.textview_live_config_title_selected);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.headerLeftIcon: {
                finish();
            }
                break;

            case R.id.tv_no_play_address: {
                viewPager.setCurrentItem(0);
            }
                break;

            case R.id.tv_have_play_address: {
                viewPager.setCurrentItem(1);
            }
                break;

            default:
                break;
        }
    }

    private class MyFragmentAdapter extends FragmentPagerAdapter {

        public MyFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new LiveConfigNoAddressFragment();
                case 1:
                    return new LiveConfigHaveAddressFragment();
                default:
                    break;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
