package org.aksw.mobydex.demo.backend.loader;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class PriorityExecutor<K> {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private final ThreadPoolExecutor executor;
    private final ConcurrentMap<K, PrioritizedFutureTask<?>> keyToTask = new ConcurrentHashMap<>();

    // private CompletableFuture<K> completableFuture = new CompletableFuture<>();

    public PriorityExecutor(int threads) {
        this.executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>());
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public <T> PrioritizedFutureTask<T> submit(K key, int priority, Callable<T> callable) {

        Callable<T> wrapper = () -> {
            keyToTask.remove(key);
            T result = callable.call();
//                try {
//                    result = callable.call();
//                } finally {
//                }
            return result;
        };

        PrioritizedFutureTask<T> task = new PrioritizedFutureTask<>(priority, SEQUENCE.getAndIncrement(), wrapper);
        executor.execute(task);
        return task;
    }

    public boolean updatePriority(K key, int newPriority) {
        PrioritizedFutureTask<?> task = keyToTask.get(key);

        if (task == null || newPriority <= task.getPriority()) {
            return false;
        }

        BlockingQueue<Runnable> queue = executor.getQueue();
        if (!queue.remove(task)) {
            // Already running or completed.
            return false;
        }

        task.setPriority(newPriority);
        queue.add(task);
        return true;
    }

    public static final class PrioritizedFutureTask<T>
            extends FutureTask<T>
            implements Comparable<PrioritizedFutureTask<?>> {

        private volatile int priority;
        private final long sequence;

        private CompletableFuture<T> completableFuture = new CompletableFuture<>();

        private PrioritizedFutureTask(
                int priority,
                long sequence,
                Callable<T> callable) {

            super(callable);
            this.priority = priority;
            this.sequence = sequence;

            // Bidirectional cancellation - canceling the future cancels this task.
            completableFuture.whenComplete((value, error) -> {
                if (completableFuture.isCancelled() && !isCancelled()) {
                    cancel(true);
                }
            });
        }

        public CompletableFuture<T> asCompletableFuture() {
            return completableFuture;
        }

        @Override
        protected void done() {
            if (isCancelled()) {
                completableFuture.cancel(false);
            } else {
                try {
                    // Non-blocking here: done() is called only after completion.
                    completableFuture.complete(get());
                } catch (ExecutionException e) {
                    completableFuture.completeExceptionally(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    completableFuture.completeExceptionally(e);
                }
            }
            super.done();
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        @Override
        public int compareTo(PrioritizedFutureTask<?> other) {
            // Higher numeric priority runs first.
            int byPriority =
                Integer.compare(other.priority, this.priority);

            if (byPriority != 0) {
                return byPriority;
            }

            // FIFO among tasks with equal priority.
            return Long.compare(this.sequence, other.sequence);
        }
    }
}
