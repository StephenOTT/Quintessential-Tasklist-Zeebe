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

        private ObservableSubscriber() {
        }


        private ObservableSubscriber<T> setOnSubscribeHandler(Handler<AsyncResult<Subscription>> onSubscribeHandler){
            this.onSubscribePromise.future().setHandler(onSubscribeHandler);
            return this;
        }

        private ObservableSubscriber<T> setOnNextHandler(Handler<AsyncResult<T>> onNextHandler){
            this.onNextHandler = onNextHandler;
            this.onNextPromise.future().setHandler(onNextHandler);
            return this;
        }

        private ObservableSubscriber<T> setOnCompleteHandler(Handler<AsyncResult<Void>> onCompleteHandler){
            this.onCompletePromise.future().setHandler(onCompleteHandler);
            return this;
        }

        @Override
        public void onSubscribe(final Subscription s) {
            this.subscription = s;
            this.onSubscribePromise.complete(s);
        }

        @Override
        public void onNext(final T t) {
            onNextPromise.complete(t);
            onNextPromise = Promise.promise();
            onNextPromise.future().setHandler(this.onNextHandler);
        }

        @Override
        public void onError(final Throwable t) {
            this.onCompletePromise.fail(t);
        }

        @Override
        public void onComplete() {
            this.onCompletePromise.complete();
        }

        public Subscription getSubscription() {
            return subscription;
        }

        public ObservableSubscriber<T> setCancelSubscribtionTrigger(Promise<Void> cancelTrigger, Handler<AsyncResult<Void>> resultHandler){
            setCancelSubscriptionTrigger(cancelTrigger).setHandler(resultHandler);
            return this;
        }

        public Future<Void> setCancelSubscriptionTrigger(Promise<Void> cancelTrigger){
            Promise<Void> cancelPromise = Promise.promise();

            cancelPromise.future().setHandler(ar -> {
                if (ar.succeeded()){
                    try {
                        getSubscription().cancel();
                        cancelPromise.complete();
                    } catch (Exception e){
                        cancelPromise.fail(new IllegalStateException("Unable to cancel promise", e));
                    }
                }
            });
            return cancelPromise.future();
        }

        public Future<Void> cancelSubscription() {
            Promise<Void> cancelSubscriptionPromise = Promise.promise();

            try {
                getSubscription().cancel();
                cancelSubscriptionPromise.complete();
            } catch (Exception e){
                cancelSubscriptionPromise.fail(new IllegalStateException("Unable to cancel subscription", e));
            }
            return cancelSubscriptionPromise.future();
        }
    }

    public static class SimpleSubscriber<T> extends ObservableSubscriber<T> {
        private List<T> received = new ArrayList<>();
        private Promise<List<T>> onCompleteListPromise = Promise.promise();
        private long batchSize = 5;
        private int receivedLimit = Integer.MAX_VALUE;

        public SimpleSubscriber(){
            super.setOnSubscribeHandler(this::onSubHandler)
                    .setOnNextHandler(this::onNextHandler)
                    .setOnCompleteHandler(this::onCompleteHandler);
        }

        public SimpleSubscriber(Handler<AsyncResult<List<T>>> resultHandler) {
            this();
            onCompleteListPromise.future().setHandler(resultHandler);
        }

        public SimpleSubscriber<T> singleResult(Handler<AsyncResult<T>> resultHandler){
            Promise<T> singleResultPromise = Promise.promise();
            singleResultPromise.future().setHandler(resultHandler);

            setReceivedLimit(1);

            onCompleteListPromise.future().setHandler(asyncResult -> {
                if (asyncResult.succeeded()){
                    try {
                        singleResultPromise.complete(getReceived().get(0));
                    } catch (Exception e){
                        singleResultPromise.fail(new IllegalStateException("Unable to complete single result request", e));
                    }
                } else {
                    singleResultPromise.fail(new IllegalStateException("Async Result failed for On Complete Promise", asyncResult.cause()));
                }
            });
            return this;
        }

        private void onSubHandler(AsyncResult<Subscription> asyncResult){
            if (asyncResult.succeeded()) {
                getSubscription().request(getBatchSize());
            } else {
                onCompleteListPromise.fail(new IllegalStateException("On Subscribe Handler failed.", asyncResult.cause()));
            }
        }

        private void onNextHandler(AsyncResult<T> asyncResult){
            if (asyncResult.succeeded()){
                try {
                    getReceived().add(asyncResult.result());
                } catch (Exception e){
                    getSubscription().cancel();
                    onCompleteListPromise.fail(new IllegalStateException("On Next Handler failed because could not add more items to received list", e));
                }
            } else {
                onCompleteListPromise.fail(new IllegalStateException("On Next handler failed.", asyncResult.cause()));
            }
        }

        private void onCompleteHandler(AsyncResult<Void> asyncResult){
            if (asyncResult.succeeded()){
                int max = getReceivedLimit();

                if (getReceived().size() <= max){
                    onCompleteListPromise.complete(getReceived());
                } else {
                    onCompleteListPromise.fail(new IllegalStateException("Received " + getReceived().size() + " results, but received limit was: " + max));
                }
            } else {
                onCompleteListPromise.fail(new IllegalStateException("On Complete Handler failed.", asyncResult.cause()));
            }
        }

        public List<T> getReceived() {
            return received;
        }

        public long getBatchSize() {
            return batchSize;
        }

        public SimpleSubscriber<T> setBatchSize(long batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public int getReceivedLimit() {
            return receivedLimit;
        }

        public SimpleSubscriber<T> setReceivedLimit(int receivedLimit) {
            this.receivedLimit = receivedLimit;
            return this;
        }
    }
}
