package com.jackiez.materialdemo.extra.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.extra.utils.UIUtil;

/**
 * Created by zsigui on 16-12-19.
 */

public class GCMonthDateView extends MonthDateView {

    private final int BASE = 360;
    private final int SCREEN_WIDTH;

    private float decMissRightPaddingY = 7;
    private float decMissRightPaddingX = 4;
    private Bitmap bpChecked;
    private Bitmap bpMissed;
    private Paint bpPaint;

    private int picBase = 25;
    private int dateState = 0x01FC;

    public final static class STATE {
        public static final int MISSED = 0;
        public static final int CHECKED = 1;
    }

    public GCMonthDateView(Context context) {
        this(context, null);
    }

    public GCMonthDateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GCMonthDateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        SCREEN_WIDTH = context.getResources().getDisplayMetrics().widthPixels;
        initValFirst();
    }

    private void initValFirst() {

        float scale = (float) SCREEN_WIDTH / BASE;
        weekendHeight = (int) (22 * scale);
        dateBgHeight = (int) (220 * scale);
        weekendTextSize = dateTextSize = (int) (12 * scale);
        decMissRightPaddingY *= scale;
        decMissRightPaddingX *= scale;
        datePaddingRight = datePaddingLeft = (int) (15 * scale);
        datePaddingTop = (int) ( 9 * scale);
        picBase *= scale;

        weekendBgPaint.setColor(UIUtil.getColor(getContext(), R.color.co_dateview_week_bg));
        weekendTextPaint.setColor(UIUtil.getColor(getContext(), R.color.co_dateview_text));
        dateTextPaint.setColor(UIUtil.getColor(getContext(), R.color.co_dateview_text));
        dateBgPaint.setColor(UIUtil.getColor(getContext(), R.color.co_dateview_date_bg));
        bpPaint = new Paint();
        bpPaint.setAntiAlias(true);
        bpPaint.setStyle(Paint.Style.FILL);
        Log.d("testAddApkDownloadInfo", "dateBgHeight = " + dateBgHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        initPicIfNeeded();
    }

    private void initPicIfNeeded() {
        if (bpChecked == null || bpMissed == null) {
            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inPreferredConfig = Bitmap.Config.RGB_565;
            op.inJustDecodeBounds = false;
            Bitmap src = BitmapFactory.decodeResource(getResources(), R.drawable.ic_date_checked, op);
            float scale = (float) picBase / src.getWidth();
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            bpChecked = resetPic(src, matrix);
            src = BitmapFactory.decodeResource(getResources(), R.drawable.ic_date_miss, op);
            bpMissed = resetPic(src, matrix);
        }

    }

    private Bitmap resetPic(Bitmap src, Matrix matrix) {
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    @Override
    protected void drawDecoration(Canvas canvas, int num, int x, int y, int width, int height) {
        if (num < 0 || num > dateInToday) {
            return;
        }
        switch (getSomeDayState(num)) {
            case STATE.CHECKED:
                canvas.drawBitmap(bpChecked, x + (width - bpChecked.getWidth()) / 2,
                        y + (height - bpChecked.getHeight()) / 2, bpPaint);
                break;
            case STATE.MISSED:
                canvas.drawBitmap(bpMissed, x + (width - bpMissed.getWidth() + decMissRightPaddingX) / 2,
                        y + (height - bpMissed.getHeight() - decMissRightPaddingY) / 2, bpPaint);
                break;
        }
    }

    public int getSomeDayState(int num) {
        return dateState >> (num - 1) & 0x0001;
    }
}
