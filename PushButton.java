package com.example.Readable;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class PushButton extends ImageButton {
    public PushButton(Context context) {
        super(context);
    }

    public PushButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setPressed(boolean pressed) {
        if (pressed) {
            this.setScaleY(0.92f);
            this.setScaleX(0.96f);
        } else {
            this.setScaleY(1.0f);
            this.setScaleX(1.0f);
        }

        super.setPressed(pressed);
    }
}
