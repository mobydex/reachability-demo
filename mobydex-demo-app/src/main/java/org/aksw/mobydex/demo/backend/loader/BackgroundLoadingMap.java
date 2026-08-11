package org.aksw.mobydex.demo.backend.loader;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.aksw.mobydex.demo.backend.loader.PriorityExecutor.PrioritizedFutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.FlowableProcessor;
import io.reactivex.rxjava3.processors.ReplayProcessor;

// TODO: Instead of keyIterator, a keySet could be used.
//   Usually the items for lazy loading is not that large, that the convenience of
//   .keySet().size() and .keySet().contains() could be provided.
public class BackgroundLoadingMap<K, V>
    implements AutoCloseable
{
    private static final Logger logger = LoggerFactory.getLogger(BackgroundLoadingMap.class);

    private static final int PRIO_BACKGROUND = 0;
    private static final int PRIO_FOREGROUND = 1;

    // private Iterator<K> keyIterator;
    private Set<K> keySet;
    private Function<K, V> loader;
    private AsyncCache<K, V> cache;

    private int nThreads;

    private final ReentrantLock activityLock = new ReentrantLock();
    private final Condition hasSubscribers = activityLock.newCondition();

    private int subscriberCount = 0;
    private boolean closed = false;
    private final Semaphore backgroundSlots;

    private final int maxBackgroundTasks;
    private CompletableFuture<Boolean> onComplete = new CompletableFuture<>();

    private final PriorityExecutor<K> priorityExecutor;

    private Thread loaderThread = null;

    private final FlowableProcessor<Entry<K, V>> updateProcessor = ReplayProcessor.<Entry<K, V>>create().toSerialized();

    private final Flowable<Entry<K, V>> updates;
    private final Flowable<Entry<K, V>> flowable;

    public BackgroundLoadingMap(int nThreads, Set<K> keySet, Function<K, V> loader) {
        super();
        this.nThreads = nThreads;
        this.keySet = keySet;
        this.loader = loader;
        this.priorityExecutor = new PriorityExecutor<>(nThreads);
        this.maxBackgroundTasks = nThreads * 2;
        this.backgroundSlots = new Semaphore(maxBackgroundTasks);
        this.cache = Caffeine.newBuilder().executor(priorityExecutor.getExecutor())
                .maximumSize(1000)
                .buildAsync();

        this.updates = Flowable.defer(() -> {
            if (!addSubscriber()) {
                return Flowable.error(
                        new IllegalStateException("BackgroundLoadingMap is closed"));
            }

            return updateProcessor
                    .hide()
                    .doFinally(this::removeSubscriber);
        });

        this.flowable = Flowable.defer(() -> {
            if (!addSubscriber()) {
                return Flowable.error(
                        new IllegalStateException("BackgroundLoadingMap is closed"));
            }

            return updates.doFinally(this::removeSubscriber);
        });
    }

    public int getThreadCount() {
        return nThreads;
    }

    public AsyncCache<K, V> getCache() {
        return cache;
    }

    public CompletableFuture<V> get(K key) {
        int prio = PRIO_FOREGROUND;
        // If the key is already scheduled then update its priority
        priorityExecutor.updatePriority(key, prio);
        return submit(key, prio);
    }

    public V getIfPresent(K key) {
        CompletableFuture<V> future = cache.getIfPresent(key);
        V result;
        try {
            result = future != null && future.isDone() ? future.get() : null;
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

    public CompletionStage<Boolean> onComplete() {
        return onComplete;
    }

    public boolean isDone() {
        return onComplete.isDone();
    }

    private boolean addSubscriber() {
        activityLock.lock();
        try {
            if (closed) {
                return false;
            }

            ++subscriberCount;

            if (loaderThread == null) {
                loaderThread = new Thread(this::runBackgroundLoader, "background-loading-map");
                loaderThread.start();
            }

            hasSubscribers.signalAll();
            return true;
        } finally {
            activityLock.unlock();
        }
    }

    private void removeSubscriber() {
        activityLock.lock();
        try {
            --subscriberCount;
        } finally {
            activityLock.unlock();
        }
    }

    /** Block until there is a subscriber. */
    private boolean awaitSubscriber() throws InterruptedException {
        activityLock.lock();
        try {
            while (!closed && subscriberCount == 0) {
                hasSubscribers.await();
            }

            return !closed;
        } finally {
            activityLock.unlock();
        }
    }

    private void runBackgroundLoader() {
        Iterator<K> it = keySet.iterator();

        try {
            while (it.hasNext()) {
                if (!awaitSubscriber()) {
                    return;
                }

                backgroundSlots.acquire();

                if (!hasSubscribers()) {
                    backgroundSlots.release();
                    continue;
                }

                K key = it.next();

                CompletableFuture<V> future =
                        submit(key, PRIO_BACKGROUND);

                future.whenComplete((value, error) -> {
                    try {
                        if (error != null) {
                            handleLoadFailure(key, error);
                        } else {
                            updateProcessor.onNext(Map.entry(key, value));
                        }
                    } finally {
                        backgroundSlots.release();
                    }
                });
            }

            // Wait until every submitted background job has finished.
            backgroundSlots.acquire(maxBackgroundTasks);
            backgroundSlots.release(maxBackgroundTasks);

            updateProcessor.onComplete();
            onComplete.complete(Boolean.TRUE);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            if (!closed) {
                Throwable error =
                        new RuntimeException("Background loader interrupted", e);

                updateProcessor.onError(error);
                onComplete.completeExceptionally(error);
            }
        }
    }

    private void handleLoadFailure(K key, Throwable error) {
        logger.warn("Failed to load " + key + ": ", error);
    }

    private boolean hasSubscribers() {
        activityLock.lock();
        try {
            return subscriberCount > 0 && !closed;
        } finally {
            activityLock.unlock();
        }
    }

    protected void shutdown() {
        priorityExecutor.getExecutor().shutdown();
        try {
            priorityExecutor.getExecutor().awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        activityLock.lock();
        try {
            if (closed) {
                return;
            }

            closed = true;
            hasSubscribers.signalAll();
        } finally {
            activityLock.unlock();
        }

        if (loaderThread != null) {
            loaderThread.interrupt();
        }

        priorityExecutor.getExecutor().shutdownNow();
    }

    public Flowable<Entry<K, V>> flow() {
        return updates;
    }
}
