package android.support.v7.widget;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.RecyclerView.LayoutParams;
import android.support.v7.widget.RecyclerView.SmoothScroller;
import android.support.v7.widget.RecyclerView.SmoothScroller.Action;
import android.support.v7.widget.RecyclerView.SmoothScroller.ScrollVectorProvider;
import android.support.v7.widget.RecyclerView.State;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

public class LinearSmoothScroller extends SmoothScroller {
    private static final boolean DEBUG = false;
    private static final float MILLISECONDS_PER_INCH = 25.0f;
    public static final int SNAP_TO_ANY = 0;
    public static final int SNAP_TO_END = 1;
    public static final int SNAP_TO_START = -1;
    private static final String TAG = "LinearSmoothScroller";
    private static final float TARGET_SEEK_EXTRA_SCROLL_RATIO = 1.2f;
    private static final int TARGET_SEEK_SCROLL_DISTANCE_PX = 10000;
    private final float MILLISECONDS_PER_PX;
    protected final DecelerateInterpolator mDecelerateInterpolator = new DecelerateInterpolator();
    protected int mInterimTargetDx = 0;
    protected int mInterimTargetDy = 0;
    protected final LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    protected PointF mTargetVector;

    public LinearSmoothScroller(Context context) {
        this.MILLISECONDS_PER_PX = calculateSpeedPerPixel(context.getResources().getDisplayMetrics());
    }

    /* Access modifiers changed, original: protected */
    public void onStart() {
    }

    /* Access modifiers changed, original: protected */
    public void onTargetFound(View targetView, State state, Action action) {
        int dx = calculateDxToMakeVisible(targetView, getHorizontalSnapPreference());
        int dy = calculateDyToMakeVisible(targetView, getVerticalSnapPreference());
        int time = calculateTimeForDeceleration((int) Math.sqrt((double) ((dx * dx) + (dy * dy))));
        if (time > 0) {
            action.update(-dx, -dy, time, this.mDecelerateInterpolator);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onSeekTargetStep(int dx, int dy, State state, Action action) {
        if (getChildCount() == 0) {
            stop();
            return;
        }
        this.mInterimTargetDx = clampApplyScroll(this.mInterimTargetDx, dx);
        this.mInterimTargetDy = clampApplyScroll(this.mInterimTargetDy, dy);
        if (this.mInterimTargetDx == 0 && this.mInterimTargetDy == 0) {
            updateActionForInterimTarget(action);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onStop() {
        this.mInterimTargetDy = 0;
        this.mInterimTargetDx = 0;
        this.mTargetVector = null;
    }

    /* Access modifiers changed, original: protected */
    public float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        return MILLISECONDS_PER_INCH / ((float) displayMetrics.densityDpi);
    }

    /* Access modifiers changed, original: protected */
    public int calculateTimeForDeceleration(int dx) {
        return (int) Math.ceil(((double) calculateTimeForScrolling(dx)) / 0.3356d);
    }

    /* Access modifiers changed, original: protected */
    public int calculateTimeForScrolling(int dx) {
        return (int) Math.ceil((double) (((float) Math.abs(dx)) * this.MILLISECONDS_PER_PX));
    }

    /* Access modifiers changed, original: protected */
    public int getHorizontalSnapPreference() {
        if (this.mTargetVector == null || this.mTargetVector.x == 0.0f) {
            return 0;
        }
        return this.mTargetVector.x > 0.0f ? 1 : -1;
    }

    /* Access modifiers changed, original: protected */
    public int getVerticalSnapPreference() {
        if (this.mTargetVector == null || this.mTargetVector.y == 0.0f) {
            return 0;
        }
        return this.mTargetVector.y > 0.0f ? 1 : -1;
    }

    /* Access modifiers changed, original: protected */
    public void updateActionForInterimTarget(Action action) {
        PointF scrollVector = computeScrollVectorForPosition(getTargetPosition());
        if (scrollVector == null || (scrollVector.x == 0.0f && scrollVector.y == 0.0f)) {
            action.jumpTo(getTargetPosition());
            stop();
            return;
        }
        normalize(scrollVector);
        this.mTargetVector = scrollVector;
        this.mInterimTargetDx = (int) (scrollVector.x * 10000.0f);
        this.mInterimTargetDy = (int) (10000.0f * scrollVector.y);
        action.update((int) (((float) this.mInterimTargetDx) * TARGET_SEEK_EXTRA_SCROLL_RATIO), (int) (((float) this.mInterimTargetDy) * TARGET_SEEK_EXTRA_SCROLL_RATIO), (int) (((float) calculateTimeForScrolling(TARGET_SEEK_SCROLL_DISTANCE_PX)) * TARGET_SEEK_EXTRA_SCROLL_RATIO), this.mLinearInterpolator);
    }

    private int clampApplyScroll(int tmpDt, int dt) {
        int before = tmpDt;
        tmpDt -= dt;
        if (before * tmpDt <= 0) {
            return 0;
        }
        return tmpDt;
    }

    public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
        switch (snapPreference) {
            case -1:
                return boxStart - viewStart;
            case 0:
                int dtStart = boxStart - viewStart;
                if (dtStart > 0) {
                    return dtStart;
                }
                int dtEnd = boxEnd - viewEnd;
                if (dtEnd < 0) {
                    return dtEnd;
                }
                return 0;
            case 1:
                return boxEnd - viewEnd;
            default:
                throw new IllegalArgumentException("snap preference should be one of the constants defined in SmoothScroller, starting with SNAP_");
        }
    }

    public int calculateDyToMakeVisible(View view, int snapPreference) {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null || !layoutManager.canScrollVertically()) {
            return 0;
        }
        LayoutParams params = (LayoutParams) view.getLayoutParams();
        return calculateDtToFit(layoutManager.getDecoratedTop(view) - params.topMargin, layoutManager.getDecoratedBottom(view) + params.bottomMargin, layoutManager.getPaddingTop(), layoutManager.getHeight() - layoutManager.getPaddingBottom(), snapPreference);
    }

    public int calculateDxToMakeVisible(View view, int snapPreference) {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null || !layoutManager.canScrollHorizontally()) {
            return 0;
        }
        LayoutParams params = (LayoutParams) view.getLayoutParams();
        return calculateDtToFit(layoutManager.getDecoratedLeft(view) - params.leftMargin, layoutManager.getDecoratedRight(view) + params.rightMargin, layoutManager.getPaddingLeft(), layoutManager.getWidth() - layoutManager.getPaddingRight(), snapPreference);
    }

    @Nullable
    public PointF computeScrollVectorForPosition(int targetPosition) {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof ScrollVectorProvider) {
            return ((ScrollVectorProvider) layoutManager).computeScrollVectorForPosition(targetPosition);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("You should override computeScrollVectorForPosition when the LayoutManager does not implement ");
        stringBuilder.append(ScrollVectorProvider.class.getCanonicalName());
        Log.w(str, stringBuilder.toString());
        return null;
    }
}
