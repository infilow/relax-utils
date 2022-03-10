package com.infilos.utils.retry;

import com.infilos.api.Maybe;
import com.infilos.api.Predicates;
import com.infilos.utils.Require;

import java.util.*;
import java.util.function.Predicate;

public class RetryPlan<T> {
    private final List<RetryRule<T>> rules;

    public RetryPlan() {
        this.rules = Collections.emptyList();
    }

    private RetryPlan(List<RetryRule<T>> rules) {
        this.rules = rules;
    }

    /**
     * Devise the plan with new condition and corresponding strategies.
     */
    public RetryPlan<T> devise(Predicate<? super Throwable> condition, List<? extends T> strategies) {
        return new RetryPlan<>(new ArrayList<RetryRule<T>>() {{
            addAll(rules);
            add(new RetryRule<>(condition, strategies));
        }});
    }

    /**
     * Devise the plan with new cause type and corresponding strategies.
     */
    public RetryPlan<T> devise(Class<? extends Throwable> causeType, List<? extends T> strategies) {
        return devise(causeType::isInstance, strategies);
    }

    /**
     * Devise the plan with new cause type, condition and corresponding strategies.
     */
    public <E extends Throwable> RetryPlan<T> devise(Class<E> causeType, Predicate<? super E> condition, List<? extends T> strategies) {
        return devise(Predicates.ofTyped(causeType, condition), strategies);
    }

    /**
     * Executes the plan and either returns an {@link RetryExecution} or throws {@code E} if the exception isn't covered by the plan or the plan decides to propagate.
     */
    public <E extends Throwable> Maybe<RetryExecution<T>, E> execute(E exception) {
        Require.checkNotNull(exception);

        RetryRule<T> applicableRule = null;
        List<RetryRule<T>> remainingRules = new ArrayList<>();

        for (RetryRule<T> rule : rules) {
            if (applicableRule == null && rule.checkException(exception)) {
                applicableRule = rule;
                remainingRules.add(rule.remaining());
            } else {
                remainingRules.add(rule);
            }
        }

        // no rule match the exception
        if (applicableRule == null) {
            return Maybe.except(exception);
        }

        return applicableRule.currentStrategy()
            .map(strategy -> new RetryExecution<>(strategy, new RetryPlan<>(remainingRules)))
            .map(Maybe::<RetryExecution<T>, E>of)
            .orElse(Maybe.except(exception));  // The rule refuses to handle it.
    }

    /**
     * Check if plan covers the exception.
     */
    public boolean anyMatches(Throwable exception) {
        Require.checkNotNull(exception);

        return rules.stream().anyMatch(rule -> rule.checkException(exception));
    }
}
