
package com.qihoo.videocloud.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.qihoo.livecloudrefactor.R;

/**
 * Created by LeiXiaojun on 2017/9/30.
 */
public class ConfirmDialog extends Dialog {

    private CheckBox mCheckView;

    private View.OnClickListener mOnOkButtonClickListener;
    private View.OnClickListener mOnCancelButtonClickListener;

    public ConfirmDialog(Context context) {
        super(context, R.style.dialog_style);
        setContentView(R.layout.dialog_confirm_layout);

        setCanceledOnTouchOutside(true);

        mCheckView = (CheckBox) findViewById(R.id.check);

        findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnOkButtonClickListener != null) {
                    mOnOkButtonClickListener.onClick(view);
                }
                dismiss();
            }
        });

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnCancelButtonClickListener != null) {
                    mOnCancelButtonClickListener.onClick(view);
                }
                dismiss();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setTitle(int resId) {
        ((TextView) findViewById(R.id.title)).setText(resId);
    }

    public void setContent(int resId) {
        ((TextView) findViewById(R.id.content)).setText(resId);
    }

    public void setCheckout(int resId) {
        if (resId > 0) {
            mCheckView.setVisibility(View.VISIBLE);
            mCheckView.setText(resId);
        } else {
            mCheckView.setVisibility(View.GONE);
        }
    }

    public void setChecked(boolean checked) {
        mCheckView.setChecked(checked);
    }

    public boolean isChecked() {
        return mCheckView.isChecked();
    }

    public void setOkButtonText(int resId) {
        ((TextView) findViewById(R.id.ok)).setText(resId);
    }

    public void setCancelButtonText(int resId) {
        ((TextView) findViewById(R.id.cancel)).setText(resId);
    }

    public void setOnOkButtonOnClickListener(View.OnClickListener onClickListener) {
        mOnOkButtonClickListener = onClickListener;
    }

    public void setOnCancelButtonClickListener(View.OnClickListener onClickListener) {
        mOnCancelButtonClickListener = onClickListener;
    }
}
