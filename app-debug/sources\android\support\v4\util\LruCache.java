package android.support.v4.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class LruCache<K, V> {
    private int createCount;
    private int evictionCount;
    private int hitCount;
    private final LinkedHashMap<K, V> map;
    private int maxSize;
    private int missCount;
    private int putCount;
    private int size;

    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap(0, 0.75f, true);
    }

    public void resize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        synchronized (this) {
            this.maxSize = maxSize;
        }
        trimToSize(maxSize);
    }

    /* JADX WARNING: Missing block: B:14:0x0023, code skipped:
            r2 = create(r6);
     */
    /* JADX WARNING: Missing block: B:15:0x0027, code skipped:
            if (r2 != null) goto L_0x002a;
     */
    /* JADX WARNING: Missing block: B:16:0x0029, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:17:0x002a, code skipped:
            monitor-enter(r5);
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            r5.createCount++;
            r1 = r5.map.put(r6, r2);
     */
    /* JADX WARNING: Missing block: B:20:0x0038, code skipped:
            if (r1 == null) goto L_0x0040;
     */
    /* JADX WARNING: Missing block: B:21:0x003a, code skipped:
            r5.map.put(r6, r1);
     */
    /* JADX WARNING: Missing block: B:22:0x0040, code skipped:
            r5.size += safeSizeOf(r6, r2);
     */
    /* JADX WARNING: Missing block: B:23:0x0049, code skipped:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:24:0x004a, code skipped:
            if (r1 == null) goto L_0x0051;
     */
    /* JADX WARNING: Missing block: B:25:0x004c, code skipped:
            entryRemoved(false, r6, r2, r1);
     */
    /* JADX WARNING: Missing block: B:26:0x0050, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:27:0x0051, code skipped:
            trimToSize(r5.maxSize);
     */
    /* JADX WARNING: Missing block: B:28:0x0056, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final V get(K key) {
        Throwable th;
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        synchronized (this) {
            V mapValue;
            try {
                mapValue = this.map.get(key);
                if (mapValue != null) {
                    try {
                        this.hitCount++;
                        return mapValue;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
                this.missCount++;
            } catch (Throwable th3) {
                Throwable th4 = th3;
                mapValue = null;
                th = th4;
                throw th;
            }
        }
    }

    public final V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }
        V previous;
        synchronized (this) {
            this.putCount++;
            this.size += safeSizeOf(key, value);
            previous = this.map.put(key, value);
            if (previous != null) {
                this.size -= safeSizeOf(key, previous);
            }
        }
        if (previous != null) {
            entryRemoved(false, key, previous, value);
        }
        trimToSize(this.maxSize);
        return previous;
    }

    /* JADX WARNING: Missing block: B:21:0x0057, code skipped:
            r3 = new java.lang.StringBuilder();
            r3.append(getClass().getName());
            r3.append(".sizeOf() is reporting inconsistent results!");
     */
    /* JADX WARNING: Missing block: B:22:0x0075, code skipped:
            throw new java.lang.IllegalStateException(r3.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void trimToSize(int maxSize) {
        V value = null;
        while (true) {
            K key;
            synchronized (this) {
                if (this.size >= 0) {
                    if (this.map.isEmpty() && this.size != 0) {
                        break;
                    } else if (this.size > maxSize) {
                        if (this.map.isEmpty()) {
                            break;
                        }
                        Entry<K, V> toEvict = (Entry) this.map.entrySet().iterator().next();
                        key = toEvict.getKey();
                        value = toEvict.getValue();
                        this.map.remove(key);
                        this.size -= safeSizeOf(key, value);
                        this.evictionCount++;
                    }
                } else {
                    break;
                }
            }
            entryRemoved(true, key, value, null);
        }
    }

    /* JADX WARNING: Missing block: B:11:0x001e, code skipped:
            if (r1 == null) goto L_0x0024;
     */
    /* JADX WARNING: Missing block: B:12:0x0020, code skipped:
            entryRemoved(false, r6, r1, null);
     */
    /* JADX WARNING: Missing block: B:13:0x0024, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final V remove(K key) {
        Throwable th;
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        synchronized (this) {
            try {
                V previous = this.map.remove(key);
                if (previous != null) {
                    try {
                        this.size -= safeSizeOf(key, previous);
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            } catch (Throwable th3) {
                Throwable th4 = th3;
                Object obj = null;
                th = th4;
                throw th;
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void entryRemoved(boolean evicted, K k, V v, V v2) {
    }

    /* Access modifiers changed, original: protected */
    public V create(K k) {
        return null;
    }

    private int safeSizeOf(K key, V value) {
        int result = sizeOf(key, value);
        if (result >= 0) {
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Negative size: ");
        stringBuilder.append(key);
        stringBuilder.append("=");
        stringBuilder.append(value);
        throw new IllegalStateException(stringBuilder.toString());
    }

    /* Access modifiers changed, original: protected */
    public int sizeOf(K k, V v) {
        return 1;
    }

    public final void evictAll() {
        trimToSize(-1);
    }

    public final synchronized int size() {
        return this.size;
    }

    public final synchronized int maxSize() {
        return this.maxSize;
    }

    public final synchronized int hitCount() {
        return this.hitCount;
    }

    public final synchronized int missCount() {
        return this.missCount;
    }

    public final synchronized int createCount() {
        return this.createCount;
    }

    public final synchronized int putCount() {
        return this.putCount;
    }

    public final synchronized int evictionCount() {
        return this.evictionCount;
    }

    public final synchronized Map<K, V> snapshot() {
        return new LinkedHashMap(this.map);
    }

    public final synchronized String toString() {
        int hitPercent;
        int accesses = this.hitCount + this.missCount;
        hitPercent = accesses != 0 ? (100 * this.hitCount) / accesses : 0;
        return String.format(Locale.US, "LruCache[maxSize=%d,hits=%d,misses=%d,hitRate=%d%%]", new Object[]{Integer.valueOf(this.maxSize), Integer.valueOf(this.hitCount), Integer.valueOf(this.missCount), Integer.valueOf(hitPercent)});
    }
}
