package android.support.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.InflateException;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public abstract class Transition implements Cloneable {
    static final boolean DBG = false;
    private static final int[] DEFAULT_MATCH_ORDER = new int[]{2, 1, 3, 4};
    private static final String LOG_TAG = "Transition";
    private static final int MATCH_FIRST = 1;
    public static final int MATCH_ID = 3;
    private static final String MATCH_ID_STR = "id";
    public static final int MATCH_INSTANCE = 1;
    private static final String MATCH_INSTANCE_STR = "instance";
    public static final int MATCH_ITEM_ID = 4;
    private static final String MATCH_ITEM_ID_STR = "itemId";
    private static final int MATCH_LAST = 4;
    public static final int MATCH_NAME = 2;
    private static final String MATCH_NAME_STR = "name";
    private static final PathMotion STRAIGHT_PATH_MOTION = new PathMotion() {
        public Path getPath(float startX, float startY, float endX, float endY) {
            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(endX, endY);
            return path;
        }
    };
    private static ThreadLocal<ArrayMap<Animator, AnimationInfo>> sRunningAnimators = new ThreadLocal();
    private ArrayList<Animator> mAnimators = new ArrayList();
    boolean mCanRemoveViews = false;
    private ArrayList<Animator> mCurrentAnimators = new ArrayList();
    long mDuration = -1;
    private TransitionValuesMaps mEndValues = new TransitionValuesMaps();
    private ArrayList<TransitionValues> mEndValuesList;
    private boolean mEnded = false;
    private EpicenterCallback mEpicenterCallback;
    private TimeInterpolator mInterpolator = null;
    private ArrayList<TransitionListener> mListeners = null;
    private int[] mMatchOrder = DEFAULT_MATCH_ORDER;
    private String mName = getClass().getName();
    private ArrayMap<String, String> mNameOverrides;
    private int mNumInstances = 0;
    TransitionSet mParent = null;
    private PathMotion mPathMotion = STRAIGHT_PATH_MOTION;
    private boolean mPaused = false;
    TransitionPropagation mPropagation;
    private ViewGroup mSceneRoot = null;
    private long mStartDelay = -1;
    private TransitionValuesMaps mStartValues = new TransitionValuesMaps();
    private ArrayList<TransitionValues> mStartValuesList;
    private ArrayList<View> mTargetChildExcludes = null;
    private ArrayList<View> mTargetExcludes = null;
    private ArrayList<Integer> mTargetIdChildExcludes = null;
    private ArrayList<Integer> mTargetIdExcludes = null;
    ArrayList<Integer> mTargetIds = new ArrayList();
    private ArrayList<String> mTargetNameExcludes = null;
    private ArrayList<String> mTargetNames = null;
    private ArrayList<Class> mTargetTypeChildExcludes = null;
    private ArrayList<Class> mTargetTypeExcludes = null;
    private ArrayList<Class> mTargetTypes = null;
    ArrayList<View> mTargets = new ArrayList();

    private static class AnimationInfo {
        String mName;
        Transition mTransition;
        TransitionValues mValues;
        View mView;
        WindowIdImpl mWindowId;

        AnimationInfo(View view, String name, Transition transition, WindowIdImpl windowId, TransitionValues values) {
            this.mView = view;
            this.mName = name;
            this.mValues = values;
            this.mWindowId = windowId;
            this.mTransition = transition;
        }
    }

    private static class ArrayListManager {
        private ArrayListManager() {
        }

        static <T> ArrayList<T> add(ArrayList<T> list, T item) {
            if (list == null) {
                list = new ArrayList();
            }
            if (!list.contains(item)) {
                list.add(item);
            }
            return list;
        }

        static <T> ArrayList<T> remove(ArrayList<T> list, T item) {
            if (list == null) {
                return list;
            }
            list.remove(item);
            if (list.isEmpty()) {
                return null;
            }
            return list;
        }
    }

    public static abstract class EpicenterCallback {
        public abstract Rect onGetEpicenter(@NonNull Transition transition);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MatchOrder {
    }

    public interface TransitionListener {
        void onTransitionCancel(@NonNull Transition transition);

        void onTransitionEnd(@NonNull Transition transition);

        void onTransitionPause(@NonNull Transition transition);

        void onTransitionResume(@NonNull Transition transition);

        void onTransitionStart(@NonNull Transition transition);
    }

    public abstract void captureEndValues(@NonNull TransitionValues transitionValues);

    public abstract void captureStartValues(@NonNull TransitionValues transitionValues);

    public Transition(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, Styleable.TRANSITION);
        XmlResourceParser parser = (XmlResourceParser) attrs;
        long duration = (long) TypedArrayUtils.getNamedInt(a, parser, "duration", 1, -1);
        if (duration >= 0) {
            setDuration(duration);
        }
        long startDelay = (long) TypedArrayUtils.getNamedInt(a, parser, "startDelay", 2, -1);
        if (startDelay > 0) {
            setStartDelay(startDelay);
        }
        int resId = TypedArrayUtils.getNamedResourceId(a, parser, "interpolator", 0, 0);
        if (resId > 0) {
            setInterpolator(AnimationUtils.loadInterpolator(context, resId));
        }
        String matchOrder = TypedArrayUtils.getNamedString(a, parser, "matchOrder", 3);
        if (matchOrder != null) {
            setMatchOrder(parseMatchOrder(matchOrder));
        }
        a.recycle();
    }

    private static int[] parseMatchOrder(String matchOrderString) {
        StringTokenizer st = new StringTokenizer(matchOrderString, ",");
        int[] matches = new int[st.countTokens()];
        int index = 0;
        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            if (MATCH_ID_STR.equalsIgnoreCase(token)) {
                matches[index] = 3;
            } else if (MATCH_INSTANCE_STR.equalsIgnoreCase(token)) {
                matches[index] = 1;
            } else if (MATCH_NAME_STR.equalsIgnoreCase(token)) {
                matches[index] = 2;
            } else if (MATCH_ITEM_ID_STR.equalsIgnoreCase(token)) {
                matches[index] = 4;
            } else if (token.isEmpty()) {
                int[] smallerMatches = new int[(matches.length - 1)];
                System.arraycopy(matches, 0, smallerMatches, 0, index);
                matches = smallerMatches;
                index--;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown match type in matchOrder: '");
                stringBuilder.append(token);
                stringBuilder.append("'");
                throw new InflateException(stringBuilder.toString());
            }
            index++;
        }
        return matches;
    }

    @NonNull
    public Transition setDuration(long duration) {
        this.mDuration = duration;
        return this;
    }

    public long getDuration() {
        return this.mDuration;
    }

    @NonNull
    public Transition setStartDelay(long startDelay) {
        this.mStartDelay = startDelay;
        return this;
    }

    public long getStartDelay() {
        return this.mStartDelay;
    }

    @NonNull
    public Transition setInterpolator(@Nullable TimeInterpolator interpolator) {
        this.mInterpolator = interpolator;
        return this;
    }

    @Nullable
    public TimeInterpolator getInterpolator() {
        return this.mInterpolator;
    }

    @Nullable
    public String[] getTransitionProperties() {
        return null;
    }

    @Nullable
    public Animator createAnimator(@NonNull ViewGroup sceneRoot, @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        return null;
    }

    public void setMatchOrder(int... matches) {
        if (matches == null || matches.length == 0) {
            this.mMatchOrder = DEFAULT_MATCH_ORDER;
            return;
        }
        int i = 0;
        while (i < matches.length) {
            if (!isValidMatch(matches[i])) {
                throw new IllegalArgumentException("matches contains invalid value");
            } else if (alreadyContains(matches, i)) {
                throw new IllegalArgumentException("matches contains a duplicate value");
            } else {
                i++;
            }
        }
        this.mMatchOrder = (int[]) matches.clone();
    }

    private static boolean isValidMatch(int match) {
        return match >= 1 && match <= 4;
    }

    private static boolean alreadyContains(int[] array, int searchIndex) {
        int value = array[searchIndex];
        for (int i = 0; i < searchIndex; i++) {
            if (array[i] == value) {
                return true;
            }
        }
        return false;
    }

    private void matchInstances(ArrayMap<View, TransitionValues> unmatchedStart, ArrayMap<View, TransitionValues> unmatchedEnd) {
        for (int i = unmatchedStart.size() - 1; i >= 0; i--) {
            View view = (View) unmatchedStart.keyAt(i);
            if (view != null && isValidTarget(view)) {
                TransitionValues end = (TransitionValues) unmatchedEnd.remove(view);
                if (!(end == null || end.view == null || !isValidTarget(end.view))) {
                    this.mStartValuesList.add((TransitionValues) unmatchedStart.removeAt(i));
                    this.mEndValuesList.add(end);
                }
            }
        }
    }

    private void matchItemIds(ArrayMap<View, TransitionValues> unmatchedStart, ArrayMap<View, TransitionValues> unmatchedEnd, LongSparseArray<View> startItemIds, LongSparseArray<View> endItemIds) {
        int numStartIds = startItemIds.size();
        for (int i = 0; i < numStartIds; i++) {
            View startView = (View) startItemIds.valueAt(i);
            if (startView != null && isValidTarget(startView)) {
                View endView = (View) endItemIds.get(startItemIds.keyAt(i));
                if (endView != null && isValidTarget(endView)) {
                    TransitionValues startValues = (TransitionValues) unmatchedStart.get(startView);
                    TransitionValues endValues = (TransitionValues) unmatchedEnd.get(endView);
                    if (!(startValues == null || endValues == null)) {
                        this.mStartValuesList.add(startValues);
                        this.mEndValuesList.add(endValues);
                        unmatchedStart.remove(startView);
                        unmatchedEnd.remove(endView);
                    }
                }
            }
        }
    }

    private void matchIds(ArrayMap<View, TransitionValues> unmatchedStart, ArrayMap<View, TransitionValues> unmatchedEnd, SparseArray<View> startIds, SparseArray<View> endIds) {
        int numStartIds = startIds.size();
        for (int i = 0; i < numStartIds; i++) {
            View startView = (View) startIds.valueAt(i);
            if (startView != null && isValidTarget(startView)) {
                View endView = (View) endIds.get(startIds.keyAt(i));
                if (endView != null && isValidTarget(endView)) {
                    TransitionValues startValues = (TransitionValues) unmatchedStart.get(startView);
                    TransitionValues endValues = (TransitionValues) unmatchedEnd.get(endView);
                    if (!(startValues == null || endValues == null)) {
                        this.mStartValuesList.add(startValues);
                        this.mEndValuesList.add(endValues);
                        unmatchedStart.remove(startView);
                        unmatchedEnd.remove(endView);
                    }
                }
            }
        }
    }

    private void matchNames(ArrayMap<View, TransitionValues> unmatchedStart, ArrayMap<View, TransitionValues> unmatchedEnd, ArrayMap<String, View> startNames, ArrayMap<String, View> endNames) {
        int numStartNames = startNames.size();
        for (int i = 0; i < numStartNames; i++) {
            View startView = (View) startNames.valueAt(i);
            if (startView != null && isValidTarget(startView)) {
                View endView = (View) endNames.get(startNames.keyAt(i));
                if (endView != null && isValidTarget(endView)) {
                    TransitionValues startValues = (TransitionValues) unmatchedStart.get(startView);
                    TransitionValues endValues = (TransitionValues) unmatchedEnd.get(endView);
                    if (!(startValues == null || endValues == null)) {
                        this.mStartValuesList.add(startValues);
                        this.mEndValuesList.add(endValues);
                        unmatchedStart.remove(startView);
                        unmatchedEnd.remove(endView);
                    }
                }
            }
        }
    }

    private void addUnmatched(ArrayMap<View, TransitionValues> unmatchedStart, ArrayMap<View, TransitionValues> unmatchedEnd) {
        int i = 0;
        for (int i2 = 0; i2 < unmatchedStart.size(); i2++) {
            TransitionValues start = (TransitionValues) unmatchedStart.valueAt(i2);
            if (isValidTarget(start.view)) {
                this.mStartValuesList.add(start);
                this.mEndValuesList.add(null);
            }
        }
        while (i < unmatchedEnd.size()) {
            TransitionValues end = (TransitionValues) unmatchedEnd.valueAt(i);
            if (isValidTarget(end.view)) {
                this.mEndValuesList.add(end);
                this.mStartValuesList.add(null);
            }
            i++;
        }
    }

    private void matchStartAndEnd(TransitionValuesMaps startValues, TransitionValuesMaps endValues) {
        ArrayMap<View, TransitionValues> unmatchedStart = new ArrayMap(startValues.mViewValues);
        ArrayMap<View, TransitionValues> unmatchedEnd = new ArrayMap(endValues.mViewValues);
        for (int i : this.mMatchOrder) {
            switch (i) {
                case 1:
                    matchInstances(unmatchedStart, unmatchedEnd);
                    break;
                case 2:
                    matchNames(unmatchedStart, unmatchedEnd, startValues.mNameValues, endValues.mNameValues);
                    break;
                case 3:
                    matchIds(unmatchedStart, unmatchedEnd, startValues.mIdValues, endValues.mIdValues);
                    break;
                case 4:
                    matchItemIds(unmatchedStart, unmatchedEnd, startValues.mItemIdValues, endValues.mItemIdValues);
                    break;
                default:
                    break;
            }
        }
        addUnmatched(unmatchedStart, unmatchedEnd);
    }

    /* Access modifiers changed, original: protected */
    @RestrictTo({Scope.LIBRARY_GROUP})
    public void createAnimators(ViewGroup sceneRoot, TransitionValuesMaps startValues, TransitionValuesMaps endValues, ArrayList<TransitionValues> startValuesList, ArrayList<TransitionValues> endValuesList) {
        ViewGroup viewGroup = sceneRoot;
        ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
        SparseIntArray startDelays = new SparseIntArray();
        int startValuesListCount = startValuesList.size();
        long minStartDelay = Long.MAX_VALUE;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= startValuesListCount) {
                break;
            }
            int startValuesListCount2;
            int i3;
            TransitionValues start = (TransitionValues) startValuesList.get(i2);
            TransitionValues end = (TransitionValues) endValuesList.get(i2);
            if (!(start == null || start.mTargetedTransitions.contains(this))) {
                start = null;
            }
            TransitionValues start2 = start;
            if (!(end == null || end.mTargetedTransitions.contains(this))) {
                end = null;
            }
            TransitionValues end2 = end;
            if (start2 == null && end2 == null) {
                startValuesListCount2 = startValuesListCount;
                i3 = i2;
            } else {
                boolean z = start2 == null || end2 == null || isTransitionRequired(start2, end2);
                if (z) {
                    Animator animator = createAnimator(viewGroup, start2, end2);
                    if (animator != null) {
                        TransitionValues infoValues;
                        View view;
                        end = null;
                        if (end2 != null) {
                            Animator animator2;
                            View view2 = end2.view;
                            String[] properties = getTransitionProperties();
                            if (view2 == null || properties == null) {
                                animator2 = animator;
                                startValuesListCount2 = startValuesListCount;
                                i3 = i2;
                            } else {
                                animator2 = animator;
                                if (properties.length > null) {
                                    TransitionValues newValues;
                                    end = new TransitionValues();
                                    end.view = view2;
                                    startValuesListCount2 = startValuesListCount;
                                    startValuesListCount = (TransitionValues) endValues.mViewValues.get(view2);
                                    if (startValuesListCount != null) {
                                        int j = 0;
                                        while (true) {
                                            int j2 = j;
                                            if (j2 >= properties.length) {
                                                break;
                                            }
                                            i3 = i2;
                                            newValues = startValuesListCount;
                                            end.values.put(properties[j2], startValuesListCount.values.get(properties[j2]));
                                            j = j2 + 1;
                                            i2 = i3;
                                            startValuesListCount = newValues;
                                            TransitionValuesMaps animator3 = endValues;
                                            ArrayList<TransitionValues> arrayList = startValuesList;
                                            ArrayList<TransitionValues> arrayList2 = endValuesList;
                                        }
                                    }
                                    newValues = startValuesListCount;
                                    i3 = i2;
                                    animator = runningAnimators.size();
                                    startValuesListCount = 0;
                                    while (startValuesListCount < animator) {
                                        int numExistingAnims;
                                        AnimationInfo info = (AnimationInfo) runningAnimators.get((Animator) runningAnimators.keyAt(startValuesListCount));
                                        if (info.mValues != null && info.mView == view2) {
                                            numExistingAnims = animator;
                                            if (!(info.mName.equals(getName()) == null || info.mValues.equals(end) == null)) {
                                                animator = null;
                                                break;
                                            }
                                        }
                                        numExistingAnims = animator;
                                        startValuesListCount++;
                                        animator = numExistingAnims;
                                    }
                                } else {
                                    startValuesListCount2 = startValuesListCount;
                                    i3 = i2;
                                }
                            }
                            animator = animator2;
                            startValuesListCount = animator;
                            infoValues = end;
                            view = view2;
                        } else {
                            startValuesListCount2 = startValuesListCount;
                            i3 = i2;
                            infoValues = null;
                            startValuesListCount = animator;
                            view = start2.view;
                        }
                        if (startValuesListCount != 0) {
                            if (this.mPropagation != null) {
                                long delay = this.mPropagation.getStartDelay(viewGroup, this, start2, end2);
                                startDelays.put(this.mAnimators.size(), (int) delay);
                                minStartDelay = Math.min(delay, minStartDelay);
                            }
                            i2 = minStartDelay;
                            runningAnimators.put(startValuesListCount, new AnimationInfo(view, getName(), this, ViewUtils.getWindowId(sceneRoot), infoValues));
                            this.mAnimators.add(startValuesListCount);
                            minStartDelay = i2;
                        }
                    }
                }
                startValuesListCount2 = startValuesListCount;
                i3 = i2;
            }
            i = i3 + 1;
            startValuesListCount = startValuesListCount2;
        }
        if (minStartDelay != 0) {
            int i4 = 0;
            while (true) {
                i = i4;
                if (i < startDelays.size()) {
                    Animator animator4 = (Animator) this.mAnimators.get(startDelays.keyAt(i));
                    animator4.setStartDelay((((long) startDelays.valueAt(i)) - minStartDelay) + animator4.getStartDelay());
                    i4 = i + 1;
                } else {
                    return;
                }
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    /* JADX WARNING: Missing block: B:37:0x0082, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isValidTarget(View target) {
        int targetId = target.getId();
        if (this.mTargetIdExcludes != null && this.mTargetIdExcludes.contains(Integer.valueOf(targetId))) {
            return false;
        }
        if (this.mTargetExcludes != null && this.mTargetExcludes.contains(target)) {
            return false;
        }
        int numTypes;
        if (this.mTargetTypeExcludes != null) {
            numTypes = this.mTargetTypeExcludes.size();
            for (int i = 0; i < numTypes; i++) {
                if (((Class) this.mTargetTypeExcludes.get(i)).isInstance(target)) {
                    return false;
                }
            }
        }
        if (this.mTargetNameExcludes != null && ViewCompat.getTransitionName(target) != null && this.mTargetNameExcludes.contains(ViewCompat.getTransitionName(target))) {
            return false;
        }
        if ((this.mTargetIds.size() == 0 && this.mTargets.size() == 0 && ((this.mTargetTypes == null || this.mTargetTypes.isEmpty()) && (this.mTargetNames == null || this.mTargetNames.isEmpty()))) || this.mTargetIds.contains(Integer.valueOf(targetId)) || this.mTargets.contains(target)) {
            return true;
        }
        if (this.mTargetNames != null && this.mTargetNames.contains(ViewCompat.getTransitionName(target))) {
            return true;
        }
        if (this.mTargetTypes != null) {
            for (numTypes = 0; numTypes < this.mTargetTypes.size(); numTypes++) {
                if (((Class) this.mTargetTypes.get(numTypes)).isInstance(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ArrayMap<Animator, AnimationInfo> getRunningAnimators() {
        ArrayMap<Animator, AnimationInfo> runningAnimators = (ArrayMap) sRunningAnimators.get();
        if (runningAnimators != null) {
            return runningAnimators;
        }
        runningAnimators = new ArrayMap();
        sRunningAnimators.set(runningAnimators);
        return runningAnimators;
    }

    /* Access modifiers changed, original: protected */
    @RestrictTo({Scope.LIBRARY_GROUP})
    public void runAnimators() {
        start();
        ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
        Iterator it = this.mAnimators.iterator();
        while (it.hasNext()) {
            Animator anim = (Animator) it.next();
            if (runningAnimators.containsKey(anim)) {
                start();
                runAnimator(anim, runningAnimators);
            }
        }
        this.mAnimators.clear();
        end();
    }

    private void runAnimator(Animator animator, final ArrayMap<Animator, AnimationInfo> runningAnimators) {
        if (animator != null) {
            animator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    Transition.this.mCurrentAnimators.add(animation);
                }

                public void onAnimationEnd(Animator animation) {
                    runningAnimators.remove(animation);
                    Transition.this.mCurrentAnimators.remove(animation);
                }
            });
            animate(animator);
        }
    }

    @NonNull
    public Transition addTarget(@NonNull View target) {
        this.mTargets.add(target);
        return this;
    }

    @NonNull
    public Transition addTarget(@IdRes int targetId) {
        if (targetId != 0) {
            this.mTargetIds.add(Integer.valueOf(targetId));
        }
        return this;
    }

    @NonNull
    public Transition addTarget(@NonNull String targetName) {
        if (this.mTargetNames == null) {
            this.mTargetNames = new ArrayList();
        }
        this.mTargetNames.add(targetName);
        return this;
    }

    @NonNull
    public Transition addTarget(@NonNull Class targetType) {
        if (this.mTargetTypes == null) {
            this.mTargetTypes = new ArrayList();
        }
        this.mTargetTypes.add(targetType);
        return this;
    }

    @NonNull
    public Transition removeTarget(@NonNull View target) {
        this.mTargets.remove(target);
        return this;
    }

    @NonNull
    public Transition removeTarget(@IdRes int targetId) {
        if (targetId != 0) {
            this.mTargetIds.remove(Integer.valueOf(targetId));
        }
        return this;
    }

    @NonNull
    public Transition removeTarget(@NonNull String targetName) {
        if (this.mTargetNames != null) {
            this.mTargetNames.remove(targetName);
        }
        return this;
    }

    @NonNull
    public Transition removeTarget(@NonNull Class target) {
        if (this.mTargetTypes != null) {
            this.mTargetTypes.remove(target);
        }
        return this;
    }

    private static <T> ArrayList<T> excludeObject(ArrayList<T> list, T target, boolean exclude) {
        if (target == null) {
            return list;
        }
        if (exclude) {
            return ArrayListManager.add(list, target);
        }
        return ArrayListManager.remove(list, target);
    }

    @NonNull
    public Transition excludeTarget(@NonNull View target, boolean exclude) {
        this.mTargetExcludes = excludeView(this.mTargetExcludes, target, exclude);
        return this;
    }

    @NonNull
    public Transition excludeTarget(@IdRes int targetId, boolean exclude) {
        this.mTargetIdExcludes = excludeId(this.mTargetIdExcludes, targetId, exclude);
        return this;
    }

    @NonNull
    public Transition excludeTarget(@NonNull String targetName, boolean exclude) {
        this.mTargetNameExcludes = excludeObject(this.mTargetNameExcludes, targetName, exclude);
        return this;
    }

    @NonNull
    public Transition excludeChildren(@NonNull View target, boolean exclude) {
        this.mTargetChildExcludes = excludeView(this.mTargetChildExcludes, target, exclude);
        return this;
    }

    @NonNull
    public Transition excludeChildren(@IdRes int targetId, boolean exclude) {
        this.mTargetIdChildExcludes = excludeId(this.mTargetIdChildExcludes, targetId, exclude);
        return this;
    }

    private ArrayList<Integer> excludeId(ArrayList<Integer> list, int targetId, boolean exclude) {
        if (targetId <= 0) {
            return list;
        }
        if (exclude) {
            return ArrayListManager.add(list, Integer.valueOf(targetId));
        }
        return ArrayListManager.remove(list, Integer.valueOf(targetId));
    }

    private ArrayList<View> excludeView(ArrayList<View> list, View target, boolean exclude) {
        if (target == null) {
            return list;
        }
        if (exclude) {
            return ArrayListManager.add(list, target);
        }
        return ArrayListManager.remove(list, target);
    }

    @NonNull
    public Transition excludeTarget(@NonNull Class type, boolean exclude) {
        this.mTargetTypeExcludes = excludeType(this.mTargetTypeExcludes, type, exclude);
        return this;
    }

    @NonNull
    public Transition excludeChildren(@NonNull Class type, boolean exclude) {
        this.mTargetTypeChildExcludes = excludeType(this.mTargetTypeChildExcludes, type, exclude);
        return this;
    }

    private ArrayList<Class> excludeType(ArrayList<Class> list, Class type, boolean exclude) {
        if (type == null) {
            return list;
        }
        if (exclude) {
            return ArrayListManager.add(list, type);
        }
        return ArrayListManager.remove(list, type);
    }

    @NonNull
    public List<Integer> getTargetIds() {
        return this.mTargetIds;
    }

    @NonNull
    public List<View> getTargets() {
        return this.mTargets;
    }

    @Nullable
    public List<String> getTargetNames() {
        return this.mTargetNames;
    }

    @Nullable
    public List<Class> getTargetTypes() {
        return this.mTargetTypes;
    }

    /* Access modifiers changed, original: 0000 */
    public void captureValues(ViewGroup sceneRoot, boolean start) {
        int i;
        View view;
        clearValues(start);
        int i2 = 0;
        if ((this.mTargetIds.size() > 0 || this.mTargets.size() > 0) && ((this.mTargetNames == null || this.mTargetNames.isEmpty()) && (this.mTargetTypes == null || this.mTargetTypes.isEmpty()))) {
            for (i = 0; i < this.mTargetIds.size(); i++) {
                view = sceneRoot.findViewById(((Integer) this.mTargetIds.get(i)).intValue());
                if (view != null) {
                    TransitionValues values = new TransitionValues();
                    values.view = view;
                    if (start) {
                        captureStartValues(values);
                    } else {
                        captureEndValues(values);
                    }
                    values.mTargetedTransitions.add(this);
                    capturePropagationValues(values);
                    if (start) {
                        addViewValues(this.mStartValues, view, values);
                    } else {
                        addViewValues(this.mEndValues, view, values);
                    }
                }
            }
            for (i = 0; i < this.mTargets.size(); i++) {
                View view2 = (View) this.mTargets.get(i);
                TransitionValues values2 = new TransitionValues();
                values2.view = view2;
                if (start) {
                    captureStartValues(values2);
                } else {
                    captureEndValues(values2);
                }
                values2.mTargetedTransitions.add(this);
                capturePropagationValues(values2);
                if (start) {
                    addViewValues(this.mStartValues, view2, values2);
                } else {
                    addViewValues(this.mEndValues, view2, values2);
                }
            }
        } else {
            captureHierarchy(sceneRoot, start);
        }
        if (!start && this.mNameOverrides != null) {
            i = this.mNameOverrides.size();
            ArrayList<View> overriddenViews = new ArrayList(i);
            for (int i3 = 0; i3 < i; i3++) {
                overriddenViews.add(this.mStartValues.mNameValues.remove((String) this.mNameOverrides.keyAt(i3)));
            }
            while (i2 < i) {
                view = (View) overriddenViews.get(i2);
                if (view != null) {
                    this.mStartValues.mNameValues.put((String) this.mNameOverrides.valueAt(i2), view);
                }
                i2++;
            }
        }
    }

    private static void addViewValues(TransitionValuesMaps transitionValuesMaps, View view, TransitionValues transitionValues) {
        transitionValuesMaps.mViewValues.put(view, transitionValues);
        int id = view.getId();
        if (id >= 0) {
            if (transitionValuesMaps.mIdValues.indexOfKey(id) >= 0) {
                transitionValuesMaps.mIdValues.put(id, null);
            } else {
                transitionValuesMaps.mIdValues.put(id, view);
            }
        }
        String name = ViewCompat.getTransitionName(view);
        if (name != null) {
            if (transitionValuesMaps.mNameValues.containsKey(name)) {
                transitionValuesMaps.mNameValues.put(name, null);
            } else {
                transitionValuesMaps.mNameValues.put(name, view);
            }
        }
        if (view.getParent() instanceof ListView) {
            ListView listview = (ListView) view.getParent();
            if (listview.getAdapter().hasStableIds()) {
                long itemId = listview.getItemIdAtPosition(listview.getPositionForView(view));
                if (transitionValuesMaps.mItemIdValues.indexOfKey(itemId) >= 0) {
                    View alreadyMatched = (View) transitionValuesMaps.mItemIdValues.get(itemId);
                    if (alreadyMatched != null) {
                        ViewCompat.setHasTransientState(alreadyMatched, false);
                        transitionValuesMaps.mItemIdValues.put(itemId, null);
                        return;
                    }
                    return;
                }
                ViewCompat.setHasTransientState(view, true);
                transitionValuesMaps.mItemIdValues.put(itemId, view);
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void clearValues(boolean start) {
        if (start) {
            this.mStartValues.mViewValues.clear();
            this.mStartValues.mIdValues.clear();
            this.mStartValues.mItemIdValues.clear();
            return;
        }
        this.mEndValues.mViewValues.clear();
        this.mEndValues.mIdValues.clear();
        this.mEndValues.mItemIdValues.clear();
    }

    private void captureHierarchy(View view, boolean start) {
        if (view != null) {
            int id = view.getId();
            if (this.mTargetIdExcludes != null && this.mTargetIdExcludes.contains(Integer.valueOf(id))) {
                return;
            }
            if (this.mTargetExcludes == null || !this.mTargetExcludes.contains(view)) {
                int numTypes;
                int i;
                int i2 = 0;
                if (this.mTargetTypeExcludes != null) {
                    numTypes = this.mTargetTypeExcludes.size();
                    i = 0;
                    while (i < numTypes) {
                        if (!((Class) this.mTargetTypeExcludes.get(i)).isInstance(view)) {
                            i++;
                        } else {
                            return;
                        }
                    }
                }
                if (view.getParent() instanceof ViewGroup) {
                    TransitionValues values = new TransitionValues();
                    values.view = view;
                    if (start) {
                        captureStartValues(values);
                    } else {
                        captureEndValues(values);
                    }
                    values.mTargetedTransitions.add(this);
                    capturePropagationValues(values);
                    if (start) {
                        addViewValues(this.mStartValues, view, values);
                    } else {
                        addViewValues(this.mEndValues, view, values);
                    }
                }
                if ((view instanceof ViewGroup) && (this.mTargetIdChildExcludes == null || !this.mTargetIdChildExcludes.contains(Integer.valueOf(id)))) {
                    if (this.mTargetChildExcludes == null || !this.mTargetChildExcludes.contains(view)) {
                        if (this.mTargetTypeChildExcludes != null) {
                            numTypes = this.mTargetTypeChildExcludes.size();
                            i = 0;
                            while (i < numTypes) {
                                if (!((Class) this.mTargetTypeChildExcludes.get(i)).isInstance(view)) {
                                    i++;
                                } else {
                                    return;
                                }
                            }
                        }
                        ViewGroup parent = (ViewGroup) view;
                        while (i2 < parent.getChildCount()) {
                            captureHierarchy(parent.getChildAt(i2), start);
                            i2++;
                        }
                    }
                }
            }
        }
    }

    @Nullable
    public TransitionValues getTransitionValues(@NonNull View view, boolean start) {
        if (this.mParent != null) {
            return this.mParent.getTransitionValues(view, start);
        }
        return (TransitionValues) (start ? this.mStartValues : this.mEndValues).mViewValues.get(view);
    }

    /* Access modifiers changed, original: 0000 */
    public TransitionValues getMatchedTransitionValues(View view, boolean viewInStart) {
        if (this.mParent != null) {
            return this.mParent.getMatchedTransitionValues(view, viewInStart);
        }
        ArrayList<TransitionValues> lookIn = viewInStart ? this.mStartValuesList : this.mEndValuesList;
        if (lookIn == null) {
            return null;
        }
        int count = lookIn.size();
        int index = -1;
        for (int i = 0; i < count; i++) {
            TransitionValues values = (TransitionValues) lookIn.get(i);
            if (values == null) {
                return null;
            }
            if (values.view == view) {
                index = i;
                break;
            }
        }
        TransitionValues values2 = null;
        if (index >= 0) {
            values2 = (TransitionValues) (viewInStart ? this.mEndValuesList : this.mStartValuesList).get(index);
        }
        return values2;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void pause(View sceneRoot) {
        if (!this.mEnded) {
            ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
            int numOldAnims = runningAnimators.size();
            WindowIdImpl windowId = ViewUtils.getWindowId(sceneRoot);
            for (int i = numOldAnims - 1; i >= 0; i--) {
                AnimationInfo info = (AnimationInfo) runningAnimators.valueAt(i);
                if (info.mView != null && windowId.equals(info.mWindowId)) {
                    AnimatorUtils.pause((Animator) runningAnimators.keyAt(i));
                }
            }
            if (this.mListeners != null && this.mListeners.size() > 0) {
                ArrayList<TransitionListener> tmpListeners = (ArrayList) this.mListeners.clone();
                int numListeners = tmpListeners.size();
                for (int i2 = 0; i2 < numListeners; i2++) {
                    ((TransitionListener) tmpListeners.get(i2)).onTransitionPause(this);
                }
            }
            this.mPaused = true;
        }
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void resume(View sceneRoot) {
        if (this.mPaused) {
            if (!this.mEnded) {
                ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
                int numOldAnims = runningAnimators.size();
                WindowIdImpl windowId = ViewUtils.getWindowId(sceneRoot);
                for (int i = numOldAnims - 1; i >= 0; i--) {
                    AnimationInfo info = (AnimationInfo) runningAnimators.valueAt(i);
                    if (info.mView != null && windowId.equals(info.mWindowId)) {
                        AnimatorUtils.resume((Animator) runningAnimators.keyAt(i));
                    }
                }
                if (this.mListeners != null && this.mListeners.size() > 0) {
                    ArrayList<TransitionListener> tmpListeners = (ArrayList) this.mListeners.clone();
                    int numListeners = tmpListeners.size();
                    for (int i2 = 0; i2 < numListeners; i2++) {
                        ((TransitionListener) tmpListeners.get(i2)).onTransitionResume(this);
                    }
                }
            }
            this.mPaused = false;
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void playTransition(ViewGroup sceneRoot) {
        this.mStartValuesList = new ArrayList();
        this.mEndValuesList = new ArrayList();
        matchStartAndEnd(this.mStartValues, this.mEndValues);
        ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
        int numOldAnims = runningAnimators.size();
        WindowIdImpl windowId = ViewUtils.getWindowId(sceneRoot);
        for (int i = numOldAnims - 1; i >= 0; i--) {
            Animator anim = (Animator) runningAnimators.keyAt(i);
            if (anim != null) {
                AnimationInfo oldInfo = (AnimationInfo) runningAnimators.get(anim);
                if (!(oldInfo == null || oldInfo.mView == null || !windowId.equals(oldInfo.mWindowId))) {
                    TransitionValues oldValues = oldInfo.mValues;
                    View oldView = oldInfo.mView;
                    boolean cancel = true;
                    TransitionValues startValues = getTransitionValues(oldView, true);
                    TransitionValues endValues = getMatchedTransitionValues(oldView, true);
                    if ((startValues == null && endValues == null) || !oldInfo.mTransition.isTransitionRequired(oldValues, endValues)) {
                        cancel = false;
                    }
                    if (cancel) {
                        if (anim.isRunning() || anim.isStarted()) {
                            anim.cancel();
                        } else {
                            runningAnimators.remove(anim);
                        }
                    }
                }
            }
        }
        createAnimators(sceneRoot, this.mStartValues, this.mEndValues, this.mStartValuesList, this.mEndValuesList);
        runAnimators();
    }

    public boolean isTransitionRequired(@Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return false;
        }
        String[] properties = getTransitionProperties();
        if (properties != null) {
            for (String property : properties) {
                if (isValueChanged(startValues, endValues, property)) {
                    return true;
                }
            }
            return false;
        }
        for (String key : startValues.values.keySet()) {
            if (isValueChanged(startValues, endValues, key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValueChanged(TransitionValues oldValues, TransitionValues newValues, String key) {
        Object oldValue = oldValues.values.get(key);
        Object newValue = newValues.values.get(key);
        if (oldValue == null && newValue == null) {
            return false;
        }
        if (oldValue == null || newValue == null) {
            return true;
        }
        return oldValue.equals(newValue) ^ 1;
    }

    /* Access modifiers changed, original: protected */
    @RestrictTo({Scope.LIBRARY_GROUP})
    public void animate(Animator animator) {
        if (animator == null) {
            end();
            return;
        }
        if (getDuration() >= 0) {
            animator.setDuration(getDuration());
        }
        if (getStartDelay() >= 0) {
            animator.setStartDelay(getStartDelay());
        }
        if (getInterpolator() != null) {
            animator.setInterpolator(getInterpolator());
        }
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                Transition.this.end();
                animation.removeListener(this);
            }
        });
        animator.start();
    }

    /* Access modifiers changed, original: protected */
    @RestrictTo({Scope.LIBRARY_GROUP})
    public void start() {
        if (this.mNumInstances == 0) {
            if (this.mListeners != null && this.mListeners.size() > 0) {
                ArrayList<TransitionListener> tmpListeners = (ArrayList) this.mListeners.clone();
                int numListeners = tmpListeners.size();
                for (int i = 0; i < numListeners; i++) {
                    ((TransitionListener) tmpListeners.get(i)).onTransitionStart(this);
                }
            }
            this.mEnded = false;
        }
        this.mNumInstances++;
    }

    /* Access modifiers changed, original: protected */
    @RestrictTo({Scope.LIBRARY_GROUP})
    public void end() {
        this.mNumInstances--;
        if (this.mNumInstances == 0) {
            int i;
            View view;
            if (this.mListeners != null && this.mListeners.size() > 0) {
                ArrayList<TransitionListener> tmpListeners = (ArrayList) this.mListeners.clone();
                int numListeners = tmpListeners.size();
                for (int i2 = 0; i2 < numListeners; i2++) {
                    ((TransitionListener) tmpListeners.get(i2)).onTransitionEnd(this);
                }
            }
            for (i = 0; i < this.mStartValues.mItemIdValues.size(); i++) {
                view = (View) this.mStartValues.mItemIdValues.valueAt(i);
                if (view != null) {
                    ViewCompat.setHasTransientState(view, false);
                }
            }
            for (i = 0; i < this.mEndValues.mItemIdValues.size(); i++) {
                view = (View) this.mEndValues.mItemIdValues.valueAt(i);
                if (view != null) {
                    ViewCompat.setHasTransientState(view, false);
                }
            }
            this.mEnded = true;
        }
    }

    /* Access modifiers changed, original: 0000 */
    @RestrictTo({Scope.LIBRARY_GROUP})
    public void forceToEnd(ViewGroup sceneRoot) {
        ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
        int numOldAnims = runningAnimators.size();
        if (sceneRoot != null) {
            WindowIdImpl windowId = ViewUtils.getWindowId(sceneRoot);
            for (int i = numOldAnims - 1; i >= 0; i--) {
                AnimationInfo info = (AnimationInfo) runningAnimators.valueAt(i);
                if (!(info.mView == null || windowId == null || !windowId.equals(info.mWindowId))) {
                    ((Animator) runningAnimators.keyAt(i)).end();
                }
            }
        }
    }

    /* Access modifiers changed, original: protected */
    @RestrictTo({Scope.LIBRARY_GROUP})
    public void cancel() {
        for (int i = this.mCurrentAnimators.size() - 1; i >= 0; i--) {
            ((Animator) this.mCurrentAnimators.get(i)).cancel();
        }
        if (this.mListeners != null && this.mListeners.size() > 0) {
            ArrayList<TransitionListener> tmpListeners = (ArrayList) this.mListeners.clone();
            int numListeners = tmpListeners.size();
            for (int i2 = 0; i2 < numListeners; i2++) {
                ((TransitionListener) tmpListeners.get(i2)).onTransitionCancel(this);
            }
        }
    }

    @NonNull
    public Transition addListener(@NonNull TransitionListener listener) {
        if (this.mListeners == null) {
            this.mListeners = new ArrayList();
        }
        this.mListeners.add(listener);
        return this;
    }

    @NonNull
    public Transition removeListener(@NonNull TransitionListener listener) {
        if (this.mListeners == null) {
            return this;
        }
        this.mListeners.remove(listener);
        if (this.mListeners.size() == 0) {
            this.mListeners = null;
        }
        return this;
    }

    public void setPathMotion(@Nullable PathMotion pathMotion) {
        if (pathMotion == null) {
            this.mPathMotion = STRAIGHT_PATH_MOTION;
        } else {
            this.mPathMotion = pathMotion;
        }
    }

    @NonNull
    public PathMotion getPathMotion() {
        return this.mPathMotion;
    }

    public void setEpicenterCallback(@Nullable EpicenterCallback epicenterCallback) {
        this.mEpicenterCallback = epicenterCallback;
    }

    @Nullable
    public EpicenterCallback getEpicenterCallback() {
        return this.mEpicenterCallback;
    }

    @Nullable
    public Rect getEpicenter() {
        if (this.mEpicenterCallback == null) {
            return null;
        }
        return this.mEpicenterCallback.onGetEpicenter(this);
    }

    public void setPropagation(@Nullable TransitionPropagation transitionPropagation) {
        this.mPropagation = transitionPropagation;
    }

    @Nullable
    public TransitionPropagation getPropagation() {
        return this.mPropagation;
    }

    /* Access modifiers changed, original: 0000 */
    public void capturePropagationValues(TransitionValues transitionValues) {
        if (!(this.mPropagation == null || transitionValues.values.isEmpty())) {
            String[] propertyNames = this.mPropagation.getPropagationProperties();
            if (propertyNames != null) {
                boolean containsAll = true;
                for (Object containsKey : propertyNames) {
                    if (!transitionValues.values.containsKey(containsKey)) {
                        containsAll = false;
                        break;
                    }
                }
                if (!containsAll) {
                    this.mPropagation.captureValues(transitionValues);
                }
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public Transition setSceneRoot(ViewGroup sceneRoot) {
        this.mSceneRoot = sceneRoot;
        return this;
    }

    /* Access modifiers changed, original: 0000 */
    public void setCanRemoveViews(boolean canRemoveViews) {
        this.mCanRemoveViews = canRemoveViews;
    }

    public String toString() {
        return toString("");
    }

    public Transition clone() {
        try {
            Transition clone = (Transition) super.clone();
            clone.mAnimators = new ArrayList();
            clone.mStartValues = new TransitionValuesMaps();
            clone.mEndValues = new TransitionValuesMaps();
            clone.mStartValuesList = null;
            clone.mEndValuesList = null;
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @NonNull
    public String getName() {
        return this.mName;
    }

    /* Access modifiers changed, original: 0000 */
    public String toString(String indent) {
        StringBuilder stringBuilder;
        String result = new StringBuilder();
        result.append(indent);
        result.append(getClass().getSimpleName());
        result.append("@");
        result.append(Integer.toHexString(hashCode()));
        result.append(": ");
        result = result.toString();
        if (this.mDuration != -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(result);
            stringBuilder.append("dur(");
            stringBuilder.append(this.mDuration);
            stringBuilder.append(") ");
            result = stringBuilder.toString();
        }
        if (this.mStartDelay != -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(result);
            stringBuilder.append("dly(");
            stringBuilder.append(this.mStartDelay);
            stringBuilder.append(") ");
            result = stringBuilder.toString();
        }
        if (this.mInterpolator != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(result);
            stringBuilder.append("interp(");
            stringBuilder.append(this.mInterpolator);
            stringBuilder.append(") ");
            result = stringBuilder.toString();
        }
        if (this.mTargetIds.size() <= 0 && this.mTargets.size() <= 0) {
            return result;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(result);
        stringBuilder.append("tgts(");
        result = stringBuilder.toString();
        int i = 0;
        if (this.mTargetIds.size() > 0) {
            String result2 = result;
            for (int i2 = 0; i2 < this.mTargetIds.size(); i2++) {
                StringBuilder stringBuilder2;
                if (i2 > 0) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(result2);
                    stringBuilder2.append(", ");
                    result2 = stringBuilder2.toString();
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(result2);
                stringBuilder2.append(this.mTargetIds.get(i2));
                result2 = stringBuilder2.toString();
            }
            result = result2;
        }
        if (this.mTargets.size() > 0) {
            while (true) {
                int i3 = i;
                if (i3 >= this.mTargets.size()) {
                    break;
                }
                StringBuilder stringBuilder3;
                if (i3 > 0) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(result);
                    stringBuilder3.append(", ");
                    result = stringBuilder3.toString();
                }
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(result);
                stringBuilder3.append(this.mTargets.get(i3));
                result = stringBuilder3.toString();
                i = i3 + 1;
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(result);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
