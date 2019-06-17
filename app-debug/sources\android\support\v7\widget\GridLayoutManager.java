package android.support.v7.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.RecyclerView.LayoutManager.LayoutPrefetchRegistry;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.MarginLayoutParams;
import java.util.Arrays;

public class GridLayoutManager extends LinearLayoutManager {
    private static final boolean DEBUG = false;
    public static final int DEFAULT_SPAN_COUNT = -1;
    private static final String TAG = "GridLayoutManager";
    int[] mCachedBorders;
    final Rect mDecorInsets = new Rect();
    boolean mPendingSpanCountChange = false;
    final SparseIntArray mPreLayoutSpanIndexCache = new SparseIntArray();
    final SparseIntArray mPreLayoutSpanSizeCache = new SparseIntArray();
    View[] mSet;
    int mSpanCount = -1;
    SpanSizeLookup mSpanSizeLookup = new DefaultSpanSizeLookup();

    public static abstract class SpanSizeLookup {
        private boolean mCacheSpanIndices = false;
        final SparseIntArray mSpanIndexCache = new SparseIntArray();

        public abstract int getSpanSize(int i);

        public void setSpanIndexCacheEnabled(boolean cacheSpanIndices) {
            this.mCacheSpanIndices = cacheSpanIndices;
        }

        public void invalidateSpanIndexCache() {
            this.mSpanIndexCache.clear();
        }

        public boolean isSpanIndexCacheEnabled() {
            return this.mCacheSpanIndices;
        }

        /* Access modifiers changed, original: 0000 */
        public int getCachedSpanIndex(int position, int spanCount) {
            if (!this.mCacheSpanIndices) {
                return getSpanIndex(position, spanCount);
            }
            int existing = this.mSpanIndexCache.get(position, -1);
            if (existing != -1) {
                return existing;
            }
            int value = getSpanIndex(position, spanCount);
            this.mSpanIndexCache.put(position, value);
            return value;
        }

        public int getSpanIndex(int position, int spanCount) {
            int positionSpanSize = getSpanSize(position);
            if (positionSpanSize == spanCount) {
                return 0;
            }
            int prevKey;
            int span = 0;
            int startPos = 0;
            if (this.mCacheSpanIndices && this.mSpanIndexCache.size() > 0) {
                prevKey = findReferenceIndexFromCache(position);
                if (prevKey >= 0) {
                    span = this.mSpanIndexCache.get(prevKey) + getSpanSize(prevKey);
                    startPos = prevKey + 1;
                }
            }
            prevKey = span;
            for (span = startPos; span < position; span++) {
                int size = getSpanSize(span);
                prevKey += size;
                if (prevKey == spanCount) {
                    prevKey = 0;
                } else if (prevKey > spanCount) {
                    prevKey = size;
                }
            }
            if (prevKey + positionSpanSize <= spanCount) {
                return prevKey;
            }
            return 0;
        }

        /* Access modifiers changed, original: 0000 */
        public int findReferenceIndexFromCache(int position) {
            int hi;
            int lo = 0;
            int hi2 = this.mSpanIndexCache.size() - 1;
            while (lo <= hi2) {
                hi = (lo + hi2) >>> 1;
                if (this.mSpanIndexCache.keyAt(hi) < position) {
                    lo = hi + 1;
                } else {
                    hi2 = hi - 1;
                }
            }
            hi = lo - 1;
            if (hi < 0 || hi >= this.mSpanIndexCache.size()) {
                return -1;
            }
            return this.mSpanIndexCache.keyAt(hi);
        }

        public int getSpanGroupIndex(int adapterPosition, int spanCount) {
            int span = 0;
            int group = 0;
            int positionSpanSize = getSpanSize(adapterPosition);
            for (int i = 0; i < adapterPosition; i++) {
                int size = getSpanSize(i);
                span += size;
                if (span == spanCount) {
                    span = 0;
                    group++;
                } else if (span > spanCount) {
                    span = size;
                    group++;
                }
            }
            if (span + positionSpanSize > spanCount) {
                return group + 1;
            }
            return group;
        }
    }

    public static final class DefaultSpanSizeLookup extends SpanSizeLookup {
        public int getSpanSize(int position) {
            return 1;
        }

        public int getSpanIndex(int position, int spanCount) {
            return position % spanCount;
        }
    }

    public static class LayoutParams extends android.support.v7.widget.RecyclerView.LayoutParams {
        public static final int INVALID_SPAN_ID = -1;
        int mSpanIndex = -1;
        int mSpanSize = 0;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(android.support.v7.widget.RecyclerView.LayoutParams source) {
            super(source);
        }

        public int getSpanIndex() {
            return this.mSpanIndex;
        }

        public int getSpanSize() {
            return this.mSpanSize;
        }
    }

    public GridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setSpanCount(LayoutManager.getProperties(context, attrs, defStyleAttr, defStyleRes).spanCount);
    }

    public GridLayoutManager(Context context, int spanCount) {
        super(context);
        setSpanCount(spanCount);
    }

    public GridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        setSpanCount(spanCount);
    }

    public void setStackFromEnd(boolean stackFromEnd) {
        if (stackFromEnd) {
            throw new UnsupportedOperationException("GridLayoutManager does not support stack from end. Consider using reverse layout");
        }
        super.setStackFromEnd(false);
    }

    public int getRowCountForAccessibility(Recycler recycler, State state) {
        if (this.mOrientation == 0) {
            return this.mSpanCount;
        }
        if (state.getItemCount() < 1) {
            return 0;
        }
        return getSpanGroupIndex(recycler, state, state.getItemCount() - 1) + 1;
    }

    public int getColumnCountForAccessibility(Recycler recycler, State state) {
        if (this.mOrientation == 1) {
            return this.mSpanCount;
        }
        if (state.getItemCount() < 1) {
            return 0;
        }
        return getSpanGroupIndex(recycler, state, state.getItemCount() - 1) + 1;
    }

    public void onInitializeAccessibilityNodeInfoForItem(Recycler recycler, State state, View host, AccessibilityNodeInfoCompat info) {
        android.view.ViewGroup.LayoutParams lp = host.getLayoutParams();
        if (lp instanceof LayoutParams) {
            LayoutParams glp = (LayoutParams) lp;
            int spanGroupIndex = getSpanGroupIndex(recycler, state, glp.getViewLayoutPosition());
            if (this.mOrientation == 0) {
                int spanIndex = glp.getSpanIndex();
                int spanSize = glp.getSpanSize();
                boolean z = this.mSpanCount > 1 && glp.getSpanSize() == this.mSpanCount;
                info.setCollectionItemInfo(CollectionItemInfoCompat.obtain(spanIndex, spanSize, spanGroupIndex, 1, z, false));
            } else {
                int spanIndex2 = glp.getSpanIndex();
                int spanSize2 = glp.getSpanSize();
                boolean z2 = this.mSpanCount > 1 && glp.getSpanSize() == this.mSpanCount;
                info.setCollectionItemInfo(CollectionItemInfoCompat.obtain(spanGroupIndex, 1, spanIndex2, spanSize2, z2, false));
            }
            return;
        }
        super.onInitializeAccessibilityNodeInfoForItem(host, info);
    }

    public void onLayoutChildren(Recycler recycler, State state) {
        if (state.isPreLayout()) {
            cachePreLayoutSpanMapping();
        }
        super.onLayoutChildren(recycler, state);
        clearPreLayoutSpanMappingCache();
    }

    public void onLayoutCompleted(State state) {
        super.onLayoutCompleted(state);
        this.mPendingSpanCountChange = false;
    }

    private void clearPreLayoutSpanMappingCache() {
        this.mPreLayoutSpanSizeCache.clear();
        this.mPreLayoutSpanIndexCache.clear();
    }

    private void cachePreLayoutSpanMapping() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            int viewPosition = lp.getViewLayoutPosition();
            this.mPreLayoutSpanSizeCache.put(viewPosition, lp.getSpanSize());
            this.mPreLayoutSpanIndexCache.put(viewPosition, lp.getSpanIndex());
        }
    }

    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    public void onItemsChanged(RecyclerView recyclerView) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    public android.support.v7.widget.RecyclerView.LayoutParams generateDefaultLayoutParams() {
        if (this.mOrientation == 0) {
            return new LayoutParams(-2, -1);
        }
        return new LayoutParams(-1, -2);
    }

    public android.support.v7.widget.RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    public android.support.v7.widget.RecyclerView.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams lp) {
        if (lp instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    public boolean checkLayoutParams(android.support.v7.widget.RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup) {
        this.mSpanSizeLookup = spanSizeLookup;
    }

    public SpanSizeLookup getSpanSizeLookup() {
        return this.mSpanSizeLookup;
    }

    private void updateMeasurements() {
        int totalSpace;
        if (getOrientation() == 1) {
            totalSpace = (getWidth() - getPaddingRight()) - getPaddingLeft();
        } else {
            totalSpace = (getHeight() - getPaddingBottom()) - getPaddingTop();
        }
        calculateItemBorders(totalSpace);
    }

    public void setMeasuredDimension(Rect childrenBounds, int wSpec, int hSpec) {
        int width;
        int height;
        if (this.mCachedBorders == null) {
            super.setMeasuredDimension(childrenBounds, wSpec, hSpec);
        }
        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        if (this.mOrientation == 1) {
            int chooseSize = LayoutManager.chooseSize(hSpec, childrenBounds.height() + verticalPadding, getMinimumHeight());
            width = LayoutManager.chooseSize(wSpec, this.mCachedBorders[this.mCachedBorders.length - 1] + horizontalPadding, getMinimumWidth());
            height = chooseSize;
        } else {
            width = LayoutManager.chooseSize(wSpec, childrenBounds.width() + horizontalPadding, getMinimumWidth());
            height = LayoutManager.chooseSize(hSpec, this.mCachedBorders[this.mCachedBorders.length - 1] + verticalPadding, getMinimumHeight());
        }
        setMeasuredDimension(width, height);
    }

    private void calculateItemBorders(int totalSpace) {
        this.mCachedBorders = calculateItemBorders(this.mCachedBorders, this.mSpanCount, totalSpace);
    }

    static int[] calculateItemBorders(int[] cachedBorders, int spanCount, int totalSpace) {
        int i = 1;
        if (!(cachedBorders != null && cachedBorders.length == spanCount + 1 && cachedBorders[cachedBorders.length - 1] == totalSpace)) {
            cachedBorders = new int[(spanCount + 1)];
        }
        cachedBorders[0] = 0;
        int sizePerSpan = totalSpace / spanCount;
        int sizePerSpanRemainder = totalSpace % spanCount;
        int consumedPixels = 0;
        int additionalSize = 0;
        while (i <= spanCount) {
            int itemSize = sizePerSpan;
            additionalSize += sizePerSpanRemainder;
            if (additionalSize > 0 && spanCount - additionalSize < sizePerSpanRemainder) {
                itemSize++;
                additionalSize -= spanCount;
            }
            consumedPixels += itemSize;
            cachedBorders[i] = consumedPixels;
            i++;
        }
        return cachedBorders;
    }

    /* Access modifiers changed, original: 0000 */
    public int getSpaceForSpanRange(int startSpan, int spanSize) {
        if (this.mOrientation == 1 && isLayoutRTL()) {
            return this.mCachedBorders[this.mSpanCount - startSpan] - this.mCachedBorders[(this.mSpanCount - startSpan) - spanSize];
        }
        return this.mCachedBorders[startSpan + spanSize] - this.mCachedBorders[startSpan];
    }

    /* Access modifiers changed, original: 0000 */
    public void onAnchorReady(Recycler recycler, State state, AnchorInfo anchorInfo, int itemDirection) {
        super.onAnchorReady(recycler, state, anchorInfo, itemDirection);
        updateMeasurements();
        if (state.getItemCount() > 0 && !state.isPreLayout()) {
            ensureAnchorIsInCorrectSpan(recycler, state, anchorInfo, itemDirection);
        }
        ensureViewSet();
    }

    private void ensureViewSet() {
        if (this.mSet == null || this.mSet.length != this.mSpanCount) {
            this.mSet = new View[this.mSpanCount];
        }
    }

    public int scrollHorizontallyBy(int dx, Recycler recycler, State state) {
        updateMeasurements();
        ensureViewSet();
        return super.scrollHorizontallyBy(dx, recycler, state);
    }

    public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
        updateMeasurements();
        ensureViewSet();
        return super.scrollVerticallyBy(dy, recycler, state);
    }

    private void ensureAnchorIsInCorrectSpan(Recycler recycler, State state, AnchorInfo anchorInfo, int itemDirection) {
        boolean layingOutInPrimaryDirection = itemDirection == 1;
        int span = getSpanIndex(recycler, state, anchorInfo.mPosition);
        if (layingOutInPrimaryDirection) {
            while (span > 0 && anchorInfo.mPosition > 0) {
                anchorInfo.mPosition--;
                span = getSpanIndex(recycler, state, anchorInfo.mPosition);
            }
            return;
        }
        int indexLimit = state.getItemCount() - 1;
        int pos = anchorInfo.mPosition;
        int bestSpan = span;
        while (pos < indexLimit) {
            int next = getSpanIndex(recycler, state, pos + 1);
            if (next <= bestSpan) {
                break;
            }
            pos++;
            bestSpan = next;
        }
        anchorInfo.mPosition = pos;
    }

    /* Access modifiers changed, original: 0000 */
    public View findReferenceChild(Recycler recycler, State state, int start, int end, int itemCount) {
        ensureLayoutState();
        View outOfBoundsMatch = null;
        int boundsStart = this.mOrientationHelper.getStartAfterPadding();
        int boundsEnd = this.mOrientationHelper.getEndAfterPadding();
        int diff = end > start ? 1 : -1;
        View invalidMatch = null;
        for (int i = start; i != end; i += diff) {
            View view = getChildAt(i);
            int position = getPosition(view);
            if (position >= 0 && position < itemCount && getSpanIndex(recycler, state, position) == 0) {
                if (((android.support.v7.widget.RecyclerView.LayoutParams) view.getLayoutParams()).isItemRemoved()) {
                    if (invalidMatch == null) {
                        invalidMatch = view;
                    }
                } else if (this.mOrientationHelper.getDecoratedStart(view) < boundsEnd && this.mOrientationHelper.getDecoratedEnd(view) >= boundsStart) {
                    return view;
                } else {
                    if (outOfBoundsMatch == null) {
                        outOfBoundsMatch = view;
                    }
                }
            }
        }
        return outOfBoundsMatch != null ? outOfBoundsMatch : invalidMatch;
    }

    private int getSpanGroupIndex(Recycler recycler, State state, int viewPosition) {
        if (!state.isPreLayout()) {
            return this.mSpanSizeLookup.getSpanGroupIndex(viewPosition, this.mSpanCount);
        }
        int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(viewPosition);
        if (adapterPosition != -1) {
            return this.mSpanSizeLookup.getSpanGroupIndex(adapterPosition, this.mSpanCount);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot find span size for pre layout position. ");
        stringBuilder.append(viewPosition);
        Log.w(str, stringBuilder.toString());
        return 0;
    }

    private int getSpanIndex(Recycler recycler, State state, int pos) {
        if (!state.isPreLayout()) {
            return this.mSpanSizeLookup.getCachedSpanIndex(pos, this.mSpanCount);
        }
        int cached = this.mPreLayoutSpanIndexCache.get(pos, -1);
        if (cached != -1) {
            return cached;
        }
        int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition != -1) {
            return this.mSpanSizeLookup.getCachedSpanIndex(adapterPosition, this.mSpanCount);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot find span size for pre layout position. It is not cached, not in the adapter. Pos:");
        stringBuilder.append(pos);
        Log.w(str, stringBuilder.toString());
        return 0;
    }

    private int getSpanSize(Recycler recycler, State state, int pos) {
        if (!state.isPreLayout()) {
            return this.mSpanSizeLookup.getSpanSize(pos);
        }
        int cached = this.mPreLayoutSpanSizeCache.get(pos, -1);
        if (cached != -1) {
            return cached;
        }
        int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition != -1) {
            return this.mSpanSizeLookup.getSpanSize(adapterPosition);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot find span size for pre layout position. It is not cached, not in the adapter. Pos:");
        stringBuilder.append(pos);
        Log.w(str, stringBuilder.toString());
        return 1;
    }

    /* Access modifiers changed, original: 0000 */
    public void collectPrefetchPositionsForLayoutState(State state, LayoutState layoutState, LayoutPrefetchRegistry layoutPrefetchRegistry) {
        int remainingSpan = this.mSpanCount;
        for (int count = 0; count < this.mSpanCount && layoutState.hasMore(state) && remainingSpan > 0; count++) {
            int pos = layoutState.mCurrentPosition;
            layoutPrefetchRegistry.addPosition(pos, Math.max(0, layoutState.mScrollingOffset));
            remainingSpan -= this.mSpanSizeLookup.getSpanSize(pos);
            layoutState.mCurrentPosition += layoutState.mItemDirection;
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void layoutChunk(Recycler recycler, State state, LayoutState layoutState, LayoutChunkResult result) {
        int pos;
        int spanSize;
        Recycler recycler2 = recycler;
        State state2 = state;
        LayoutState layoutState2 = layoutState;
        LayoutChunkResult layoutChunkResult = result;
        int otherDirSpecMode = this.mOrientationHelper.getModeInOther();
        int i = 0;
        boolean flexibleInOtherDir = otherDirSpecMode != 1073741824;
        int currentOtherDirSize = getChildCount() > 0 ? this.mCachedBorders[this.mSpanCount] : 0;
        if (flexibleInOtherDir) {
            updateMeasurements();
        }
        boolean layingOutInPrimaryDirection = layoutState2.mItemDirection == 1;
        int remainingSpan = this.mSpanCount;
        if (!layingOutInPrimaryDirection) {
            remainingSpan = getSpanIndex(recycler2, state2, layoutState2.mCurrentPosition) + getSpanSize(recycler2, state2, layoutState2.mCurrentPosition);
        }
        int count = 0;
        int consumedSpanCount = 0;
        while (count < this.mSpanCount && layoutState2.hasMore(state2) && remainingSpan > 0) {
            pos = layoutState2.mCurrentPosition;
            spanSize = getSpanSize(recycler2, state2, pos);
            if (spanSize > this.mSpanCount) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Item at position ");
                stringBuilder.append(pos);
                stringBuilder.append(" requires ");
                stringBuilder.append(spanSize);
                stringBuilder.append(" spans but GridLayoutManager has only ");
                stringBuilder.append(this.mSpanCount);
                stringBuilder.append(" spans.");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            remainingSpan -= spanSize;
            if (remainingSpan < 0) {
                break;
            }
            View view = layoutState2.next(recycler2);
            if (view == null) {
                break;
            }
            consumedSpanCount += spanSize;
            this.mSet[count] = view;
            count++;
        }
        int remainingSpan2 = remainingSpan;
        if (count == 0) {
            layoutChunkResult.mFinished = true;
            return;
        }
        View view2;
        int size;
        float maxSizeInOther;
        int horizontalInsets;
        int otherDirSpecMode2;
        int remainingSpan3;
        int count2 = count;
        int currentOtherDirSize2 = currentOtherDirSize;
        assignSpans(recycler2, state2, count, consumedSpanCount, layingOutInPrimaryDirection);
        pos = 0;
        spanSize = 0;
        float maxSizeInOther2 = 0.0f;
        while (pos < count2) {
            view2 = this.mSet[pos];
            if (layoutState2.mScrapList == null) {
                if (layingOutInPrimaryDirection) {
                    addView(view2);
                } else {
                    addView(view2, i);
                }
            } else if (layingOutInPrimaryDirection) {
                addDisappearingView(view2);
            } else {
                addDisappearingView(view2, i);
            }
            calculateItemDecorationsForChild(view2, this.mDecorInsets);
            measureChild(view2, otherDirSpecMode, i);
            size = this.mOrientationHelper.getDecoratedMeasurement(view2);
            if (size > spanSize) {
                spanSize = size;
            }
            float otherSize = (1.0f * ((float) this.mOrientationHelper.getDecoratedMeasurementInOther(view2))) / ((float) ((LayoutParams) view2.getLayoutParams()).mSpanSize);
            if (otherSize > maxSizeInOther2) {
                maxSizeInOther2 = otherSize;
            }
            pos++;
            i = 0;
        }
        if (flexibleInOtherDir) {
            guessMeasurement(maxSizeInOther2, currentOtherDirSize2);
            spanSize = 0;
            for (pos = 0; pos < count2; pos++) {
                view2 = this.mSet[pos];
                measureChild(view2, 1073741824, true);
                size = this.mOrientationHelper.getDecoratedMeasurement(view2);
                if (size > spanSize) {
                    spanSize = size;
                }
            }
            count = spanSize;
        } else {
            count = spanSize;
        }
        pos = 0;
        while (pos < count2) {
            View view3 = this.mSet[pos];
            if (this.mOrientationHelper.getDecoratedMeasurement(view3) != count) {
                int wSpec;
                LayoutParams lp = (LayoutParams) view3.getLayoutParams();
                Rect decorInsets = lp.mDecorInsets;
                maxSizeInOther = maxSizeInOther2;
                maxSizeInOther2 = ((decorInsets.top + decorInsets.bottom) + lp.topMargin) + lp.bottomMargin;
                horizontalInsets = ((decorInsets.left + decorInsets.right) + lp.leftMargin) + lp.rightMargin;
                size = getSpaceForSpanRange(lp.mSpanIndex, lp.mSpanSize);
                otherDirSpecMode2 = otherDirSpecMode;
                if (this.mOrientation == 1) {
                    remainingSpan3 = remainingSpan2;
                    wSpec = LayoutManager.getChildMeasureSpec(size, 1073741824, horizontalInsets, lp.width, false);
                    otherDirSpecMode = MeasureSpec.makeMeasureSpec(count - maxSizeInOther2, 1073741824);
                    LayoutParams layoutParams = lp;
                } else {
                    remainingSpan3 = remainingSpan2;
                    wSpec = MeasureSpec.makeMeasureSpec(count - horizontalInsets, 1073741824);
                    otherDirSpecMode = LayoutManager.getChildMeasureSpec(size, 1073741824, maxSizeInOther2, lp.height, null);
                }
                measureChildWithDecorationsAndMargin(view3, wSpec, otherDirSpecMode, true);
            } else {
                maxSizeInOther = maxSizeInOther2;
                otherDirSpecMode2 = otherDirSpecMode;
                remainingSpan3 = remainingSpan2;
            }
            pos++;
            maxSizeInOther2 = maxSizeInOther;
            otherDirSpecMode = otherDirSpecMode2;
            remainingSpan2 = remainingSpan3;
            recycler2 = recycler;
            state2 = state;
        }
        maxSizeInOther = maxSizeInOther2;
        otherDirSpecMode2 = otherDirSpecMode;
        remainingSpan3 = remainingSpan2;
        layoutChunkResult.mConsumed = count;
        pos = 0;
        spanSize = 0;
        remainingSpan = 0;
        size = 0;
        if (this.mOrientation == 1) {
            if (layoutState2.mLayoutDirection == -1) {
                size = layoutState2.mOffset;
                remainingSpan = size - count;
            } else {
                remainingSpan = layoutState2.mOffset;
                size = remainingSpan + count;
            }
        } else if (layoutState2.mLayoutDirection == -1) {
            spanSize = layoutState2.mOffset;
            pos = spanSize - count;
        } else {
            pos = layoutState2.mOffset;
            spanSize = pos + count;
        }
        int i2 = 0;
        while (true) {
            horizontalInsets = i2;
            int right;
            int maxSize;
            float maxSizeInOther3;
            if (horizontalInsets < count2) {
                int left;
                int bottom;
                View view4 = this.mSet[horizontalInsets];
                LayoutParams params = (LayoutParams) view4.getLayoutParams();
                if (this.mOrientation != 1) {
                    left = pos;
                    right = spanSize;
                    pos = getPaddingTop() + this.mCachedBorders[params.mSpanIndex];
                    remainingSpan2 = pos;
                    bottom = this.mOrientationHelper.getDecoratedMeasurementInOther(view4) + pos;
                } else if (isLayoutRTL()) {
                    currentOtherDirSize = getPaddingLeft() + this.mCachedBorders[this.mSpanCount - params.mSpanIndex];
                    left = currentOtherDirSize - this.mOrientationHelper.getDecoratedMeasurementInOther(view4);
                    remainingSpan2 = remainingSpan;
                    bottom = size;
                    right = currentOtherDirSize;
                } else {
                    left = pos;
                    right = spanSize;
                    pos = getPaddingLeft() + this.mCachedBorders[params.mSpanIndex];
                    left = pos;
                    right = this.mOrientationHelper.getDecoratedMeasurementInOther(view4) + pos;
                    remainingSpan2 = remainingSpan;
                    bottom = size;
                }
                maxSize = count;
                maxSizeInOther3 = maxSizeInOther;
                layoutDecoratedWithMargins(view4, left, remainingSpan2, right, bottom);
                if (params.isItemRemoved() != 0 || params.isItemChanged() != 0) {
                    layoutChunkResult.mIgnoreConsumed = true;
                }
                layoutChunkResult.mFocusable |= view4.hasFocusable();
                i2 = horizontalInsets + 1;
                remainingSpan = remainingSpan2;
                size = bottom;
                count = maxSize;
                maxSizeInOther = maxSizeInOther3;
                pos = left;
                spanSize = right;
            } else {
                right = spanSize;
                maxSize = count;
                maxSizeInOther3 = maxSizeInOther;
                Arrays.fill(this.mSet, null);
                return;
            }
        }
    }

    private void measureChild(View view, int otherDirParentSpecMode, boolean alreadyMeasured) {
        int wSpec;
        int hSpec;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        Rect decorInsets = lp.mDecorInsets;
        int verticalInsets = ((decorInsets.top + decorInsets.bottom) + lp.topMargin) + lp.bottomMargin;
        int horizontalInsets = ((decorInsets.left + decorInsets.right) + lp.leftMargin) + lp.rightMargin;
        int availableSpaceInOther = getSpaceForSpanRange(lp.mSpanIndex, lp.mSpanSize);
        if (this.mOrientation == 1) {
            wSpec = LayoutManager.getChildMeasureSpec(availableSpaceInOther, otherDirParentSpecMode, horizontalInsets, lp.width, false);
            hSpec = LayoutManager.getChildMeasureSpec(this.mOrientationHelper.getTotalSpace(), getHeightMode(), verticalInsets, lp.height, true);
        } else {
            hSpec = LayoutManager.getChildMeasureSpec(availableSpaceInOther, otherDirParentSpecMode, verticalInsets, lp.height, false);
            wSpec = LayoutManager.getChildMeasureSpec(this.mOrientationHelper.getTotalSpace(), getWidthMode(), horizontalInsets, lp.width, true);
        }
        measureChildWithDecorationsAndMargin(view, wSpec, hSpec, alreadyMeasured);
    }

    private void guessMeasurement(float maxSizeInOther, int currentOtherDirSize) {
        calculateItemBorders(Math.max(Math.round(((float) this.mSpanCount) * maxSizeInOther), currentOtherDirSize));
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec, boolean alreadyMeasured) {
        boolean measure;
        android.support.v7.widget.RecyclerView.LayoutParams lp = (android.support.v7.widget.RecyclerView.LayoutParams) child.getLayoutParams();
        if (alreadyMeasured) {
            measure = shouldReMeasureChild(child, widthSpec, heightSpec, lp);
        } else {
            measure = shouldMeasureChild(child, widthSpec, heightSpec, lp);
        }
        if (measure) {
            child.measure(widthSpec, heightSpec);
        }
    }

    private void assignSpans(Recycler recycler, State state, int count, int consumedSpanCount, boolean layingOutInPrimaryDirection) {
        int start;
        int end;
        int diff;
        if (layingOutInPrimaryDirection) {
            start = 0;
            end = count;
            diff = 1;
        } else {
            start = count - 1;
            end = -1;
            diff = -1;
        }
        int span = 0;
        for (int i = start; i != end; i += diff) {
            View view = this.mSet[i];
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            params.mSpanSize = getSpanSize(recycler, state, getPosition(view));
            params.mSpanIndex = span;
            span += params.mSpanSize;
        }
    }

    public int getSpanCount() {
        return this.mSpanCount;
    }

    public void setSpanCount(int spanCount) {
        if (spanCount != this.mSpanCount) {
            this.mPendingSpanCountChange = true;
            if (spanCount < 1) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Span count should be at least 1. Provided ");
                stringBuilder.append(spanCount);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.mSpanCount = spanCount;
            this.mSpanSizeLookup.invalidateSpanIndexCache();
            requestLayout();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:74:0x013e  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x013e  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x013e  */
    /* JADX WARNING: Missing block: B:28:0x0089, code skipped:
            r29 = r3;
            r33 = r4;
            r34 = r5;
            r30 = r11;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public View onFocusSearchFailed(View focused, int focusDirection, Recycler recycler, State state) {
        Recycler recycler2 = recycler;
        State state2 = state;
        View prevFocusedChild = findContainingItemView(focused);
        if (prevFocusedChild == null) {
            return null;
        }
        LayoutParams lp = (LayoutParams) prevFocusedChild.getLayoutParams();
        int prevSpanStart = lp.mSpanIndex;
        int prevSpanEnd = lp.mSpanIndex + lp.mSpanSize;
        View view = super.onFocusSearchFailed(focused, focusDirection, recycler, state);
        if (view == null) {
            return null;
        }
        int start;
        int inc;
        int limit;
        int layoutDir = convertFocusDirectionToLayoutDirection(focusDirection);
        boolean ascend = (layoutDir == 1) != this.mShouldReverseLayout;
        if (ascend) {
            start = getChildCount() - 1;
            inc = -1;
            limit = -1;
        } else {
            start = 0;
            inc = 1;
            limit = getChildCount();
        }
        boolean preferLastSpan = this.mOrientation == 1 && isLayoutRTL();
        View unfocusableWeakCandidate = null;
        int focusableSpanGroupIndex = getSpanGroupIndex(recycler2, state2, start);
        int focusableWeakCandidateSpanIndex = -1;
        int focusableWeakCandidateOverlap = 0;
        int unfocusableWeakCandidateSpanIndex = -1;
        layoutDir = 0;
        View focusableWeakCandidate = null;
        int i = start;
        while (true) {
            boolean ascend2 = ascend;
            int i2 = i;
            int i3;
            int i4;
            int i5;
            int i6;
            if (i2 == limit) {
                i3 = focusableWeakCandidateSpanIndex;
                i4 = focusableWeakCandidateOverlap;
                i5 = focusableSpanGroupIndex;
                i6 = start;
                break;
            }
            i6 = start;
            start = getSpanGroupIndex(recycler2, state2, i2);
            View candidate = getChildAt(i2);
            if (candidate == prevFocusedChild) {
                break;
            }
            View prevFocusedChild2;
            if (!candidate.hasFocusable() || start == focusableSpanGroupIndex) {
                LayoutParams candidateLp = (LayoutParams) candidate.getLayoutParams();
                prevFocusedChild2 = prevFocusedChild;
                prevFocusedChild = candidateLp.mSpanIndex;
                i5 = focusableSpanGroupIndex;
                focusableSpanGroupIndex = candidateLp.mSpanIndex + candidateLp.mSpanSize;
                if (candidate.hasFocusable() && prevFocusedChild == prevSpanStart && focusableSpanGroupIndex == prevSpanEnd) {
                    return candidate;
                }
                boolean assignAsWeek;
                if (!(candidate.hasFocusable() && focusableWeakCandidate == null) && (candidate.hasFocusable() || unfocusableWeakCandidate != null)) {
                    boolean assignAsWeek2 = false;
                    start = Math.min(focusableSpanGroupIndex, prevSpanEnd) - Math.max(prevFocusedChild, prevSpanStart);
                    boolean z;
                    if (!candidate.hasFocusable()) {
                        i3 = focusableWeakCandidateSpanIndex;
                        if (focusableWeakCandidate == null) {
                            i4 = focusableWeakCandidateOverlap;
                            z = true;
                            if (isViewPartiallyVisible(candidate, false, true)) {
                                if (start > layoutDir) {
                                    assignAsWeek = true;
                                } else if (start == layoutDir) {
                                    if (prevFocusedChild <= unfocusableWeakCandidateSpanIndex) {
                                        z = false;
                                    }
                                    if (preferLastSpan == z) {
                                        assignAsWeek = true;
                                    }
                                }
                                if (assignAsWeek) {
                                }
                            }
                        } else {
                            i4 = focusableWeakCandidateOverlap;
                        }
                    } else if (start > focusableWeakCandidateOverlap) {
                        i3 = focusableWeakCandidateSpanIndex;
                        i4 = focusableWeakCandidateOverlap;
                        assignAsWeek = true;
                        if (assignAsWeek) {
                            if (candidate.hasFocusable()) {
                                focusableWeakCandidate = candidate;
                                focusableWeakCandidateSpanIndex = candidateLp.mSpanIndex;
                                focusableWeakCandidateOverlap = Math.min(focusableSpanGroupIndex, prevSpanEnd) - Math.max(prevFocusedChild, prevSpanStart);
                            } else {
                                unfocusableWeakCandidate = candidate;
                                layoutDir = Math.min(focusableSpanGroupIndex, prevSpanEnd) - Math.max(prevFocusedChild, prevSpanStart);
                                focusableWeakCandidateSpanIndex = i3;
                                unfocusableWeakCandidateSpanIndex = candidateLp.mSpanIndex;
                                focusableWeakCandidateOverlap = i4;
                            }
                            i = i2 + inc;
                            ascend = ascend2;
                            start = i6;
                            prevFocusedChild = prevFocusedChild2;
                            focusableSpanGroupIndex = i5;
                            recycler2 = recycler;
                            state2 = state;
                        }
                    } else if (start == focusableWeakCandidateOverlap) {
                        if (prevFocusedChild > focusableWeakCandidateSpanIndex) {
                            i3 = focusableWeakCandidateSpanIndex;
                            z = true;
                        } else {
                            i3 = focusableWeakCandidateSpanIndex;
                            z = false;
                        }
                        if (preferLastSpan == z) {
                            assignAsWeek = true;
                        } else {
                            i4 = focusableWeakCandidateOverlap;
                        }
                    } else {
                        i3 = focusableWeakCandidateSpanIndex;
                        i4 = focusableWeakCandidateOverlap;
                    }
                    assignAsWeek = assignAsWeek2;
                    if (assignAsWeek) {
                    }
                } else {
                    assignAsWeek = true;
                    i3 = focusableWeakCandidateSpanIndex;
                }
                i4 = focusableWeakCandidateOverlap;
                if (assignAsWeek) {
                }
            } else if (focusableWeakCandidate != null) {
                break;
            } else {
                prevFocusedChild2 = prevFocusedChild;
                i3 = focusableWeakCandidateSpanIndex;
                i4 = focusableWeakCandidateOverlap;
                i5 = focusableSpanGroupIndex;
            }
            focusableWeakCandidateSpanIndex = i3;
            focusableWeakCandidateOverlap = i4;
            i = i2 + inc;
            ascend = ascend2;
            start = i6;
            prevFocusedChild = prevFocusedChild2;
            focusableSpanGroupIndex = i5;
            recycler2 = recycler;
            state2 = state;
        }
        return focusableWeakCandidate != null ? focusableWeakCandidate : unfocusableWeakCandidate;
    }

    public boolean supportsPredictiveItemAnimations() {
        return this.mPendingSavedState == null && !this.mPendingSpanCountChange;
    }
}
