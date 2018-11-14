
package com.qihoo.videocloud.upload;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloudrefactor.R;

import java.text.NumberFormat;

/**
 * Created by guohailiang on 2017/9/21.
 */

public class CommonProgressDialog extends AlertDialog {

    private String TAG = CommonProgressDialog.class.getSimpleName();

    private ProgressBar mProgress;
    private TextView mCancel;
    private TextView mProgressMessage;
    private TextView mProgressPercent;
    private Handler mViewUpdateHandler;
    private int mMax;
    private CharSequence mMessage;
    private boolean mHasStarted;
    private int mProgressVal;

    private NumberFormat mProgressPercentFormat;
    OnCancelListener mCancelListener;

    public CommonProgressDialog(Context context) {
        super(context);
        initFormats();
    }

    public void setOnCancelListener(OnCancelListener cancelListener) {
        this.mCancelListener = cancelListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress_dialog_upload);
        mProgress = (ProgressBar) findViewById(R.id.pb_progress);
        mProgressMessage = (TextView) findViewById(R.id.tv_progress_message);
        mCancel = (TextView) findViewById(R.id.tv_cancel);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
                if (mCancelListener != null) {
                    mCancelListener.onCancel(CommonProgressDialog.this);
                }
            }
        });
        mProgressPercent = (TextView) findViewById(R.id.tv_percent);
        mViewUpdateHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                int progress = mProgress.getProgress();
                int max = mProgress.getMax();
                double dProgress = (double) progress / (double) (1024 * 1024);
                double dMax = (double) max / (double) (1024 * 1024);
                if (mProgressPercentFormat != null) {
                    double percent = (double) progress / (double) max;
                    SpannableString tmp = new SpannableString(mProgressPercentFormat.format(percent));
                    tmp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            0, tmp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    mProgressPercent.setText(tmp);
                } else {
                    mProgressPercent.setText("");
                }
            }
        };
        onProgressChanged();
        if (mMessage != null) {
            setMessage(mMessage);
        }
        if (mMax > 0) {
            setMax(mMax);
        }
        if (mProgressVal > 0) {
            setProgress(mProgressVal);
        }
    }

    private void initFormats() {
        mProgressPercentFormat = NumberFormat.getPercentInstance();
        mProgressPercentFormat.setMaximumFractionDigits(0);
    }

    private void onProgressChanged() {
        mViewUpdateHandler.sendEmptyMessage(0);

    }

    public void setProgressStyle(int style) {
        //mProgressStyle = style;  
    }

    public int getMax() {
        if (mProgress != null) {
            return mProgress.getMax();
        }
        return mMax;
    }

    public void setMax(int max) {
        if (mProgress != null) {
            mProgress.setMax(max);
            onProgressChanged();
        } else {
            mMax = max;
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        if (mProgress != null) {
            mProgress.setIndeterminate(indeterminate);
        }
    }

    public void setProgress(int value) {
        if (mHasStarted) {
            Logger.d(TAG, "progerss: " + value);
            mProgress.setProgress(value);
            onProgressChanged();
        } else {
            mProgressVal = value;
        }
    }

    @Override
    public void setMessage(CharSequence message) {
        //super.setMessage(message);  
        if (mProgressMessage != null) {
            mProgressMessage.setText(message);
        } else {
            mMessage = message;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mHasStarted = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHasStarted = false;
    }

}
