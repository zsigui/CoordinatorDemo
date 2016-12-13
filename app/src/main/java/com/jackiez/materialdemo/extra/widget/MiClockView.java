package com.jackiez.materialdemo.extra.widget;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.view.View;

import com.jackiez.materialdemo.extra.utils.UIUtil;

/**
 * 仿MIUI 系统时钟，带触摸变形
 * Created by zsigui on 16-12-13.
 */
public class MiClockView extends View {

    private int clockSize = 400;
    private float centerX;
    private float centerY;
    private int outRingBorderRadius = 186;
    private float outRingBorderStrokeWidth = 1;
    private int dashRingRadius = 140;
    private int dashStrokeWidth = 20;
    private float dashWidth = 2;
    private int secondTriangleLength = 16;
    private int trianglePointerOffset = 8;
    private int minuteLength = 80;
    private float minuteBottomRadius = 1.5f;
    private float minuteTopRadius = 1f;
    private int hourLength = 75;
    private float hourBottomRadius = 2f;
    private float hourTopRadius = 1.25f;
    private int inRingRadius = 8;
    private int inRingStrokeWidth = 5;
    private int textSize = 20;
    private int textPadding = 1;

    private float zDepthScaleRing = 130;
    private float zDepthTriangleRing = 100;
    private float zDepthHourRing = 50;
    private float zDepthMinuteRing = 0;

    private int canvasRotateY;
    private int canvasRotateX;

    private int textAngle = 5;
    private String[] textStr;
    private float[][] textCoordinate;


    private Paint outRingPaint;
    private Paint textPaint;
    private Paint secondPaint;
    private Paint secondShaderPaint;
    private Paint minutePaint;
    private Paint hourPaint;
    private Paint inRingPaint;

    private Path secondPath;
    private Path minutePath;
    private Path hourPath;


    private int clockColor = Color.WHITE;
    private int darkerColor = Color.WHITE;


    private Matrix matrix = new Matrix();
    private Matrix matrixShader = new Matrix();
    private Camera camera = new Camera();

    private Shader gradientShader;


    private float secondRotateDegree = 40;
    private float minuteRotateDegree = 170;
    private float hourRotateDegree = 0;

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
        this.clockColor = clockColor;
        darkerColor = UIUtil.getDarkerColor(this.clockColor);
    }


    private float getProgressDegree() {
        return 0;
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
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rotateCanvas(canvas);
        drawClock(canvas);
    }

    private void drawClock(Canvas canvas) {

        // 绘制圆弧面
        outRingPaint.setStyle(Paint.Style.STROKE);
        canvas.save();
        translateCanvas(canvas, 0f, 0f, zDepthScaleRing);
        RectF rectF = new RectF(centerX - outRingBorderRadius, centerY - outRingBorderRadius,
                centerX + outRingBorderRadius, centerY + outRingBorderRadius);
        for (int i = 0; i < textStr.length; i++) {
            canvas.drawArc(rectF, 90 * i + textAngle, 90 - 2 * textAngle, false, outRingPaint);
            canvas.drawText(textStr[i], textCoordinate[i][0], textCoordinate[i][1], textPaint);
        }
        canvas.restore();

        // 绘制秒相关
        canvas.save();
        translateCanvas(canvas, 0f, 0f, zDepthTriangleRing);
        canvas.drawCircle(centerX, centerY, dashRingRadius, secondShaderPaint);
        canvas.rotate(secondRotateDegree, centerX, centerY);
        canvas.drawPath(secondPath, secondPaint);
        canvas.restore();

        // 绘制分针
        canvas.save();
        translateCanvas(canvas, 0f, 0f, zDepthMinuteRing);
        canvas.rotate(minuteRotateDegree, centerX, centerY);
        canvas.drawPath(minutePath, minutePaint);
        canvas.restore();

        // 绘制时针
        canvas.save();
        translateCanvas(canvas, 0f, 0f, zDepthHourRing);
        canvas.rotate(hourRotateDegree, centerX, centerY);
        canvas.drawPath(hourPath, hourPaint);
        canvas.restore();

        // 绘制指针圆心
        canvas.save();
        translateCanvas(canvas, 0f, 0f, zDepthHourRing);
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
        textPadding *= scale;

    }

    private void initOutRing() {
        outRingPaint = new Paint();
        outRingPaint.setColor(darkerColor);
        outRingPaint.setAntiAlias(true);
        outRingPaint.setStyle(Paint.Style.STROKE);
        outRingPaint.setStrokeWidth(outRingBorderStrokeWidth);
    }

    private void initInnerRing() {
        inRingPaint = new Paint();
        inRingPaint.setStrokeWidth(inRingStrokeWidth);
        inRingPaint.setColor(clockColor);
        inRingPaint.setAntiAlias(true);
        inRingPaint.setStyle(Paint.Style.STROKE);
    }

    private void initTextCoordinate() {
        textPaint = new Paint();
        textPaint.setTextSize(textSize);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(true);
        textPaint.setColor(darkerColor);
        float twoTextWidth = textPaint.measureText("12");
        float oneTextWidth = textPaint.measureText("6");
        float halfTextHeight = twoTextWidth / 2;

        textStr = new String[]{"3", "6", "9", "12"};
        textCoordinate = new float[4][2];
        textCoordinate[0][0] = centerX + outRingBorderRadius - oneTextWidth / 2 + textPadding;
        textCoordinate[0][1] = centerY + halfTextHeight;
        textCoordinate[1][0] = centerX - oneTextWidth / 2;
        textCoordinate[1][1] = centerY + outRingBorderRadius + halfTextHeight - textPadding;
        textCoordinate[2][0] = centerX - outRingBorderRadius - oneTextWidth / 2 + textPadding;
        textCoordinate[2][1] = centerY + halfTextHeight;
        textCoordinate[3][0] = centerX - twoTextWidth / 2;
        textCoordinate[3][1] = centerY - outRingBorderRadius + halfTextHeight - textPadding;

    }

    private void initSecondDashRing() {
        matrixShader.postRotate(getProgressDegree(), centerX, centerY);
        gradientShader = new SweepGradient(centerX, centerY, new int[]{clockColor, darkerColor}, null);
        gradientShader.setLocalMatrix(matrixShader);

        secondShaderPaint = new Paint();
        secondShaderPaint.setStrokeWidth(dashStrokeWidth);
        secondShaderPaint.setShader(gradientShader);
        secondShaderPaint.setStyle(Paint.Style.STROKE);
        secondShaderPaint.setPathEffect(new DashPathEffect(new float[]{dashWidth, dashWidth}, 0));
    }

    private void initTriangleSecondPointer() {
        secondPaint = new Paint();
        secondPaint.setColor(UIUtil.getDarkerColor(darkerColor));
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
        minutePaint.setColor(clockColor);
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
        hourPaint.setColor(darkerColor);
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
}
