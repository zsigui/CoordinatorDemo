package com.luna.powersaver.gp.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.luna.powersaver.gp.R;
import com.luna.powersaver.gp.utils.ViewUtil;

import java.util.Locale;

/**
 * Created by zsigui on 17-1-9.
 */

public class CircleProgressView extends View {


    private static final int DIAMETER_BORDER_RATE = 28;
    private static final int MAX_ANGLE = 360;
    private static final int START_ANGLE = -90;
    private static final int DIAMETER_TEXT_RATE = 4;

    // 绘制百分比
    private Paint mCirclePaintBg;
    private Paint mCirclePaintFg;
    private Paint mTextPaint;
    private RectF mCircleRectF;
    private int mStrokeSize = 10;
    private int mDiameter = 200;
    private int mPercent = 100;
    private int textX;
    private int textY;

    private int mWidth;
    private int mHeight;

    private static final int ROTATE_ANGLE_CHANGE = 6;
    private static final int ANIM_INTERVAL = 50;
    private boolean isCircleAnim = false;
    private int rotateAngle = 0;


    public CircleProgressView(Context context) {
        this(context, null);
    }

    public CircleProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mCirclePaintBg = new Paint();
        mCirclePaintBg.setStyle(Paint.Style.STROKE);
        mCirclePaintBg.setStrokeWidth(mStrokeSize);
        mCirclePaintBg.setColor(ViewUtil.getColor(context, R.color.powersaver_progress_bg));
        mCirclePaintBg.setAntiAlias(true);
        mCirclePaintBg.setStrokeCap(Paint.Cap.ROUND);
        mCirclePaintFg = new Paint();
        mCirclePaintFg.setStrokeWidth(mStrokeSize);
        mCirclePaintFg.setStyle(Paint.Style.STROKE);
        mCirclePaintFg.setAntiAlias(true);
        mCirclePaintFg.setStrokeCap(Paint.Cap.ROUND);
        mCirclePaintFg.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        mCirclePaintFg.setColor(ViewUtil.getColor(context, R.color.powersaver_item_color));

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setColor(ViewUtil.getColor(context, R.color.powersaver_main_color));

        mCircleRectF = new RectF();
    }


    public void setPercent(int percent) {
        this.mPercent = percent;
        postInvalidate();
    }

    public void setStrokeSize(int strokeSize) {
        mStrokeSize = strokeSize;
        postInvalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getMeasuredHeight() != 0 && getMeasuredWidth() != 0
                && (mWidth != getMeasuredWidth() || mHeight != getMeasuredHeight())) {
            mWidth = getMeasuredWidth();
            mHeight = getMeasuredHeight();
            int di = (mHeight > mWidth ? mWidth : mHeight);
            mDiameter = (di - 2 * mStrokeSize);
            mStrokeSize = mDiameter / DIAMETER_BORDER_RATE;
            // 确保不为0
            mStrokeSize = (mStrokeSize == 0 ? 1 : mStrokeSize);
            int centerX = mHeight / 2;
            int centerY = mWidth / 2;
            int radius = mDiameter / 2;
            mCircleRectF.set(
                    centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius
            );
            mCirclePaintBg.setStrokeWidth(mStrokeSize);
            mCirclePaintFg.setStrokeWidth(mStrokeSize);
            mTextPaint.setTextSize(mDiameter / DIAMETER_TEXT_RATE);
            mTextPaint.setStrokeWidth(mStrokeSize / 2);
            Paint.FontMetrics metrics = mTextPaint.getFontMetrics();
            float height = metrics.ascent + metrics.descent + metrics.leading;
            textX = centerX;
            textY = (int) ((mHeight - height) / 2);
//            startAnim();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getMeasuredWidth() == 0 || getMeasuredHeight() == 0) {
            postInvalidate();
            return;
        }
        int startAngle = START_ANGLE;
        if (isCircleAnim) {
            startAngle = START_ANGLE + rotateAngle;
        }
        canvas.drawArc(mCircleRectF, startAngle, MAX_ANGLE, false, mCirclePaintBg);
        canvas.drawArc(mCircleRectF, startAngle, MAX_ANGLE * mPercent / 100, false, mCirclePaintFg);
        if (isCircleAnim) {
            rotateAngle += ROTATE_ANGLE_CHANGE;
        }
        String s = String.format(Locale.getDefault(), "%d%%", mPercent);
        canvas.drawText(s, textX, textY, mTextPaint);
        if (isCircleAnim) {
            postInvalidateDelayed(ANIM_INTERVAL);
        }
    }

    public void startAnim() {
        if (!isCircleAnim) {
            isCircleAnim = true;
            postInvalidate();
        }
    }

    public void stopAnim() {
        if (isCircleAnim) {
            isCircleAnim = false;
            rotateAngle = 0;
            postInvalidate();
        }
    }

}
