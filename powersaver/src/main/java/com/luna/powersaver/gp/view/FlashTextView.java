package com.luna.powersaver.gp.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

import com.luna.powersaver.gp.R;
import com.luna.powersaver.gp.utils.ViewUtil;

/**
 * Created by zsigui on 17-1-10.
 */

public class FlashTextView extends TextView {

    private int mViewWidth = 0;
    private TextPaint mPaint;
    private LinearGradient mLinearGradient;
    private Matrix mGradientMatrix;
    private int mTranslate = 0;

    private int colorOne;
    private int colorTwo;

    public FlashTextView(Context context) {
        this(context, null);
    }

    public FlashTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlashTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        colorOne = ViewUtil.getColor(context, R.color.powersaver_sencond_color);
        colorTwo = Color.WHITE;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mGradientMatrix != null) {
            mTranslate += mViewWidth / 7;
            if (mTranslate > 2 * mViewWidth) {
                mTranslate = -mViewWidth;
            }
            mGradientMatrix.setTranslate(mTranslate, 0);
            mLinearGradient.setLocalMatrix(mGradientMatrix);
            postInvalidateDelayed(50);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mViewWidth == 0) {
            mViewWidth = getMeasuredWidth();
            if (mViewWidth > 0) {
                mPaint = getPaint();
                //Shader.TileMode.MIRROR   镜子，反射，反映
                //Shader.TileMode.REPEAT  重复
                mLinearGradient = new LinearGradient(0, 0, mViewWidth, 0, new int[]{colorOne, colorTwo},
                        null, Shader.TileMode.CLAMP);
                mPaint.setShader(mLinearGradient);
                mGradientMatrix = new Matrix();

            }
        }
    }
}
