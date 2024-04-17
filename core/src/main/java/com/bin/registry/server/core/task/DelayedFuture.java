package com.bin.registry.server.core.task;

import com.bin.registry.server.core.timer.DelayedOperation;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DelayedFuture extends DelayedOperation {

    private List<CompletableFuture> futures;

    private Consumer<?> responseCallback;

    public DelayedFuture(long delayMs,List<CompletableFuture> futures,Consumer<?> responseCallback) {
        super(delayMs);
        this.futures = futures;
        this.responseCallback = responseCallback;
    }

    @Override
    public void onExpiration() {

    }

    @Override
    public void onComplete() {
        List<CompletableFuture> pendingFutures = futures.stream().filter(future -> !future.isDone()).collect(Collectors.toList());

        // trace("Completing operation for ${futures.size} futures, expired ${pendingFutures.size}")

        pendingFutures.forEach(cf -> {
            cf.completeExceptionally(new TimeoutException("Request has been timed out after " + getDelayMs() + " ms"));
        });
        responseCallback.accept(null);
    }

    @Override
    public boolean tryComplete() {
        long pending = futures.stream().filter(future -> future.isDone()).count();
        if (pending == 0) {
            //  trace("All futures have been completed or have errors, completing the delayed operation")
            return forceComplete();
        } else {
            //   trace(s"$pending future still pending, not completing the delayed operation")
            return false;
        }
    }


}
