package com.jackiez.materialdemo.extra.widget;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * 仿MIUI 系统时钟，带触摸变形
 * Created by zsigui on 16-12-13.
 */
public class MiClockView extends View {

    // 控制各项大小
    private int clockSize = 400;
    private float centerX;
    private float centerY;
    private int outRingBorderRadius = 186;
    private float outRingBorderStrokeWidth = 1;
    private int dashRingRadius = 145;
    private int dashStrokeWidth = 20;
    private float dashWidth = 3.141516f * dashRingRadius / 180;
    private int secondTriangleLength = 18;
    private int trianglePointerOffset = 15;
    private int minuteLength = 105;
    private float minuteBottomRadius = 1.5f;
    private float minuteTopRadius = 1f;
    private int hourLength = 80;
    private float hourBottomRadius = 2f;
    private float hourTopRadius = 1.25f;
    private int inRingRadius = 8;
    private int inRingStrokeWidth = 5;
    private int textSize = 24;

    private float zDepthScaleRing = 100;
    private float zDepthTriangleRing = 70;
    private float zDepthHourRing = 30;
    private float zDepthMinuteRing = 0;

    private float canvasRotateY;
    private float canvasRotateX;

    // 字体相关
    private int textAngle = 5;
    private String[] textStr;
    private float[][] textCoordinate;

    // 画笔
    private Paint outRingPaint;
    private Paint textPaint;
    private Paint secondPaint;
    private Paint secondShaderPaint;
    private Paint minutePaint;
    private Paint hourPaint;
    private Paint inRingPaint;

    // 路径
    private Path secondPath;
    private Path minutePath;
    private Path hourPath;

    // 钟表色彩
    private int minuteColor = 0xccffffff;
    private int hourColor = 0xccfefefe;
    private int triColor = 0xeefefefe;
    private int outRingColor = 0x80ffffff;

    // 变换相关
    private Matrix matrix = new Matrix();
    private Matrix matrixShader = new Matrix();
    private Camera camera = new Camera();
    private Shader gradientShader;

    // 时分秒角度
    private float secondRotateDegree = 40;
    private float minuteRotateDegree = 170;
    private float hourRotateDegree = 0;
    private float canvasMaxRotateDegree = 20;


    // 动画
    private ValueAnimator boundAnim;
    private ValueAnimator timeAnim;

    public MiClockView(Context context) {
        this(context, null);
    }

    public MiClockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiClockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setClockColor(@ColorInt int clockColor) {
        if (clockColor == 0)
            return;
    }


    private float getProgressDegree() {
        return (secondRotateDegree + 270) % 360;
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getMeasuredWidth() != 0 && gradientShader == null) {
            initVal();
            initHourPointer();
            initInnerRing();
            initMinutePointer();
            initOutRing();
            initSecondDashRing();
            initTextCoordinate();
            initTriangleSecondPointer();
            updateTimePointer();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rotateCanvas(canvas);
        drawClock(canvas);
    }

    private void drawClock(Canvas canvas) {
        matrixShader.reset();
        matrixShader.setRotate(getProgressDegree(), centerX, centerY);
        gradientShader.setLocalMatrix(matrixShader);

        // 绘制圆弧面
        outRingPaint.setStyle(Paint.Style.STROKE);
        canvas.save();
        translateCanvas(canvas, canvasRotateX * 0.8f, canvasRotateY * 0.8f, zDepthScaleRing);
        RectF rectF = new RectF(centerX - outRingBorderRadius, centerY - outRingBorderRadius,
                centerX + outRingBorderRadius, centerY + outRingBorderRadius);
        for (int i = 0; i < textStr.length; i++) {
            canvas.drawArc(rectF, 90 * i + textAngle, 90 - 2 * textAngle, false, outRingPaint);
            canvas.drawText(textStr[i], textCoordinate[i][0], textCoordinate[i][1], textPaint);
        }
        canvas.restore();

        // 绘制秒相关
        canvas.save();
        translateCanvas(canvas, canvasRotateX * 0.6f, canvasRotateY * 0.6f, zDepthTriangleRing);
//        canvas.drawCircle(centerX, centerY, dashRingRadius, secondShaderPaint);
        rectF = new RectF(centerX - dashRingRadius, centerY - dashRingRadius,
                centerX + dashRingRadius, centerY + dashRingRadius);
        canvas.drawArc(rectF, 0f, 360f, false, secondShaderPaint);
        canvas.rotate(secondRotateDegree, centerX, centerY);
        canvas.drawPath(secondPath, secondPaint);
        canvas.restore();

        // 绘制分针
        canvas.save();
        translateCanvas(canvas, canvasRotateX * 0.4f, canvasRotateY * 0.4f, zDepthMinuteRing);
        canvas.rotate(minuteRotateDegree, centerX, centerY);
        canvas.drawPath(minutePath, minutePaint);
        canvas.restore();

        // 绘制时针
        canvas.save();
        translateCanvas(canvas, canvasRotateX * 0.4f, canvasRotateY * 0.4f, zDepthHourRing);
        canvas.rotate(hourRotateDegree, centerX, centerY);
        canvas.drawPath(hourPath, hourPaint);
        canvas.restore();

        // 绘制指针圆心
        canvas.save();
        translateCanvas(canvas, canvasRotateX * 0.4f, canvasRotateY * 0.4f, zDepthHourRing);
        canvas.drawCircle(centerX, centerY, inRingRadius, inRingPaint);
        canvas.restore();
    }

    private void initVal() {
        float scale = (float) getMeasuredWidth() / clockSize;

        centerX = getMeasuredWidth() / 2;
        centerY = getMeasuredHeight() /2;

        zDepthScaleRing *= scale;
        zDepthTriangleRing *= scale;
        zDepthMinuteRing *= scale;
        zDepthHourRing *= scale;

        outRingBorderRadius *= scale;
        outRingBorderStrokeWidth *= scale;
        dashRingRadius *= scale;
        dashStrokeWidth *= scale;
        dashWidth *= scale;
        secondTriangleLength *= scale;
        trianglePointerOffset *= scale;
        minuteLength *= scale;
        minuteBottomRadius *= scale;
        minuteTopRadius *= scale;
        hourLength *= scale;
        hourBottomRadius *= scale;
        hourTopRadius *= scale;
        inRingRadius *= scale;
        inRingStrokeWidth *= scale;
        textSize *= scale;

        Log.e("testAddApkDownloadInfo", "dashWidth = " + dashWidth);
    }

    private void initOutRing() {
        outRingPaint = new Paint();
        outRingPaint.setColor(outRingColor);
        outRingPaint.setAntiAlias(true);
        outRingPaint.setStyle(Paint.Style.STROKE);
        outRingPaint.setStrokeWidth(outRingBorderStrokeWidth);
    }

    private void initInnerRing() {
        inRingPaint = new Paint();
        inRingPaint.setStrokeWidth(inRingStrokeWidth);
        inRingPaint.setColor(minuteColor);
        inRingPaint.setAntiAlias(true);
        inRingPaint.setStyle(Paint.Style.STROKE);
    }

    private void initTextCoordinate() {
        textPaint = new Paint();
        textPaint.setTextSize(textSize);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(true);
        textPaint.setColor(outRingColor);
        Paint.FontMetrics fm = textPaint.getFontMetrics();

        float twoTextWidth = textPaint.measureText("12");
        float oneTextWidth = textPaint.measureText("6");
        float halfTextHeight = (fm.leading + fm.ascent + fm.descent)  / 2;

        textCoordinate = new float[4][2];
        textCoordinate[0][0] = centerX + outRingBorderRadius - oneTextWidth / 2;
        textCoordinate[0][1] = centerY - halfTextHeight;
        textCoordinate[1][0] = centerX - oneTextWidth / 2;
        textCoordinate[1][1] = centerY + outRingBorderRadius - halfTextHeight;
        textCoordinate[2][0] = centerX - outRingBorderRadius - oneTextWidth / 2;
        textCoordinate[2][1] = textCoordinate[0][1];
        textCoordinate[3][0] = centerX - twoTextWidth / 2;
        textCoordinate[3][1] = centerY - outRingBorderRadius - halfTextHeight;

        textStr = new String[]{"3", "6", "9", "12"};

    }

    private void initSecondDashRing() {
        gradientShader = new SweepGradient(centerX, centerY, new int[]{minuteColor, outRingColor}, null);

        secondShaderPaint = new Paint();
        secondShaderPaint.setStrokeWidth(dashStrokeWidth);
        secondShaderPaint.setShader(gradientShader);
        secondShaderPaint.setStyle(Paint.Style.STROKE);
        secondShaderPaint.setPathEffect(new DashPathEffect(new float[]{dashWidth, 2 * dashWidth}, 0));
    }

    private void initTriangleSecondPointer() {
        secondPaint = new Paint();
        secondPaint.setColor(triColor);
        secondPaint.setStyle(Paint.Style.FILL);
        secondPaint.setAntiAlias(true);

        // 三角形公式获得
        float height = (float) (secondTriangleLength * Math.sqrt(3.0) / 2);

        float point1x = centerX - secondTriangleLength / 2;
        float point1y = centerY - dashRingRadius + dashStrokeWidth + trianglePointerOffset;
        float point2x = centerX;
        float point2y = point1y - height;
        float point3x = centerX + secondTriangleLength / 2;
        // float point3y = point1y;

        secondPath = new Path();
        secondPath.moveTo(point1x, point1y);
        secondPath.lineTo(point2x, point2y);
        secondPath.lineTo(point3x, point1y);
        secondPath.close();
    }

    private void initMinutePointer() {
        minutePaint = new Paint();
        minutePaint.setColor(minuteColor);
        minutePaint.setStyle(Paint.Style.FILL);
        minutePaint.setAntiAlias(true);

        float p1x = centerX - minuteBottomRadius;
        // 不是 -inRingRadius 是保证stroke和指针交接部分会完全重叠
        float p1y = centerY - inRingRadius + 2;
        float p2x = centerX - minuteTopRadius;
        float p2y = p1y - minuteLength + inRingRadius;
        float p3x = centerX;
        float p3y = p2y - minuteTopRadius;
        float p4x = centerX + minuteTopRadius;
//        float p4y = p2y;
        float p5x = centerX + minuteBottomRadius;
//        float p5y = p1y;

        minutePath = new Path();
        minutePath.moveTo(p1x, p1y);
        minutePath.lineTo(p2x, p2y);
        minutePath.quadTo(p3x, p3y, p4x, p2y);
        minutePath.lineTo(p5x, p1y);
        minutePath.close();
    }

    private void initHourPointer() {
        hourPaint = new Paint();
        hourPaint.setColor(hourColor);
        hourPaint.setStyle(Paint.Style.FILL);
        hourPaint.setAntiAlias(true);

        float p1x = centerX - hourBottomRadius;
        // 不是 -inRingRadius 是保证stroke和指针交接部分会完全重叠
        float p1y = centerY - inRingRadius + 2;
        float p2x = centerX - hourTopRadius;
        float p2y = p1y - hourLength + inRingRadius;
        float p3x = centerX;
        float p3y = p2y - hourTopRadius;
        float p4x = centerX + hourTopRadius;
//        float p4y = p2y;
        float p5x = centerX + hourBottomRadius;
//        float p5y = p1y;

        hourPath = new Path();
        hourPath.moveTo(p1x, p1y);
        hourPath.lineTo(p2x, p2y);
        hourPath.quadTo(p3x, p3y, p4x, p2y);
        hourPath.lineTo(p5x, p1y);
        hourPath.close();
    }

    private void translateCanvas(Canvas canvas, float x, float y, float z) {
        matrix.reset();

        camera.save();
        camera.translate(x, y, z);
        camera.getMatrix(matrix);
        camera.restore();

        matrix.preTranslate(-centerX, -centerY);
        matrix.postTranslate(centerX, centerY);
        canvas.concat(matrix);

    }

    private void rotateCanvas(Canvas canvas) {
        matrix.reset();
        camera.save();
        camera.rotateX(canvasRotateX);
        camera.rotateY(canvasRotateY);
        camera.getMatrix(matrix);
        camera.restore();

        // 位移 rotate 的中心点，在执行 camera 变换之前
        matrix.preTranslate(-centerX, -centerY);
        // 恢复之前的位移中心点，在执行 camera 变换之后
        matrix.postTranslate(centerX, centerY);
        canvas.concat(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        cancelBoundAnimIfNeeded();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                rotateCanvasWhenMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                startBoundAnim();
                break;
        }
        return true;
    }

    private void rotateCanvasWhenMove(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;

        float percentX = dx / (getMeasuredWidth() / 2);
        float percentY = dy / (getMeasuredHeight() / 2);

        if (percentX > 1f) {
            percentX = 1f;
        } else if (percentX < -1f) {
            percentX = -1f;
        }

        if (percentY > 1f) {
            percentY = 1f;
        } else if (percentY < -1f){
            percentY = -1f;
        }

        canvasRotateX = canvasMaxRotateDegree * percentX * -1;
        canvasRotateY = canvasMaxRotateDegree * percentY ;
    }

    private void startBoundAnim() {
        cancelBoundAnimIfNeeded();
        PropertyValuesHolder holderRotateX = PropertyValuesHolder.ofFloat("canvasRotateX", canvasRotateX, 0f);
        PropertyValuesHolder holderRotateY = PropertyValuesHolder.ofFloat("canvasRotateY", canvasRotateY, 0f);

        boundAnim = ValueAnimator.ofPropertyValuesHolder(holderRotateX, holderRotateY);
        boundAnim.setDuration(1000);
        boundAnim.setInterpolator(new BounceInterpolator());
        boundAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                canvasRotateX = (float) animation.getAnimatedValue("canvasRotateX");
                canvasRotateY = (float) animation.getAnimatedValue("canvasRotateY");
                invalidate();
            }
        });
        boundAnim.start();
    }

    private void cancelBoundAnimIfNeeded() {
        if (boundAnim != null && (boundAnim.isStarted() || boundAnim.isRunning())) {
            boundAnim.cancel();
            boundAnim = null;
        }
    }


    private void updateTimePointer() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        int second = calendar.get(Calendar.SECOND);
        int minute = calendar.get(Calendar.MINUTE);
        int hour = calendar.get(Calendar.HOUR);
        secondRotateDegree = 360 * ((float) second / 60) % 360;
        minuteRotateDegree = 360 * ((float) (second + minute * 60) / 3600) % 360;
        hourRotateDegree = 360 * ((float) (second + minute * 60 + hour * 3600) / (60 * 60 * 12)) % 360;
    }

    private void startTimeAnim() {
        cancelTimeAnimIfNeeded();

        updateTimePointer();
        timeAnim = ValueAnimator.ofFloat(0f, 360f);
        timeAnim.setDuration(60 * 1000);
        timeAnim.setInterpolator(new LinearInterpolator());
        timeAnim.setRepeatMode(ValueAnimator.RESTART);
        timeAnim.setRepeatCount(ValueAnimator.INFINITE);
        timeAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private final float updateInterval = 30f;
            private float lastVal = 0f;

            private float lastDrawValue = 0;
            private float drawInterval = 0.1f;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                float diff = val - lastVal;
                if (diff < 0) {
                    diff += 360;
                }
                if (diff > updateInterval) {
                    lastVal = val;
                    updateTimePointer();
                }

                float diffDraw = val - lastDrawValue;
                if (diffDraw < 0) {
                    diffDraw += 360;
                }
                if (diffDraw > drawInterval) {
                    lastDrawValue = val;
                    secondRotateDegree = (secondRotateDegree + diffDraw) % 360;
                    invalidate();
                }
            }
        });
        timeAnim.start();
    }

    private void cancelTimeAnimIfNeeded() {
        if (timeAnim != null && (timeAnim.isStarted() || timeAnim.isRunning())) {
            timeAnim.cancel();
            timeAnim = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelTimeAnimIfNeeded();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startTimeAnim();
    }
}
