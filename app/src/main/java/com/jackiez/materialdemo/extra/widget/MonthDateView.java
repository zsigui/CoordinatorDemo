package com.jackiez.materialdemo.extra.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;

/**
 * Created by zsigui on 16-12-13.
 */

public class MonthDateView extends View {

    private static final int NO_INDEX = -1;
    private static final int DOUBLE_TAP_TIME_SLOP = 100;
    private static final int LONG_TAP_TIME_SLOP = 1000;
//    private static final int TAP_DISTANCE_SLOP = 100;
//    private float downX;
//    private float downY;
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

    private int dateStart;
    private int dateEnd;

    private int btnStartX;
    private int btnStartY;
    private int btnEndX;
    private int btnEndY;
    private onItemTapListener mItemTapListener;


    private int cellWidth;
    private int cellHeight;
    private int dateTextSize;
    private int weekendTextSize;
    private int weekendHeight;
    private int dateBgWidth;
    private int dateBgHeight;

    private int datePaddingTop;
    private int datePaddingBottom;
    private int datePaddingLeft;
    private int datePaddingRight;


    private Paint weekendBgPaint;
    private Paint dateBgPaint;
    private TextPaint mWeekendTextPaint;
    private TextPaint mDateTextPaint;
    private Paint decorationBgPaint;

    private int decorationState;

    public final static class DECORATION_STATE {
        private DECORATION_STATE() {
        }

        public static final int NONE = 0;

        public static final int CHECKED = 1;

        public static final int DISABLED = 2;
    }

    public MonthDateView(Context context) {
        this(context, null);
    }

    public MonthDateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MonthDateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVal();
    }

    private void initVal() {
        mDateTextPaint = new TextPaint();
        mDateTextPaint.setTextSize(dateTextSize);
        mDateTextPaint.setAntiAlias(false);
        mDateTextPaint.setStyle(Paint.Style.FILL);
        mWeekendTextPaint = new TextPaint();
        mWeekendTextPaint.setTextSize(dateTextSize);
        mWeekendTextPaint.setAntiAlias(false);
        mWeekendTextPaint.setStyle(Paint.Style.FILL);
    }

//    public void setSidePadding(int sidePadding) {
//        this.sidePadding = sidePadding;
//        this.isAutoSidePadding = false;
//        postInvalidate();
//    }


    public void cacluteTime() {
        int year, month, day, dayCount;
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH) - 1;
        dateStart = calendar.getFirstDayOfWeek();
        calendar.set(Calendar.DATE, 1);
        calendar.roll(Calendar.DATE, false);
        dayCount = calendar.get(Calendar.DATE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getMeasuredWidth() != 0) {
//            float weekendTextWidth = mWeekendTextPaint.measureText("25");
//            if (isAutoSidePadding) {
//                this.sidePadding = (int) ((width - weekendTextWidth * 7 ) / 8 / 2);
//                this.cellHeight = this.cellWidth = (int) (weekendTextWidth + 2 * this.sidePadding);
//            } else {
//                this.cellWidth = this.cellHeight =  width - 2 * this.sidePadding / 7;
//            }
            this.dateBgWidth = getMeasuredWidth();
            this.cellWidth = (this.dateBgWidth - this.datePaddingLeft - this.datePaddingRight) / 7;
            if (this.dateBgHeight == 0) {
                this.cellHeight = this.cellWidth;
                this.dateBgHeight = this.cellHeight * 5 + this.datePaddingTop + this.datePaddingBottom;
            } else {
                this.cellHeight = (this.dateBgHeight - this.datePaddingTop - this.datePaddingBottom) / 5;
            }

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int column, row, left, top, right, bottom;
        long curTimeInMill;
        boolean hasItemTouch = false;
        for (int i = 0; i < 35; i++) {
            if (i < dateStart || i > dateEnd) {
                continue;
            }
            column = i % 7;
            row = i / 7;
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
                            // 判断为双击事件,移除之前判断的单击事件
                            removeCallbacks(oneTapDelayRunnable);
                        }
//                        downX = event.getX();
//                        downY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (lastTouchItemIndex != NO_INDEX
                                && lastTouchItemIndex != i) {
                            // 滑动移除位置,不进行点击时间判断
                            lastTouchItemIndex = NO_INDEX;
                            lastTouchItemIndexForTap = NO_INDEX;
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        if (lastTouchItemIndex != NO_INDEX) {
//                          if (lastTouchItemIndex != NO_INDEX && Math.abs(x - downX) < TAP_DISTANCE_SLOP
//                                    && Math.abs(y - downY) < TAP_DISTANCE_SLOP) {
                            curTimeInMill = System.currentTimeMillis();
                            if (lastTouchItemIndexForTap == i
                                    && lastTouchFromNowInMill < DOUBLE_TAP_TIME_SLOP) {
                                // 双击事件处理
                                if (mItemTapListener != null) {
                                    mItemTapListener.onItemDoubleTap(i);
                                }
                                lastTouchItemIndexForTap = NO_INDEX;
                            } else if (curTimeInMill - lastTouchTimeInMill > LONG_TAP_TIME_SLOP) {
                                // 长按事件处理
                                if (mItemTapListener != null) {
                                    mItemTapListener.onItemLongTap(i);
                                }
                                lastTouchItemIndexForTap = NO_INDEX;
                            } else {
                                // 单击事件处理
                                // 由于处理双击冲突,单击事件需要延迟进行判断,延迟间隔为双击间隔的判断时间
                                final int oneTapItemIndex = i;
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
                        break;
                }
                // 已经找到处理的项,跳出循环
                break;
            }
        }
        if (!hasItemTouch) {
            lastTouchItemIndexForTap = NO_INDEX;
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
            drawDecoration(canvas, num, x, y, cellWidth, cellHeight, decorationState);
        }

    }

    private void drawBg(Canvas canvas) {
        RectF rectF = new RectF(0, 0, dateBgWidth, weekendHeight);
        canvas.drawRect(rectF, weekendBgPaint);
        rectF = new RectF(0, dateBgHeight, dateBgWidth, weekendHeight + dateBgHeight);
        canvas.drawRect(rectF, dateBgPaint);
    }

    private void drawWeekendText(Canvas canvas, int num,
                                 int x, int y,
                                 int width, int height) {
        Paint.FontMetrics fm = mWeekendTextPaint.getFontMetrics();
        float textWidth = mWeekendTextPaint.measureText(DATE_POS[num]);
        float textHeight = fm.ascent + fm.leading + fm.descent;
        canvas.drawText(DATE_POS[num],
                x + (width - textWidth) / 2,
                y + (height - textHeight) / 2,
                mWeekendTextPaint);
    }

    private void drawDateText(Canvas canvas, int num,
                              int x, int y,
                              int width, int height) {
        String s = String.valueOf(num);
        Paint.FontMetricsInt fm = mDateTextPaint.getFontMetricsInt();
        float textWidth = mDateTextPaint.measureText(s);
        float textHeight = fm.ascent + fm.leading + fm.descent;
        canvas.drawText(s,
                x + (width - textWidth) / 2,
                y + (height - textHeight) / 2,
                mDateTextPaint);
    }

    private void drawDecoration(Canvas canvas, int num,
                                int x, int y,
                                int width, int height, int state) {
        switch (state) {
            case DECORATION_STATE.CHECKED:
                break;
            case DECORATION_STATE.DISABLED:
                break;
            case DECORATION_STATE.NONE:
            default:
        }
    }


    public interface onItemTapListener {

        void onItemTap(int index);

        void onItemLongTap(int index);

        void onItemDoubleTap(int index);
    }
}
