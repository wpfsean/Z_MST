package com.tehike.client.mst.app.project.ui.views.bitmapview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

/**
 * Created by ZKTH
 * Time ： 2018/11/23.9:48
 * data ：
 */

public class PointImageView extends android.support.v7.widget.AppCompatImageView {

    private final float MAX_RADIUS = 35; //圆的最大半径
    private boolean first = true;
    private int interval = 300; //缩小速率
    private Paint paint;
    private Point point;
    private float radius = MAX_RADIUS; //圆的半径
    private float speed = 34.9f; //圆缩小的速度

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {

            radius -= speed;

            if (radius < 0) {
                radius = MAX_RADIUS;
            }

            invalidate();
            postDelayed(runnable, interval);
        }
    };

    public PointImageView(Context context) {
        super(context);
        init();
    }

    public PointImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PointImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);
    }

    /**
     * 设置圆心
     *
     * @param point
     */
    public void setPoint(Point point) {
        if (this.point != point) {
            this.point = point;
        }
        invalidate();
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (point != null) {
            canvas.drawCircle(point.x, point.y, radius, paint);
            if (first) {
                post(runnable);
                first = false;
            }

        }

    }

    /**
     * 清除红圈
     */
    public void clearCircle() {
        point = null;
        first = true;
        removeCallbacks(runnable);
        invalidate();
    }
}
