package com.jackiez.materialdemo.extra.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.jackiez.materialdemo.extra.utils.DensityUtil;

import java.util.Calendar;

/**
 * Created by zsigui on 16-12-13.
 */

public class MonthDateView extends View {

    private static final int NO_INDEX = -1;
    private static final int DOUBLE_TAP_TIME_SLOP = 200;
    private static final int LONG_TAP_TIME_SLOP = 750;
//    private static final int TAP_DISTANCE_SLOP = 100;
//    private float downX;
//    private float downY;
    private boolean isJudgeDoubleTap = false;
    private int lastTouchItemIndex;
    private int lastTouchItemIndexForTap;
    private long lastTouchFromNowInMill;
    private long lastTouchTimeInMill;

    private Runnable oneTapDelayRunnable;

//    /**
//     * 自动适配两边的留空, 大致等于 (screen_width - measure_width("25") * 7 ) / 8 / 2
//     */
//    private boolean isAutoSidePadding = true;
//    private int sidePadding = 0;

    private final String[] DATE_POS = {"日", "一", "二", "三", "四", "五", "六"};


    private boolean isInitValFirst = false;
    private int dateStart;
    private int dateEnd;
    protected int dateInToday;
    private onItemTapListener mItemTapListener;


    protected int cellWidth;
    protected int cellHeight;
    protected int dateTextSize;
    protected int weekendTextSize;
    protected int weekendHeight;
    protected int dateBgWidth;
    protected int dateBgHeight;

    protected int datePaddingTop;
    protected int datePaddingBottom;
    protected int datePaddingLeft;
    protected int datePaddingRight;


    protected Paint weekendBgPaint;
    protected Paint dateBgPaint;
    protected TextPaint weekendTextPaint;
    protected TextPaint dateTextPaint;

    public MonthDateView(Context context) {
        this(context, null);
    }

    public MonthDateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MonthDateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        calculateTime();
        initPaint();
    }

    public void setItemTapListener(onItemTapListener itemTapListener) {
        mItemTapListener = itemTapListener;
    }

    private void initPaint() {
        if (dateTextPaint == null) {
            dateTextPaint = new TextPaint();
            dateTextPaint.setTextSize(dateTextSize);
            dateTextPaint.setAntiAlias(true);
            dateTextPaint.setStyle(Paint.Style.FILL);
            dateTextPaint.setColor(Color.GRAY);
        }
        if (weekendTextPaint == null) {
            weekendTextPaint = new TextPaint();
            weekendTextPaint.setTextSize(weekendTextSize);
            weekendTextPaint.setAntiAlias(true);
            weekendTextPaint.setStyle(Paint.Style.FILL);
            weekendTextPaint.setColor(Color.RED);
        }
        if (weekendBgPaint == null) {
            weekendBgPaint = new TextPaint();
            weekendBgPaint.setAntiAlias(true);
            weekendBgPaint.setStyle(Paint.Style.FILL);
            weekendBgPaint.setColor(Color.DKGRAY);
        }
        if (dateBgPaint == null) {
            dateBgPaint = new TextPaint();
            dateBgPaint.setAntiAlias(true);
            dateBgPaint.setColor(Color.WHITE);
            dateBgPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        }
    }

    public void setWeekendBgColor(int color) {
        weekendBgPaint.setColor(color);
        postInvalidate();
    }

    public void setWeekendText(int color, int sizeInPx) {
        weekendTextPaint.setColor(color);
        weekendTextPaint.setTextSize(sizeInPx);
        postInvalidate();
    }

    public void setDateText(int color, int sizeInPx) {
        dateTextPaint.setColor(color);
        dateTextPaint.setTextSize(sizeInPx);
        postInvalidate();
    }

    public void setDateBgColor(int color) {
        dateBgPaint.setColor(color);
        postInvalidate();
    }

    public void calculateTime() {
        int year, month, day, dayCount;
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        dateInToday = calendar.get(Calendar.DAY_OF_MONTH) - 1;
        dateStart = 4;
        dateEnd = 34;
        calendar.set(Calendar.DATE, 1);
        calendar.roll(Calendar.DATE, false);
        dayCount = calendar.get(Calendar.DATE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getMeasuredWidth() != 0 && !isInitValFirst) {
            this.dateBgWidth = (dateBgWidth == 0 ? getMeasuredWidth() : dateBgWidth);
            this.cellWidth = (this.dateBgWidth - this.datePaddingLeft - this.datePaddingRight) / 7;
            if (this.dateBgHeight == 0) {
                this.cellHeight = (this.cellHeight == 0 ? this.cellWidth : this.cellHeight);
                this.dateBgHeight = (this.cellHeight * 5 + this.datePaddingTop + this.datePaddingBottom);
            } else {
                this.cellHeight = (this.dateBgHeight - this.datePaddingTop - this.datePaddingBottom) / 5;
            }
            this.dateTextSize = (dateTextSize == 0 ? DensityUtil.dp2px(getContext(), 14) : dateTextSize);
            this.weekendTextSize = (weekendTextSize == 0 ? DensityUtil.dp2px(getContext(), 14) : weekendTextSize);
            this.weekendHeight = (this.weekendHeight == 0 ?
                    this.weekendTextSize + 2 * DensityUtil.dp2px(getContext(), 8) : this.weekendHeight);
            dateTextPaint.setTextSize(dateTextSize);
            weekendTextPaint.setTextSize(weekendTextSize);
            isInitValFirst = true;
        }
        if (((getMeasuredWidth() != dateBgWidth) || getMeasuredHeight() != (dateBgHeight + weekendHeight))
                && (dateBgWidth != 0 && (dateBgHeight + weekendHeight != 0))) {
            setMeasuredDimension(this.dateBgWidth, this.dateBgHeight + weekendHeight);
            Log.d("test", "width = " + this.dateBgWidth + ", height = " + dateBgHeight + ", cw = " + cellWidth + ", ch = " + cellHeight);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int column, row, left, top, right, bottom, num;
        long curTimeInMill;
        boolean hasItemTouch = false;
        for (int i = 0; i < 35; i++) {
            if (i < dateStart || i > dateEnd) {
                continue;
            }
            column = i % 7;
            row = i / 7;
            num = i - dateStart + 1;
            left = datePaddingLeft + column * cellWidth;
            top = weekendHeight + datePaddingTop + row * cellHeight;
            right = left + cellWidth;
            bottom = top + cellHeight;
            if (x > left && x < right
                    && y > top && y < bottom) {
                // 当前处于某个按钮的触发范围
                hasItemTouch = true;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouchItemIndex = i;
                        curTimeInMill = System.currentTimeMillis();
                        lastTouchFromNowInMill = curTimeInMill - lastTouchFromNowInMill;
                        lastTouchTimeInMill = curTimeInMill;
                        if (lastTouchItemIndexForTap == i
                                && lastTouchFromNowInMill < DOUBLE_TAP_TIME_SLOP) {
                            // 判断为双击事件,移除之前判断的单击事件,双击事件必要也可以在此处判断
                            isJudgeDoubleTap = true;
                            removeCallbacks(oneTapDelayRunnable);
                        }
//                        downX = event.getX();
//                        downY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // 如果已经判断为双击,则不响应长按事件
                        if (!isJudgeDoubleTap && lastTouchItemIndex != NO_INDEX
                                && System.currentTimeMillis() - lastTouchTimeInMill > LONG_TAP_TIME_SLOP) {
                            // 长按事件处理
                            if (mItemTapListener != null) {
                                mItemTapListener.onItemLongTap(num);
                            }
                            lastTouchItemIndex = NO_INDEX;
                            lastTouchItemIndexForTap = NO_INDEX;
                        }
                        if (lastTouchItemIndex != NO_INDEX
                                && lastTouchItemIndex != i) {
                            // 滑动移除位置,不进行点击时间判断
                            lastTouchItemIndex = NO_INDEX;
                            lastTouchItemIndexForTap = NO_INDEX;
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
//                        if (lastTouchItemIndex != NO_INDEX && Math.abs(x - downX) < TAP_DISTANCE_SLOP
//                                && Math.abs(y - downY) < TAP_DISTANCE_SLOP) {
                        if (lastTouchItemIndex != NO_INDEX) {
                            curTimeInMill = System.currentTimeMillis();
                            if (isJudgeDoubleTap) {
                                // 双击事件处理
                                if (mItemTapListener != null) {
                                    mItemTapListener.onItemDoubleTap(num);
                                }
                                lastTouchItemIndexForTap = NO_INDEX;
                            } else {
                                // 单击事件处理
                                // 由于处理双击冲突,单击事件需要延迟进行判断,延迟间隔为双击间隔的判断时间
                                final int oneTapItemIndex = num;
                                oneTapDelayRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mItemTapListener != null) {
                                            mItemTapListener.onItemTap(oneTapItemIndex);
                                        }
                                        // 单击事件处理, 清除双击识别,防止冲突
                                        lastTouchItemIndexForTap = NO_INDEX;
                                    }
                                };
                                postDelayed(oneTapDelayRunnable, DOUBLE_TAP_TIME_SLOP);
                                lastTouchItemIndexForTap = lastTouchItemIndex;
                            }
                            lastTouchFromNowInMill = curTimeInMill;
                            lastTouchItemIndex = NO_INDEX;
                        }
                        isJudgeDoubleTap = false;
                        break;
                }
                // 已经找到处理的项,跳出循环
                break;
            }
        }
        if (!hasItemTouch) {
            lastTouchItemIndexForTap = NO_INDEX;
        } else {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制日期表的背景
        drawBg(canvas);

        // 绘制周末栏样式
        int x, y;
        int i = 0;
        for (; i < 7; i++) {
            x = datePaddingLeft + i * cellWidth;
            y = 0;
//            right = x + cellWidth;
//            bottom = y + weekendHeight;
            // 日到六,下标从 0 开始
            drawWeekendText(canvas, i, x, y, cellWidth, weekendHeight);
        }

        // 绘制日期栏样式
        int column, row, num;
        for (i = 0; i < 35; i++) {
            column = i % 7;
            row = i / 7;
            x = datePaddingLeft + column * cellWidth;
            y = weekendHeight + datePaddingTop + row * cellHeight;
//            right = x + cellWidth;
//            bottom = y + cellHeight;
            if (i < dateStart || i > dateEnd) {
                // 跳过最前面和后面的空白,以-1进行指示
                num = -1;
            } else {
                // 1 ~ 31, 指示日期
                num = i - dateStart + 1;
            }
            drawDateText(canvas, num, x, y, cellWidth, cellHeight);
            drawDecoration(canvas, num, x, y, cellWidth, cellHeight);
        }
    }

    protected void drawBg(Canvas canvas) {
        RectF rectF = new RectF(0, 0, dateBgWidth, weekendHeight);
        canvas.drawRect(rectF, weekendBgPaint);
        rectF = new RectF(0, weekendHeight, dateBgWidth, weekendHeight + dateBgHeight);
        canvas.drawRect(rectF, dateBgPaint);
    }

    protected void drawWeekendText(Canvas canvas, int num,
                                 int x, int y,
                                 int width, int height) {
        Paint.FontMetrics fm = weekendTextPaint.getFontMetrics();
        float textWidth = weekendTextPaint.measureText(DATE_POS[num]);
        float textHeight = fm.ascent + fm.leading + fm.descent;
        canvas.drawText(DATE_POS[num],
                x + (width - textWidth) / 2,
                y + (height - textHeight) / 2,
                weekendTextPaint);
    }

    protected void drawDateText(Canvas canvas, int num,
                              int x, int y,
                              int width, int height) {
        if (num < 0) {
            return;
        }
        String s = String.valueOf(num);
        Paint.FontMetricsInt fm = dateTextPaint.getFontMetricsInt();
        float textWidth = dateTextPaint.measureText(s);
        float textHeight = fm.ascent + fm.leading + fm.descent;
        canvas.drawText(s,
                x + (width - textWidth) / 2,
                y + (height - textHeight) / 2,
                dateTextPaint);
    }

    protected void drawDecoration(Canvas canvas, int num,
                                int x, int y,
                                int width, int height) {
        // 重写该方法写装饰
    }


    public interface onItemTapListener {

        void onItemTap(int index);

        void onItemLongTap(int index);

        void onItemDoubleTap(int index);
    }
}
