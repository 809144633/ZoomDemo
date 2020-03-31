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

import androidx.core.widget.NestedScrollView;

import com.yolo.zoomdemo.R;

/**
 * @author Administrator
 * 缩放对象需在xml中设置android:tag="zoom"标签
 * 滑动位移对象需在xml中设置android:tag="content"标签
 */
public class ZoomLayout extends NestedScrollView {
    private static final String TAG = "ZoomLayout";
    public static final String ZOOM = "zoom";
    public static final String CONTENT = "content";
    private final float MAX_SENSITIVITY = 1f;
    private final float MIN_SENSITIVITY = 0.1f;

    private boolean zoomEnable;
    //放大灵敏度
    private float sensitivity;
    private View zoomView;
    private View contentView;
    private int touchSlop;

    float curY;
    float lastY;

    //触碰滑动偏移量
    float shiftOffset;

    float zoomScale = 1f;
    int translateScale;

    //记录位移控件的原始顶部位置
    private int originTop = -1;

    //判断是否缩放状态
    boolean hasZoom = false;
    ValueAnimator zoomAnimator;
    ValueAnimator translateAnimator;

    float interceptLastY;
    float interceptCurY;

    public ZoomLayout(Context context) {
        this(context, null);
    }

    public ZoomLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.ZoomLayout);
            zoomEnable = array.getBoolean(R.styleable.ZoomLayout_zoom_enable, false);
            sensitivity = array.getFloat(R.styleable.ZoomLayout_zoom_sensity, MIN_SENSITIVITY);
            array.recycle();
        }
        sensitivity = Math.min(MAX_SENSITIVITY, Math.max(sensitivity, MIN_SENSITIVITY));
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
                String tag = (String) childView.getTag();
                if (ZOOM.equals(tag) && zoomView == null) {
                    zoomView = childView;
                }
                if (CONTENT.equals(tag) && contentView == null) {
                    contentView = childView;
                }
                if (contentView == null || zoomView == null) {
                    if (childView instanceof ViewGroup) {
                        findChildTags(childView);
                    }
                }
            }
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (originTop == -1) {
            originTop = contentView.getTop();
        }
        curY = ev.getY();

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                if (lastY == 0) {
                    lastY = curY;
                }
                shiftOffset = curY - lastY;
                if (shiftOffset > 0 && zoomEnable) {
                    Log.e(TAG, "onTouchEvent: " + shiftOffset);
                    if (isTop() && isTouchPointInView(contentView, (int) ev.getX(), (int) ev.getY())) {
                        hasZoom = true;
                        zoomScale = (zoomScale + shiftOffset * sensitivity / 1000);
                        zoomView.setScaleX(zoomScale);
                        zoomView.setScaleY(zoomScale);
                        translateScale = (int) (shiftOffset * sensitivity / 2 + 0.5f);
                        MarginLayoutParams lp = (MarginLayoutParams) contentView.getLayoutParams();
                        int topMargin = lp.topMargin;
                        topMargin += translateScale;
                        lp.setMargins(lp.leftMargin, topMargin, lp.rightMargin, lp.bottomMargin);
                        contentView.setLayoutParams(lp);
                    }
                }
                lastY = curY;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (hasZoom && zoomEnable) {
                    zoomAnimator = ValueAnimator.ofFloat(zoomScale, 1).setDuration(200);
                    zoomAnimator.addUpdateListener((animation) -> {
                        zoomScale = (float) animation.getAnimatedValue();
                        zoomView.setScaleY(zoomScale);
                        zoomView.setScaleX(zoomScale);
                    });
                    translateAnimator = ValueAnimator.ofInt(contentView.getTop(), originTop).setDuration(200);
                    translateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Integer topMargin = (Integer) animation.getAnimatedValue();
                            MarginLayoutParams lp = (MarginLayoutParams) contentView.getLayoutParams();
                            lp.setMargins(lp.leftMargin, topMargin, lp.rightMargin, lp.bottomMargin);
                            contentView.setLayoutParams(lp);
                        }
                    });
                    zoomAnimator.start();
                    translateAnimator.start();
                    hasZoom = false;
                }
                lastY = 0;
                break;
            default:
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (zoomAnimator.isRunning()) {
            zoomAnimator.cancel();
            zoomAnimator.removeAllUpdateListeners();
            zoomAnimator = null;
        }
        if (translateAnimator.isRunning()) {
            translateAnimator.cancel();
            translateAnimator.removeAllUpdateListeners();
            translateAnimator = null;
        }
    }

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
            default:
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * 点击区域判断
     *
     * @param view
     * @param x
     * @param y
     * @return
     */
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
