package com.github.stephenott.usertask.mongo;

import com.mongodb.MongoTimeoutException;
import com.mongodb.reactivestreams.client.Success;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class Subscribers {

    private Subscribers() {
    }

    /**
     * A Subscriber that stores the publishers results and provides a latch so can block on completion.
     *
     * @param <T> The publishers result type
     */
    public static class ObservableSubscriber<T> implements Subscriber<T> {
        private final List<T> received;
        private final List<Throwable> errors;
        private final CountDownLatch latch;
        private volatile Subscription subscription;
        private volatile boolean completed;

        ObservableSubscriber() {
            this.received = new ArrayList<>();
            this.errors = new ArrayList<>();
            this.latch = new CountDownLatch(1);
        }

        @Override
        public void onSubscribe(final Subscription s) {
            subscription = s;
        }

        @Override
        public void onNext(final T t) {
            received.add(t);
        }

        @Override
        public void onError(final Throwable t) {
            errors.add(t);
            onComplete();
        }

        @Override
        public void onComplete() {
            completed = true;
            latch.countDown();
        }

        public Subscription getSubscription() {
            return subscription;
        }

        public List<T> getReceived() {
            return received;
        }

        public Throwable getError() {
            if (errors.size() > 0) {
                return errors.get(0);
            }
            return null;
        }

        public boolean isCompleted() {
            return completed;
        }

        public List<T> get(final long timeout, final TimeUnit unit) throws Throwable {
            return await(timeout, unit).getReceived();
        }

        public ObservableSubscriber<T> await() throws Throwable {
            return await(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        }

        public ObservableSubscriber<T> await(final long timeout, final TimeUnit unit) throws Throwable {
            subscription.request(Integer.MAX_VALUE);
            if (!latch.await(timeout, unit)) {
                throw new MongoTimeoutException("Publisher onComplete timed out");
            }
            if (!errors.isEmpty()) {
                throw errors.get(0);
            }
            return this;
        }
    }


    public static class AsyncResultSubscriber<T> extends ObservableSubscriber<T> {

        private Promise<List<T>> onCompletePromise = Promise.promise();
        private Promise<Void> onSubscribePromise = Promise.promise();
        private Promise<Void> onRequestPromise = Promise.promise();
        private boolean shouldGetAllResultsOnSubscribe = true;

        public AsyncResultSubscriber() {
        }

        /**
         * Helper for determining if the received result is a Void result or in Java Reactive Stream Mongo terms, a "Success" object.
         * If onCompleted has not been received, then will return false.
         * @return boolean indicating if its a Success.class result
         */
        public static boolean isSuccessResult(List received){
            return received.size() == 1 &&
                    received.get(0).getClass().isAssignableFrom(Success.class);
        }

        public AsyncResultSubscriber(Handler<AsyncResult<List<T>>> onCompleteHandler) {
            this.onCompletePromise.future().setHandler(onCompleteHandler);
        }

        public AsyncResultSubscriber<T> setOnCompleteHandler(Handler<AsyncResult<List<T>>> handler){
            this.onCompletePromise.future().setHandler(handler);
            return this;
        }

        public AsyncResultSubscriber<T> setOnSubscribeHandler(Handler<AsyncResult<Void>> handler){
            this.onSubscribePromise.future().setHandler(handler);
            return this;
        }

        public AsyncResultSubscriber<T> shouldGetAllResultsOnSubscribe(boolean shouldGetAllResultsOnSubscribe){
            this.shouldGetAllResultsOnSubscribe = shouldGetAllResultsOnSubscribe;
            return this;
        }

        @Override
        public void onComplete() {
            super.onComplete();
            if (getError() == null){
                this.onCompletePromise.complete(getReceived());
            } else {
                this.onCompletePromise.fail(getError());
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            super.onSubscribe(s);
            this.onSubscribePromise.complete();

            if (shouldGetAllResultsOnSubscribe){
                s.request(Integer.MAX_VALUE);
                onRequestPromise.complete();
            }

        }
    }

}
