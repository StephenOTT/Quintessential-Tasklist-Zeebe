package com.github.stephenott.usertask.mongo;

import com.mongodb.reactivestreams.client.Success;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;

public class Subscribers {

    public static class ObservableSubscriber<T> implements Subscriber<T> {
        private Promise<T> onNextPromise = Promise.promise();
        private Promise<Void> onCompletePromise = Promise.promise();
        private Promise<Subscription> onSubscribePromise = Promise.promise();
        private Handler<AsyncResult<T>> onNextHandler;
        private Subscription subscription;
        private long batchSize = 5;

        private ObservableSubscriber() {
        }

        public ObservableSubscriber(Handler<AsyncResult<Subscription>> onSubscribeHandler, Handler<AsyncResult<T>> onNextHandler, Handler<AsyncResult<Void>> onCompleteHandler) {
            this.onSubscribePromise.future().setHandler(onSubscribeHandler);
            this.onNextHandler = onNextHandler;
            this.onNextPromise.future().setHandler(onNextHandler);
            this.onCompletePromise.future().setHandler(onCompleteHandler);
        }


        @Override
        public void onSubscribe(final Subscription s) {
            this.subscription = s;
            this.onSubscribePromise.complete(s);
        }

        public Subscription getSubscription() {
            return subscription;
        }

        public ObservableSubscriber<T> setSubscription(Subscription subscription) {
            this.subscription = subscription;
            return this;
        }

        @Override
        public void onNext(final T t) {
            onNextPromise.complete(t);
            onNextPromise = Promise.promise();
            onNextPromise.future().setHandler(this.onNextHandler);
        }

        public ObservableSubscriber<T> onNextDo(Handler<AsyncResult<T>> handler) {
            this.onNextHandler = handler;
            return this;
        }

        @Override
        public void onError(final Throwable t) {
            this.onCompletePromise.fail(t);
        }

        @Override
        public void onComplete() {
            this.onCompletePromise.complete();
        }

        public long getBatchSize() {
            return batchSize;
        }

        public ObservableSubscriber<T> setBatchSize(long batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public ObservableSubscriber<T> cancelSubscriptionPromise(Promise<Void> cancelPromise) {
            cancelPromise.future().setHandler(action -> {
                if (action.succeeded()) {
                    this.subscription.cancel();
                } else {
                    this.subscription.cancel();
                }
            });
            return this;
        }
    }

    public static class SuccessSubscriber extends ObservableSubscriber<Success> {
        List<Success> received = new ArrayList<>();
        private ObservableSubscriber<Success> observableSubscriber;

        public SuccessSubscriber(Handler<AsyncResult<Success>> resultHandler) {

            observableSubscriber = new ObservableSubscriber<>(
                    onSubscribe -> {
                        if (onSubscribe.succeeded()) {
                            onSubscribe.result().request(getBatchSize());
                        } else {
                            throw new IllegalStateException("Internal Failure: OnSubscribe promise failed.");
                        }
                    },
                    onNext -> {
                        if (onNext.succeeded()) {
                            received.add(onNext.result());
                        } else {
                            throw new IllegalStateException("Internal Failure: OnNext promise failed.");
                        }
                    },
                    onComplete -> {
                        if (onComplete.succeeded()) {
                            if (received.size() == 1) {
                                if (received.get(0).getClass().isAssignableFrom(Success.class)) {
                                    resultHandler.handle(Future.succeededFuture(received.get(0)));
                                } else {
                                    resultHandler.handle(Future.failedFuture(new IllegalStateException("Success was not returned.")));
                                }
                            } else {
                                resultHandler.handle(Future.failedFuture(new IllegalStateException("More than 1 Success was returned, but was only expecting 1.")));
                            }
                        } else {
                            resultHandler.handle(Future.failedFuture(onComplete.cause()));
                        }
                    });
        }

        public ObservableSubscriber<Success> getObservableSubscriber() {
            return observableSubscriber;
        }
    }

    public static class SimpleListSubscriber<T> extends ObservableSubscriber<T> {
        private List<T> received = new ArrayList<>();
        private ObservableSubscriber<T> observableSubscriber;

        private SimpleListSubscriber() {
        }

        public SimpleListSubscriber(Handler<AsyncResult<List<T>>> resultHandler) {
            this.observableSubscriber = new ObservableSubscriber<>(
                    onSubscribe -> {
                        if (onSubscribe.succeeded()) {
                            onSubscribe.result().request(getBatchSize());
                        } else {
                            throw new IllegalStateException("Internal Failure: OnSubscribe promise failed.");
                        }
                    },
                    onNext -> {
                        if (onNext.succeeded()) {
                            received.add(onNext.result());
                            getSubscription().request(getBatchSize());
                        } else {
                            throw new IllegalStateException("Internal Failure: OnNext promise failed.");
                        }
                    },
                    onComplete -> {
                        if (onComplete.succeeded()) {
                            resultHandler.handle(Future.succeededFuture(received));

                        } else {
                            resultHandler.handle(Future.failedFuture(onComplete.cause()));
                        }
                    });
        }

        public ObservableSubscriber<T> getObservableSubscriber() {
            return observableSubscriber;
        }
    }

    public static class SimpleSingleResultSubscriber<T> extends SimpleListSubscriber<T>{

        public SimpleSingleResultSubscriber(Handler<AsyncResult<T>> resultHandler) {
            super.observableSubscriber = new ObservableSubscriber<>(
                    onSubscribe -> {
                        if (onSubscribe.succeeded()) {
                            System.out.println("SUBSCRIBED!!");
                            onSubscribe.result().request(getBatchSize());
                        } else {
                            throw new IllegalStateException("Internal Failure: OnSubscribe promise failed.");
                        }
                    },
                    onNext -> {
                        if (onNext.succeeded()) {
                            System.out.println("ON-NEXT!!");
                            super.received.add(onNext.result());
                            super.getSubscription().request(getBatchSize());
                        } else {
                            throw new IllegalStateException("Internal Failure: OnNext promise failed.");
                        }
                    },
                    onComplete -> {
                        if (onComplete.succeeded()) {
                            if (super.received.size() == 1){
                                resultHandler.handle(Future.succeededFuture(super.received.get(0)));
                            } else {
                                resultHandler.handle(Future.failedFuture(new IllegalStateException("More than 1 result was returned from DB, but only expected 1.")));
                            }

                        } else {
                            resultHandler.handle(Future.failedFuture(onComplete.cause()));
                        }
                    });
        }
    }
}
