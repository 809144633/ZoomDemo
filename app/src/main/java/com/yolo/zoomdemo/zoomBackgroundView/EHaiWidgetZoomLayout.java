package com.yolo.zoomdemo.zoomBackgroundView;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
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
public class EHaiWidgetZoomLayout extends NestedScrollView {
    private static final String TAG = "ZoomLayout";
    public static final String ZOOM = "zoom";
    public static final String CONTENT = "content";
    private final float MAX_SENSITIVITY = 1f;
    private final float MIN_SENSITIVITY = 0.1f;

    private boolean zoomEnable;

    //放大灵敏度
    private float zoomSensitivity;

    //缩放对象
    private View zoomView;
    //移动对象
    private View contentView;

    private int touchSlop;

    //最大偏移量
    private float maxOffset;


    public EHaiWidgetZoomLayout(Context context) {
        this(context, null);
    }

    public EHaiWidgetZoomLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EHaiWidgetZoomLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.EHaiWidgetZoomLayout);
            zoomEnable = array.getBoolean(R.styleable.EHaiWidgetZoomLayout_zoom_enable, false);
            zoomSensitivity = array.getFloat(R.styleable.EHaiWidgetZoomLayout_zoom_sensitivity, MIN_SENSITIVITY);
            maxOffset = array.getInteger(R.styleable.EHaiWidgetZoomLayout_zoom_max_offset, 100);
            array.recycle();
        }
        zoomSensitivity = Math.min(MAX_SENSITIVITY, Math.max(zoomSensitivity, MIN_SENSITIVITY));
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

    private float curTouchY;
    private float lastTouchY;

    //触碰滑动偏移量
    private float shiftOffset;

    private float zoomScale = 1f;

    //记录位移控件的原始距离顶部位置
    private int originTopMargin = -1;

    //判断是否缩放状态
    private boolean hasZoom = false;
    private ValueAnimator zoomAnimator;
    private ValueAnimator translateAnimator;
    private MarginLayoutParams mlp;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!zoomEnable || zoomView == null || contentView == null) {
            return super.onTouchEvent(ev);
        }
        curTouchY = ev.getY();
        mlp = (MarginLayoutParams) contentView.getLayoutParams();
        if (originTopMargin == -1) {
            originTopMargin = mlp.topMargin;
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (!isTouchPointInView(contentView, ev.getX(), ev.getY()) || !isTop()) {
                    lastTouchY = curTouchY;
                    return super.onTouchEvent(ev);
                }
                shiftOffset = curTouchY - lastTouchY;
                if (shiftOffset > 0 && Math.abs(originTopMargin - mlp.topMargin) <= maxOffset) {
                    hasZoom = true;

                    mlp.topMargin += (int) (shiftOffset * zoomSensitivity / 2 + 0.5f);
                    mlp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, mlp.bottomMargin);
                    contentView.setLayoutParams(mlp);

                    zoomScale += shiftOffset * zoomSensitivity / 1000;
                    zoomView.setScaleX(zoomScale);
                    zoomView.setScaleY(zoomScale);
                }
                lastTouchY = curTouchY;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!hasZoom) {
                    return super.onTouchEvent(ev);
                }
                zoomAnimator = ValueAnimator.ofFloat(zoomScale, 1).setDuration(200);
                zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        zoomScale = (Float) animation.getAnimatedValue();
                        zoomView.setScaleY(zoomScale);
                        zoomView.setScaleX(zoomScale);
                    }
                });

                translateAnimator = ValueAnimator.ofInt(mlp.topMargin, originTopMargin).setDuration(200);
                translateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mlp.setMargins(
                                mlp.leftMargin,
                                (Integer) animation.getAnimatedValue(),
                                mlp.rightMargin,
                                mlp.bottomMargin);
                        contentView.setLayoutParams(mlp);
                    }
                });

                zoomAnimator.start();
                translateAnimator.start();
                //复位
                hasZoom = false;
                break;
            default:
        }
        return super.onTouchEvent(ev);
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
            animator.removeAllUpdateListeners();
            animator = null;
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

    public void refreshMargin() {
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
