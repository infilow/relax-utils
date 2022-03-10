package com.infilos.retry.execute;

import com.infilos.retry.Retry;
import com.infilos.retry.RetryConfig;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SyncExecutorTest_ListenersTest {

    @Mock
    private DummyMock dummyMock;

    private Callable<String> callable;
    private RetryConfig<String> config;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);

        callable = () -> dummyMock.callableCallThis();

        config = Retry.config(String.class)
            .retryOnAnyError()
            .withMaxAttempts(5)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();
    }

    @Test
    public void verifyAfterFailedListener() {
        when(dummyMock.callableCallThis())
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException())
            .thenReturn("success!");

        Retry.runSync(
            config.builder().listenAfterFailTry(
                status -> dummyMock.listenersCallThis()
            ).build(),
            callable
        );

        verify(dummyMock, timeout(1000).times(2)).listenersCallThis();
    }

    @Test
    public void verifyAfterFailedListener_populatesException() {
        when(dummyMock.callableCallThis())
            .thenThrow(new IllegalArgumentException())
            .thenReturn("success!");

        Retry.runSync(
            config.builder().listenAfterFailTry(
                status -> dummyMock.listenersCallThis(status.getLastException())
            ).build(),
            callable
        );

        verify(dummyMock, timeout(1000).times(1)).listenersCallThis(isA(IllegalArgumentException.class));
    }

    @Test
    public void verifyBeforeNextTryListener() {
        when(dummyMock.callableCallThis())
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException())
            .thenReturn("success!");

        Retry.runSync(
            config.builder().listenBeforeNextTry(
                status -> dummyMock.listenersCallThis()
            ).build(),
            callable
        );

        verify(dummyMock, timeout(1000).times(2)).listenersCallThis();
    }

    @Test
    public void verifyBeforeNextTryListener_NotCalledAfterLastFailure() {
        when(dummyMock.callableCallThis())
            .thenThrow(new NullPointerException())
            .thenThrow(new NullPointerException())
            .thenThrow(new NullPointerException())
            .thenThrow(new NullPointerException())
            .thenThrow(new IllegalArgumentException());

        try {
            Retry.runSync(
                config.builder().listenBeforeNextTry(
                    status -> dummyMock.listenersCallThis(status.getLastException())
                ).build(),
                callable
            );
        } catch (Exception ignored) {
        }

        // the beforeNextTry listener should only be called before a retry (5 attempts == 4 retries)
        verify(dummyMock, timeout(1000).times(4)).listenersCallThis(isA(NullPointerException.class));
        verify(dummyMock, after(1000).never()).listenersCallThis(isA(IllegalArgumentException.class));
    }

    @Test
    public void verifyOnSuccessListener() {
        when(dummyMock.callableCallThis())
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException())
            .thenReturn("success!");

        Retry.runSync(
            config.builder().listenSuccedTry(
                status -> dummyMock.listenersCallThis()
            ).build(),
            callable
        );

        verify(dummyMock, timeout(1000).times(1)).listenersCallThis();
    }

    @Test
    public void verifyOnFailureListener() {
        when(dummyMock.callableCallThis())
            .thenThrow(new RuntimeException());

        Retry.runSync(
            config.builder().listenFailedTry(
                status -> dummyMock.listenersCallThis()
            ).build(),
            callable
        );

        verify(dummyMock, timeout(1000).times(1)).listenersCallThis();
    }

    @Test
    public void verifyOnCompletionListener_isCalledAfterSuccess() {
        when(dummyMock.callableCallThis())
            .thenReturn("success!");

        Retry.runSync(
            config.builder().listenCompleteTry(
                status -> dummyMock.listenersCallThis()
            ).build(),
            callable
        );

        verify(dummyMock, timeout(1000).times(1)).listenersCallThis();
    }

    @Test
    public void verifyOnCompletionListener_isCalledAfterFailure() {
        when(dummyMock.callableCallThis())
            .thenThrow(new RuntimeException());

        try {
            Retry.runSync(
                config.builder().listenCompleteTry(
                    status -> dummyMock.listenersCallThis()
                ).build(),
                callable
            );
        } catch (Exception e) {
        }

        verify(dummyMock, timeout(1000).times(1)).listenersCallThis();
    }

    @Test
    public void verifyChainedListeners_successImmediately() {
        Retry.runSync(
            config.builder()
                .listenSuccedTry(status -> dummyMock.listenersCallThis())
                .listenFailedTry(status -> dummyMock.listenersCallThis())
                .listenCompleteTry(status -> dummyMock.listenersCallThis())
                .listenAfterFailTry(status -> dummyMock.listenersCallThis())
                .listenBeforeNextTry(status -> dummyMock.listenersCallThis())
                .build(),
            callable
        );

        //only success and completion should wind up being called
        verify(dummyMock, timeout(1000).times(2)).listenersCallThis();
    }

    @Test
    public void verifyChainedListeners_successAfterRetries() {
        when(dummyMock.callableCallThis())
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException())
            .thenReturn("success!");

        Retry.runSync(
            config.builder()
                .listenSuccedTry(status -> dummyMock.listenersCallThis())
                .listenFailedTry(status -> dummyMock.listenersCallThis())
                .listenCompleteTry(status -> dummyMock.listenersCallThis())
                .listenAfterFailTry(status -> dummyMock.listenersCallThis())
                .listenBeforeNextTry(status -> dummyMock.listenersCallThis())
                .build(),
            callable
        );

        //only calls success once, completion once and the retry listeners 3 times each
        verify(dummyMock, timeout(1000).times(8)).listenersCallThis();
    }

    @Test
    public void verifyOnFailureListener_populatesException() {
        when(dummyMock.callableCallThis())
            .thenThrow(new RuntimeException())
            .thenThrow(new IllegalArgumentException());

        Retry.runSync(
            config.builder()
                .listenFailedTry(status -> dummyMock.listenersCallThis(status.getLastException()))
                .build(),
            callable
        );

        verify(dummyMock, timeout(1000)).listenersCallThis(isA(IllegalArgumentException.class));
    }

    @Test
    public void verifyOnListener_resultHasTypeOfCallExecutor() {
        List<String> methodCalls = new ArrayList<>();
        
        Retry.runSync(
            config.builder()
                .listenSuccedTry(status -> {
                    methodCalls.add("onSuccess");
                    assertThat(status.getResult()).isInstanceOf(String.class);
                })
                .listenCompleteTry(status -> {
                    methodCalls.add("onCompletion");
                    assertThat(status.getResult()).isInstanceOf(String.class);
                })
                .build(),
            () -> ""
        );
        assertThat(methodCalls).contains("onSuccess", "onCompletion");
    }

    private class DummyMock {

        public String listenersCallThis() {
            return "this is to use to verify listeners call the mock";
        }

        public String listenersCallThis(Exception e) {
            return "this is to verify exceptions in the after failed call listener";
        }

        public String callableCallThis() {
            return "this is to use for mocking the executed callable";
        }
    }
}