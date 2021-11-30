package com.infilos.utils.future;

import com.infilos.utils.Futures;
import com.infilos.utils.Require;

import java.util.concurrent.*;

/**
 * {@link CurrentReducer} is used to queue tasks which will be
 * executed in a manner reducing the number of concurrent tasks.
 *
 * Note: This is a port of ConcurrencyLimiter from futures-extra for use with CompletionStages
 */
public class CurrentReducer<T> {
    private final BlockingQueue<Job<T>> queue;
    private final Semaphore limit;
    private final int maxQueueSize;
    private final int maxConcurrency;

    private CurrentReducer(int maxConcurrency, int maxQueueSize) {
        this.maxConcurrency = maxConcurrency;
        this.maxQueueSize = maxQueueSize;
        if(maxConcurrency <=0) {
            throw new IllegalArgumentException("maxConcurrency must be at least 0");
        }
        if(maxQueueSize <=0) {
            throw new IllegalArgumentException("maxQueueSize must be at least 0");
        }
        this.queue = new ArrayBlockingQueue<>(maxQueueSize);
        this.limit = new Semaphore(maxConcurrency);
    }

    /**
     * @param maxConcurrency maximum number of futures in progress,
     * @param maxQueueSize   maximum number of jobs in queue. This is a soft bound and may be
     *                       temporarily exceeded if add() is called concurrently.
     * @return a new concurrency limiter
     */
    public static <T> CurrentReducer<T> create(int maxConcurrency, int maxQueueSize) {
        return new CurrentReducer<>(maxConcurrency, maxQueueSize);
    }

    public CompletableFuture<T> add(final Callable<? extends CompletionStage<T>> callable) {
        Require.checkNotNull(callable);
        final CompletableFuture<T> response = new CompletableFuture<>();
        final Job<T> job = new Job<>(callable, response);
        if(!queue.offer(job)) {
            final String message = "Queue size has reached capacity: " + maxQueueSize;
            return Futures.ofFailed(new CapacityReachedException(message));
        }

        pump();

        return response;
    }

    /**
     * @return the number of callables that are queued up and haven't started yet.
     */
    public int queuedSize() {
        return queue.size();
    }

    /**
     * @return the number of currently active futures that have not yet completed.
     */
    public int activeSize() {
        return maxConcurrency - limit.availablePermits();
    }

    /**
     * @return the number of additional callables that can be queued before failing.
     */
    public int remainingQueueCapacity() {
        return queue.remainingCapacity();
    }

    /**
     * @return the number of additional callables that can be run without queueing.
     */
    public int remainingActiveCapacity() {
        return limit.availablePermits();
    }

    private Job<T> grabJob() {
        if(!limit.tryAcquire()) {
            return null;
        }

        final Job<T> job = queue.poll();
        if(job != null) {
            return job;
        }

        limit.release();

        return null;
    }

    private void pump() {
        Job<T> job;
        while ((job = grabJob()) != null) {
            final CompletableFuture<T> response = job.response;

            if(response.isCancelled()){
                limit.release();
            } else {
                invoke(response, job.callable);
            }
        }
    }

    private void invoke(final CompletableFuture<T> response,
                        final Callable<? extends CompletionStage<T>> callable) {
        final CompletionStage<T> future;
        try {
            future = callable.call();
            if(future == null) {
                limit.release();
                response.completeExceptionally(new NullPointerException());
                return;
            }
        } catch (Throwable e) {
            limit.release();
            response.completeExceptionally(e);
            return;
        }

        future.whenComplete((result, t) -> {
           if( t != null) {
               limit.release();
               response.completeExceptionally(t);
               pump();
           } else {
               limit.release();
               response.complete(result);
               pump();
           }
        });
    }



    private static class Job<T> {

        private final Callable<? extends CompletionStage<T>> callable;
        private final CompletableFuture<T> response;

        public Job(Callable<? extends CompletionStage<T>> callable, CompletableFuture<T> response) {
            this.callable = callable;
            this.response = response;
        }
    }

    public static class CapacityReachedException extends RuntimeException {

        public CapacityReachedException(String errorMessage) {
            super(errorMessage);
        }
    }
}
