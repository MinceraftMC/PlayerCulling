package de.pianoman911.playerculling.platformcommon.util;
// Created by booky10 in PlayerCulling (01:58 21.05.2025)

import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongFunction;

/**
 * Mostly copied from {@link ConcurrentHashMap}, with some important changes:
 * <ul>
 * <li>Directly use primitive longs instead of wrapping them (saves a few gigabytes in heap allocation)</li>
 * <li>Don't check and extract comparable classes, directly compare longs (saves a lot of cpu time)</li>
 * <li>Single lambda function instead of re-creating it in computeIfAbsent (saves a few gigabytes in heap allocation)</li>
 * </ul>
 */
@NullMarked
public final class ConcurrentLongCache<V> implements Iterable<V> {

    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int DEFAULT_CAPACITY = 16;
    private static final int TREEIFY_THRESHOLD = 8;
    private static final int UNTREEIFY_THRESHOLD = 6;
    private static final int MIN_TREEIFY_CAPACITY = 64;
    private static final int MIN_TRANSFER_STRIDE = 16;
    private static final int RESIZE_STAMP_BITS = 16;
    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

    private static final int MOVED = -1; // hash for forwarding nodes
    private static final int TREEBIN = -2; // hash for roots of trees
    private static final int RESERVED = -3; // hash for transient reservations
    private static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    private static final VarHandle NODE_ARRAY_ELEMENT, TREEBIN_WAITERTHREAD;
    private static final MethodHandle GET_THREAD_THREAD_LOCAL_RANDOM_PROBE, THREAD_LOCAL_RANDOM_LOCAL_INIT, THREAD_LOCAL_RANDOM_ADVANCE_PROBE;

    static {
        try {
            MethodHandles.Lookup lookup = ReflectionUtil.getTrustedLookup();
            NODE_ARRAY_ELEMENT = MethodHandles.arrayElementVarHandle(Node[].class);
            TREEBIN_WAITERTHREAD = lookup.findVarHandle(TreeBin.class, "waiter", Thread.class);
            GET_THREAD_THREAD_LOCAL_RANDOM_PROBE = lookup.findGetter(Thread.class,
                    "threadLocalRandomProbe", int.class);
            THREAD_LOCAL_RANDOM_LOCAL_INIT = lookup.findStatic(ThreadLocalRandom.class,
                    "localInit", MethodType.methodType(void.class));
            THREAD_LOCAL_RANDOM_ADVANCE_PROBE = lookup.findStatic(ThreadLocalRandom.class,
                    "advanceProbe", MethodType.methodType(int.class, int.class));
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }

        ReservationNode.class.getClasses(); // load
    }

    private final LongFunction<V> ctor;

    private volatile @Nullable Node<V> @Nullable [] table;
    private volatile @Nullable Node<V> @Nullable [] nextTable;
    private final AtomicLong baseCount = new AtomicLong();
    private final AtomicInteger sizeCtl = new AtomicInteger();
    private final AtomicInteger transferIndex = new AtomicInteger();
    private final AtomicInteger cellsBusy = new AtomicInteger();
    private volatile @Nullable CounterCell @Nullable [] counterCells;

    public ConcurrentLongCache(LongFunction<V> ctor) {
        this.ctor = ctor;
    }

    private static int saferHashCode(long l) {
        // copied from mojang's ChunkPos#hash, as this is a lot safer and causes a lot less
        // hash collisions; we combine two (relatively small) ints together, while java splits
        // the long up again and XORs it
        return (0x19660D * (int) (l >>> 32) + 0x3C6EF35F) ^ (0x19660D * ((int) l ^ 0xDEADBEEF) + 0x3C6EF35F);
    }

    private static int getThreadLocalRandomProbe() {
        try {
            return (int) GET_THREAD_THREAD_LOCAL_RANDOM_PROBE.invoke(Thread.currentThread());
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private static int spread(int hash) {
        return (hash ^ (hash >>> 16)) & HASH_BITS;
    }

    private static int tableSizeFor(int c) {
        int n = -1 >>> Integer.numberOfLeadingZeros(c - 1);
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    private static int resizeStamp(int n) {
        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
    }

    private static <V> Node<V> untreeify(@Nullable Node<V> b) {
        Node<V> hd = null, tl = null;
        for (Node<V> q = b; q != null; q = q.next) {
            Node<V> p = new Node<>(q.hash, q.key, q.val);
            if (tl == null) {
                hd = p;
            } else {
                tl.next = p;
            }
            tl = p;
        }
        return hd;
    }

    @SuppressWarnings("unchecked")
    private final void treeifyBin(@Nullable Node<V> @Nullable [] tab, int index) {
        Node<V> b;
        int n;
        if (tab != null) {
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY) {
                this.tryPresize(n << 1);
            } else if ((b = (Node<V>) NODE_ARRAY_ELEMENT.getAcquire(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    if (NODE_ARRAY_ELEMENT.getAcquire(tab, index) == b) {
                        TreeNode<V> hd = null, tl = null;
                        for (Node<V> e = b; e != null; e = e.next) {
                            TreeNode<V> p = new TreeNode<>(e.hash, e.key, e.val, null, null);
                            if ((p.prev = tl) == null) {
                                hd = p;
                            } else {
                                tl.next = p;
                            }
                            tl = p;
                        }
                        NODE_ARRAY_ELEMENT.setRelease(tab, index, new TreeBin<>(hd));
                    }
                }
            }
        }
    }

    private final @Nullable Node<V>[] initTable() {
        @Nullable Node<V>[] tab;
        int sc;
        while ((tab = this.table) == null || tab.length == 0) {
            if ((sc = this.sizeCtl.get()) < 0) {
                Thread.yield(); // lost initialization race; just spin
            } else if (this.sizeCtl.compareAndSet(sc, -1)) {
                try {
                    if ((tab = this.table) == null || tab.length == 0) {
                        int n = sc > 0 ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        @Nullable Node<V>[] nt = (Node<V>[]) new Node<?>[n];
                        this.table = tab = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    this.sizeCtl.set(sc);
                }
                break;
            }
        }
        return tab;
    }

    private final void addCount(long x, int check) {
        @Nullable CounterCell[] cs;
        long b, s;
        if ((cs = this.counterCells) != null
                || !this.baseCount.compareAndSet(b = this.baseCount.get(), s = b + x)) {
            CounterCell c;
            long v;
            int m;
            boolean uncontended = true;
            if (cs == null || (m = cs.length - 1) < 0
                    || (c = cs[getThreadLocalRandomProbe() & m]) == null
                    || !(uncontended = c.value.compareAndSet(v = c.value.get(), v + x))) {
                this.fullAddCount(x, uncontended);
                return;
            }
            if (check <= 1) {
                return;
            }
            s = this.sumCount();
        }
        if (check >= 0) {
            @Nullable Node<V>[] tab, nt;
            int n, sc;
            while (s >= (long) (sc = this.sizeCtl.get()) && (tab = this.table) != null
                    && (n = tab.length) < MAXIMUM_CAPACITY) {
                int rs = resizeStamp(n) << RESIZE_STAMP_SHIFT;
                if (sc < 0) {
                    if (sc == rs + MAX_RESIZERS || sc == rs + 1
                            || (nt = this.nextTable) == null || this.transferIndex.get() <= 0)
                        break;
                    if (this.sizeCtl.compareAndSet(sc, sc + 1)) {
                        this.transfer(tab, nt);
                    }
                } else if (this.sizeCtl.compareAndSet(sc, rs + 2)) {
                    this.transfer(tab, null);
                }
                s = this.sumCount();
            }
        }
    }

    private final @Nullable Node<V> @Nullable [] helpTransfer(@Nullable Node<V> @Nullable [] tab, Node<V> f) {
        Node<V>[] nextTab;
        int sc;
        if (tab != null && f instanceof ForwardingNode
                && (nextTab = ((ForwardingNode<V>) f).nextTable) != null) {
            int rs = resizeStamp(tab.length) << RESIZE_STAMP_SHIFT;
            while (nextTab == this.nextTable && this.table == tab
                    && (sc = this.sizeCtl.get()) < 0) {
                if (sc == rs + MAX_RESIZERS || sc == rs + 1
                        || this.transferIndex.get() <= 0) {
                    break;
                }
                if (this.sizeCtl.compareAndSet(sc, sc + 1)) {
                    this.transfer(tab, nextTab);
                    break;
                }
            }
            return nextTab;
        }
        return this.table;
    }

    private final void tryPresize(int size) {
        int c = (size >= (MAXIMUM_CAPACITY >>> 1))
                ? MAXIMUM_CAPACITY : tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        while ((sc = this.sizeCtl.get()) >= 0) {
            @Nullable Node<V>[] tab = this.table;
            int n;
            if (tab == null || (n = tab.length) == 0) {
                n = Math.max(sc, c);
                if (this.sizeCtl.compareAndSet(sc, -1)) {
                    try {
                        if (this.table == tab) {
                            @SuppressWarnings("unchecked")
                            @Nullable Node<V>[] nt = (Node<V>[]) new Node<?>[n];
                            this.table = nt;
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        this.sizeCtl.set(sc);
                    }
                }
            } else if (c <= sc || n >= MAXIMUM_CAPACITY) {
                break;
            } else if (tab == table) {
                int rs = resizeStamp(n);
                if (this.sizeCtl.compareAndSet(sc, (rs << RESIZE_STAMP_SHIFT) + 2)) {
                    this.transfer(tab, null);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private final void transfer(@Nullable Node<V>[] tab, @Nullable Node<V> @Nullable [] nextTab) {
        int n = tab.length, stride;
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE) {
            stride = MIN_TRANSFER_STRIDE; // subdivide range
        }
        if (nextTab == null) { // initiating
            try {
                Node<V>[] nt = (Node<V>[]) new Node<?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) { // try to cope with OOME
                this.sizeCtl.set(Integer.MAX_VALUE);
                return;
            }
            this.nextTable = nextTab;
            this.transferIndex.set(n);
        }
        int nextn = nextTab.length;
        ForwardingNode<V> fwd = new ForwardingNode<>(nextTab);
        boolean advance = true;
        boolean finishing = false; // to ensure sweep before committing nextTab
        for (int i = 0, bound = 0; ; ) {
            Node<V> f;
            int fh;
            while (advance) {
                int nextIndex, nextBound;
                if (--i >= bound || finishing) {
                    advance = false;
                } else if ((nextIndex = this.transferIndex.get()) <= 0) {
                    i = -1;
                    advance = false;
                } else if (this.transferIndex.compareAndSet(nextIndex,
                        nextBound = (nextIndex > stride ? nextIndex - stride : 0))) {
                    bound = nextBound;
                    i = nextIndex - 1;
                    advance = false;
                }
            }
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;
                if (finishing) {
                    this.nextTable = null;
                    this.table = nextTab;
                    this.sizeCtl.set((n << 1) - (n >>> 1));
                    return;
                }
                if (this.sizeCtl.compareAndSet(sc = this.sizeCtl.get(), sc - 1)) {
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT) {
                        return;
                    }
                    finishing = advance = true;
                    i = n; // recheck before commit
                }
            } else if ((f = (Node<V>) NODE_ARRAY_ELEMENT.getAcquire(tab, i)) == null) {
                advance = NODE_ARRAY_ELEMENT.compareAndSet(tab, i, null, fwd);
            } else if ((fh = f.hash) == MOVED) {
                advance = true; // already processed
            } else {
                synchronized (f) {
                    if (NODE_ARRAY_ELEMENT.getAcquire(tab, i) == f) {
                        Node<V> ln, hn;
                        if (fh >= 0) {
                            int runBit = fh & n;
                            Node<V> lastRun = f;
                            for (Node<V> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            } else {
                                hn = lastRun;
                                ln = null;
                            }
                            for (Node<V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash;
                                long pk = p.key;
                                V pv = p.val;
                                if ((ph & n) == 0) {
                                    ln = new Node<>(ph, pk, pv, ln);
                                } else {
                                    hn = new Node<>(ph, pk, pv, hn);
                                }
                            }
                            NODE_ARRAY_ELEMENT.setRelease(nextTab, i, ln);
                            NODE_ARRAY_ELEMENT.setRelease(nextTab, i + n, hn);
                            NODE_ARRAY_ELEMENT.setRelease(tab, i, fwd);
                            advance = true;
                        } else if (f instanceof TreeBin<V> t) {
                            TreeNode<V> lo = null, loTail = null;
                            TreeNode<V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            for (Node<V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                TreeNode<V> p = new TreeNode<>(h, e.key, e.val, null, null);
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null) {
                                        lo = p;
                                    } else {
                                        loTail.next = p;
                                    }
                                    loTail = p;
                                    ++lc;
                                } else {
                                    if ((p.prev = hiTail) == null) {
                                        hi = p;
                                    } else {
                                        hiTail.next = p;
                                    }
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            ln = (lc <= UNTREEIFY_THRESHOLD)
                                    ? untreeify(lo) : (hc != 0) ? new TreeBin<>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD)
                                    ? untreeify(hi) : (lc != 0) ? new TreeBin<>(hi) : t;
                            NODE_ARRAY_ELEMENT.setRelease(nextTab, i, ln);
                            NODE_ARRAY_ELEMENT.setRelease(nextTab, i + n, hn);
                            NODE_ARRAY_ELEMENT.setRelease(tab, i, fwd);
                            advance = true;
                        } else if (f instanceof ReservationNode) {
                            throw new IllegalStateException("Recursive update");
                        }
                    }
                }
            }
        }
    }

    // @jdk.internal.vm.annotation.Contended
    private static final class CounterCell {

        private final AtomicLong value;

        public CounterCell(long x) {
            this.value = new AtomicLong(x);
        }
    }

    private final long sumCount() {
        @Nullable CounterCell[] cs = this.counterCells;
        long sum = this.baseCount.get();
        if (cs != null) {
            for (CounterCell c : cs) {
                if (c != null) {
                    sum += c.value.get();
                }
            }
        }
        return sum;
    }

    private final void fullAddCount(long x, boolean wasUncontended) {
        int h;
        if ((h = getThreadLocalRandomProbe()) == 0) {
            try {
                THREAD_LOCAL_RANDOM_LOCAL_INIT.invoke(); // force initialization
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
            h = getThreadLocalRandomProbe();
            wasUncontended = true;
        }
        boolean collide = false; // True if last slot nonempty
        for (; ; ) {
            @Nullable CounterCell[] cs;
            CounterCell c;
            int n;
            long v;
            if ((cs = this.counterCells) != null && (n = cs.length) > 0) {
                if ((c = cs[(n - 1) & h]) == null) {
                    if (this.cellsBusy.get() == 0) { // Try to attach new Cell
                        CounterCell r = new CounterCell(x); // Optimistic create
                        if (this.cellsBusy.get() == 0 && this.cellsBusy.compareAndSet(0, 1)) {
                            boolean created = false;
                            try { // Recheck under lock
                                CounterCell[] rs;
                                int m, j;
                                if ((rs = this.counterCells) != null
                                        && (m = rs.length) > 0
                                        && rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                this.cellsBusy.set(0);
                            }
                            if (created) {
                                break;
                            }
                            continue; // Slot is now non-empty
                        }
                    }
                    collide = false;
                } else if (!wasUncontended) { // CAS already known to fail
                    wasUncontended = true; // Continue after rehash
                } else if (c.value.compareAndSet(v = c.value.get(), v + x)) {
                    break;
                } else if (this.counterCells != cs || n >= NCPU) {
                    collide = false; // At max size or stale
                } else if (!collide) {
                    collide = true;
                } else if (this.cellsBusy.get() == 0 && this.cellsBusy.compareAndSet(0, 1)) {
                    try {
                        if (this.counterCells == cs) { // Expand table unless stale
                            this.counterCells = Arrays.copyOf(cs, n << 1);
                        }
                    } finally {
                        this.cellsBusy.set(0);
                    }
                    collide = false;
                    continue; // Retry with expanded table
                }
                try {
                    h = (int) THREAD_LOCAL_RANDOM_ADVANCE_PROBE.invoke(h);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            } else if (this.cellsBusy.get() == 0 && this.counterCells == cs
                    && this.cellsBusy.compareAndSet(0, 1)) {
                boolean init = false;
                try {                           // Initialize table
                    if (this.counterCells == cs) {
                        @Nullable CounterCell[] rs = new CounterCell[2];
                        rs[h & 1] = new CounterCell(x);
                        this.counterCells = rs;
                        init = true;
                    }
                } finally {
                    this.cellsBusy.set(0);
                }
                if (init) {
                    break;
                }
            } else if (this.baseCount.compareAndSet(v = this.baseCount.get(), v + x)) {
                break; // Fall back on using base
            }
        }
    }

    public int size() {
        long n = this.sumCount();
        if (n < 0L) {
            return 0;
        } else if (n > (long) Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) n;
    }

    public boolean isEmpty() {
        return this.sumCount() <= 0L;
    }

    @SuppressWarnings("unchecked")
    public @Nullable V get(long key) {
        @Nullable Node<V>[] tab;
        Node<V> e, p;
        int n, eh;
        int h = spread(saferHashCode(key));
        if ((tab = this.table) != null && (n = tab.length) > 0
                && (e = (Node<V>) NODE_ARRAY_ELEMENT.getAcquire(tab, (n - 1) & h)) != null) {
            if ((eh = e.hash) == h) {
                if (e.key == key) {
                    return e.val;
                }
            } else if (eh < 0) {
                return (p = e.find(h, key)) != null ? p.val : null;
            }
            while ((e = e.next) != null) {
                if (e.hash == h && e.key == key) {
                    return e.val;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public V getOrCompute(long key) {
        int h = spread(saferHashCode(key));
        V val = null;
        int binCount = 0;
        for (@Nullable Node<V>[] tab = this.table; ; ) {
            Node<V> f;
            int n, i, fh;
            V fv;
            if (tab == null || (n = tab.length) == 0) {
                tab = this.initTable();
            } else if ((f = (Node<V>) NODE_ARRAY_ELEMENT.getAcquire(tab, i = (n - 1) & h)) == null) {
                Node<V> r = new ReservationNode<>();
                synchronized (r) {
                    if (NODE_ARRAY_ELEMENT.compareAndSet(tab, i, null, r)) {
                        binCount = 1;
                        Node<V> node = null;
                        try {
                            val = this.ctor.apply(key);
                            node = new Node<>(h, key, val);
                        } finally {
                            NODE_ARRAY_ELEMENT.setRelease(tab, i, node);
                        }
                    }
                }
                if (binCount != 0) {
                    break;
                }
            } else if ((fh = f.hash) == MOVED) {
                tab = this.helpTransfer(tab, f);
            } else if (fh == h && f.key == key && (fv = f.val) != null) { // check first node without acquiring lock
                return fv;
            } else {
                boolean added = false;
                synchronized (f) {
                    if (NODE_ARRAY_ELEMENT.getAcquire(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<V> e = f; ; ++binCount) {
                                if (e.hash == h && e.key == key) {
                                    val = e.val;
                                    break;
                                }
                                Node<V> pred = e;
                                if ((e = e.next) == null) {
                                    val = this.ctor.apply(key);
                                    if (pred.next != null) {
                                        throw new IllegalStateException("Recursive update");
                                    }
                                    added = true;
                                    pred.next = new Node<>(h, key, val);
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin<V> t) {
                            binCount = 2;
                            TreeNode<V> r, p;
                            if ((r = t.root) != null
                                    && (p = r.findTreeNode(h, key)) != null) {
                                val = p.val;
                            } else {
                                val = this.ctor.apply(key);
                                added = true;
                                t.putTreeVal(h, key, val);
                            }
                        } else if (f instanceof ReservationNode) {
                            throw new IllegalStateException("Recursive update");
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD) {
                        this.treeifyBin(tab, i);
                    }
                    if (!added) {
                        return val;
                    }
                    break;
                }
            }
        }
        this.addCount(1L, binCount);
        return val;
    }

    public @Nullable V remove(long key) {
        return this.replaceNode(key, null, null);
    }

    @SuppressWarnings("unchecked")
    private final @Nullable V replaceNode(long key, @Nullable V value, @Nullable Object cv) {
        int hash = spread(saferHashCode(key));
        for (@Nullable Node<V>[] tab = this.table; ; ) {
            Node<V> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0
                    || (f = (Node<V>) NODE_ARRAY_ELEMENT.getAcquire(tab, i = (n - 1) & hash)) == null) {
                break;
            } else if ((fh = f.hash) == MOVED) {
                tab = this.helpTransfer(tab, f);
            } else {
                V oldVal = null;
                boolean validated = false;
                synchronized (f) {
                    if (NODE_ARRAY_ELEMENT.getAcquire(tab, i) == f) {
                        if (fh >= 0) {
                            validated = true;
                            for (Node<V> e = f, pred = null; ; ) {
                                if (e.hash == hash && (e.key == key)) {
                                    V ev = e.val;
                                    if (cv == null || cv == ev || cv.equals(ev)) {
                                        oldVal = ev;
                                        if (value != null) {
                                            e.val = value;
                                        } else if (pred != null) {
                                            pred.next = e.next;
                                        } else {
                                            NODE_ARRAY_ELEMENT.set(tab, i, e.next);
                                        }
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin<V> t) {
                            validated = true;
                            TreeNode<V> r, p;
                            if ((r = t.root) != null && (p = r.findTreeNode(hash, key)) != null) {
                                V pv = p.val;
                                if (cv == null || cv == pv || cv.equals(pv)) {
                                    oldVal = pv;
                                    if (value != null) {
                                        p.val = value;
                                    } else if (t.removeTreeNode(p)) {
                                        NODE_ARRAY_ELEMENT.setRelease(tab, i, untreeify(t.first));
                                    }
                                }
                            }
                        } else if (f instanceof ReservationNode) {
                            throw new IllegalStateException("Recursive update");
                        }
                    }
                }
                if (validated) {
                    if (oldVal != null) {
                        if (value == null) {
                            this.addCount(-1L, -1);
                        }
                        return oldVal;
                    }
                    break;
                }
            }
        }
        return null;
    }

    @Override
    public Iterator<V> iterator() {
        @Nullable Node<V>[] t = this.table;
        int f = t == null ? 0 : t.length;
        return new ValueIterator<>(t, f, 0, f);
    }

    public interface Entry<V> {

        long getKey();

        V getValue();
    }

    private static class Node<V> implements Entry<V> {

        final int hash;
        final long key;
        volatile @UnknownNullability V val;
        volatile @Nullable Node<V> next;

        Node(int hash, long key, @UnknownNullability V val) {
            this.hash = hash;
            this.key = key;
            this.val = val;
        }

        Node(int hash, long key, @UnknownNullability V val, @Nullable Node<V> next) {
            this(hash, key, val);
            this.next = next;
        }

        @Override
        public long getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.val;
        }

        @Override
        public final boolean equals(Object obj) {
            return obj instanceof Entry<?> entry
                    && entry.getKey() == this.key
                    && Objects.equals(entry.getValue(), this.val);
        }

        @Override
        public final int hashCode() {
            return saferHashCode(this.key) ^ this.val.hashCode();
        }

        @Nullable Node<V> find(int h, long k) {
            Node<V> node = this;
            do {
                if (node.hash == h && node.key == k) {
                    return node;
                }
            } while ((node = node.next) != null);
            return null;
        }
    }

    private static final class ForwardingNode<V> extends Node<V> {

        private final @Nullable Node<V> @Nullable [] nextTable;

        public ForwardingNode(@Nullable Node<V> @Nullable [] tab) {
            super(MOVED, Long.MIN_VALUE, null);
            this.nextTable = tab;
        }

        @SuppressWarnings("unchecked")
        @Override
        @Nullable Node<V> find(int h, long k) {
            // loop to avoid arbitrarily deep recursion on forwarding nodes
            outer:
            for (@Nullable Node<V>[] tab = this.nextTable; ; ) {
                Node<V> e;
                int n;
                if (tab == null || (n = tab.length) == 0
                        || (e = (Node<V>) NODE_ARRAY_ELEMENT.getAcquire(tab, (n - 1) & h)) == null)
                    return null;
                for (; ; ) {
                    int eh;
                    if ((eh = e.hash) == h && (e.key == k)) {
                        return e;
                    } else if (eh < 0) {
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<V>) e).nextTable;
                            continue outer;
                        } else {
                            return e.find(h, k);
                        }
                    } else if ((e = e.next) == null) {
                        return null;
                    }
                }
            }
        }
    }

    private static final class ReservationNode<V> extends Node<V> {

        public ReservationNode() {
            super(RESERVED, Long.MIN_VALUE, null);
        }

        @Override
        @Nullable Node<V> find(int h, long k) {
            return null;
        }
    }

    private static final class TreeNode<V> extends Node<V> {

        private @Nullable TreeNode<V> parent; // red-black tree links
        private @Nullable TreeNode<V> left;
        private @Nullable TreeNode<V> right;
        private @Nullable TreeNode<V> prev; // needed to unlink next upon deltion
        private boolean red;

        public TreeNode(int hash, long key, V val, @Nullable Node<V> next, @Nullable TreeNode<V> parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }

        public final @Nullable TreeNode<V> findTreeNode(int h, long k) {
            TreeNode<V> p = this;
            do {
                int ph;
                long pk;
                TreeNode<V> pl = p.left;
                TreeNode<V> pr = p.right;
                if ((ph = p.hash) > h) {
                    p = pl;
                } else if (ph < h) {
                    p = pr;
                } else if ((pk = p.key) == k) {
                    return p;
                } else if (pl == null) {
                    p = pr;
                } else if (pr == null) {
                    p = pl;
                } else {
                    p = k < pk ? pl : pr;
                }
            } while (p != null);
            return null;
        }
    }

    private static final class TreeBin<V> extends Node<V> {

        private static final int WRITER = 1; // set while holding write lock
        private static final int WAITER = 2; // set when waiting for write lock
        private static final int READER = 4; // increment value for setting read lock

        private @Nullable TreeNode<V> root;
        private volatile @Nullable TreeNode<V> first;
        private volatile @Nullable Thread waiter;
        private final AtomicInteger lockState = new AtomicInteger();

        TreeBin(TreeNode<V> b) {
            super(TREEBIN, Long.MIN_VALUE, null);

            this.first = b;
            TreeNode<V> r = null;
            for (TreeNode<V> x = b, next; x != null; x = next) {
                next = (TreeNode<V>) x.next;
                x.left = x.right = null;
                if (r == null) {
                    x.parent = null;
                    x.red = false;
                    r = x;
                } else {
                    long k = x.key;
                    int h = x.hash;
                    for (TreeNode<V> p = r; ; ) {
                        int dir, ph;
                        long pk = p.key;
                        if ((ph = p.hash) > h) {
                            dir = -1;
                        } else if (ph < h) {
                            dir = 1;
                        } else if ((dir = Long.compare(k, pk)) == 0) {
                            dir = tieBreakOrder(k, pk);
                        }
                        TreeNode<V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0) {
                                xp.left = x;
                            } else {
                                xp.right = x;
                            }
                            r = balanceInsertion(r, x);
                            break;
                        }
                    }
                }
            }
            this.root = r;
            assert checkInvariants(root);
        }

        private static int tieBreakOrder(long a, long b) {
            return System.identityHashCode(a) <= System.identityHashCode(b) ? -1 : 1;
        }

        static <V> TreeNode<V> rotateLeft(TreeNode<V> root, @Nullable TreeNode<V> p) {
            TreeNode<V> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null) {
                    rl.parent = p;
                }
                if ((pp = r.parent = p.parent) == null) {
                    (root = r).red = false;
                } else if (pp.left == p) {
                    pp.left = r;
                } else {
                    pp.right = r;
                }
                r.left = p;
                p.parent = r;
            }
            return root;
        }

        private static <V> TreeNode<V> rotateRight(TreeNode<V> root, @Nullable TreeNode<V> p) {
            TreeNode<V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null) {
                    lr.parent = p;
                }
                if ((pp = l.parent = p.parent) == null) {
                    (root = l).red = false;
                } else if (pp.right == p) {
                    pp.right = l;
                } else {
                    pp.left = l;
                }
                l.right = p;
                p.parent = l;
            }
            return root;
        }

        private static <V> TreeNode<V> balanceInsertion(TreeNode<V> root, TreeNode<V> x) {
            x.red = true;
            for (TreeNode<V> xp, xpp, xppl, xppr; ; ) {
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                if (xp == (xppl = xpp.left)) {
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                } else {
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }

        private static <V> TreeNode<V> balanceDeletion(TreeNode<V> root, TreeNode<V> x) {
            for (TreeNode<V> xp, xpl, xpr; ; ) {
                if (x == null || x == root)
                    return root;
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (x.red) {
                    x.red = false;
                    return root;
                } else if ((xpl = xp.left) == x) {
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        x = xp;
                    else {
                        TreeNode<V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                                (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        } else {
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                        null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                } else { // symmetric
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        x = xp;
                    else {
                        TreeNode<V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                                (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        } else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                        null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        private static <V> boolean checkInvariants(TreeNode<V> t) {
            TreeNode<V> tp = t.parent;
            TreeNode<V> tl = t.left;
            TreeNode<V> tr = t.right;
            TreeNode<V> tb = t.prev;
            TreeNode<V> tn = (TreeNode<V>) t.next;
            if (tb != null && tb.next != t) {
                return false;
            } else if (tn != null && tn.prev != t) {
                return false;
            } else if (tp != null && t != tp.left && t != tp.right) {
                return false;
            } else if (tl != null && (tl.parent != t || tl.hash > t.hash)) {
                return false;
            } else if (tr != null && (tr.parent != t || tr.hash < t.hash)) {
                return false;
            } else if (t.red && tl != null && tl.red && tr != null && tr.red) {
                return false;
            } else if (tl != null && !checkInvariants(tl)) {
                return false;
            }
            return tr == null || checkInvariants(tr);
        }

        private final void lockRoot() {
            if (!this.lockState.compareAndSet(0, WRITER)) {
                this.contendedLock(); // offload to separate method
            }
        }

        private final void unlockRoot() {
            this.lockState.set(0);
        }

        private final void contendedLock() {
            Thread current = Thread.currentThread(), w;
            for (int s; ; ) {
                if (((s = this.lockState.get()) & ~WAITER) == 0) {
                    if (this.lockState.compareAndSet(s, WRITER)) {
                        if (this.waiter == current) {
                            TREEBIN_WAITERTHREAD.compareAndSet(this, current, null);
                        }
                        return;
                    }
                } else if ((s & WAITER) == 0) {
                    this.lockState.compareAndSet(s, s | WAITER);
                } else if ((w = this.waiter) == null) {
                    TREEBIN_WAITERTHREAD.compareAndSet(this, null, current);
                } else if (w == current) {
                    LockSupport.park(this);
                }
            }
        }

        @Override
        @Nullable Node<V> find(int h, long k) {
            for (Node<V> e = this.first; e != null; ) {
                int s;
                if (((s = this.lockState.get()) & (WAITER | WRITER)) != 0) {
                    if (e.hash == h && (e.key == k)) {
                        return e;
                    }
                    e = e.next;
                } else if (this.lockState.compareAndSet(s, s + READER)) {
                    TreeNode<V> r, p;
                    try {
                        p = ((r = this.root) == null ? null : r.findTreeNode(h, k));
                    } finally {
                        Thread w;
                        if (this.lockState.getAndAdd(-READER) ==
                                (READER | WAITER) && (w = this.waiter) != null) {
                            LockSupport.unpark(w);
                        }
                    }
                    return p;
                }
            }
            return null;
        }

        public final @Nullable TreeNode<V> putTreeVal(int h, long k, V v) {
            boolean searched = false;
            for (TreeNode<V> p = this.root; ; ) {
                int dir, ph;
                long pk;
                if (p == null) {
                    this.first = this.root = new TreeNode<>(h, k, v, null, null);
                    break;
                } else if ((ph = p.hash) > h) {
                    dir = -1;
                } else if (ph < h) {
                    dir = 1;
                } else if ((pk = p.key) == k) {
                    return p;
                } else {
                    dir = Long.compare(k, pk);
                }

                TreeNode<V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    TreeNode<V> x, f = this.first;
                    this.first = x = new TreeNode<>(h, k, v, f, xp);
                    if (f != null) {
                        f.prev = x;
                    }
                    if (dir <= 0) {
                        xp.left = x;
                    } else {
                        xp.right = x;
                    }
                    if (!xp.red) {
                        x.red = true;
                    } else {
                        this.lockRoot();
                        try {
                            this.root = balanceInsertion(this.root, x);
                        } finally {
                            this.unlockRoot();
                        }
                    }
                    break;
                }
            }
            assert checkInvariants(this.root);
            return null;
        }

        public final boolean removeTreeNode(TreeNode<V> p) {
            TreeNode<V> next = (TreeNode<V>) p.next;
            TreeNode<V> pred = p.prev;  // unlink traversal pointers
            TreeNode<V> r, rl;
            if (pred == null) {
                this.first = next;
            } else {
                pred.next = next;
            }
            if (next != null) {
                next.prev = pred;
            }
            if (this.first == null) {
                this.root = null;
                return true;
            }
            if ((r = this.root) == null || r.right == null // too small
                    || (rl = r.left) == null || rl.left == null) {
                return true;
            }
            this.lockRoot();
            try {
                TreeNode<V> replacement;
                TreeNode<V> pl = p.left;
                TreeNode<V> pr = p.right;
                if (pl != null && pr != null) {
                    TreeNode<V> s = pr, sl;
                    while ((sl = s.left) != null) {// find successor
                        s = sl;
                    }
                    boolean c = s.red;
                    s.red = p.red;
                    p.red = c; // swap colors
                    TreeNode<V> sr = s.right;
                    TreeNode<V> pp = p.parent;
                    if (s == pr) { // p was s's direct parent
                        p.parent = s;
                        s.right = p;
                    } else {
                        TreeNode<V> sp = s.parent;
                        if ((p.parent = sp) != null) {
                            if (s == sp.left) {
                                sp.left = p;
                            } else {
                                sp.right = p;
                            }
                        }
                        s.right = pr;
                        pr.parent = s;
                    }
                    p.left = null;
                    if ((p.right = sr) != null) {
                        sr.parent = p;
                    }
                    s.left = pl;
                    pl.parent = s;
                    if ((s.parent = pp) == null) {
                        r = s;
                    } else if (p == pp.left) {
                        pp.left = s;
                    } else {
                        pp.right = s;
                    }
                    replacement = Objects.requireNonNullElse(sr, p);
                } else if (pl != null) {
                    replacement = pl;
                } else {
                    replacement = Objects.requireNonNullElse(pr, p);
                }
                if (replacement != p) {
                    TreeNode<V> pp = replacement.parent = p.parent;
                    if (pp == null) {
                        r = replacement;
                    } else if (p == pp.left) {
                        pp.left = replacement;
                    } else {
                        pp.right = replacement;
                    }
                    p.left = p.right = p.parent = null;
                }
                this.root = (p.red) ? r : balanceDeletion(r, replacement);
                if (p == replacement) { // detach pointers
                    TreeNode<V> pp;
                    if ((pp = p.parent) != null) {
                        if (p == pp.left) {
                            pp.left = null;
                        } else if (p == pp.right) {
                            pp.right = null;
                        }
                        p.parent = null;
                    }
                }
            } finally {
                this.unlockRoot();
            }
            assert checkInvariants(this.root);
            return false;
        }
    }

    static final class TableStack<V> {

        int length;
        int index;
        @Nullable Node<V> @Nullable [] tab;
        @Nullable TableStack<V> next;
    }

    private static class Traverser<V> {

        private @Nullable Node<V> @Nullable [] tab; // current table; updated if resized
        protected @Nullable Node<V> next; // the next entry to use
        private @Nullable TableStack<V> stack, spare; // to save/restore on ForwardingNodes
        private int index; // index of bin to use next
        private int baseIndex; // current index of initial table
        private final int baseLimit; // index bound for initial table
        private final int baseSize; // initial table size

        public Traverser(@Nullable Node<V> @Nullable [] tab, int size, int index, int limit) {
            this.tab = tab;
            this.baseSize = size;
            this.baseIndex = index;
            this.index = index;
            this.baseLimit = limit;
        }

        @SuppressWarnings("unchecked")
        public final @Nullable Node<V> advance() {
            Node<V> e;
            if ((e = this.next) != null) {
                e = e.next;
            }
            for (; ; ) {
                @Nullable Node<V>[] t;
                int i, n; // must use locals in checks
                if (e != null) {
                    return this.next = e;
                } else if (this.baseIndex >= this.baseLimit || (t = this.tab) == null
                        || (n = t.length) <= (i = this.index) || i < 0) {
                    return this.next = null;
                } else if ((e = (Node<V>) NODE_ARRAY_ELEMENT.getAcquire(t, i)) != null && e.hash < 0) {
                    if (e instanceof ForwardingNode) {
                        this.tab = ((ForwardingNode<V>) e).nextTable;
                        e = null;
                        this.pushState(t, i, n);
                    } else if (e instanceof TreeBin) {
                        e = ((TreeBin<V>) e).first;
                    } else {
                        e = null;
                    }
                } else if (this.stack != null) {
                    this.recoverState(n);
                } else if ((this.index = i + this.baseSize) >= n) {
                    this.index = ++this.baseIndex; // visit upper slots if present
                }
            }
        }

        private void pushState(@Nullable Node<V>[] t, int i, int n) {
            TableStack<V> s = this.spare; // reuse if possible
            if (s != null) {
                this.spare = s.next;
            } else {
                s = new TableStack<>();
            }
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = this.stack;
            this.stack = s;
        }

        private void recoverState(int n) {
            TableStack<V> s;
            int len;
            while ((s = this.stack) != null && (this.index += (len = s.length)) >= n) {
                n = len;
                this.index = s.index;
                this.tab = s.tab;
                s.tab = null;
                TableStack<V> next = s.next;
                s.next = this.spare; // save for reuse
                this.stack = next;
                this.spare = s;
            }
            if (s == null && (this.index += this.baseSize) >= n) {
                this.index = ++this.baseIndex;
            }
        }
    }

    private static final class ValueIterator<V> extends Traverser<V> implements Iterator<V> {

        public ValueIterator(@Nullable Node<V> @Nullable [] tab, int size, int index, int limit) {
            super(tab, size, index, limit);
            // initialize first entry immediately on iterator construction
            this.advance();
        }

        @Override
        public boolean hasNext() {
            return this.next != null;
        }

        @Override
        public V next() {
            Node<V> p;
            if ((p = this.next) == null) {
                throw new NoSuchElementException();
            }
            V v = p.val;
            this.advance();
            return v;
        }
    }
}
