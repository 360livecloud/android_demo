
package com.qihoo.videocloud.view;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * 类说明：实现文字跑马灯效果(有无焦点都实现跑马灯)
 */
public class MarqueeTextView extends TextView {
    public MarqueeTextView(Context con) {
        super(con);
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEllipsize(TruncateAt.MARQUEE);
        setFocusableInTouchMode(true);
        setMarqueeRepeatLimit(-1);
        setSingleLine(true);
    }

    public MarqueeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setEllipsize(TruncateAt.MARQUEE);
        setFocusableInTouchMode(true);
        setMarqueeRepeatLimit(-1);
        setSingleLine(true);
    }

    @Override
    public boolean isFocused() {
        return true;
    }

}
