package android.support.v7.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.InputDeviceCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class LinearLayoutCompat extends ViewGroup {
    public static final int HORIZONTAL = 0;
    private static final int INDEX_BOTTOM = 2;
    private static final int INDEX_CENTER_VERTICAL = 0;
    private static final int INDEX_FILL = 3;
    private static final int INDEX_TOP = 1;
    public static final int SHOW_DIVIDER_BEGINNING = 1;
    public static final int SHOW_DIVIDER_END = 4;
    public static final int SHOW_DIVIDER_MIDDLE = 2;
    public static final int SHOW_DIVIDER_NONE = 0;
    public static final int VERTICAL = 1;
    private static final int VERTICAL_GRAVITY_COUNT = 4;
    private boolean mBaselineAligned;
    private int mBaselineAlignedChildIndex;
    private int mBaselineChildTop;
    private Drawable mDivider;
    private int mDividerHeight;
    private int mDividerPadding;
    private int mDividerWidth;
    private int mGravity;
    private int[] mMaxAscent;
    private int[] mMaxDescent;
    private int mOrientation;
    private int mShowDividers;
    private int mTotalLength;
    private boolean mUseLargestChild;
    private float mWeightSum;

    @RestrictTo({Scope.LIBRARY_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DividerMode {
    }

    public static class LayoutParams extends MarginLayoutParams {
        public int gravity;
        public float weight;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.gravity = -1;
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.LinearLayoutCompat_Layout);
            this.weight = a.getFloat(R.styleable.LinearLayoutCompat_Layout_android_layout_weight, 0.0f);
            this.gravity = a.getInt(R.styleable.LinearLayoutCompat_Layout_android_layout_gravity, -1);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.gravity = -1;
            this.weight = 0.0f;
        }

        public LayoutParams(int width, int height, float weight) {
            super(width, height);
            this.gravity = -1;
            this.weight = weight;
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams p) {
            super(p);
            this.gravity = -1;
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
            this.gravity = -1;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.gravity = -1;
            this.weight = source.weight;
            this.gravity = source.gravity;
        }
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OrientationMode {
    }

    public LinearLayoutCompat(Context context) {
        this(context, null);
    }

    public LinearLayoutCompat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinearLayoutCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mBaselineAligned = true;
        this.mBaselineAlignedChildIndex = -1;
        this.mBaselineChildTop = 0;
        this.mGravity = 8388659;
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, R.styleable.LinearLayoutCompat, defStyleAttr, 0);
        int index = a.getInt(R.styleable.LinearLayoutCompat_android_orientation, -1);
        if (index >= 0) {
            setOrientation(index);
        }
        index = a.getInt(R.styleable.LinearLayoutCompat_android_gravity, -1);
        if (index >= 0) {
            setGravity(index);
        }
        boolean baselineAligned = a.getBoolean(R.styleable.LinearLayoutCompat_android_baselineAligned, true);
        if (!baselineAligned) {
            setBaselineAligned(baselineAligned);
        }
        this.mWeightSum = a.getFloat(R.styleable.LinearLayoutCompat_android_weightSum, -1.0f);
        this.mBaselineAlignedChildIndex = a.getInt(R.styleable.LinearLayoutCompat_android_baselineAlignedChildIndex, -1);
        this.mUseLargestChild = a.getBoolean(R.styleable.LinearLayoutCompat_measureWithLargestChild, false);
        setDividerDrawable(a.getDrawable(R.styleable.LinearLayoutCompat_divider));
        this.mShowDividers = a.getInt(R.styleable.LinearLayoutCompat_showDividers, 0);
        this.mDividerPadding = a.getDimensionPixelSize(R.styleable.LinearLayoutCompat_dividerPadding, 0);
        a.recycle();
    }

    public void setShowDividers(int showDividers) {
        if (showDividers != this.mShowDividers) {
            requestLayout();
        }
        this.mShowDividers = showDividers;
    }

    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public int getShowDividers() {
        return this.mShowDividers;
    }

    public Drawable getDividerDrawable() {
        return this.mDivider;
    }

    public void setDividerDrawable(Drawable divider) {
        if (divider != this.mDivider) {
            this.mDivider = divider;
            boolean z = false;
            if (divider != null) {
                this.mDividerWidth = divider.getIntrinsicWidth();
                this.mDividerHeight = divider.getIntrinsicHeight();
            } else {
                this.mDividerWidth = 0;
                this.mDividerHeight = 0;
            }
            if (divider == null) {
                z = true;
            }
            setWillNotDraw(z);
            requestLayout();
        }
    }

    public void setDividerPadding(int padding) {
        this.mDividerPadding = padding;
    }

    public int getDividerPadding() {
        return this.mDividerPadding;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public int getDividerWidth() {
        return this.mDividerWidth;
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        if (this.mDivider != null) {
            if (this.mOrientation == 1) {
                drawDividersVertical(canvas);
            } else {
                drawDividersHorizontal(canvas);
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void drawDividersVertical(Canvas canvas) {
        int count = getVirtualChildCount();
        int i = 0;
        while (i < count) {
            View child = getVirtualChildAt(i);
            if (!(child == null || child.getVisibility() == 8 || !hasDividerBeforeChildAt(i))) {
                drawHorizontalDivider(canvas, (child.getTop() - ((LayoutParams) child.getLayoutParams()).topMargin) - this.mDividerHeight);
            }
            i++;
        }
        if (hasDividerBeforeChildAt(count)) {
            int bottom;
            View child2 = getVirtualChildAt(count - 1);
            if (child2 == null) {
                bottom = (getHeight() - getPaddingBottom()) - this.mDividerHeight;
            } else {
                bottom = child2.getBottom() + ((LayoutParams) child2.getLayoutParams()).bottomMargin;
            }
            drawHorizontalDivider(canvas, bottom);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void drawDividersHorizontal(Canvas canvas) {
        int count = getVirtualChildCount();
        boolean isLayoutRtl = ViewUtils.isLayoutRtl(this);
        int i = 0;
        while (i < count) {
            View child = getVirtualChildAt(i);
            if (!(child == null || child.getVisibility() == 8 || !hasDividerBeforeChildAt(i))) {
                int position;
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (isLayoutRtl) {
                    position = child.getRight() + lp.rightMargin;
                } else {
                    position = (child.getLeft() - lp.leftMargin) - this.mDividerWidth;
                }
                drawVerticalDivider(canvas, position);
            }
            i++;
        }
        if (hasDividerBeforeChildAt(count)) {
            int position2;
            View child2 = getVirtualChildAt(count - 1);
            if (child2 != null) {
                LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                if (isLayoutRtl) {
                    position2 = (child2.getLeft() - lp2.leftMargin) - this.mDividerWidth;
                } else {
                    position2 = child2.getRight() + lp2.rightMargin;
                }
            } else if (isLayoutRtl) {
                position2 = getPaddingLeft();
            } else {
                position2 = (getWidth() - getPaddingRight()) - this.mDividerWidth;
            }
            drawVerticalDivider(canvas, position2);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void drawHorizontalDivider(Canvas canvas, int top) {
        this.mDivider.setBounds(getPaddingLeft() + this.mDividerPadding, top, (getWidth() - getPaddingRight()) - this.mDividerPadding, this.mDividerHeight + top);
        this.mDivider.draw(canvas);
    }

    /* Access modifiers changed, original: 0000 */
    public void drawVerticalDivider(Canvas canvas, int left) {
        this.mDivider.setBounds(left, getPaddingTop() + this.mDividerPadding, this.mDividerWidth + left, (getHeight() - getPaddingBottom()) - this.mDividerPadding);
        this.mDivider.draw(canvas);
    }

    public boolean isBaselineAligned() {
        return this.mBaselineAligned;
    }

    public void setBaselineAligned(boolean baselineAligned) {
        this.mBaselineAligned = baselineAligned;
    }

    public boolean isMeasureWithLargestChildEnabled() {
        return this.mUseLargestChild;
    }

    public void setMeasureWithLargestChildEnabled(boolean enabled) {
        this.mUseLargestChild = enabled;
    }

    public int getBaseline() {
        if (this.mBaselineAlignedChildIndex < 0) {
            return super.getBaseline();
        }
        if (getChildCount() <= this.mBaselineAlignedChildIndex) {
            throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout set to an index that is out of bounds.");
        }
        View child = getChildAt(this.mBaselineAlignedChildIndex);
        int childBaseline = child.getBaseline();
        if (childBaseline != -1) {
            int childTop = this.mBaselineChildTop;
            if (this.mOrientation == 1) {
                int majorGravity = this.mGravity & 112;
                if (majorGravity != 48) {
                    if (majorGravity == 16) {
                        childTop += ((((getBottom() - getTop()) - getPaddingTop()) - getPaddingBottom()) - this.mTotalLength) / 2;
                    } else if (majorGravity == 80) {
                        childTop = ((getBottom() - getTop()) - getPaddingBottom()) - this.mTotalLength;
                    }
                }
            }
            return (((LayoutParams) child.getLayoutParams()).topMargin + childTop) + childBaseline;
        } else if (this.mBaselineAlignedChildIndex == 0) {
            return -1;
        } else {
            throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout points to a View that doesn't know how to get its baseline.");
        }
    }

    public int getBaselineAlignedChildIndex() {
        return this.mBaselineAlignedChildIndex;
    }

    public void setBaselineAlignedChildIndex(int i) {
        if (i < 0 || i >= getChildCount()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("base aligned child index out of range (0, ");
            stringBuilder.append(getChildCount());
            stringBuilder.append(")");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.mBaselineAlignedChildIndex = i;
    }

    /* Access modifiers changed, original: 0000 */
    public View getVirtualChildAt(int index) {
        return getChildAt(index);
    }

    /* Access modifiers changed, original: 0000 */
    public int getVirtualChildCount() {
        return getChildCount();
    }

    public float getWeightSum() {
        return this.mWeightSum;
    }

    public void setWeightSum(float weightSum) {
        this.mWeightSum = Math.max(0.0f, weightSum);
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mOrientation == 1) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /* Access modifiers changed, original: protected */
    @RestrictTo({Scope.LIBRARY})
    public boolean hasDividerBeforeChildAt(int childIndex) {
        boolean hasVisibleViewBefore = false;
        if (childIndex == 0) {
            if ((this.mShowDividers & 1) != 0) {
                hasVisibleViewBefore = true;
            }
            return hasVisibleViewBefore;
        } else if (childIndex == getChildCount()) {
            if ((this.mShowDividers & 4) != 0) {
                hasVisibleViewBefore = true;
            }
            return hasVisibleViewBefore;
        } else if ((this.mShowDividers & 2) == 0) {
            return false;
        } else {
            hasVisibleViewBefore = false;
            for (int i = childIndex - 1; i >= 0; i--) {
                if (getChildAt(i).getVisibility() != 8) {
                    hasVisibleViewBefore = true;
                    break;
                }
            }
            return hasVisibleViewBefore;
        }
    }

    /* Access modifiers changed, original: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x03d0  */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x03cd  */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x03e1  */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x03d7  */
    /* JADX WARNING: Removed duplicated region for block: B:192:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x0456  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        int i = widthMeasureSpec;
        int i2 = heightMeasureSpec;
        this.mTotalLength = 0;
        int childState = 0;
        float totalWeight = 0.0f;
        int count = getVirtualChildCount();
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        boolean skippedMeasure = false;
        int baselineChildIndex = this.mBaselineAlignedChildIndex;
        boolean useLargestChild = this.mUseLargestChild;
        boolean matchWidth = false;
        int alternativeMaxWidth = 0;
        int maxWidth = 0;
        int i3 = 0;
        boolean weightedMaxWidth = false;
        int largestChildHeight = 0;
        boolean allFillParent = true;
        while (true) {
            boolean weightedMaxWidth2 = weightedMaxWidth;
            View child;
            int count2;
            int heightMode2;
            LayoutParams lp;
            boolean skippedMeasure2;
            int childState2;
            int i4;
            boolean matchWidthLocally;
            int margin;
            if (i3 < count) {
                child = getVirtualChildAt(i3);
                int childState3;
                if (child == null) {
                    childState3 = childState;
                    this.mTotalLength += measureNullChild(i3);
                    count2 = count;
                    heightMode2 = heightMode;
                    weightedMaxWidth = weightedMaxWidth2;
                    childState = childState3;
                } else {
                    childState3 = childState;
                    int maxWidth2 = maxWidth;
                    if (child.getVisibility() == 8) {
                        i3 += getChildrenSkipCount(child, i3);
                        count2 = count;
                        heightMode2 = heightMode;
                        weightedMaxWidth = weightedMaxWidth2;
                        childState = childState3;
                        maxWidth = maxWidth2;
                    } else {
                        View child2;
                        boolean weightedMaxWidth3;
                        int maxWidth3;
                        int i5;
                        if (hasDividerBeforeChildAt(i3)) {
                            this.mTotalLength += this.mDividerHeight;
                        }
                        LayoutParams lp2 = (LayoutParams) child.getLayoutParams();
                        float totalWeight2 = totalWeight + lp2.weight;
                        int i6;
                        if (heightMode == 1073741824 && lp2.height == 0 && lp2.weight > 0.0f) {
                            childState = this.mTotalLength;
                            i6 = i3;
                            this.mTotalLength = Math.max(childState, (lp2.topMargin + childState) + lp2.bottomMargin);
                            lp = lp2;
                            child2 = child;
                            i2 = alternativeMaxWidth;
                            count2 = count;
                            heightMode2 = heightMode;
                            skippedMeasure2 = true;
                            weightedMaxWidth3 = weightedMaxWidth2;
                            childState2 = childState3;
                            maxWidth3 = maxWidth2;
                            i5 = i6;
                        } else {
                            i6 = i3;
                            i3 = Integer.MIN_VALUE;
                            if (lp2.height == 0 && lp2.weight > 0.0f) {
                                i3 = 0;
                                lp2.height = -2;
                            }
                            i5 = i6;
                            skippedMeasure2 = skippedMeasure;
                            childState2 = childState3;
                            LayoutParams lp3 = lp2;
                            maxWidth3 = maxWidth2;
                            heightMode2 = heightMode;
                            heightMode = largestChildHeight;
                            largestChildHeight = i;
                            child2 = child;
                            count2 = count;
                            weightedMaxWidth3 = weightedMaxWidth2;
                            count = i3;
                            i4 = i2;
                            i2 = alternativeMaxWidth;
                            measureChildBeforeLayout(child, i5, largestChildHeight, 0, i4, totalWeight2 == 0.0f ? this.mTotalLength : 0);
                            if (count != Integer.MIN_VALUE) {
                                lp = lp3;
                                lp.height = count;
                            } else {
                                lp = lp3;
                            }
                            childState = child2.getMeasuredHeight();
                            maxWidth = this.mTotalLength;
                            this.mTotalLength = Math.max(maxWidth, (((maxWidth + childState) + lp.topMargin) + lp.bottomMargin) + getNextLocationOffset(child2));
                            if (useLargestChild) {
                                largestChildHeight = Math.max(childState, heightMode);
                            } else {
                                largestChildHeight = heightMode;
                            }
                        }
                        if (baselineChildIndex >= 0) {
                            childState = i5;
                            if (baselineChildIndex == childState + 1) {
                                this.mBaselineChildTop = this.mTotalLength;
                            }
                        } else {
                            childState = i5;
                        }
                        if (childState >= baselineChildIndex || lp.weight <= 0.0f) {
                            boolean allFillParent2;
                            matchWidthLocally = false;
                            if (widthMode != 1073741824) {
                                alternativeMaxWidth = -1;
                                if (lp.width == -1) {
                                    matchWidth = true;
                                    matchWidthLocally = true;
                                }
                            } else {
                                alternativeMaxWidth = -1;
                            }
                            margin = lp.leftMargin + lp.rightMargin;
                            i4 = child2.getMeasuredWidth() + margin;
                            count = Math.max(maxWidth3, i4);
                            heightMode = View.combineMeasuredStates(childState2, child2.getMeasuredState());
                            boolean allFillParent3 = allFillParent && lp.width == alternativeMaxWidth;
                            if (lp.weight > 0.0f) {
                                allFillParent2 = allFillParent3;
                                allFillParent3 = Math.max(weightedMaxWidth3, matchWidthLocally ? margin : i4);
                            } else {
                                allFillParent2 = allFillParent3;
                                allFillParent3 = weightedMaxWidth3;
                                i2 = Math.max(i2, matchWidthLocally ? margin : i4);
                            }
                            i3 = childState + getChildrenSkipCount(child2, childState);
                            weightedMaxWidth = allFillParent3;
                            alternativeMaxWidth = i2;
                            maxWidth = count;
                            childState = heightMode;
                            totalWeight = totalWeight2;
                            skippedMeasure = skippedMeasure2;
                            allFillParent = allFillParent2;
                        } else {
                            throw new RuntimeException("A child of LinearLayout with index less than mBaselineAlignedChildIndex has weight > 0, which won't work.  Either remove the weight, or don't set mBaselineAlignedChildIndex.");
                        }
                    }
                }
                i3++;
                heightMode = heightMode2;
                count = count2;
                i = widthMeasureSpec;
                i2 = heightMeasureSpec;
            } else {
                int delta;
                float totalWeight3;
                int alternativeMaxWidth2;
                int i7;
                boolean z;
                int i8;
                int i9;
                i2 = alternativeMaxWidth;
                count2 = count;
                heightMode2 = heightMode;
                skippedMeasure2 = skippedMeasure;
                alternativeMaxWidth = weightedMaxWidth2;
                childState2 = childState;
                count = maxWidth;
                heightMode = largestChildHeight;
                if (this.mTotalLength > 0) {
                    childState = count2;
                    if (hasDividerBeforeChildAt(childState)) {
                        this.mTotalLength += this.mDividerHeight;
                    }
                } else {
                    childState = count2;
                }
                if (useLargestChild) {
                    maxWidth = heightMode2;
                    if (maxWidth == Integer.MIN_VALUE || maxWidth == 0) {
                        this.mTotalLength = 0;
                        largestChildHeight = 0;
                        while (largestChildHeight < childState) {
                            int i10;
                            child = getVirtualChildAt(largestChildHeight);
                            if (child == null) {
                                this.mTotalLength += measureNullChild(largestChildHeight);
                            } else if (child.getVisibility() == 8) {
                                largestChildHeight += getChildrenSkipCount(child, largestChildHeight);
                            } else {
                                lp = (LayoutParams) child.getLayoutParams();
                                i = this.mTotalLength;
                                i10 = largestChildHeight;
                                this.mTotalLength = Math.max(i, (((i + heightMode) + lp.topMargin) + lp.bottomMargin) + getNextLocationOffset(child));
                                largestChildHeight = i10 + 1;
                            }
                            i10 = largestChildHeight;
                            largestChildHeight = i10 + 1;
                        }
                    }
                } else {
                    maxWidth = heightMode2;
                }
                this.mTotalLength += getPaddingTop() + getPaddingBottom();
                largestChildHeight = heightMeasureSpec;
                i = View.resolveSizeAndState(Math.max(this.mTotalLength, getSuggestedMinimumHeight()), largestChildHeight, 0);
                i3 = i & ViewCompat.MEASURED_SIZE_MASK;
                margin = i3 - this.mTotalLength;
                int i11;
                if (skippedMeasure2) {
                    delta = margin;
                    totalWeight3 = totalWeight;
                    i11 = alternativeMaxWidth;
                } else if (margin == 0 || totalWeight <= 0.0f) {
                    i2 = Math.max(i2, alternativeMaxWidth);
                    if (useLargestChild) {
                        if (maxWidth != 1073741824) {
                            int i12 = 0;
                            while (true) {
                                i3 = i12;
                                if (i3 >= childState) {
                                    break;
                                }
                                delta = margin;
                                child = getVirtualChildAt(i3);
                                if (child != null) {
                                    totalWeight3 = totalWeight;
                                    i11 = alternativeMaxWidth;
                                    if (child.getVisibility() == 8) {
                                        alternativeMaxWidth2 = i2;
                                    } else {
                                        LayoutParams totalWeight4 = (LayoutParams) child.getLayoutParams();
                                        float childExtra = totalWeight4.weight;
                                        if (childExtra > 0.0f) {
                                            LayoutParams lp4 = totalWeight4;
                                            alternativeMaxWidth2 = i2;
                                            child.measure(MeasureSpec.makeMeasureSpec(child.getMeasuredWidth(), 1073741824), MeasureSpec.makeMeasureSpec(heightMode, 1073741824));
                                        } else {
                                            alternativeMaxWidth2 = i2;
                                        }
                                    }
                                } else {
                                    totalWeight3 = totalWeight;
                                    i11 = alternativeMaxWidth;
                                    alternativeMaxWidth2 = i2;
                                }
                                i12 = i3 + 1;
                                margin = delta;
                                totalWeight = totalWeight3;
                                alternativeMaxWidth = i11;
                                i2 = alternativeMaxWidth2;
                            }
                        }
                        totalWeight3 = totalWeight;
                        i11 = alternativeMaxWidth;
                        alternativeMaxWidth2 = i2;
                    } else {
                        delta = margin;
                        totalWeight3 = totalWeight;
                        i11 = alternativeMaxWidth;
                        alternativeMaxWidth2 = i2;
                    }
                    i7 = maxWidth;
                    z = useLargestChild;
                    i8 = heightMode;
                    i9 = baselineChildIndex;
                    heightMode = widthMeasureSpec;
                    if (!(allFillParent || widthMode == 1073741824)) {
                        count = alternativeMaxWidth2;
                    }
                    setMeasuredDimension(View.resolveSizeAndState(Math.max(count + (getPaddingLeft() + getPaddingRight()), getSuggestedMinimumWidth()), heightMode, childState2), i);
                    if (!matchWidth) {
                        forceUniformWidth(childState, largestChildHeight);
                        return;
                    }
                    return;
                } else {
                    int i13 = i3;
                    delta = margin;
                    totalWeight3 = totalWeight;
                    boolean z2 = alternativeMaxWidth;
                }
                totalWeight = this.mWeightSum > 0.0f ? this.mWeightSum : totalWeight3;
                this.mTotalLength = 0;
                i3 = 0;
                alternativeMaxWidth = delta;
                while (i3 < childState) {
                    child = getVirtualChildAt(i3);
                    z = useLargestChild;
                    i8 = heightMode;
                    if (child.getVisibility() == 8) {
                        i7 = maxWidth;
                        i9 = baselineChildIndex;
                        heightMode = widthMeasureSpec;
                    } else {
                        int delta2;
                        int margin2;
                        boolean allFillParent4;
                        float weightSum;
                        LayoutParams lp5 = (LayoutParams) child.getLayoutParams();
                        float childExtra2 = lp5.weight;
                        if (childExtra2 > 0.0f) {
                            i9 = baselineChildIndex;
                            baselineChildIndex = (int) ((((float) alternativeMaxWidth) * childExtra2) / totalWeight);
                            float weightSum2 = totalWeight - childExtra2;
                            delta2 = alternativeMaxWidth - baselineChildIndex;
                            i4 = getChildMeasureSpec(widthMeasureSpec, ((getPaddingLeft() + getPaddingRight()) + lp5.leftMargin) + lp5.rightMargin, lp5.width);
                            if (lp5.height != 0) {
                                i7 = maxWidth;
                            } else if (maxWidth != 1073741824) {
                                i7 = maxWidth;
                            } else {
                                i7 = maxWidth;
                                child.measure(i4, MeasureSpec.makeMeasureSpec(baselineChildIndex > 0 ? baselineChildIndex : 0, 1073741824));
                                int i14 = baselineChildIndex;
                                childState2 = View.combineMeasuredStates(childState2, child.getMeasuredState() & InputDeviceCompat.SOURCE_ANY);
                                totalWeight = weightSum2;
                            }
                            maxWidth = child.getMeasuredHeight() + baselineChildIndex;
                            if (maxWidth < 0) {
                                maxWidth = 0;
                            }
                            child.measure(i4, MeasureSpec.makeMeasureSpec(maxWidth, 1073741824));
                            childState2 = View.combineMeasuredStates(childState2, child.getMeasuredState() & InputDeviceCompat.SOURCE_ANY);
                            totalWeight = weightSum2;
                        } else {
                            i7 = maxWidth;
                            float f = childExtra2;
                            i9 = baselineChildIndex;
                            heightMode = widthMeasureSpec;
                            delta2 = alternativeMaxWidth;
                        }
                        maxWidth = lp5.leftMargin + lp5.rightMargin;
                        alternativeMaxWidth = child.getMeasuredWidth() + maxWidth;
                        count = Math.max(count, alternativeMaxWidth);
                        if (widthMode != 1073741824) {
                            margin2 = maxWidth;
                            if (lp5.width == -1) {
                                matchWidthLocally = true;
                                i2 = Math.max(i2, matchWidthLocally ? margin2 : alternativeMaxWidth);
                                boolean matchWidthLocally2;
                                if (allFillParent) {
                                    matchWidthLocally2 = matchWidthLocally;
                                } else {
                                    matchWidthLocally2 = matchWidthLocally;
                                    if (lp5.width == -1) {
                                        allFillParent4 = true;
                                        maxWidth = this.mTotalLength;
                                        weightSum = totalWeight;
                                        this.mTotalLength = Math.max(maxWidth, (((maxWidth + child.getMeasuredHeight()) + lp5.topMargin) + lp5.bottomMargin) + getNextLocationOffset(child));
                                        allFillParent = allFillParent4;
                                        alternativeMaxWidth = delta2;
                                        totalWeight = weightSum;
                                    }
                                }
                                allFillParent4 = false;
                                maxWidth = this.mTotalLength;
                                weightSum = totalWeight;
                                this.mTotalLength = Math.max(maxWidth, (((maxWidth + child.getMeasuredHeight()) + lp5.topMargin) + lp5.bottomMargin) + getNextLocationOffset(child));
                                allFillParent = allFillParent4;
                                alternativeMaxWidth = delta2;
                                totalWeight = weightSum;
                            }
                        } else {
                            margin2 = maxWidth;
                        }
                        matchWidthLocally = false;
                        if (matchWidthLocally) {
                        }
                        i2 = Math.max(i2, matchWidthLocally ? margin2 : alternativeMaxWidth);
                        if (allFillParent) {
                        }
                        allFillParent4 = false;
                        maxWidth = this.mTotalLength;
                        weightSum = totalWeight;
                        this.mTotalLength = Math.max(maxWidth, (((maxWidth + child.getMeasuredHeight()) + lp5.topMargin) + lp5.bottomMargin) + getNextLocationOffset(child));
                        allFillParent = allFillParent4;
                        alternativeMaxWidth = delta2;
                        totalWeight = weightSum;
                    }
                    i3++;
                    useLargestChild = z;
                    heightMode = i8;
                    baselineChildIndex = i9;
                    maxWidth = i7;
                }
                z = useLargestChild;
                i8 = heightMode;
                i9 = baselineChildIndex;
                heightMode = widthMeasureSpec;
                this.mTotalLength += getPaddingTop() + getPaddingBottom();
                delta = alternativeMaxWidth;
                alternativeMaxWidth2 = i2;
                count = alternativeMaxWidth2;
                setMeasuredDimension(View.resolveSizeAndState(Math.max(count + (getPaddingLeft() + getPaddingRight()), getSuggestedMinimumWidth()), heightMode, childState2), i);
                if (!matchWidth) {
                }
            }
        }
    }

    private void forceUniformWidth(int count, int heightMeasureSpec) {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 1073741824);
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.width == -1) {
                    int oldHeight = lp.height;
                    lp.height = child.getMeasuredHeight();
                    measureChildWithMargins(child, uniformMeasureSpec, 0, heightMeasureSpec, 0);
                    lp.height = oldHeight;
                }
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x0215  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0205  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x01be  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x01f6  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0205  */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x0215  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x01be  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x01f6  */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x0215  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0205  */
    /* JADX WARNING: Removed duplicated region for block: B:203:0x051c  */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x04e4  */
    /* JADX WARNING: Removed duplicated region for block: B:224:0x05d1  */
    /* JADX WARNING: Removed duplicated region for block: B:223:0x05c9  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
        boolean baselineAligned;
        boolean matchHeightLocally;
        int alternativeMaxHeight;
        int childState;
        int i;
        float f;
        int i2;
        int count;
        boolean z;
        int i3;
        int i4 = widthMeasureSpec;
        int i5 = heightMeasureSpec;
        this.mTotalLength = 0;
        float totalWeight = 0.0f;
        int count2 = getVirtualChildCount();
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (this.mMaxAscent == null || this.mMaxDescent == null) {
            this.mMaxAscent = new int[4];
            this.mMaxDescent = new int[4];
        }
        int[] maxAscent = this.mMaxAscent;
        int[] maxDescent = this.mMaxDescent;
        boolean matchHeight = false;
        maxAscent[3] = -1;
        maxAscent[2] = -1;
        maxAscent[1] = -1;
        maxAscent[0] = -1;
        maxDescent[3] = -1;
        maxDescent[2] = -1;
        maxDescent[1] = -1;
        maxDescent[0] = -1;
        boolean baselineAligned2 = this.mBaselineAligned;
        boolean skippedMeasure = false;
        boolean useLargestChild = this.mUseLargestChild;
        int[] maxDescent2 = maxDescent;
        boolean isExactly = widthMode == 1073741824;
        int childState2 = 0;
        int largestChildWidth = 0;
        boolean matchHeight2 = matchHeight;
        matchHeight = true;
        int maxHeight = 0;
        int i6 = 0;
        int alternativeMaxHeight2 = 0;
        int weightedMaxHeight = 0;
        while (i6 < count2) {
            View child = getVirtualChildAt(i6);
            int largestChildWidth2;
            if (child == null) {
                largestChildWidth2 = largestChildWidth;
                this.mTotalLength += measureNullChild(i6);
                baselineAligned = baselineAligned2;
                largestChildWidth = largestChildWidth2;
            } else {
                largestChildWidth2 = largestChildWidth;
                int weightedMaxHeight2 = weightedMaxHeight;
                if (child.getVisibility() == 8) {
                    i6 += getChildrenSkipCount(child, i6);
                    baselineAligned = baselineAligned2;
                    largestChildWidth = largestChildWidth2;
                    weightedMaxHeight = weightedMaxHeight2;
                } else {
                    LayoutParams lp;
                    int maxHeight2;
                    int i7;
                    int margin;
                    boolean allFillParent;
                    if (hasDividerBeforeChildAt(i6)) {
                        this.mTotalLength += this.mDividerWidth;
                    }
                    LayoutParams lp2 = (LayoutParams) child.getLayoutParams();
                    float totalWeight2 = totalWeight + lp2.weight;
                    int i8;
                    int alternativeMaxHeight3;
                    int weightedMaxHeight3;
                    if (widthMode == 1073741824 && lp2.width == 0 && lp2.weight > 0.0f) {
                        if (isExactly) {
                            i8 = i6;
                            this.mTotalLength += lp2.leftMargin + lp2.rightMargin;
                        } else {
                            i8 = i6;
                            i6 = this.mTotalLength;
                            this.mTotalLength = Math.max(i6, (lp2.leftMargin + i6) + lp2.rightMargin);
                        }
                        if (baselineAligned2) {
                            largestChildWidth = MeasureSpec.makeMeasureSpec(0, 0);
                            child.measure(largestChildWidth, largestChildWidth);
                            lp = lp2;
                            alternativeMaxHeight3 = alternativeMaxHeight2;
                            maxHeight2 = maxHeight;
                            baselineAligned = baselineAligned2;
                            lp2 = largestChildWidth2;
                            weightedMaxHeight3 = weightedMaxHeight2;
                            i7 = i8;
                            i5 = -1;
                        } else {
                            skippedMeasure = true;
                            lp = lp2;
                            alternativeMaxHeight3 = alternativeMaxHeight2;
                            maxHeight2 = maxHeight;
                            baselineAligned = baselineAligned2;
                            weightedMaxHeight3 = weightedMaxHeight2;
                            i7 = i8;
                            i5 = -1;
                            matchHeightLocally = false;
                            matchHeight2 = true;
                            matchHeightLocally = true;
                            weightedMaxHeight = lp.topMargin + lp.bottomMargin;
                            alternativeMaxHeight2 = child.getMeasuredHeight() + weightedMaxHeight;
                            maxHeight = View.combineMeasuredStates(childState2, child.getMeasuredState());
                            if (baselineAligned) {
                            }
                            margin = weightedMaxHeight;
                            weightedMaxHeight = Math.max(maxHeight2, alternativeMaxHeight2);
                            if (!matchHeight) {
                            }
                            if (lp.weight <= 0.0f) {
                            }
                            i4 = i7;
                            i6 = i4 + getChildrenSkipCount(child, i4);
                            childState2 = maxHeight;
                            matchHeight = allFillParent;
                            alternativeMaxHeight2 = alternativeMaxHeight;
                            totalWeight = totalWeight2;
                            largestChildWidth = largestChildWidth2;
                            maxHeight = weightedMaxHeight;
                            weightedMaxHeight = i5;
                        }
                    } else {
                        i8 = i6;
                        i6 = Integer.MIN_VALUE;
                        if (lp2.width == 0 && lp2.weight > 0.0f) {
                            i6 = 0;
                            lp2.width = -2;
                        }
                        i7 = i8;
                        int largestChildWidth3 = largestChildWidth2;
                        LayoutParams lp3 = lp2;
                        weightedMaxHeight3 = weightedMaxHeight2;
                        alternativeMaxHeight3 = alternativeMaxHeight2;
                        maxHeight2 = maxHeight;
                        i4 = i6;
                        int i9 = i5;
                        baselineAligned = baselineAligned2;
                        i5 = -1;
                        measureChildBeforeLayout(child, i7, i4, totalWeight2 == 0.0f ? this.mTotalLength : 0, i9, false);
                        if (i4 != Integer.MIN_VALUE) {
                            lp = lp3;
                            lp.width = i4;
                        } else {
                            lp = lp3;
                        }
                        largestChildWidth = child.getMeasuredWidth();
                        if (isExactly) {
                            this.mTotalLength += ((lp.leftMargin + largestChildWidth) + lp.rightMargin) + getNextLocationOffset(child);
                        } else {
                            weightedMaxHeight = this.mTotalLength;
                            this.mTotalLength = Math.max(weightedMaxHeight, (((weightedMaxHeight + largestChildWidth) + lp.leftMargin) + lp.rightMargin) + getNextLocationOffset(child));
                        }
                        if (useLargestChild) {
                            largestChildWidth2 = Math.max(largestChildWidth, largestChildWidth3);
                            matchHeightLocally = false;
                            if (heightMode != 1073741824 && lp.height == i5) {
                                matchHeight2 = true;
                                matchHeightLocally = true;
                            }
                            weightedMaxHeight = lp.topMargin + lp.bottomMargin;
                            alternativeMaxHeight2 = child.getMeasuredHeight() + weightedMaxHeight;
                            maxHeight = View.combineMeasuredStates(childState2, child.getMeasuredState());
                            if (baselineAligned) {
                                i9 = child.getBaseline();
                                if (i9 != i5) {
                                    i4 = ((((lp.gravity < 0 ? this.mGravity : lp.gravity) & 112) >> 4) & -2) >> 1;
                                    maxAscent[i4] = Math.max(maxAscent[i4], i9);
                                    margin = weightedMaxHeight;
                                    maxDescent2[i4] = Math.max(maxDescent2[i4], alternativeMaxHeight2 - i9);
                                    weightedMaxHeight = Math.max(maxHeight2, alternativeMaxHeight2);
                                    allFillParent = matchHeight && lp.height == -1;
                                    if (lp.weight <= 0.0f) {
                                        i5 = Math.max(weightedMaxHeight3, matchHeightLocally ? margin : alternativeMaxHeight2);
                                        alternativeMaxHeight = alternativeMaxHeight3;
                                    } else {
                                        i5 = weightedMaxHeight3;
                                        alternativeMaxHeight = Math.max(alternativeMaxHeight3, matchHeightLocally ? margin : alternativeMaxHeight2);
                                    }
                                    i4 = i7;
                                    i6 = i4 + getChildrenSkipCount(child, i4);
                                    childState2 = maxHeight;
                                    matchHeight = allFillParent;
                                    alternativeMaxHeight2 = alternativeMaxHeight;
                                    totalWeight = totalWeight2;
                                    largestChildWidth = largestChildWidth2;
                                    maxHeight = weightedMaxHeight;
                                    weightedMaxHeight = i5;
                                }
                            }
                            margin = weightedMaxHeight;
                            weightedMaxHeight = Math.max(maxHeight2, alternativeMaxHeight2);
                            if (matchHeight) {
                            }
                            if (lp.weight <= 0.0f) {
                            }
                            i4 = i7;
                            i6 = i4 + getChildrenSkipCount(child, i4);
                            childState2 = maxHeight;
                            matchHeight = allFillParent;
                            alternativeMaxHeight2 = alternativeMaxHeight;
                            totalWeight = totalWeight2;
                            largestChildWidth = largestChildWidth2;
                            maxHeight = weightedMaxHeight;
                            weightedMaxHeight = i5;
                        } else {
                            lp2 = largestChildWidth3;
                        }
                    }
                    largestChildWidth2 = lp2;
                    matchHeightLocally = false;
                    matchHeight2 = true;
                    matchHeightLocally = true;
                    weightedMaxHeight = lp.topMargin + lp.bottomMargin;
                    alternativeMaxHeight2 = child.getMeasuredHeight() + weightedMaxHeight;
                    maxHeight = View.combineMeasuredStates(childState2, child.getMeasuredState());
                    if (baselineAligned) {
                    }
                    margin = weightedMaxHeight;
                    weightedMaxHeight = Math.max(maxHeight2, alternativeMaxHeight2);
                    if (matchHeight) {
                    }
                    if (lp.weight <= 0.0f) {
                    }
                    i4 = i7;
                    i6 = i4 + getChildrenSkipCount(child, i4);
                    childState2 = maxHeight;
                    matchHeight = allFillParent;
                    alternativeMaxHeight2 = alternativeMaxHeight;
                    totalWeight = totalWeight2;
                    largestChildWidth = largestChildWidth2;
                    maxHeight = weightedMaxHeight;
                    weightedMaxHeight = i5;
                }
            }
            i6++;
            baselineAligned2 = baselineAligned;
            i4 = widthMeasureSpec;
            i5 = heightMeasureSpec;
        }
        i5 = weightedMaxHeight;
        i4 = maxHeight;
        baselineAligned = baselineAligned2;
        alternativeMaxHeight = childState2;
        weightedMaxHeight = largestChildWidth;
        if (this.mTotalLength > 0 && hasDividerBeforeChildAt(count2)) {
            this.mTotalLength += this.mDividerWidth;
        }
        if (maxAscent[1] == -1 && maxAscent[0] == -1 && maxAscent[2] == -1 && maxAscent[3] == -1) {
            childState = alternativeMaxHeight;
            maxHeight = i4;
        } else {
            childState = alternativeMaxHeight;
            maxHeight = Math.max(i4, Math.max(maxAscent[3], Math.max(maxAscent[0], Math.max(maxAscent[1], maxAscent[2]))) + Math.max(maxDescent2[3], Math.max(maxDescent2[0], Math.max(maxDescent2[1], maxDescent2[2]))));
        }
        if (useLargestChild && (widthMode == Integer.MIN_VALUE || widthMode == 0)) {
            this.mTotalLength = 0;
            i6 = 0;
            while (i6 < count2) {
                int i10;
                View child2 = getVirtualChildAt(i6);
                if (child2 == null) {
                    this.mTotalLength += measureNullChild(i6);
                    i10 = i6;
                } else if (child2.getVisibility() == 8) {
                    i6 += getChildrenSkipCount(child2, i6);
                    i6++;
                } else {
                    LayoutParams lp4 = (LayoutParams) child2.getLayoutParams();
                    if (isExactly) {
                        i10 = i6;
                        this.mTotalLength += ((lp4.leftMargin + weightedMaxHeight) + lp4.rightMargin) + getNextLocationOffset(child2);
                    } else {
                        i10 = i6;
                        i6 = this.mTotalLength;
                        this.mTotalLength = Math.max(i6, (((i6 + weightedMaxHeight) + lp4.leftMargin) + lp4.rightMargin) + getNextLocationOffset(child2));
                    }
                }
                i6 = i10;
                i6++;
            }
        }
        this.mTotalLength += getPaddingLeft() + getPaddingRight();
        i4 = View.resolveSizeAndState(Math.max(this.mTotalLength, getSuggestedMinimumWidth()), widthMeasureSpec, 0);
        i6 = i4 & ViewCompat.MEASURED_SIZE_MASK;
        alternativeMaxHeight = i6 - this.mTotalLength;
        int i11;
        int i12;
        int widthSize;
        if (skippedMeasure) {
            i11 = weightedMaxHeight;
            i12 = maxHeight;
        } else if (alternativeMaxHeight == 0 || totalWeight <= 0.0f) {
            alternativeMaxHeight2 = Math.max(alternativeMaxHeight2, i5);
            if (useLargestChild && widthMode != 1073741824) {
                int i13 = 0;
                while (true) {
                    i = i13;
                    if (i >= count2) {
                        break;
                    }
                    int alternativeMaxHeight4;
                    widthSize = i6;
                    View child3 = getVirtualChildAt(i);
                    if (child3 != null) {
                        alternativeMaxHeight4 = alternativeMaxHeight2;
                        i12 = maxHeight;
                        if (child3.getVisibility() == 8) {
                            i11 = weightedMaxHeight;
                        } else {
                            LayoutParams lp5 = (LayoutParams) child3.getLayoutParams();
                            float childExtra = lp5.weight;
                            if (childExtra > 0.0f) {
                                i11 = weightedMaxHeight;
                                child3.measure(MeasureSpec.makeMeasureSpec(weightedMaxHeight, 1073741824), MeasureSpec.makeMeasureSpec(child3.getMeasuredHeight(), 1073741824));
                            } else {
                                i11 = weightedMaxHeight;
                            }
                        }
                    } else {
                        i11 = weightedMaxHeight;
                        alternativeMaxHeight4 = alternativeMaxHeight2;
                        i12 = maxHeight;
                    }
                    i13 = i + 1;
                    i6 = widthSize;
                    alternativeMaxHeight2 = alternativeMaxHeight4;
                    maxHeight = i12;
                    weightedMaxHeight = i11;
                }
            }
            i11 = weightedMaxHeight;
            f = totalWeight;
            i2 = i5;
            count = count2;
            z = useLargestChild;
            weightedMaxHeight = alternativeMaxHeight2;
            i = maxHeight;
            i3 = heightMeasureSpec;
            if (!(matchHeight || heightMode == 1073741824)) {
                i = weightedMaxHeight;
            }
            setMeasuredDimension((childState & ViewCompat.MEASURED_STATE_MASK) | i4, View.resolveSizeAndState(Math.max(i + (getPaddingTop() + getPaddingBottom()), getSuggestedMinimumHeight()), i3, childState << 16));
            if (matchHeight2) {
                largestChildWidth = widthMeasureSpec;
                return;
            }
            forceUniformHeight(count, widthMeasureSpec);
            return;
        } else {
            widthSize = i6;
            i11 = weightedMaxHeight;
            i12 = maxHeight;
        }
        float weightSum = this.mWeightSum > 0.0f ? this.mWeightSum : totalWeight;
        maxAscent[3] = -1;
        maxAscent[2] = -1;
        maxAscent[1] = -1;
        maxAscent[0] = -1;
        maxDescent2[3] = -1;
        maxDescent2[2] = -1;
        maxDescent2[1] = -1;
        maxDescent2[0] = -1;
        i = -1;
        this.mTotalLength = 0;
        weightedMaxHeight = alternativeMaxHeight2;
        maxHeight = childState;
        float weightSum2 = weightSum;
        i6 = 0;
        while (i6 < count2) {
            f = totalWeight;
            totalWeight = getVirtualChildAt(i6);
            if (totalWeight != null) {
                i2 = i5;
                z = useLargestChild;
                if (totalWeight.getVisibility()) {
                    count = count2;
                    i3 = heightMeasureSpec;
                } else {
                    float weightSum3;
                    int delta;
                    boolean allFillParent2;
                    int alternativeMaxHeight5;
                    LayoutParams weightedMaxHeight4 = (LayoutParams) totalWeight.getLayoutParams();
                    float childExtra2 = weightedMaxHeight4.weight;
                    if (childExtra2 > 0.0f) {
                        largestChildWidth = (int) ((((float) alternativeMaxHeight) * childExtra2) / weightSum2);
                        weightSum3 = weightSum2 - childExtra2;
                        delta = alternativeMaxHeight - largestChildWidth;
                        count = count2;
                        weightSum2 = getChildMeasureSpec(heightMeasureSpec, ((getPaddingTop() + getPaddingBottom()) + weightedMaxHeight4.topMargin) + weightedMaxHeight4.bottomMargin, weightedMaxHeight4.height);
                        if (weightedMaxHeight4.width == 0 && widthMode == 1073741824) {
                            totalWeight.measure(MeasureSpec.makeMeasureSpec(largestChildWidth > 0 ? largestChildWidth : 0, 1073741824), weightSum2);
                            int i14 = largestChildWidth;
                        } else {
                            alternativeMaxHeight = totalWeight.getMeasuredWidth() + largestChildWidth;
                            if (alternativeMaxHeight < 0) {
                                alternativeMaxHeight = 0;
                            }
                            totalWeight.measure(MeasureSpec.makeMeasureSpec(alternativeMaxHeight, 1073741824), weightSum2);
                        }
                        maxHeight = View.combineMeasuredStates(maxHeight, totalWeight.getMeasuredState() & ViewCompat.MEASURED_STATE_MASK);
                    } else {
                        count = count2;
                        float f2 = childExtra2;
                        useLargestChild = heightMeasureSpec;
                        weightSum3 = weightSum2;
                        delta = alternativeMaxHeight;
                    }
                    if (isExactly) {
                        this.mTotalLength += ((totalWeight.getMeasuredWidth() + weightedMaxHeight4.leftMargin) + weightedMaxHeight4.rightMargin) + getNextLocationOffset(totalWeight);
                    } else {
                        largestChildWidth = this.mTotalLength;
                        this.mTotalLength = Math.max(largestChildWidth, (((totalWeight.getMeasuredWidth() + largestChildWidth) + weightedMaxHeight4.leftMargin) + weightedMaxHeight4.rightMargin) + getNextLocationOffset(totalWeight));
                    }
                    matchHeightLocally = heightMode != 1073741824 && weightedMaxHeight4.height == -1;
                    alternativeMaxHeight2 = weightedMaxHeight4.topMargin + weightedMaxHeight4.bottomMargin;
                    alternativeMaxHeight = totalWeight.getMeasuredHeight() + alternativeMaxHeight2;
                    count2 = Math.max(i, alternativeMaxHeight);
                    weightedMaxHeight = Math.max(weightedMaxHeight, matchHeightLocally ? alternativeMaxHeight2 : alternativeMaxHeight);
                    if (matchHeight) {
                        if (weightedMaxHeight4.height) {
                            matchHeightLocally = true;
                            if (baselineAligned) {
                                allFillParent2 = matchHeightLocally;
                                alternativeMaxHeight5 = weightedMaxHeight;
                            } else {
                                boolean childBaseline = totalWeight.getBaseline();
                                allFillParent2 = matchHeightLocally;
                                if (!childBaseline) {
                                    matchHeightLocally = (weightedMaxHeight4.gravity >= false ? this.mGravity : weightedMaxHeight4.gravity) & 112;
                                    int index = ((matchHeightLocally >> 4) & -2) >> 1;
                                    boolean gravity = matchHeightLocally;
                                    maxAscent[index] = Math.max(maxAscent[index], childBaseline);
                                    alternativeMaxHeight5 = weightedMaxHeight;
                                    maxDescent2[index] = Math.max(maxDescent2[index], alternativeMaxHeight - childBaseline);
                                } else {
                                    alternativeMaxHeight5 = weightedMaxHeight;
                                }
                            }
                            i = count2;
                            weightSum2 = weightSum3;
                            alternativeMaxHeight = delta;
                            matchHeight = allFillParent2;
                            weightedMaxHeight = alternativeMaxHeight5;
                        }
                    }
                    matchHeightLocally = false;
                    if (baselineAligned) {
                    }
                    i = count2;
                    weightSum2 = weightSum3;
                    alternativeMaxHeight = delta;
                    matchHeight = allFillParent2;
                    weightedMaxHeight = alternativeMaxHeight5;
                }
            } else {
                i2 = i5;
                count = count2;
                z = useLargestChild;
                useLargestChild = heightMeasureSpec;
            }
            i6++;
            totalWeight = f;
            i5 = i2;
            useLargestChild = z;
            count2 = count;
            largestChildWidth = widthMeasureSpec;
        }
        i2 = i5;
        count = count2;
        z = useLargestChild;
        i3 = heightMeasureSpec;
        this.mTotalLength += getPaddingLeft() + getPaddingRight();
        if (!(maxAscent[1] == -1 && maxAscent[0] == -1 && maxAscent[2] == -1 && maxAscent[3] == -1)) {
            i = Math.max(i, Math.max(maxAscent[3], Math.max(maxAscent[0], Math.max(maxAscent[1], maxAscent[2]))) + Math.max(maxDescent2[3], Math.max(maxDescent2[0], Math.max(maxDescent2[1], maxDescent2[2]))));
        }
        childState = maxHeight;
        i = weightedMaxHeight;
        setMeasuredDimension((childState & ViewCompat.MEASURED_STATE_MASK) | i4, View.resolveSizeAndState(Math.max(i + (getPaddingTop() + getPaddingBottom()), getSuggestedMinimumHeight()), i3, childState << 16));
        if (matchHeight2) {
        }
    }

    private void forceUniformHeight(int count, int widthMeasureSpec) {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), 1073741824);
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.height == -1) {
                    int oldWidth = lp.width;
                    lp.width = child.getMeasuredWidth();
                    measureChildWithMargins(child, widthMeasureSpec, 0, uniformMeasureSpec, 0);
                    lp.width = oldWidth;
                }
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public int getChildrenSkipCount(View child, int index) {
        return 0;
    }

    /* Access modifiers changed, original: 0000 */
    public int measureNullChild(int childIndex) {
        return 0;
    }

    /* Access modifiers changed, original: 0000 */
    public void measureChildBeforeLayout(View child, int childIndex, int widthMeasureSpec, int totalWidth, int heightMeasureSpec, int totalHeight) {
        measureChildWithMargins(child, widthMeasureSpec, totalWidth, heightMeasureSpec, totalHeight);
    }

    /* Access modifiers changed, original: 0000 */
    public int getLocationOffset(View child) {
        return 0;
    }

    /* Access modifiers changed, original: 0000 */
    public int getNextLocationOffset(View child) {
        return 0;
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (this.mOrientation == 1) {
            layoutVertical(l, t, r, b);
        } else {
            layoutHorizontal(l, t, r, b);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void layoutVertical(int left, int top, int right, int bottom) {
        int childTop;
        int paddingLeft = getPaddingLeft();
        int width = right - left;
        int childRight = width - getPaddingRight();
        int childSpace = (width - paddingLeft) - getPaddingRight();
        int count = getVirtualChildCount();
        int majorGravity = this.mGravity & 112;
        int minorGravity = this.mGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if (majorGravity == 16) {
            childTop = getPaddingTop() + (((bottom - top) - this.mTotalLength) / 2);
        } else if (majorGravity != 80) {
            childTop = getPaddingTop();
        } else {
            childTop = ((getPaddingTop() + bottom) - top) - this.mTotalLength;
        }
        int i = 0;
        while (true) {
            int i2 = i;
            int paddingLeft2;
            if (i2 < count) {
                int majorGravity2;
                View child = getVirtualChildAt(i2);
                if (child == null) {
                    childTop += measureNullChild(i2);
                    majorGravity2 = majorGravity;
                    paddingLeft2 = paddingLeft;
                } else if (child.getVisibility() != 8) {
                    int childWidth = child.getMeasuredWidth();
                    int childHeight = child.getMeasuredHeight();
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    int gravity = lp.gravity;
                    if (gravity < 0) {
                        gravity = minorGravity;
                    }
                    int layoutDirection = ViewCompat.getLayoutDirection(this);
                    int gravity2 = gravity;
                    gravity = GravityCompat.getAbsoluteGravity(gravity, layoutDirection) & 7;
                    majorGravity2 = majorGravity;
                    gravity = gravity != 1 ? gravity != 5 ? lp.leftMargin + paddingLeft : (childRight - childWidth) - lp.rightMargin : ((((childSpace - childWidth) / 2) + paddingLeft) + lp.leftMargin) - lp.rightMargin;
                    if (hasDividerBeforeChildAt(i2) != 0) {
                        childTop += this.mDividerHeight;
                    }
                    gravity2 = childTop + lp.topMargin;
                    LayoutParams lp2 = lp;
                    View child2 = child;
                    paddingLeft2 = paddingLeft;
                    paddingLeft = i2;
                    setChildFrame(child, gravity, gravity2 + getLocationOffset(child), childWidth, childHeight);
                    i2 = paddingLeft + getChildrenSkipCount(child2, paddingLeft);
                    childTop = gravity2 + ((childHeight + lp2.bottomMargin) + getNextLocationOffset(child2));
                } else {
                    majorGravity2 = majorGravity;
                    paddingLeft2 = paddingLeft;
                    paddingLeft = i2;
                }
                i = i2 + 1;
                majorGravity = majorGravity2;
                paddingLeft = paddingLeft2;
            } else {
                paddingLeft2 = paddingLeft;
                return;
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x00c3  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x00fb  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x00cf  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x010e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void layoutHorizontal(int left, int top, int right, int bottom) {
        boolean isLayoutRtl = ViewUtils.isLayoutRtl(this);
        int paddingTop = getPaddingTop();
        int height = bottom - top;
        int childBottom = height - getPaddingBottom();
        int childSpace = (height - paddingTop) - getPaddingBottom();
        int count = getVirtualChildCount();
        int majorGravity = this.mGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        int minorGravity = this.mGravity & 112;
        boolean baselineAligned = this.mBaselineAligned;
        int[] maxAscent = this.mMaxAscent;
        int[] maxDescent = this.mMaxDescent;
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        int absoluteGravity = GravityCompat.getAbsoluteGravity(majorGravity, layoutDirection);
        if (absoluteGravity != 1) {
            if (absoluteGravity != 5) {
                absoluteGravity = getPaddingLeft();
            } else {
                absoluteGravity = ((getPaddingLeft() + right) - left) - this.mTotalLength;
            }
        } else {
            int i = layoutDirection;
            absoluteGravity = getPaddingLeft() + (((right - left) - this.mTotalLength) / 2);
        }
        layoutDirection = absoluteGravity;
        absoluteGravity = 0;
        int dir = 1;
        if (isLayoutRtl) {
            absoluteGravity = count - 1;
            dir = -1;
        }
        int i2 = 0;
        int childLeft = layoutDirection;
        while (true) {
            layoutDirection = i2;
            int[] maxAscent2;
            boolean baselineAligned2;
            int majorGravity2;
            int count2;
            boolean isLayoutRtl2;
            if (layoutDirection < count) {
                int[] maxDescent2;
                int childIndex = absoluteGravity + (dir * layoutDirection);
                View child = getVirtualChildAt(childIndex);
                if (child == null) {
                    childLeft += measureNullChild(childIndex);
                    maxDescent2 = maxDescent;
                    maxAscent2 = maxAscent;
                    baselineAligned2 = baselineAligned;
                    majorGravity2 = majorGravity;
                    count2 = count;
                    isLayoutRtl2 = isLayoutRtl;
                } else {
                    int i3 = layoutDirection;
                    majorGravity2 = majorGravity;
                    if (child.getVisibility() != 8) {
                        int childBaseline;
                        i2 = child.getMeasuredWidth();
                        int childHeight = child.getMeasuredHeight();
                        LayoutParams lp = (LayoutParams) child.getLayoutParams();
                        int childBaseline2 = -1;
                        if (baselineAligned) {
                            baselineAligned2 = baselineAligned;
                            if (!lp.height) {
                                childBaseline = child.getBaseline();
                                layoutDirection = lp.gravity;
                                if (layoutDirection < 0) {
                                    layoutDirection = minorGravity;
                                }
                                layoutDirection &= 112;
                                count2 = count;
                                if (layoutDirection != 16) {
                                    layoutDirection = ((((childSpace - childHeight) / 2) + paddingTop) + lp.topMargin) - lp.bottomMargin;
                                } else if (layoutDirection == 48) {
                                    layoutDirection = lp.topMargin + paddingTop;
                                    if (childBaseline != -1) {
                                        layoutDirection += maxAscent[1] - childBaseline;
                                    }
                                } else if (layoutDirection != 80) {
                                    layoutDirection = paddingTop;
                                } else {
                                    layoutDirection = (childBottom - childHeight) - lp.bottomMargin;
                                    if (childBaseline != -1) {
                                        layoutDirection -= maxDescent[2] - (child.getMeasuredHeight() - childBaseline);
                                    }
                                }
                                if (hasDividerBeforeChildAt(childIndex)) {
                                    childLeft += this.mDividerWidth;
                                }
                                childLeft += lp.leftMargin;
                                maxDescent2 = maxDescent;
                                maxAscent2 = maxAscent;
                                isLayoutRtl2 = isLayoutRtl;
                                isLayoutRtl = lp;
                                setChildFrame(child, childLeft + getLocationOffset(child), layoutDirection, i2, childHeight);
                                childLeft += (i2 + isLayoutRtl.rightMargin) + getNextLocationOffset(child);
                                layoutDirection = i3 + getChildrenSkipCount(child, childIndex);
                            }
                        } else {
                            baselineAligned2 = baselineAligned;
                        }
                        childBaseline = childBaseline2;
                        layoutDirection = lp.gravity;
                        if (layoutDirection < 0) {
                        }
                        layoutDirection &= 112;
                        count2 = count;
                        if (layoutDirection != 16) {
                        }
                        if (hasDividerBeforeChildAt(childIndex)) {
                        }
                        childLeft += lp.leftMargin;
                        maxDescent2 = maxDescent;
                        maxAscent2 = maxAscent;
                        isLayoutRtl2 = isLayoutRtl;
                        isLayoutRtl = lp;
                        setChildFrame(child, childLeft + getLocationOffset(child), layoutDirection, i2, childHeight);
                        childLeft += (i2 + isLayoutRtl.rightMargin) + getNextLocationOffset(child);
                        layoutDirection = i3 + getChildrenSkipCount(child, childIndex);
                    } else {
                        maxDescent2 = maxDescent;
                        maxAscent2 = maxAscent;
                        baselineAligned2 = baselineAligned;
                        count2 = count;
                        isLayoutRtl2 = isLayoutRtl;
                        layoutDirection = i3;
                    }
                }
                i2 = layoutDirection + 1;
                majorGravity = majorGravity2;
                baselineAligned = baselineAligned2;
                maxDescent = maxDescent2;
                count = count2;
                maxAscent = maxAscent2;
                isLayoutRtl = isLayoutRtl2;
            } else {
                maxAscent2 = maxAscent;
                baselineAligned2 = baselineAligned;
                majorGravity2 = majorGravity;
                count2 = count;
                isLayoutRtl2 = isLayoutRtl;
                return;
            }
        }
    }

    private void setChildFrame(View child, int left, int top, int width, int height) {
        child.layout(left, top, left + width, top + height);
    }

    public void setOrientation(int orientation) {
        if (this.mOrientation != orientation) {
            this.mOrientation = orientation;
            requestLayout();
        }
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setGravity(int gravity) {
        if (this.mGravity != gravity) {
            if ((GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK & gravity) == 0) {
                gravity |= GravityCompat.START;
            }
            if ((gravity & 112) == 0) {
                gravity |= 48;
            }
            this.mGravity = gravity;
            requestLayout();
        }
    }

    public int getGravity() {
        return this.mGravity;
    }

    public void setHorizontalGravity(int horizontalGravity) {
        int gravity = horizontalGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if ((GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK & this.mGravity) != gravity) {
            this.mGravity = (this.mGravity & -8388616) | gravity;
            requestLayout();
        }
    }

    public void setVerticalGravity(int verticalGravity) {
        int gravity = verticalGravity & 112;
        if ((this.mGravity & 112) != gravity) {
            this.mGravity = (this.mGravity & -113) | gravity;
            requestLayout();
        }
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /* Access modifiers changed, original: protected */
    public LayoutParams generateDefaultLayoutParams() {
        if (this.mOrientation == 0) {
            return new LayoutParams(-2, -2);
        }
        if (this.mOrientation == 1) {
            return new LayoutParams(-1, -2);
        }
        return null;
    }

    /* Access modifiers changed, original: protected */
    public LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    /* Access modifiers changed, original: protected */
    public boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(LinearLayoutCompat.class.getName());
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(LinearLayoutCompat.class.getName());
    }
}
