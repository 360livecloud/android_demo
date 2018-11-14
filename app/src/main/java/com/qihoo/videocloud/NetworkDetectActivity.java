
package com.qihoo.videocloud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.netease.LDNetDiagnoService.NetworkDetector;
import com.qihoo.livecloudrefactor.R;

/**
 * 网络诊断页
 * Created by jiangyiwang on 2016/7/20.
 */
public class NetworkDetectActivity extends Activity {

    private TextView textConsole;
    private ScrollView scrollView;
    private NetworkDetector networkDetector;

    private String detectResult;

    public static void start(Context context) {
        context.startActivity(new Intent(context, NetworkDetectActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_detect);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        textConsole = (TextView) findViewById(R.id.textConsole);

        networkDetect();
    }

    /**
     * 返回
     *
     * @param view
     */
    public void onClickTopLeftListener(View view) {
        finish();
    }

    private void scrollBottom() {
        scrollView.fullScroll(View.FOCUS_DOWN);
    }

    public void networkDetect() {
        textConsole.setText("");
        networkDetector = new NetworkDetector(this)
                .addDomain("g2.live.360.cn")
                .addDomain("qos.live.360.cn")
                .addDomain("fb.live.360.cn")
                .addDomain("sdk.live.360.cn")
                .addDomain("www.so.com")
                .setDetectorListener(new NetworkDetector.DetectorListener() {
                    @Override
                    public void onOutput(String output) {
                        String content = textConsole.getText().toString();
                        content += output;
                        textConsole.setText(content);
                        scrollBottom();
                    }

                    @Override
                    public void onFinish(String result) {
                        detectResult = result;
                        scrollBottom();
                        //                        ToastUtils.showToast(getApplicationContext(), "诊断结束");
                        //                        LogManager.getInstance().collectEventLog("网络诊断结果:" + result);
                        //                        LogManager.getInstance().doZipAndUpload(UserUtils.getUserId(), detectResult);
                    }
                })
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkDetector != null) {
            networkDetector.destroy();
        }
    }
}
