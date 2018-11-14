
package com.qihoo.videocloud.upload;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo.livecloud.sdk.QHVCSdk;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.tools.MD5;
import com.qihoo.livecloud.tools.URLSafeBase64;
import com.qihoo.livecloud.upload.OnUploadListener;
import com.qihoo.livecloud.upload.QHVCUpload;
import com.qihoo.livecloud.upload.QHVCUploadConfig;
import com.qihoo.livecloud.upload.QHVCUploadEvent;
import com.qihoo.livecloud.upload.core.UploadConstant;
import com.qihoo.livecloud.upload.utils.UploadError;
import com.qihoo.livecloud.upload.utils.UploadLogger;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.utils.AndroidUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Random;

public class UploadActivity extends Activity implements View.OnClickListener {

    private final static String TAG = "UploadActivity";

    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = 1000;
    private final static int REQUEST_CODE_FILE_BROWSE = 1;

    private EditText akEditText;
    private EditText skEditText;
    private EditText bidEditText;
    private EditText cidEditText;
    private EditText bucketEditText;
    private EditText fileEditText;
    private EditText maxSpeedEditText;

    private CommonProgressDialog mProgressDialog;

    private QHVCUploadEvent uploadEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkSelfPermissionAndRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);

        initView();
        initData();
    }

    public boolean checkSelfPermissionAndRequest(String permission, int requestCode) {
        Logger.d(TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] {
                            permission
                    },
                    requestCode);
            return false;
        }

        return true;
    }

    private void initView() {
        setContentView(R.layout.activty_upload);

        findViewById(R.id.upload_header_left_icon).setOnClickListener(this);
        findViewById(R.id.upload_file_browse).setOnClickListener(this);
        findViewById(R.id.upload_start).setOnClickListener(this);
        findViewById(R.id.upload_key_logger).setOnClickListener(this);

        akEditText = (EditText) findViewById(R.id.upload_ak);
        skEditText = (EditText) findViewById(R.id.upload_sk);
        bidEditText = (EditText) findViewById(R.id.upload_business_id);
        cidEditText = (EditText) findViewById(R.id.upload_channel_id);
        bucketEditText = (EditText) findViewById(R.id.upload_bucket);
        fileEditText = (EditText) findViewById(R.id.upload_file);
        maxSpeedEditText = (EditText) findViewById(R.id.upload_max_speed);

    }

    private void initData() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_FILE_BROWSE: {
                if (resultCode == Activity.RESULT_OK) {
                    doFileBrowseResult(data);
                }
                break;
            }
            default: {
                break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.upload_header_left_icon: {
                finish();
                break;
            }
            case R.id.upload_file_browse: {
                AndroidUtil.openFileBrowse(this, "请选择一个要上传的文件", "*/*", REQUEST_CODE_FILE_BROWSE);
                break;
            }
            case R.id.upload_start:/*开始上传*/ {
                doUpload();
                break;
            }
            case R.id.upload_key_logger: {
                mProgressDialog = createdProgressDialog();
                mProgressDialog.show();
                int maxSpeed = Integer.parseInt(maxSpeedEditText.getText().toString().trim());
                if (maxSpeed < 0) {
                    maxSpeed = 0;
                }
                QHVCUpload.setUploadLimitSpeed(maxSpeed); // 100kbps
                uploadEvent = QHVCUpload.uploadLog(UploadActivity.this, getUploadConfig(null), blockUploadListener);
                break;
            }
            default:
                break;
        }
    }

    private void doFileBrowseResult(Intent data) {
        String file = AndroidUtil.uriToPath(this, data.getData());
        if (!TextUtils.isEmpty(file)) {
            fileEditText.setText(file);
        }
    }

    private void doUpload() {
        String ak = akEditText.getText().toString().trim();
        if (TextUtils.isEmpty(ak)) {
            AndroidUtil.showToast(this, "AK 不能为空");
            return;
        }

        String sk = skEditText.getText().toString().trim();
        if (TextUtils.isEmpty(sk)) {
            AndroidUtil.showToast(this, "SK 不能为空");
            return;
        }

        String bid = bidEditText.getText().toString().trim();
        if (TextUtils.isEmpty(bid)) {
            AndroidUtil.showToast(this, "Bid 不能为空");
            return;
        }

        String cid = cidEditText.getText().toString().trim();
        if (TextUtils.isEmpty(cid)) {
            AndroidUtil.showToast(this, "Cid 不能为空");
            return;
        }

        String bucket = bucketEditText.getText().toString().trim();
        if (TextUtils.isEmpty(bucket)) {
            AndroidUtil.showToast(this, "Bucket 不能为空");
            return;
        }

        String filename = fileEditText.getText().toString().trim();
        if (TextUtils.isEmpty(filename)) {
            AndroidUtil.showToast(this, "File 不能为空");
            return;
        }

        int maxSpeed = Integer.parseInt(maxSpeedEditText.getText().toString().trim());
        if (maxSpeed < 0) {
            AndroidUtil.showToast(this, "最大速度输入不正确");
            return;
        }

        File file = new File(filename);
        int parallelNum = QHVCUpload.getParallel(file.length());
        //警告：token的计算应该由业务服务端生成，严禁将ak、sk放到客户端，此处放客户端仅用于演示
        String token;
        if (parallelNum == 0) { // 表单上传
            token = getFormToken(file, ak, sk, bucket);
        } else { // 分片上传
            token = getBlockToken(file, parallelNum, ak, sk, bucket);
        }
        mProgressDialog = createdProgressDialog();
        mProgressDialog.show();
        //        uploadEvent = QHVCUpload.uploadFile(file, token, getUploadConfig(file), blockUploadListener);

        String dir = new File(Environment.getExternalStorageDirectory(), "LiveCloud/UploadCache").getAbsolutePath();
        UploadLogger.d(TAG, "dir: " + dir);
        try {
            QHVCUpload.setUploadLimitSpeed(maxSpeed); // 100kbps
            uploadEvent = QHVCUpload.uploadFile(file, token, getUploadConfig(file), blockUploadListener, new FileRecorder(dir), new FileKeyGenerator());
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
        }
    }

    /**
     * 警告：token的计算应该由业务服务端生成，严禁将ak、sk放到客户端，此处放客户端仅用于演示
     */
    private String getFormToken(File file, String ak, String sk, String bucket) {
        String strategyJson = formToken(file.getName(), bucket);
        Logger.d(TAG, strategyJson);
        String safeEncode = URLSafeBase64.encodeToString(strategyJson);
        Logger.d(TAG, safeEncode);
        String sign = MD5.encryptMD5(safeEncode + sk);
        return ak + ":" + sign + ":" + safeEncode;
    }

    private String formToken(String fileName, String bucket) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("bucket", bucket);
            jsonObject.put("object", fileName);
            jsonObject.put("deadline", System.currentTimeMillis() + 3600 * 1000);
            jsonObject.put("insertOnly", 0);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 警告：token的计算应该由业务服务端生成，严禁将ak、sk放到客户端，此处放客户端仅用于演示
     */
    private String getBlockToken(File file, int parallelNum, String ak, String sk, String bucket) {
        String strategyJson = toGetTokenJson(file, parallelNum, bucket);
        UploadLogger.d(UploadConstant.TAG, strategyJson);
        Logger.d(TAG, strategyJson);
        String safeEncode = URLSafeBase64.encodeToString(strategyJson);
        Logger.d(TAG, safeEncode);
        String sign = MD5.encryptMD5(safeEncode + sk);
        return ak + ":" + sign + ":" + safeEncode;
    }

    private String toGetTokenJson(File file, int parallelNum, String bucket) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.putOpt("bucket", bucket)
                    .putOpt("object", file.getName())
                    .putOpt("fsize", file.length())
                    .putOpt("parallel", parallelNum)
                    .putOpt("insertOnly", 0);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    private OnUploadListener blockUploadListener = new OnUploadListener() {
        @Override
        public void onSuccess(String result) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
                showToast(0, null);
            }
            Logger.d(TAG, "ian, onSuccess, result: " + result);
        }

        @Override
        public void onProgress(long total, long progress) {
            if (mProgressDialog != null) {

                if (total > 0 && progress >= 0) {
                    int ps = (int) (progress * 100 / total);
                    mProgressDialog.setProgress(ps);

                    Logger.d(TAG, "percent: " + ps + " total=" + total + " progress=" + progress);
                }
            }
        }

        @Override
        public void onBlockProgress(int total, int currBlock) {
            if (total > 0 && currBlock >= 0) {
                Logger.d(TAG, "block percent." + (int) (currBlock * 100 / total) + " totalBlock: " + total + ", currBlock: " + currBlock);
            }
        }

        @Override
        public void onFailed(UploadError uploadError) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
                showToast(uploadError.getErrCode(), uploadError.getErrMsg());
            }
            Logger.e(TAG, "ian, " + uploadError.toString());
        }
    };

    private QHVCUploadConfig getUploadConfig(File file) {
        QHVCUploadConfig config = new QHVCUploadConfig();

        String bid = bidEditText.getText().toString().trim();
        QHVCSdk.getInstance().getConfig().setBusinessId(bid);

        String cid = cidEditText.getText().toString().trim();
        config.setCid(cid);

        String uid = QHVCSdk.getInstance().getConfig().getUserId();
        config.setUid(uid);

        config.setVer(QHVCSdk.getInstance().getConfig().getAppVersion());

        String sid = MD5.encryptMD5(String.valueOf(System.currentTimeMillis()) + String.valueOf(new Random().nextInt()));
        config.setSid(sid);

        config.setMid(QHVCSdk.getInstance().getConfig().getMachineId());

        config.setNet(QHVCSdk.getInstance().getConfig().getNetworkType());

        String rid = "";
        if (file != null) {
            rid = file.getAbsolutePath();
        }
        config.setRid(rid);

        return config;
    }

    private CommonProgressDialog createdProgressDialog() {
        CommonProgressDialog dialog = new CommonProgressDialog(this);
        dialog.setMessage("上传文件");
        dialog.setMax(100);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancelUpload();
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.setProgress(1);
        return dialog;
    }

    /**
     * 取消上传
     */
    private void cancelUpload() {
        if (uploadEvent != null) {
            uploadEvent.cancel();
        }
    }

    private void showToast(final int errCode, final String errMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View v = View.inflate(UploadActivity.this, R.layout.toast_upload, null);
                ImageView imageView = (ImageView) v.findViewById(R.id.iv_image);
                TextView textView = (TextView) v.findViewById(R.id.tv_text);
                if (errCode == 0) {
                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.upload_toast_success));
                    textView.setText("上传完成");
                } else {
                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.upload_toast_failed));
                    textView.setText("上传失败(" + errCode + " " + errMsg + ")");
                }
                Toast mToast = new Toast(UploadActivity.this);

                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                DisplayMetrics outMetrics = new DisplayMetrics();
                wm.getDefaultDisplay().getMetrics(outMetrics);

                RelativeLayout.LayoutParams vlp = new RelativeLayout.LayoutParams(outMetrics.widthPixels,
                        outMetrics.heightPixels);
                vlp.setMargins(0, 0, 0, 0);
                v.setLayoutParams(vlp);

                mToast.setDuration(Toast.LENGTH_SHORT);
                mToast.setView(v);
                mToast.setGravity(Gravity.FILL, 0, 0);
                mToast.show();
            }
        });
    }
}
