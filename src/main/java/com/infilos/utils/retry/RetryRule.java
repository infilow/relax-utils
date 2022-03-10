package com.infilos.utils.retry;

import com.infilos.utils.Require;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class RetryRule<T> {
    private final Predicate<? super Throwable> condition;
    private final List<? extends T> strategies;
    private final int currentStrategyIndex;

    private RetryRule(Predicate<? super Throwable> condition, List<? extends T> strategies, int index) {
        Require.checkNotNull(condition);
        Require.checkNotNull(strategies);
        this.condition = condition;
        this.strategies = strategies;
        this.currentStrategyIndex = index;
    }

    RetryRule(Predicate<? super Throwable> condition, List<? extends T> strategies) {
        this(condition, strategies, 0);
    }

    /**
     * Check if this rule match hint the exception.
     */
    boolean checkException(Throwable exception) {
        return condition.test(exception);
    }

    /**
     * Return new rule with remaining strategies.
     */
    RetryRule<T> remaining() {
        return new RetryRule<>(condition, strategies, currentStrategyIndex + 1);
    }

    Optional<T> currentStrategy() {
        if (currentStrategyIndex >= strategies.size()) {
            return Optional.empty();
        }
        
        try {
            return Optional.of(strategies.get(currentStrategyIndex));
        } catch (IndexOutOfBoundsException ignore) {
            // In case the list just changed due to race condition or side-effects.
            return Optional.empty();
        }
    }
}
