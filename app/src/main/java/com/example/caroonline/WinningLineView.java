package com.example.caroonline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class WinningLineView extends View {

    private Paint paint;
    private boolean showLine = false;

    private float startX, startY, endX, endY;

    public WinningLineView(Context context) {
        super(context);
        init();
    }

    public WinningLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WinningLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.rgb(0, 188, 212));
        paint.setStrokeWidth(12f);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        setWillNotDraw(false);
    }

    public void showWinningLine(float startX, float startY, float endX, float endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.showLine = true;
        invalidate();
    }

    public void clearLine() {
        this.showLine = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (showLine) {
            canvas.drawLine(startX, startY, endX, endY, paint);
        }
    }
}