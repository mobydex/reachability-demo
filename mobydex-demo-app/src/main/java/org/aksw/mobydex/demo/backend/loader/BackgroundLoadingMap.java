package org.aksw.mobydex.demo.backend.loader;

import java.io.Closeable;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.aksw.mobydex.demo.backend.loader.PriorityExecutor.PrioritizedFutureTask;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;

public class BackgroundLoadingMap<K, V>
    implements Closeable
{
    private static final int PRIO_BACKGROUND = 0;
    private static final int PRIO_FOREGROUND = 1;

    private Iterator<K> keyIterator;
    private Function<K, V> loader;

    private AsyncCache<K, V> cache;

    private AtomicLong counter = new AtomicLong(1);
    private CompletableFuture<Boolean> onComplete = new CompletableFuture<>();

    private int nThreads;
    private PriorityExecutor<K> priorityExecutor;

    private Object lock = new Object();
    private Thread loaderThread = null;

    private Flowable<K> flowable;

//    private volatile boolean isPaused = false;
//    private Lock pauseLock = new ReentrantLock();
//    private Condition unpauseCondition = pauseLock.newCondition();

    // TODO Ideally we'd have a producer / consumer architecture with a shared blocking queue.
    //      So the keyIterator would be replaced with a queue and a poison pill.
    public BackgroundLoadingMap(int nThreads, Iterator<K> keyIterator, Function<K, V> loader) {
        super();
        this.nThreads = nThreads;
        this.keyIterator = keyIterator;
        this.loader = loader;
        this.priorityExecutor = new PriorityExecutor<>(nThreads);
        this.cache = Caffeine.newBuilder().executor(priorityExecutor.getExecutor()).buildAsync();

        this.flowable = Flowable.<K>create(emitter -> {
                // emitter.setCancellable(null);
                runInternal(emitter);
            }, BackpressureStrategy.BUFFER)
                .replay().autoConnect();
    }

    public AsyncCache<K, V> getCache() {
        return cache;
    }

//    public void startLoading() {
//        synchronized (lock) {
//            if (loaderThread == null) {
//                loaderThread = new Thread(this::runInternal);
//                loaderThread.start();
//            }
//        }
//    }

//
//    public void pauseLoading() {
//        synchronized (lock) {
//            if (loaderThread != null) {
//                loaderThread.interrupt();
//                try {
//                    loaderThread.join();
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    loaderThread = null;
//                }
//            }
//        }
//    }
//
//    public void stopLoading() {
//        pauseLoading();
//    }

    public CompletableFuture<V> get(K key) {
        int prio = PRIO_FOREGROUND;
        priorityExecutor.updatePriority(key, prio);
        return submit(key, prio);
    }

    public V getIfPresent(K key) {
        CompletableFuture<V> future = cache.getIfPresent(key);
        V result;
        try {
            result = future.isDone() ? future.get() : null;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    protected CompletableFuture<V> submit(K key, int prio) {
        CompletableFuture<V> result = cache.get(key, (cn, executor) -> {
            PrioritizedFutureTask<V> task = priorityExecutor.submit(key, prio, () -> loader.apply(key));
            return task.asCompletableFuture();
        });
        return result;
    }

    public CompletableFuture<Boolean> onComplete() {
        return onComplete();
    }

    public boolean isDone() {
        return onComplete.isDone();
    }

    protected void runInternal(Emitter<K> emitter) {
        int i = 0;
        while (keyIterator.hasNext()) {
            if (Thread.interrupted()) {
                return;
            }

            ++i;
            if (i > 50) {
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }


            K item = keyIterator.next();
            counter.incrementAndGet();
            CompletableFuture<V> completableFuture = submit(item, PRIO_BACKGROUND);
            completableFuture.whenComplete((v, error) -> {
                if (error != null) {
                    emitter.onError(error);
                } else {
                    emitter.onNext(item);
                }
                decrementCounter(emitter);
            });
        }
        decrementCounter(emitter);
    }

    protected void decrementCounter(Emitter<K> emitter) {
        long count = counter.decrementAndGet();
        if (count == 0) {
            onComplete.complete(Boolean.TRUE);
            emitter.onComplete();
            shutdown();
        }
    }

    protected void shutdown() {
        priorityExecutor.getExecutor().shutdown();
        try {
            priorityExecutor.getExecutor().awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        // stopLoading();
        shutdown();
    }

    public Flowable<K> flow() {
        return flowable;
    }
}
