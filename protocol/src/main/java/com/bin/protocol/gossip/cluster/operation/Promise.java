package com.bin.protocol.gossip.cluster.operation;

import java.util.Arrays;

public class Promise<T> {


    private static final Object PROMISE_NOT_SET = new Object();

    private T result = (T) PROMISE_NOT_SET;

    private Callback<? super T> next;

    private long timeout = -1;

    private long start = System.currentTimeMillis();


    public Promise() {

    }

    public Promise(long timeout) {
        this.timeout = timeout;
    }




    public static Promise<Operation> ofOperation(long timeout) {
        return new Promise<Operation>(timeout);
    }

    public static Promise<Operation> ofOperation() {
        return new Promise<Operation>();
    }

    public void set(T result) {
        if (timeout > 0) {
            if (System.currentTimeMillis() - start > timeout)
                // 超时丢弃
                return;
        }
        complete(result);
    }


    public final boolean isComplete() {
        return result != PROMISE_NOT_SET;
    }

    public T getResult() {
        return result != PROMISE_NOT_SET ? result : null;
    }


    private void complete(T value){
        result = value;
        if (next != null) {
            next.execute(value);
        }
    }



    public void subscribe(Callback<? super T> callback) {
        if (next == null) {
            next = callback;
        } else if (next instanceof CallbackList) {
            ((CallbackList<T>) next).add(callback);
        } else {
            next = new CallbackList<>(next, callback);
        }
    }


    private static class CallbackList<T> implements Callback<T> {
        private int index = 2;
        private Callback<? super T>[] callbacks = new Callback[4];

        public CallbackList(Callback<? super T> first, Callback<? super T> second) {
            callbacks[0] = first;
            callbacks[1] = second;
        }

        public void add(Callback<? super T> callback) {
            if (index == callbacks.length) {
                callbacks = Arrays.copyOf(callbacks, callbacks.length * 2);
            }
            callbacks[index++] = callback;
        }

        @Override
        public void execute(T result) {
            for (int i = 0; i < index; i++) {
                callbacks[i].execute(result);
            }
        }
    }
}
