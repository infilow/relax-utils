package com.infilos.utils;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public final class Scheduler implements Loggable {

    public static Scheduler create(int threads, String threadNamePrefix, boolean daemon) {
        return new Scheduler(threads, threadNamePrefix, daemon);
    }

    public static Scheduler create(int threads, String threadNamePrefix) {
        return new Scheduler(threads, threadNamePrefix);
    }

    public static Scheduler create(int threads) {
        return new Scheduler(threads);
    }

    public static Scheduler create() {
        return new Scheduler(2);
    }

    private final int threads;
    private String threadNamePrefix = "scheduler";
    private boolean daemon = true;

    private ScheduledThreadPoolExecutor executor = null;
    private final AtomicInteger scheduledThreads = new AtomicInteger(0);

    private Scheduler(int threads, String threadNamePrefix, boolean daemon) {
        this.threads = threads;
        this.threadNamePrefix = threadNamePrefix;
        this.daemon = daemon;
    }

    private Scheduler(int threads, String threadNamePrefix) {
        this.threads = threads;
        this.threadNamePrefix = threadNamePrefix;
    }

    private Scheduler(int threads) {
        this.threads = threads;
    }

    public Scheduler startup() {
        log().debug("Initializing task scheduler.");
        synchronized (this) {
            if (isStarted()) {
                throw new IllegalStateException("This scheduler has already been started!");
            }
            executor = new ScheduledThreadPoolExecutor(threads);
            executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
            executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            executor.setThreadFactory(r -> {
                Integer id = scheduledThreads.getAndIncrement();
                Thread thread = new Thread(r, threadNamePrefix + id);
                thread.setDaemon(daemon);
                thread.setUncaughtExceptionHandler((t, e) -> {
                    log().error("Uncaught exception in scheduler thread '{}-{}':", threadNamePrefix, id, e);
                });
                return thread;
            });

            return this;
        }
    }

    public void resizeThreads(int newSize) {
        executor.setCorePoolSize(newSize);
    }

    public boolean isStarted() {
        synchronized (this) {
            return executor!=null;
        }
    }

    private void ensureRunning() {
        if (!isStarted()) {
            throw new IllegalStateException("Scheduler is not running.");
        }
    }

    public void shutdown() {
        log().debug("Shutting down task scheduler.");
        // We use the local variable to avoid NullPointerException if another thread shuts down scheduler at same time.
        ScheduledThreadPoolExecutor cached = this.executor;
        if (cached!=null) {
            synchronized (this) {
                cached.shutdown();
                this.executor = null;
            }
            try {
                cached.awaitTermination(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log().error("Await shutting down task scheduler failed, ", e);
            }
        }
    }

    public void scheduleOnce(String name, Runnable action, long delayInMills) {
        schedule(name, action, delayInMills, -1L, TimeUnit.MILLISECONDS);
    }

    public void scheduleOnce(String name, Runnable action, long delay, TimeUnit unit) {
        schedule(name, action, delay, -1L, unit);
    }

    public void schedule(String name, Runnable action, long delay, long interval, TimeUnit unit) {
        log().debug(String.format(
            "Scheduling task %s with initial delay %d ms and interval %d ms.",
            name, TimeUnit.MILLISECONDS.convert(delay, unit), TimeUnit.MILLISECONDS.convert(interval, unit))
        );

        synchronized (this) {
            ensureRunning();
            Runnable runnable = () -> {
                try {
                    log().debug("Beginning execution of scheduled task " + name);
                    action.run();
                } catch (Throwable ex) {
                    log().error("Uncaught exception in scheduled task " + name, ex);
                } finally {
                    log().debug("Completed execution of scheduled task " + name);
                }
            };
            if (interval > 0) {
                executor.scheduleAtFixedRate(runnable, delay, interval, unit);
            } else {
                executor.schedule(runnable, delay, unit);
            }
        }
    }
}
