package com.github.stephenott.zeebe.client;

import com.github.stephenott.common.Common;
import com.github.stephenott.executors.JobResult;
import com.github.stephenott.conf.ApplicationConfiguration;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.ZeebeFuture;
import io.zeebe.client.api.command.ClientException;
import io.zeebe.client.api.command.ClientStatusException;
import io.zeebe.client.api.command.FinalCommandStep;
import io.zeebe.client.api.response.ActivateJobsResponse;
import io.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;


public class ZeebeClientVerticle extends AbstractVerticle {

    private Logger log;
    private EventBus eb;

    private ZeebeClient zClient;

    private ApplicationConfiguration.ZeebeClientConfiguration clientConfiguration;

    private CircuitBreaker pollingBreaker;

    @Override
    public void start() throws Exception {
        clientConfiguration = config().mapTo(ApplicationConfiguration.ZeebeClientConfiguration.class);

        log = LoggerFactory.getLogger("ClientVerticle." + clientConfiguration.getName());

        pollingBreaker = CircuitBreaker.create("Breaker.ClientVerticle." + clientConfiguration.getName(), vertx,
                new CircuitBreakerOptions()
                        .setMaxFailures(5)
                        .setTimeout(-1) // Timeout is currently managed by the zeebeClient usage and futures
                        .setFailuresRollingWindow(Duration.ofHours(1).toMillis())
                        .setResetTimeout(Duration.ofMinutes(30).toMillis())
        );

        eb = vertx.eventBus();

        zClient = createZeebeClient(clientConfiguration);

        createJobCompletionConsumer();

        eb.<JsonObject>localConsumer("createJobConsumer", act -> {
            String jobType = act.body().getString("jobType");
            String consumerName = act.body().getString("consumerName");
            createJobConsumer(jobType, consumerName);
        });

        // Consumers are equal to "Zeebe Workers"
        clientConfiguration.getWorkers().forEach(consumer -> {
            consumer.getJobTypes().forEach(jobType -> {
                JsonObject body = new JsonObject()
                        .put("jobType", jobType)
                        .put("consumerName", consumer.getName());

                DeliveryOptions options = new DeliveryOptions().setLocalOnly(true);
                eb.send("createJobConsumer", body, options);
            });
        });
    }


    private ZeebeClient createZeebeClient(ApplicationConfiguration.ZeebeClientConfiguration configuration) {
        log.info("Creating ZeebeClient for " + clientConfiguration.getName());

        return ZeebeClient.newClientBuilder()
                .brokerContactPoint(configuration.getBrokerContactPoint())
                .defaultRequestTimeout(Duration.ofMinutes(1L))
                .defaultMessageTimeToLive(Duration.ofHours(1))
                .usePlaintext() //@TODO remove and replace with cert  /-/SECURITY/-/
                .build();
    }

    @Override
    public void stop() throws Exception {
        zClient.close();
    }

    private void createJobConsumerWithEb(String jobType, String workerName) {
        JsonObject body = new JsonObject()
                .put("jobType", jobType)
                .put("consumerName", workerName);

        DeliveryOptions options = new DeliveryOptions().setLocalOnly(true);
        eb.publish("createJobConsumer", body, options);
    }

    private void createJobConsumer(String jobType, String workerName) {

//        pollingBreaker.execute(brkCmd -> {

            pollForJobs(jobType, workerName, pollResult -> {

                if (pollResult.succeeded()) {
//                    brkCmd.complete();

                    // If no results from poll:
                    if (pollResult.result().isEmpty()) {
//                        brkCmd.complete();
                        log.info(workerName + " found NO Jobs for " + jobType + ", looping...");

                        createJobConsumerWithEb(jobType, workerName);

                        //If found jobs in the results of the poll:
                    } else {
                        log.info(workerName + " found some Jobs for " + jobType + ", count: " + pollResult.result().size());

                        //For Each Job that was returned
                        pollResult.result().forEach(this::handleJob);

                        log.info("Done handling jobs....");

                        createJobConsumerWithEb(jobType, workerName); //Basically a non-blocking loop
                    }
                } else {
//                    brkCmd.fail(pollResult.cause());
                }
            }); // End of Poll

//        }); // End of Breaker
    }

    private void createJobCompletionConsumer(){
        eb.<JsonObject>consumer(clientConfiguration.getName() + ".job.completion").handler(msg->{
            JobResult jobResult = msg.body().mapTo(JobResult.class);

            if (jobResult.getResult().equals(JobResult.Result.COMPLETE)){
                reportJobComplete(jobResult);
            } else {
                reportJobFail(jobResult);
            }
        });
    }

    private void pollForJobs(String jobType, String workerName, Handler<AsyncResult<List<ActivatedJob>>> handler) {
        log.info("Starting Activate Jobs Command for: " + jobType + "  " + workerName);

        //Convert to a dedicated Verticle / Thread Worker management
        vertx.<List<ActivatedJob>>executeBlocking(blockProm -> {
                    log.info(workerName + " is waiting for " + jobType + " jobs");

                    try {
                        FinalCommandStep<ActivateJobsResponse> finalCommandStep = zClient.newActivateJobsCommand()
                                .jobType(jobType)
                                .maxJobsToActivate(1)
                                .workerName(workerName)
                                .requestTimeout(Duration.ofMinutes(1L));

                        ZeebeFuture<ActivateJobsResponse> jobsResponse = finalCommandStep.send();

                        blockProm.complete(jobsResponse.join().getJobs());

                    } catch (Exception e) {
                        blockProm.fail(e);
                    }
                }, false, result -> {
                    if (result.succeeded()) {
                        handler.handle(Future.succeededFuture(result.result()));
                    } else {
                        handler.handle(Future.failedFuture(result.cause()));
                    }
                }

        );
    }

    private void handleJob(ActivatedJob job) {
        log.info("Handling Job... {}", job.getKey());

        DeliveryOptions options = new DeliveryOptions().setSendTimeout(1200)
                .addHeader("sourceClient", clientConfiguration.getName());
        JsonObject object = (JsonObject) Json.decodeValue(job.toJson());

        log.info("OBJECT:--> {}", object.toString());

        String address = Common.JOB_ADDRESS_PREFIX + job.getType();
        log.info("Sending Job work to address: {}", address);

        eb.send(address, object, options);
    }

    private Future<Void> reportJobComplete(JobResult jobResult) {
        Promise<Void> promise = Promise.promise();

        log.info("Reporting job is complete... {}", jobResult.getJobKey());

        vertx.executeBlocking(blkProm -> {
            //@TODO Add support for variables and custom timeout configs
            // Variables are currently not supported do to complications with the builder
            ZeebeFuture<Void> completeCommandFuture = zClient
                    .newCompleteCommand(jobResult.getJobKey())
                    .send();

            log.info("Sending Complete Command to Zeebe");

            try {
                completeCommandFuture.join();

                log.info("Complete Command was successfully sent");

                blkProm.complete();

            } catch (ClientStatusException e) {
                blkProm.fail(e);
            } catch (ClientException e) {
                blkProm.fail(e);
            }

        }, false, res -> {
            if (res.succeeded()) {
                promise.complete();

            } else {
                promise.fail(res.cause());

            }
        });

        return promise.future();
    }


    private Future<Void> reportJobFail(JobResult jobResult) {
        Promise<Void> promise = Promise.promise();

        vertx.executeBlocking(blkProm -> {
            ZeebeFuture<Void> failCommandFuture = zClient.newFailCommand(jobResult.getJobKey())
                    .retries(jobResult.getRetries())
                    .errorMessage(jobResult.getErrorMessage())
                    .send();

            log.info("Sending Fail-Command to Zeebe");

            try {
                failCommandFuture.join();

                log.info("Fail-Command was successfully sent");

                blkProm.complete();

            } catch (ClientStatusException e) {
                blkProm.fail(e);
            } catch (ClientException e) {
                blkProm.fail(e);
            }

        }, false, res -> {
            if (res.succeeded()) {
                //ExecuteBlocking was successfully completed
                promise.complete();

            } else {
                // Error in the execute blocking
                promise.fail(res.cause());

            }
        });

        return promise.future();
    }

}