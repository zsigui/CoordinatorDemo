package com.luna.powersaver.gp.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
/**
 * Created by zsigui on 17-1-9.
 */

public class ECGView extends View implements Runnable {

    // 指明基准线位置
    private int baselineValue = 0;
    // 存储绘制点
    private ArrayList<Point> mPoints;
    private Paint mPaint;
    private int mStrokeSize = 10;
    private int mStrokeColor = Color.CYAN;
    private Path mECGPath;
    private int xChange = 0;


    public static final int TYPE_STEADY = 0;
    public static final int TYPE_VIOLENT = 1;
    private int type = TYPE_VIOLENT;

    private static final int INVALIDATE_TIME_DIFF = 20;
    private static final int X_MOVE_DIFF = 10;
    private int elapse = 50;
    private int MAX_Y_DIFF = 200;


    // 绘制百分比
    private Paint mCirclePaintBg;
    private Paint mCirclePaintFg;
    private RectF mCircleRectF;
    private int mCircleRadius = 200;
    private int percent = 100;
    private int maxAngle = 270;
    private int startAngle = -225;


    public ECGView(Context context) {
        this(context, null);
    }

    public ECGView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ECGView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mCirclePaintBg = new Paint();
        mCirclePaintBg.setStyle(Paint.Style.STROKE);
        mCirclePaintBg.setStrokeWidth(mStrokeSize);
        mCirclePaintBg.setColor(Color.GRAY);
        mCirclePaintBg.setAntiAlias(true);
        mCirclePaintBg.setStrokeCap(Paint.Cap.ROUND);
        mCirclePaintFg = new Paint();
        mCirclePaintFg.setStrokeWidth(mStrokeSize);
        mCirclePaintFg.setStyle(Paint.Style.STROKE);
        mCirclePaintFg.setAntiAlias(true);
        mCirclePaintFg.setStrokeCap(Paint.Cap.ROUND);
        mCirclePaintFg.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

        mPoints = new ArrayList<>();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        baselineValue = (int) (0.5f * getMeasuredHeight());
        if (mPoints.isEmpty()) {
            initPointData();

            int centerX = getMeasuredWidth() / 2;
            int centerY = getMeasuredHeight() / 2;
            SweepGradient gradient = new SweepGradient(
                    centerX,
                    centerY,
                    new int[]{Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED},
                    new float[]{0.2f, 0.4f, 0.6f, 0.8f}
            );
            Matrix matrix = new Matrix();
            matrix.setRotate(90, centerX, centerY);
            gradient.setLocalMatrix(matrix);
            mCirclePaintFg.setShader(gradient);


            mCircleRectF = new RectF(centerX - mCircleRadius, centerY - mCircleRadius, centerX + mCircleRadius, centerY + mCircleRadius);
        }
    }

    public void setStrokeSize(int strokeSize) {
        mStrokeSize = strokeSize;
        postInvalidate();
    }

    public void setStrokeColor(int strokeColor) {
        mStrokeColor = strokeColor;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        mPaint.setStrokeWidth(mStrokeSize);
        mPaint.setColor(mStrokeColor);
        canvas.drawPath(mECGPath, mPaint);

        canvas.drawArc(mCircleRectF, startAngle, maxAngle, false, mCirclePaintBg);
        canvas.drawArc(mCircleRectF, startAngle, maxAngle * percent / 100, false, mCirclePaintFg);
    }

    private Handler mHandler = new Handler();
    private boolean mIsRunning = false;
    private int mSteadyIndex = 0;

    public void startAnim() {
        stopAnim();
        mIsRunning = true;
//        initPointData();
        mHandler.post(this);
    }

    private void initPointData() {
        int x = 0;
        mPoints.add(new Point(x, 0));
        for (int i = 0; i < 8; i ++) {
            x += X_MOVE_DIFF;
            mPoints.add(new Point(x, 0));
        }
        switch (type) {
            case TYPE_STEADY:
                int y = 0;
                mSteadyIndex = 0;
                while (x <= getMeasuredWidth()) {
                    x += 2 * X_MOVE_DIFF;
                    switch ((++mSteadyIndex) % 4) {
                        case 0:
                        case 2:
                            y = 0;
                            break;
                        case 1:
                            y = MAX_Y_DIFF;
                            break;
                        case 3:
                            y = -MAX_Y_DIFF;
                            break;
                    }
                    mPoints.add(new Point(x, y));
                }
                break;
            case TYPE_VIOLENT:
                Log.w("test", "初始化，width = " +getMeasuredWidth());
                while (x <= getMeasuredWidth()) {
                Log.w("test", "初始化，width = " + x);
                    x += X_MOVE_DIFF;
                    mPoints.add(new Point(x, (int) ((Math.random() * MAX_Y_DIFF * 2) - MAX_Y_DIFF)));
                }
                break;
        }
    }

    public void stopAnim() {
        mIsRunning = false;
        mHandler.removeCallbacks(this);
        mPoints.clear();
    }

    @Override
    public void run() {
        if (!mIsRunning)
            return;

        if (mPoints.isEmpty()) {
            return;
        }

        int i = 0;
//        Log.d("test", "当前pointSize = " + mPoints.size() + ", xChange = " + xChange + ", y = " + baselineValue
//                + ", getMeasureWidth = " + getMeasuredWidth() + ", getMeasureHeight = " + getMeasuredHeight());
        Point p = mPoints.get(i++);
        mECGPath = new Path();
        mECGPath.moveTo(p.x + xChange, p.y + baselineValue);
//        Log.d("test", "move to x = " + (p.x + xChange) + ", y = " + (p.y + baselineValue));
        for (; i < mPoints.size(); i++) {
            p = mPoints.get(i);
            mECGPath.lineTo(p.x + xChange, p.y + baselineValue);
//            Log.d("test", "line to x = " + (p.x + xChange) + ", y = " + (p.y + baselineValue));
        }
        postInvalidate();

        xChange -= X_MOVE_DIFF;
        handleChangePoint();
//        Log.d("test", "当前xChange = " + xChange);
        mHandler.postDelayed(this, INVALIDATE_TIME_DIFF);
    }

//    private void handleChangePoint() {
//
//    }

    private void handleChangePoint() {
        int i = 0;

        Point p = mPoints.get(i);
        while (p.x + xChange < 0) {
            p = mPoints.get(++i);
        }
        for (i -= 1; i >= 0; i--) {
            mPoints.remove(i);
            switch (type) {
                case TYPE_VIOLENT:
                    xChange += X_MOVE_DIFF;
                    break;
                case TYPE_STEADY:
                    xChange += 2 * X_MOVE_DIFF;
                    break;
            }
        }

        i = mPoints.size() - 1;
        p = mPoints.get(i);
        Point newP;
        switch (type) {
            case TYPE_VIOLENT:
                while (p.x + xChange < getMeasuredWidth()) {
                    newP = new Point(p.x + X_MOVE_DIFF, (int) ((Math.random() * MAX_Y_DIFF * 2) - MAX_Y_DIFF));
                    mPoints.add(newP);
                    p = newP;
                }
                break;
            case TYPE_STEADY:
                int y = 0;
                while (p.x + xChange < getMeasuredWidth()) {
                    switch ((++mSteadyIndex) % 4) {
                        case 0:
                        case 2:
                            y = 0;
                            break;
                        case 1:
                            y = MAX_Y_DIFF;
                            break;
                        case 3:
                            y = -MAX_Y_DIFF;
                            break;
                    }
                    newP = new Point(p.x + 2 * X_MOVE_DIFF, y);
                    mPoints.add(newP);
                    p = newP;
                }
                break;
        }
    }
}
