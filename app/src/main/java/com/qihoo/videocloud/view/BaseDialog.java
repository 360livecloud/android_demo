
package com.qihoo.videocloud.view;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialog;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.utils.AndroidUtil;

/**
 * @author huchengming <br>
 *         自定义dialog
 * 
 */
public class BaseDialog extends AppCompatDialog {
    /* 声明控件：标题、提示内容、取消按钮、确认按钮 */
    private TextView mTitle;
    private TextView mMessage;
    // private TextView mCancel;
    // private TextView mConfirm;
    private LinearLayout mChooseDialog;
    private LinearLayout mListDialog;
    private LinearLayout mChooseBtn;
    private int padding = 24;

    private Context context;

    public BaseDialog(Context context) {
        super(context, R.style.baseDialog);
        this.context = context;
    }

    public BaseDialog(Context context, boolean cancelable,
            OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        this.context = context;
    }

    public BaseDialog(Context context, int theme) {
        super(context, theme);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_layout);
        initView();
        setWidth();
    }

    /**
     * 初始化dialog View;
     */
    private void initView() {
        mTitle = (TextView) findViewById(R.id.dialogTitle);
        mMessage = (TextView) findViewById(R.id.dialogMsg);
        mChooseDialog = (LinearLayout) findViewById(R.id.chooseDialog);
        mListDialog = (LinearLayout) findViewById(R.id.listDialog);
        mChooseBtn = (LinearLayout) findViewById(R.id.chooseBtn);
    }

    /**
     * 为对话框设置标题<br>
     * 
     * @param title 参数为String类型
     */
    public void setTitleText(String title) {
        mTitle.setText(title);
    }

    /**
     * 为对话框设置标题<br>
     * 
     * @param title 参数为int类型
     */
    public void setTitleText(int title) {
        mTitle.setText(title);
    }

    public void setTitleText(String title, int gravity) {
        setTitleText(title);
        mTitle.setGravity(gravity);
    }

    /**
     * 为对话框设置对话内容<br>
     * 
     * @param msg 参数为string类型
     */
    public void setMsgText(String msg) {
        msg = msg.replace("<br>", "\n");
        mMessage.setText(msg);
    }

    public TextView getMessageText() {
        return mMessage;
    }

    /**
     * 为对话框设置对话内容<br>
     * 
     * @param msg 参数为int类型
     */
    public void setMsgText(int msg) {
        String message = context.getString(msg).replace("<br>", "\n");
        mMessage.setText(message);
    }

    /**
     * 是否显示标题
     * 
     * @param has 参数为boolean类型
     */
    public void hasTitle(boolean has) {
        if (!has) {
            mTitle.setVisibility(View.GONE);
            // mBelowTitledivider.setVisibility(View.GONE);
        }
    }

    /**
     * 设置普通选择按钮对话框 通过传入参数判断显示按钮个数<br>
     * 
     * @param textArray
     */
    public void setChooseDialog(TextView[] textArray) {
        mChooseBtn.removeAllViews();
        mListDialog.setVisibility(View.GONE);
        mChooseDialog.setVisibility(View.VISIBLE);
        int max = textArray.length - 1;
        TextView p = new TextView(getContext());
        LayoutParams param = new LayoutParams(0,
                AndroidUtil.dp2px(getContext(), 1), 1);
        p.setLayoutParams(param);
        mChooseBtn.addView(p);
        if (max == 0) {
            LayoutParams params = new LayoutParams(AndroidUtil.dp2px(getContext(), 88),
                    AndroidUtil.dp2px(getContext(), 36));
            params.setMargins(AndroidUtil.dp2px(context, padding), 0, AndroidUtil.dp2px(context, padding), AndroidUtil.dp2px(context, 16));
            textArray[0].setLayoutParams(params);
            textArray[0].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textArray[0].setTextColor(context.getResources().getColor(
                    R.color.common_text_blue));
            textArray[0].setGravity(Gravity.CENTER);
            textArray[0].setBackgroundResource(R.drawable.dialog_white_item_bg);
            mChooseBtn.addView(textArray[0]);

        } else {
            for (int i = 0; i < textArray.length; i++) {
                int color = 0;
                if (i != textArray.length - 1) {
                    color = context.getResources().getColor(
                            R.color.list_text_title);
                } else {
                    color = context.getResources().getColor(
                            R.color.common_text_blue);
                }
                LayoutParams params = new LayoutParams(AndroidUtil.dp2px(getContext(), 88),
                        AndroidUtil.dp2px(getContext(), 36));
                params.setMargins(0, 0, AndroidUtil.dp2px(context, padding), AndroidUtil.dp2px(context, padding));
                textArray[i].setLayoutParams(params);
                textArray[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                textArray[i].setTextColor(color);
                textArray[i].setGravity(Gravity.CENTER);
                textArray[i].setBackgroundResource(R.drawable.dialog_white_item_bg);
                mChooseBtn.addView(textArray[i]);
            }
        }
    }

    /**
     * 设置普通选择按钮对话框 通过传入参数判断显示按钮个数<br>
     * 按钮颜色一致
     * @param textArray
     */
    public void setChooseDialog1(TextView[] textArray) {
        mChooseBtn.removeAllViews();
        mListDialog.setVisibility(View.GONE);
        mChooseDialog.setVisibility(View.VISIBLE);
        int max = textArray.length - 1;
        TextView p = new TextView(getContext());
        LayoutParams param = new LayoutParams(0,
                AndroidUtil.dp2px(getContext(), 1), 1);
        p.setLayoutParams(param);
        mChooseBtn.addView(p);
        if (max == 0) {
            LayoutParams params = new LayoutParams(AndroidUtil.dp2px(getContext(), 88),
                    AndroidUtil.dp2px(getContext(), 36));
            params.setMargins(AndroidUtil.dp2px(context, padding), 0, AndroidUtil.dp2px(context, padding), AndroidUtil.dp2px(context, 16));
            textArray[0].setLayoutParams(params);
            textArray[0].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textArray[0].setTextColor(context.getResources().getColor(
                    R.color.common_text_blue));
            textArray[0].setGravity(Gravity.CENTER);
            textArray[0].setBackgroundResource(R.drawable.dialog_white_item_bg);
            mChooseBtn.addView(textArray[0]);

        } else {
            for (int i = 0; i < textArray.length; i++) {
                int color = context.getResources().getColor(
                        R.color.common_text_blue);
                LayoutParams params = new LayoutParams(AndroidUtil.dp2px(getContext(), 88),
                        AndroidUtil.dp2px(getContext(), 36));
                params.setMargins(0, 0, AndroidUtil.dp2px(context, padding), AndroidUtil.dp2px(context, padding));
                textArray[i].setLayoutParams(params);
                textArray[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                textArray[i].setTextColor(color);
                textArray[i].setGravity(Gravity.CENTER);
                textArray[i].setBackgroundResource(R.drawable.dialog_white_item_bg);
                mChooseBtn.addView(textArray[i]);
            }
        }
    }

    /**
     * 设置为列表形式的Dialog<br>
     */
    public void setListDialog(TextView[] textArray) {
        setListDialog(textArray, false, 0);
    }

    /**
     * 设置为列表形式的Dialog<br>
     *
     * @param checkedIndex
     *            默认高亮的选项
     */
    public void setListDialog(TextView[] textArray, int checkedIndex) {
        setListDialog(textArray, true, checkedIndex);
    }

    /**
     * 设置为列表形式的Dialog<br>
     *
     * @param defaultChecked
     *            是否选中某一项
     * @param checkedIndex
     *            默认选中项的index
     */
    public void setListDialog(TextView[] textArray, boolean defaultChecked,
            int checkedIndex) {
        mListDialog.removeAllViews();
        mChooseDialog.setVisibility(View.GONE);
        mListDialog.setVisibility(View.VISIBLE);
        // 循环数组
        for (int i = 0; i < textArray.length; i++) {
            // 创建TextView控件
            // TextView childView = new TextView(context);

            // 设置位置
            textArray[i].setGravity(Gravity.CENTER_VERTICAL);
            // 构造一个LayoutParams
            LayoutParams params = new LayoutParams(
                    LayoutParams.MATCH_PARENT, AndroidUtil.dp2px(context, 72));
            // TextView配置为params
            textArray[i].setLayoutParams(params);
            // textView可点击
            textArray[i].setClickable(true);
            // 设置文字尺寸
            textArray[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textArray[i].setPadding(AndroidUtil.dp2px(context, padding), 0,
                    AndroidUtil.dp2px(context, padding), 0);
            // 设置文字颜色
            if (defaultChecked && i == checkedIndex) {
                textArray[i].setTextColor(context.getResources().getColor(
                        R.color.common_red));
            } else {
                textArray[i].setTextColor(context.getResources().getColor(
                        R.color.list_text_title));
            }
            textArray[i].setBackgroundResource(R.drawable.dialog_white_item_bg);
            mListDialog.addView(textArray[i]);
        }
    }

    /**
     * 是否显示对话内容
     *
     * @param has 参数为boolean类型
     */
    public void hasMsg(boolean has) {
        if (!has) {
            mMessage.setVisibility(View.GONE);
        }
    }

    /**
     * 获取标题文本内容
     *
     * @return返回标题内容 没有返回空
     */
    public String getTitleText() {
        String titleText = mTitle.getText().toString();
        return titleText.equals("") ? "" : titleText;
    }

    /**
     * 获取提示信息文本内容
     *
     * @return 返回标题内容 没有返回空
     */
    public String getMsgText() {
        String msgText = mMessage.getText().toString();
        return (msgText.equals("") || msgText == null) ? "" : msgText;
    }

    private void setWidth() {
        WindowManager wm = ((Activity) context).getWindowManager();
        Display d = wm.getDefaultDisplay();
        WindowManager.LayoutParams params = getWindow()
                .getAttributes();
        params.width = d.getWidth();
        getWindow().setAttributes(params);
    }
}
