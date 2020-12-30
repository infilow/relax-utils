package com.infilos.utils;

import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public final class Shutdown {
    private Shutdown() {
    }

    private static final Logger log = Loggable.logger(Shutdown.class);
    private static final ShutdownRegistry Registry = new ShutdownRegistry();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            int totaly = Registry.hooks.size();
            int succed = 0;

            log.info("Start invoke shutdown hooks, totaly {}.", totaly);

            Registry.shutdownInProgress.set(true);
            for (ShutdownHook hook : Registry.hooksInOrder()) {
                try {
                    hook.hook.run();
                    succed += 1;
                    log.info("ShutdownHook[{}] invoke succed.", hook.order);
                } catch (Throwable ex) {
                    log.error("ShutdownHook[{}] invoke failed.", hook.order, ex);
                }
            }
            log.info("Finished invoke shutdown hooks, totaly {}, succed {}.", totaly, succed);
        }));
    }

    public static void setup(int order, Runnable hook) {
        Registry.setup(hook, order);
    }

    public static boolean isShutingdown() {
        return Registry.isShutdownInProgress();
    }

    public static boolean remove(Runnable hook) {
        return Registry.remove(hook);
    }

    public static boolean contains(Runnable hook) {
        return Registry.contains(hook);
    }

    private static final class ShutdownRegistry {
        ShutdownRegistry() {
        }

        private final Set<ShutdownHook> hooks = Collections.synchronizedSet(new HashSet<>());
        private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);

        List<ShutdownHook> hooksInOrder() {
            List<ShutdownHook> list;

            synchronized (hooks) {
                list = new ArrayList<>(hooks);
            }

            list.sort(Comparator.comparingInt(h -> h.order));

            return new ArrayList<>(list);
        }

        /**
         * Adds a shutdownHook with a order, the smaller the order the earlier will run.
         * The same order can only be used by one hook.
         */
        public void setup(Runnable hook, int order) {
            if (hook==null) {
                throw new IllegalArgumentException("ShutdownHook cannot be NULL.");
            }
            if (Registry.hooks.stream().anyMatch(h -> h.order==order)) {
                throw new IllegalArgumentException("ShutdownHook order conflict: " + order);
            }
            if (shutdownInProgress.get()) {
                throw new IllegalStateException("Shutdown in progress, cannot add a hook.");
            }
            hooks.add(new ShutdownHook(hook, order));
        }

        public boolean remove(Runnable hook) {
            if (shutdownInProgress.get()) {
                throw new IllegalStateException("Shutdown in progress, cannot remove a hook.");
            }
            return hooks.remove(new ShutdownHook(hook, 0));
        }

        public boolean contains(Runnable shutdownHook) {
            return hooks.contains(new ShutdownHook(shutdownHook, 0));
        }

        public boolean isShutdownInProgress() {
            return shutdownInProgress.get();
        }
    }

    private static final class ShutdownHook {
        Runnable hook;
        int order;

        ShutdownHook(Runnable hook, int order) {
            this.hook = hook;
            this.order = order;
        }

        @Override
        public int hashCode() {
            return hook.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            boolean eq = false;
            if (obj!=null) {
                if (obj instanceof ShutdownHook) {
                    eq = (hook==((ShutdownHook) obj).hook);
                }
            }
            return eq;
        }
    }
}
