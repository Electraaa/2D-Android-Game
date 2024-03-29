package android.support.v7.util;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView.Adapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DiffUtil {
    private static final Comparator<Snake> SNAKE_COMPARATOR = new Comparator<Snake>() {
        public int compare(Snake o1, Snake o2) {
            int cmpX = o1.x - o2.x;
            return cmpX == 0 ? o1.y - o2.y : cmpX;
        }
    };

    public static abstract class Callback {
        public abstract boolean areContentsTheSame(int i, int i2);

        public abstract boolean areItemsTheSame(int i, int i2);

        public abstract int getNewListSize();

        public abstract int getOldListSize();

        @Nullable
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return null;
        }
    }

    public static class DiffResult {
        private static final int FLAG_CHANGED = 2;
        private static final int FLAG_IGNORE = 16;
        private static final int FLAG_MASK = 31;
        private static final int FLAG_MOVED_CHANGED = 4;
        private static final int FLAG_MOVED_NOT_CHANGED = 8;
        private static final int FLAG_NOT_CHANGED = 1;
        private static final int FLAG_OFFSET = 5;
        private final Callback mCallback;
        private final boolean mDetectMoves;
        private final int[] mNewItemStatuses;
        private final int mNewListSize;
        private final int[] mOldItemStatuses;
        private final int mOldListSize;
        private final List<Snake> mSnakes;

        DiffResult(Callback callback, List<Snake> snakes, int[] oldItemStatuses, int[] newItemStatuses, boolean detectMoves) {
            this.mSnakes = snakes;
            this.mOldItemStatuses = oldItemStatuses;
            this.mNewItemStatuses = newItemStatuses;
            Arrays.fill(this.mOldItemStatuses, 0);
            Arrays.fill(this.mNewItemStatuses, 0);
            this.mCallback = callback;
            this.mOldListSize = callback.getOldListSize();
            this.mNewListSize = callback.getNewListSize();
            this.mDetectMoves = detectMoves;
            addRootSnake();
            findMatchingItems();
        }

        private void addRootSnake() {
            Snake firstSnake = this.mSnakes.isEmpty() ? null : (Snake) this.mSnakes.get(0);
            if (firstSnake == null || firstSnake.x != 0 || firstSnake.y != 0) {
                Snake root = new Snake();
                root.x = 0;
                root.y = 0;
                root.removal = false;
                root.size = 0;
                root.reverse = false;
                this.mSnakes.add(0, root);
            }
        }

        private void findMatchingItems() {
            int posOld = this.mOldListSize;
            int posNew = this.mNewListSize;
            for (int i = this.mSnakes.size() - 1; i >= 0; i--) {
                Snake snake = (Snake) this.mSnakes.get(i);
                int endX = snake.x + snake.size;
                int endY = snake.y + snake.size;
                if (this.mDetectMoves) {
                    while (posOld > endX) {
                        findAddition(posOld, posNew, i);
                        posOld--;
                    }
                    while (posNew > endY) {
                        findRemoval(posOld, posNew, i);
                        posNew--;
                    }
                }
                for (int j = 0; j < snake.size; j++) {
                    int oldItemPos = snake.x + j;
                    int newItemPos = snake.y + j;
                    int changeFlag = this.mCallback.areContentsTheSame(oldItemPos, newItemPos) ? 1 : 2;
                    this.mOldItemStatuses[oldItemPos] = (newItemPos << 5) | changeFlag;
                    this.mNewItemStatuses[newItemPos] = (oldItemPos << 5) | changeFlag;
                }
                posOld = snake.x;
                posNew = snake.y;
            }
        }

        private void findAddition(int x, int y, int snakeIndex) {
            if (this.mOldItemStatuses[x - 1] == 0) {
                findMatchingItem(x, y, snakeIndex, false);
            }
        }

        private void findRemoval(int x, int y, int snakeIndex) {
            if (this.mNewItemStatuses[y - 1] == 0) {
                findMatchingItem(x, y, snakeIndex, true);
            }
        }

        private boolean findMatchingItem(int x, int y, int snakeIndex, boolean removal) {
            int myItemPos;
            int curX;
            int curY;
            if (removal) {
                myItemPos = y - 1;
                curX = x;
                curY = y - 1;
            } else {
                myItemPos = x - 1;
                curX = x - 1;
                curY = y;
            }
            int curY2 = curY;
            curY = curX;
            for (curX = snakeIndex; curX >= 0; curX--) {
                Snake snake = (Snake) this.mSnakes.get(curX);
                int endX = snake.x + snake.size;
                int endY = snake.y + snake.size;
                int changeFlag = 4;
                int pos;
                if (removal) {
                    for (pos = curY - 1; pos >= endX; pos--) {
                        if (this.mCallback.areItemsTheSame(pos, myItemPos)) {
                            if (this.mCallback.areContentsTheSame(pos, myItemPos)) {
                                changeFlag = 8;
                            }
                            this.mNewItemStatuses[myItemPos] = (pos << 5) | 16;
                            this.mOldItemStatuses[pos] = (myItemPos << 5) | changeFlag;
                            return true;
                        }
                    }
                    continue;
                } else {
                    for (pos = curY2 - 1; pos >= endY; pos--) {
                        if (this.mCallback.areItemsTheSame(myItemPos, pos)) {
                            if (this.mCallback.areContentsTheSame(myItemPos, pos)) {
                                changeFlag = 8;
                            }
                            this.mOldItemStatuses[x - 1] = (pos << 5) | 16;
                            this.mNewItemStatuses[pos] = ((x - 1) << 5) | changeFlag;
                            return true;
                        }
                    }
                    continue;
                }
                curY = snake.x;
                curY2 = snake.y;
            }
            return false;
        }

        public void dispatchUpdatesTo(Adapter adapter) {
            dispatchUpdatesTo(new AdapterListUpdateCallback(adapter));
        }

        public void dispatchUpdatesTo(ListUpdateCallback updateCallback) {
            BatchingListUpdateCallback batchingCallback;
            ListUpdateCallback updateCallback2 = updateCallback;
            if (updateCallback2 instanceof BatchingListUpdateCallback) {
                batchingCallback = (BatchingListUpdateCallback) updateCallback2;
            } else {
                batchingCallback = new BatchingListUpdateCallback(updateCallback2);
                Object updateCallback22 = batchingCallback;
            }
            BatchingListUpdateCallback batchingCallback2 = batchingCallback;
            List<PostponedUpdate> postponedUpdates = new ArrayList();
            int posOld = this.mOldListSize;
            int snakeIndex = this.mSnakes.size() - 1;
            int posOld2 = posOld;
            int posNew = this.mNewListSize;
            while (true) {
                int snakeIndex2 = snakeIndex;
                if (snakeIndex2 >= 0) {
                    int endY;
                    Snake snake = (Snake) this.mSnakes.get(snakeIndex2);
                    int snakeSize = snake.size;
                    int endX = snake.x + snakeSize;
                    int endY2 = snake.y + snakeSize;
                    if (endX < posOld2) {
                        endY = endY2;
                        dispatchRemovals(postponedUpdates, batchingCallback2, endX, posOld2 - endX, endX);
                    } else {
                        endY = endY2;
                    }
                    if (endY < posNew) {
                        posOld = snakeSize;
                        dispatchAdditions(postponedUpdates, batchingCallback2, endX, posNew - endY, endY);
                    } else {
                        posOld = snakeSize;
                    }
                    snakeSize = posOld - 1;
                    while (true) {
                        int i = snakeSize;
                        if (i < 0) {
                            break;
                        }
                        if ((this.mOldItemStatuses[snake.x + i] & 31) == 2) {
                            batchingCallback2.onChanged(snake.x + i, 1, this.mCallback.getChangePayload(snake.x + i, snake.y + i));
                        }
                        snakeSize = i - 1;
                    }
                    posOld2 = snake.x;
                    posNew = snake.y;
                    snakeIndex = snakeIndex2 - 1;
                    endY = 1;
                } else {
                    batchingCallback2.dispatchLastEvent();
                    return;
                }
            }
        }

        private static PostponedUpdate removePostponedUpdate(List<PostponedUpdate> updates, int pos, boolean removal) {
            for (int i = updates.size() - 1; i >= 0; i--) {
                PostponedUpdate update = (PostponedUpdate) updates.get(i);
                if (update.posInOwnerList == pos && update.removal == removal) {
                    updates.remove(i);
                    for (int j = i; j < updates.size(); j++) {
                        PostponedUpdate postponedUpdate = (PostponedUpdate) updates.get(j);
                        postponedUpdate.currentPos += removal ? 1 : -1;
                    }
                    return update;
                }
            }
            return null;
        }

        private void dispatchAdditions(List<PostponedUpdate> postponedUpdates, ListUpdateCallback updateCallback, int start, int count, int globalIndex) {
            if (this.mDetectMoves) {
                for (int i = count - 1; i >= 0; i--) {
                    int status = this.mNewItemStatuses[globalIndex + i] & 31;
                    if (status == 0) {
                        updateCallback.onInserted(start, 1);
                        for (PostponedUpdate update : postponedUpdates) {
                            update.currentPos++;
                        }
                    } else if (status == 4 || status == 8) {
                        int pos = this.mNewItemStatuses[globalIndex + i] >> 5;
                        updateCallback.onMoved(removePostponedUpdate(postponedUpdates, pos, true).currentPos, start);
                        if (status == 4) {
                            updateCallback.onChanged(start, 1, this.mCallback.getChangePayload(pos, globalIndex + i));
                        }
                    } else if (status != 16) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unknown flag for pos ");
                        stringBuilder.append(globalIndex + i);
                        stringBuilder.append(" ");
                        stringBuilder.append(Long.toBinaryString((long) status));
                        throw new IllegalStateException(stringBuilder.toString());
                    } else {
                        postponedUpdates.add(new PostponedUpdate(globalIndex + i, start, false));
                    }
                }
                return;
            }
            updateCallback.onInserted(start, count);
        }

        private void dispatchRemovals(List<PostponedUpdate> postponedUpdates, ListUpdateCallback updateCallback, int start, int count, int globalIndex) {
            if (this.mDetectMoves) {
                for (int i = count - 1; i >= 0; i--) {
                    int status = this.mOldItemStatuses[globalIndex + i] & 31;
                    if (status == 0) {
                        updateCallback.onRemoved(start + i, 1);
                        for (PostponedUpdate update : postponedUpdates) {
                            update.currentPos--;
                        }
                    } else if (status == 4 || status == 8) {
                        int pos = this.mOldItemStatuses[globalIndex + i] >> 5;
                        PostponedUpdate update2 = removePostponedUpdate(postponedUpdates, pos, null);
                        updateCallback.onMoved(start + i, update2.currentPos - 1);
                        if (status == 4) {
                            updateCallback.onChanged(update2.currentPos - 1, 1, this.mCallback.getChangePayload(globalIndex + i, pos));
                        }
                    } else if (status != 16) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unknown flag for pos ");
                        stringBuilder.append(globalIndex + i);
                        stringBuilder.append(" ");
                        stringBuilder.append(Long.toBinaryString((long) status));
                        throw new IllegalStateException(stringBuilder.toString());
                    } else {
                        postponedUpdates.add(new PostponedUpdate(globalIndex + i, start + i, true));
                    }
                }
                return;
            }
            updateCallback.onRemoved(start, count);
        }

        /* Access modifiers changed, original: 0000 */
        @VisibleForTesting
        public List<Snake> getSnakes() {
            return this.mSnakes;
        }
    }

    public static abstract class ItemCallback<T> {
        public abstract boolean areContentsTheSame(T t, T t2);

        public abstract boolean areItemsTheSame(T t, T t2);

        public Object getChangePayload(T t, T t2) {
            return null;
        }
    }

    private static class PostponedUpdate {
        int currentPos;
        int posInOwnerList;
        boolean removal;

        public PostponedUpdate(int posInOwnerList, int currentPos, boolean removal) {
            this.posInOwnerList = posInOwnerList;
            this.currentPos = currentPos;
            this.removal = removal;
        }
    }

    static class Range {
        int newListEnd;
        int newListStart;
        int oldListEnd;
        int oldListStart;

        public Range(int oldListStart, int oldListEnd, int newListStart, int newListEnd) {
            this.oldListStart = oldListStart;
            this.oldListEnd = oldListEnd;
            this.newListStart = newListStart;
            this.newListEnd = newListEnd;
        }
    }

    static class Snake {
        boolean removal;
        boolean reverse;
        int size;
        int x;
        int y;

        Snake() {
        }
    }

    private DiffUtil() {
    }

    public static DiffResult calculateDiff(Callback cb) {
        return calculateDiff(cb, true);
    }

    public static DiffResult calculateDiff(Callback cb, boolean detectMoves) {
        int oldSize = cb.getOldListSize();
        int newSize = cb.getNewListSize();
        List<Snake> snakes = new ArrayList();
        ArrayList stack = new ArrayList();
        stack.add(new Range(0, oldSize, 0, newSize));
        int max = (oldSize + newSize) + Math.abs(oldSize - newSize);
        int[] forward = new int[(max * 2)];
        int[] iArr = new int[(max * 2)];
        List<Range> rangePool = new ArrayList();
        while (true) {
            List<Range> rangePool2 = rangePool;
            if (stack.isEmpty()) {
                Collections.sort(snakes, SNAKE_COMPARATOR);
                int[] backward = iArr;
                return new DiffResult(cb, snakes, forward, iArr, detectMoves);
            }
            Range range = (Range) stack.remove(stack.size() - 1);
            Snake snake = diffPartial(cb, range.oldListStart, range.oldListEnd, range.newListStart, range.newListEnd, forward, iArr, max);
            if (snake != null) {
                if (snake.size > 0) {
                    snakes.add(snake);
                }
                snake.x += range.oldListStart;
                snake.y += range.newListStart;
                Range left = rangePool2.isEmpty() ? new Range() : (Range) rangePool2.remove(rangePool2.size() - 1);
                left.oldListStart = range.oldListStart;
                left.newListStart = range.newListStart;
                if (snake.reverse) {
                    left.oldListEnd = snake.x;
                    left.newListEnd = snake.y;
                } else if (snake.removal) {
                    left.oldListEnd = snake.x - 1;
                    left.newListEnd = snake.y;
                } else {
                    left.oldListEnd = snake.x;
                    left.newListEnd = snake.y - 1;
                }
                stack.add(left);
                Range right = range;
                if (!snake.reverse) {
                    right.oldListStart = snake.x + snake.size;
                    right.newListStart = snake.y + snake.size;
                } else if (snake.removal) {
                    right.oldListStart = (snake.x + snake.size) + 1;
                    right.newListStart = snake.y + snake.size;
                } else {
                    right.oldListStart = snake.x + snake.size;
                    right.newListStart = (snake.y + snake.size) + 1;
                }
                stack.add(right);
            } else {
                rangePool2.add(range);
            }
            rangePool = rangePool2;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:86:0x008d A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x006d  */
    /* JADX WARNING: Missing block: B:17:0x0053, code skipped:
            if (r4[(r28 + r13) - 1] < r4[(r28 + r13) + 1]) goto L_0x0060;
     */
    /* JADX WARNING: Missing block: B:50:0x00f6, code skipped:
            if (r5[(r28 + r3) - 1] < r5[(r28 + r3) + 1]) goto L_0x0103;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Snake diffPartial(Callback cb, int startOld, int endOld, int startNew, int endNew, int[] forward, int[] backward, int kOffset) {
        Callback callback = cb;
        int[] iArr = forward;
        int[] iArr2 = backward;
        int oldSize = endOld - startOld;
        int newSize = endNew - startNew;
        int i;
        int i2;
        if (endOld - startOld < 1) {
            i = newSize;
        } else if (endNew - startNew < 1) {
            i2 = oldSize;
            i = newSize;
        } else {
            int delta = oldSize - newSize;
            int dLimit = ((oldSize + newSize) + 1) / 2;
            Arrays.fill(iArr, (kOffset - dLimit) - 1, (kOffset + dLimit) + 1, 0);
            Arrays.fill(iArr2, ((kOffset - dLimit) - 1) + delta, ((kOffset + dLimit) + 1) + delta, oldSize);
            boolean checkInFwd = delta % 2 != 0;
            int d = 0;
            while (d <= dLimit) {
                int i3;
                boolean z;
                int x;
                boolean removal;
                int i4;
                int k = -d;
                while (k <= d) {
                    int y;
                    int y2;
                    if (k != (-d)) {
                        if (k != d) {
                            i3 = 1;
                        } else {
                            z = true;
                        }
                        x = iArr[(kOffset + k) - z] + z;
                        removal = z;
                        y = x - k;
                        while (true) {
                            y2 = y;
                            if (x < oldSize) {
                                i2 = oldSize;
                                i = newSize;
                                i4 = y2;
                                break;
                            }
                            i4 = y2;
                            if (i4 >= newSize) {
                                i2 = oldSize;
                                i = newSize;
                                break;
                            }
                            i2 = oldSize;
                            i = newSize;
                            if (callback.areItemsTheSame(startOld + x, startNew + i4) == 0) {
                                break;
                            }
                            x++;
                            y = i4 + 1;
                            oldSize = i2;
                            newSize = i;
                        }
                        iArr[kOffset + k] = x;
                        if (checkInFwd || k < (delta - d) + 1 || k > (delta + d) - 1 || iArr[kOffset + k] < iArr2[kOffset + k]) {
                            k += 2;
                            oldSize = i2;
                            newSize = i;
                        } else {
                            oldSize = new Snake();
                            oldSize.x = iArr2[kOffset + k];
                            oldSize.y = oldSize.x - k;
                            oldSize.size = iArr[kOffset + k] - iArr2[kOffset + k];
                            oldSize.removal = removal;
                            oldSize.reverse = false;
                            return oldSize;
                        }
                    }
                    i3 = 1;
                    x = iArr[(kOffset + k) + i3];
                    removal = false;
                    y = x - k;
                    while (true) {
                        y2 = y;
                        if (x < oldSize) {
                        }
                        x++;
                        y = i4 + 1;
                        oldSize = i2;
                        newSize = i;
                    }
                    iArr[kOffset + k] = x;
                    if (checkInFwd) {
                    }
                    k += 2;
                    oldSize = i2;
                    newSize = i;
                }
                i2 = oldSize;
                i = newSize;
                x = -d;
                while (x <= d) {
                    i4 = x + delta;
                    if (i4 != d + delta) {
                        if (i4 != (-d) + delta) {
                            i3 = 1;
                        } else {
                            z = true;
                        }
                        oldSize = iArr2[(kOffset + i4) + z] - z;
                        removal = z;
                        k = oldSize - i4;
                        while (oldSize > 0 && k > 0 && callback.areItemsTheSame((startOld + oldSize) - 1, (startNew + k) - 1)) {
                            oldSize--;
                            k--;
                        }
                        iArr2[kOffset + i4] = oldSize;
                        if (!checkInFwd || x + delta < (-d) || x + delta > d || iArr[kOffset + i4] < iArr2[kOffset + i4]) {
                            x += 2;
                        } else {
                            Snake outSnake = new Snake();
                            outSnake.x = iArr2[kOffset + i4];
                            outSnake.y = outSnake.x - i4;
                            outSnake.size = iArr[kOffset + i4] - iArr2[kOffset + i4];
                            outSnake.removal = removal;
                            outSnake.reverse = true;
                            return outSnake;
                        }
                    }
                    i3 = 1;
                    oldSize = iArr2[(kOffset + i4) - i3];
                    removal = false;
                    k = oldSize - i4;
                    while (oldSize > 0) {
                        oldSize--;
                        k--;
                    }
                    iArr2[kOffset + i4] = oldSize;
                    if (checkInFwd) {
                    }
                    x += 2;
                }
                d++;
                int i5 = 1;
                oldSize = i2;
                newSize = i;
            }
            i = newSize;
            throw new IllegalStateException("DiffUtil hit an unexpected case while trying to calculate the optimal path. Please make sure your data is not changing during the diff calculation.");
        }
        return null;
    }
}
