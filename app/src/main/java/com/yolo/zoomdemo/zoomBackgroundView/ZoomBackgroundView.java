package com.yolo.zoomdemo.zoomBackgroundView;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Scroller;

import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.yolo.zoomdemo.R;

import java.util.Arrays;


/**
 * @author Administrator
 */
public class ZoomBackgroundView extends NestedScrollView {
    private final String TAG = "ZoomBackgroundView";
    private boolean zoomEnable;
    private View zoomView;
    private View contentView;
    private int touchSlop;

    public ZoomBackgroundView(Context context) {
        this(context, null);
    }

    public ZoomBackgroundView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomBackgroundView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.ZoomBackgroundView);
            zoomEnable = array.getBoolean(R.styleable.ZoomBackgroundView_zoom_enable, false);
            array.recycle();
        }
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        findChildTags(this);
    }


    private void findChildTags(View view) {
        if (view instanceof ViewGroup) {
            int childCount = ((ViewGroup) view).getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childView = ((ViewGroup) view).getChildAt(i);
                Object tag = childView.getTag();
                if ("zoom".equals(tag) && zoomView == null) {
                    zoomView = childView;
                }
                if ("content".equals(tag) && contentView == null) {
                    contentView = childView;
                }
                if (childView instanceof ViewGroup) {
                    findChildTags(childView);
                }
            }
        }
    }

    float lastY;
    float shiftY;
    float i = 1f;
    float curY;
    float zoomScale;
    float translateScale;
    int originTop;
    boolean hasZoom = false;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (originTop == 0) {
            originTop = contentView.getTop();
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                curY = ev.getY();
                shiftY = Math.abs(curY - lastY);
                if (curY > lastY) {
                    if (isTop() && shiftY > touchSlop && isTouchPointInView(contentView, (int) ev.getX(), (int) ev.getY())) {
                        hasZoom = true;
                        zoomScale = shiftY * 0.001f;
                        i = (i + zoomScale);
                        zoomView.setScaleX(i);
                        zoomView.setScaleY(i);
                        translateScale = shiftY * 0.1f;
                        contentView.layout(
                                contentView.getLeft(),
                                (int) (contentView.getTop() + translateScale),
                                contentView.getLeft() + contentView.getMeasuredWidth(),
                                (int) (contentView.getBottom() + translateScale));
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (hasZoom) {
                    ValueAnimator zoomAnimator = ValueAnimator.ofFloat(i, 1).setDuration(200);
                    zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            i = (float) animation.getAnimatedValue();
                            zoomView.setScaleY(i);
                            zoomView.setScaleX(i);
                        }
                    });
                    zoomAnimator.start();
                    ValueAnimator translateAnimator = ValueAnimator.ofInt(contentView.getTop(), originTop).setDuration(200);
                    translateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            contentView.layout(
                                    contentView.getLeft(),
                                    (Integer) animation.getAnimatedValue(),
                                    contentView.getLeft() + contentView.getMeasuredWidth(),
                                    contentView.getBottom() - (contentView.getTop() - (Integer) animation.getAnimatedValue()));
                        }
                    });
                    translateAnimator.start();
                    hasZoom = false;
                }

                break;
            default:
        }
        lastY = curY;
        return super.onTouchEvent(ev);

    }

    float interceptLastY;
    float interceptCurY;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                interceptLastY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                interceptCurY = ev.getY();
                if (Math.abs(interceptCurY - interceptLastY) > touchSlop) {
                    return true;
                }
        }
        return super.onInterceptTouchEvent(ev);
    }

    private boolean isTouchPointInView(View view, int x, int y) {
        if (view == null) {
            return false;
        }
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();
        return y >= top && y <= bottom && x >= left && x <= right;
    }

    private boolean isTop() {
        return getScrollY() == 0;
    }
}
