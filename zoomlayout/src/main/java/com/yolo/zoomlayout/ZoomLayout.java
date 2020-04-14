package com.yolo.zoomlayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import androidx.core.widget.NestedScrollView;

/**
 * @author Administrator
 * 缩放对象需在xml中设置android:tag="zoom"标签
 * 可接受滑动的对象需在xml中设置android:tag="touch"标签
 * 滑动位移对象需在xml中设置android:tag="move"标签
 */
public class ZoomLayout extends NestedScrollView {

    private static final String TAG = "ZoomLayout";
    public static final String ZOOM = "zoom";
    public static final String TOUCH = "touch";
    public static final String MOVE = "move";
    private static final float MAX_SENSITIVITY = 1f;
    private static final float MIN_SENSITIVITY = 0.1f;
    private static final int ANIMATOR_DURATION = 200;
    private boolean zoomEnable;

    //放大灵敏度
    private float zoomSensitivity;

    //缩放对象
    private View zoomView;
    //所接触的对象
    private View touchView;
    private View moveView;

    private int touchSlop;

    //最大偏移量
    private float maxOffset;

    private final ValueAnimator zoomAnimator;
    private final ValueAnimator translateAnimator;

    //缩放尺度
    private float zoomScale = 1f;
    //记录位移控件的原始距离顶部位置
    private int originTopMargin = -1;
    private MarginLayoutParams mlp;

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
            zoomEnable = array.getBoolean(R.styleable.ZoomLayout_zoom_enable, true);
            zoomSensitivity = array.getFloat(R.styleable.ZoomLayout_zoom_sensitivity, 0.35f);
            maxOffset = array.getInteger(R.styleable.ZoomLayout_zoom_max_offset, 100);
            array.recycle();
        }
        zoomSensitivity = Math.min(MAX_SENSITIVITY, Math.max(zoomSensitivity, MIN_SENSITIVITY));
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        zoomAnimator = new ValueAnimator();
        zoomAnimator.setDuration(ANIMATOR_DURATION);
        zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                zoomScale = (Float) animation.getAnimatedValue();
                if (zoomView == null) {
                    return;
                }
                zoomView.setScaleY(zoomScale);
                zoomView.setScaleX(zoomScale);
            }
        });
        translateAnimator = new ValueAnimator();
        translateAnimator.setDuration(ANIMATOR_DURATION);
        translateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (moveView == null || mlp == null) {
                    return;
                }
                mlp.setMargins(
                        mlp.leftMargin,
                        (Integer) animation.getAnimatedValue(),
                        mlp.rightMargin,
                        mlp.bottomMargin);
                moveView.setLayoutParams(mlp);
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        findChildTags(this);
        //期望触摸对象与移动对象为同一对象
        if (touchView == null) {
            touchView = moveView;
        }
        if (moveView == null) {
            moveView = touchView;
        }
    }

    private void findChildTags(View view) {
        if (!(view instanceof ViewGroup)) {
            return;
        }
        int childCount = ((ViewGroup) view).getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = ((ViewGroup) view).getChildAt(i);
            //不使用强转，避免childView设置的tag无法强转为String
            String tag = String.valueOf(childView.getTag());
            if (zoomView == null && ZOOM.equals(tag)) {
                zoomView = childView;
            }
            if (touchView == null && TOUCH.equals(tag)) {
                touchView = childView;
            }
            if (moveView == null && MOVE.equals(tag)) {
                moveView = childView;
            }
            if (zoomView != null && touchView != null && moveView != null) {
                break;
            }
            if (childView instanceof ViewGroup) {
                findChildTags(childView);
            }
            if (zoomView != null && touchView != null && moveView != null) {
                break;
            }
        }
    }

    private float curTouchY;
    private float lastTouchY;

    //触碰滑动偏移量
    private float shiftOffset;


    //判断是否缩放状态
    private boolean hasZoom = false;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!zoomEnable || zoomView == null || touchView == null || moveView == null) {
            return super.onTouchEvent(ev);
        }
        mlp = (MarginLayoutParams) moveView.getLayoutParams();
        if (originTopMargin == -1) {
            originTopMargin = mlp.topMargin;
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                curTouchY = ev.getY();
                shiftOffset = curTouchY - lastTouchY;
                lastTouchY = curTouchY;
                if (!isTouchPointInView(touchView, ev.getX(), ev.getY()) || !isTop()) {
                    return super.onTouchEvent(ev);
                }

                if (!hasZoom && shiftOffset < 0) {
                    return super.onTouchEvent(ev);
                }

                if (Math.abs(originTopMargin - mlp.topMargin) <= maxOffset || shiftOffset < 0) {
                    hasZoom = true;
                    //上划不需要具备阻尼效果
                    if (shiftOffset < 0) {
                        mlp.topMargin += recountOffset(shiftOffset);
                        zoomScale += shiftOffset / 500;
                    } else {
                        mlp.topMargin += recountOffset(shiftOffset * zoomSensitivity / 2);
                        zoomScale += shiftOffset * zoomSensitivity / 1000;
                    }
                    if (zoomScale < 1 || mlp.topMargin < originTopMargin) {
                        hasZoom = false;
                        zoomScale = 1;
                        setTransView(zoomView, zoomScale, moveView, originTopMargin);
                        return super.onTouchEvent(ev);
                    }
                    setTransView(zoomView, zoomScale, moveView, mlp.topMargin);
                }

                if (!hasZoom) {
                    return super.onTouchEvent(ev);
                } else {
                    return false;
                }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!hasZoom) {
                    return super.onTouchEvent(ev);
                }
                //复位
                hasZoom = false;
                zoomAnimator.setFloatValues(zoomScale, 1);
                zoomAnimator.start();
                translateAnimator.setIntValues(mlp.topMargin, originTopMargin);
                translateAnimator.start();
                break;
            default:
        }
        return super.onTouchEvent(ev);
    }

    private int recountOffset(float f) {
        if (f > 0) {
            return (int) (f + 0.5f);
        } else {
            return (int) (f - 0.5f);
        }
    }

    private void setTransView(View zoomView, float zoomScale, View moveView, int originTopMargin) {
        zoomView.setScaleY(zoomScale);
        zoomView.setScaleX(zoomScale);
        mlp.setMargins(mlp.leftMargin, originTopMargin, mlp.rightMargin, mlp.bottomMargin);
        moveView.setLayoutParams(mlp);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeAnimator(zoomAnimator);
        removeAnimator(translateAnimator);
    }

    private void removeAnimator(ValueAnimator animator) {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }

    private float interceptLastY;
    private float interceptCurY;
    private float interceptLastX;
    private float interceptCurX;
    private float interceptShiftX;
    private float interceptShiftY;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchY = ev.getY();
                interceptLastY = ev.getY();
                interceptLastX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                interceptCurY = ev.getY();
                interceptCurX = ev.getX();
                interceptShiftX = Math.abs(interceptCurX - interceptLastX);
                interceptShiftY = Math.abs(interceptCurY - interceptLastY);
                if (interceptShiftY > interceptShiftX && interceptShiftY > touchSlop) {
                    return true;
                }
            default:
        }
        return super.onInterceptTouchEvent(ev);
    }

    public void refreshContentViewTopMargin() {
        if (originTopMargin != -1) {
            originTopMargin = -1;
        }
    }

    /**
     * 点击区域判断
     *
     * @param view 判断目标view
     * @param x    触摸X轴
     * @param y    触摸Y轴
     * @return 是否在view内触摸
     */
    private boolean isTouchPointInView(View view, float x, float y) {
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