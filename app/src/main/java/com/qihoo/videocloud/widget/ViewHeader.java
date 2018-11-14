
package com.qihoo.videocloud.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qihoo.livecloudrefactor.R;

public class ViewHeader extends FrameLayout {
    private RelativeLayout mViewHeader;
    private ImageView mLeftIcon;
    private TextView mLeftTitle;
    private TextView mCenterTitle;
    private ImageView mRightIcon, mRightIcon2;
    private TextView mRightTitle;
    private TextView mRightTitleRange;

    public ViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mViewHeader = (RelativeLayout) inflater.inflate(R.layout.view_header, this, false);
        mLeftIcon = (ImageView) mViewHeader.findViewById(R.id.headerLeftIcon);
        mLeftTitle = (TextView) mViewHeader.findViewById(R.id.headerLeftTitle);
        mCenterTitle = (TextView) mViewHeader.findViewById(R.id.headerCenterTitle);
        mRightIcon = (ImageView) mViewHeader.findViewById(R.id.headerRightIcon);
        mRightIcon2 = (ImageView) mViewHeader.findViewById(R.id.headerRightIcon2);
        mRightTitle = (TextView) mViewHeader.findViewById(R.id.headerRightTitle);
        mRightTitleRange = (TextView) mViewHeader.findViewById(R.id.headerRightTitleRange);
        addView(mViewHeader);
    }

    public RelativeLayout getViewHeader() {
        return mViewHeader;
    }

    public ImageView getLeftIcon() {
        return mLeftIcon;
    }

    public TextView getLeftTitle() {
        return mLeftTitle;
    }

    public TextView getCenterTitle() {
        return mCenterTitle;
    }

    public ImageView getRightIcon() {
        return mRightIcon;
    }

    public ImageView getRightIcon2() {
        return mRightIcon2;
    }

    public TextView getRightTitle() {
        return mRightTitle;
    }

    public TextView getRightRangeTitle() {
        return mRightTitleRange;
    }

    public void setLeftIcon(int drawable) {
        mLeftIcon.setImageResource(drawable);
        mLeftTitle.setVisibility(View.VISIBLE);
    }

    public void setLeftText(String text) {
        if (!TextUtils.isEmpty(text)) {
            mLeftTitle.setText(text);
            mLeftTitle.setVisibility(View.VISIBLE);
        } else {
            mLeftTitle.setVisibility(View.GONE);
        }
    }

    public void setLeftText(int id) {
        setLeftText(this.getContext().getString(id));
    }

    public void setCenterText(String text) {
        if (!TextUtils.isEmpty(text)) {
            mCenterTitle.setText(text);
            mCenterTitle.setVisibility(View.VISIBLE);
        } else {
            mCenterTitle.setVisibility(View.GONE);
        }

    }

    public void setBackgroundTransparent() {
        mViewHeader.setBackgroundColor(0x00000000);
    }

    public void setRightIcon(int drawable) {
        mRightIcon.setImageResource(drawable);
        mRightIcon.setVisibility(View.VISIBLE);
    }

    public void setRightIcon2(int drawable) {
        mRightIcon2.setImageResource(drawable);
        mRightIcon2.setVisibility(View.VISIBLE);
    }

    public void setRightText(int id) {
        setRightText(this.getContext().getString(id));
    }

    public void setRightText(String text) {
        if (!TextUtils.isEmpty(text)) {
            mRightTitle.setText(text);
            mRightTitle.setVisibility(View.VISIBLE);
        } else {
            mRightTitle.setVisibility(View.GONE);
        }
    }

    public void setRightRangeText(String text) {
        if (!TextUtils.isEmpty(text)) {
            mRightTitleRange.setText(text);
            mRightTitleRange.setVisibility(View.VISIBLE);
        } else {
            mRightTitleRange.setVisibility(View.GONE);
        }
    }
}
