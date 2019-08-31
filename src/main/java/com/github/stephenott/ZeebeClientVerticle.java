package com.github.stephenott;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.ZeebeFuture;
import io.zeebe.client.api.command.ClientException;
import io.zeebe.client.api.command.ClientStatusException;
import io.zeebe.client.api.command.FinalCommandStep;
import io.zeebe.client.api.response.ActivateJobsResponse;
import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
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
    public void start(Promise<Void> startPromise) throws Exception {
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

        eb.<JsonObject>localConsumer("createJobConsumer", act -> {
            String jobType = act.body().getString("jobType");
            String consumerName = act.body().getString("consumerName");
            createJobConsumer(jobType, consumerName);
        });

        startPromise.complete();

        Thread.sleep(1500);

        deployWorkflow();

        Thread.sleep(1500);

        startWorkflowInstance();


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
                        pollResult.result().forEach(job -> {

                            handleJob(job).setHandler(result -> {
                                if (result.succeeded()) {
                                    //If the work was executed and returned a DoneJob
                                    DoneJob doneJob = result.result();
                                    if (doneJob.getResult().equals(DoneJob.Result.COMPLETE)) {

                                        //@TODO Move to method
                                        reportJobComplete(doneJob).setHandler(ar -> {
                                            if (ar.succeeded()) {
                                                log.info("Looping: " + jobType + "  " + workerName);
                                                createJobConsumerWithEb(jobType, workerName);
                                            } else {
                                                log.error("Unable to report job completion: " + job.toJson(), ar.cause());
                                            }
                                        });

                                    } else {
                                        //@TODO move to method
                                        reportJobFail(doneJob).setHandler(ar -> {
                                            if (ar.succeeded()) {
                                                log.info("Looping: " + jobType + "  " + workerName);
                                                createJobConsumerWithEb(jobType, workerName);
                                            } else {
                                                log.error("Unable to report job failure: " + job.toJson(), ar.cause());

                                            }
                                        });
                                    }
                                }
                            });
                        });
                    }
                } else {
//                    brkCmd.fail(pollResult.cause());
                }
            }); // End of Poll

//        }); // End of Breaker
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

                }, result -> {
                    if (result.succeeded()) {
                        handler.handle(Future.succeededFuture(result.result()));
                    } else {
                        handler.handle(Future.failedFuture(result.cause()));
                    }
                }

        );
    }


    private Future<DoneJob> handleJob(ActivatedJob job) {
        Promise<DoneJob> promise = Promise.promise();

        log.info("Handling Job");

        DeliveryOptions options = new DeliveryOptions().setSendTimeout(1200);

        String address = Common.JOB_ADDRESS_PREFIX + job.getType();
        JsonObject message = new ActivatedJobDto(job).toJsonObject();

        eb.<JsonObject>request(address, message, options, reply -> {
            if (reply.succeeded()) {
                log.info("Job work was done");
                try {
                    DoneJob doneJob = DoneJob.fromJsonObject(reply.result().body());
                    promise.complete(doneJob);
                } catch (Exception e){
                    log.error("JSON response from event bus could not be parsed into a DoneJob", e);
                    promise.fail(e);
                }
            } else {
                promise.fail(reply.cause());
            }
        });
        return promise.future();
    }


    private Future<Void> reportJobComplete(DoneJob doneJob) {
        Promise<Void> promise = Promise.promise();

        log.info("Reporting job is complete...");

        vertx.executeBlocking(blkProm -> {
            //@TODO Add support for variables and custom timeout configs
            // Variables are currently not supported do to complications with the builder
            ZeebeFuture<Void> completeCommandFuture = zClient
                    .newCompleteCommand(doneJob.getJobKey())
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


    private Future<Void> reportJobFail(DoneJob doneJob) {
        Promise<Void> promise = Promise.promise();

        vertx.executeBlocking(blkProm -> {
            ZeebeFuture<Void> failCommandFuture = zClient.newFailCommand(doneJob.getJobKey())
                    .retries(doneJob.getRetries())
                    .errorMessage(doneJob.getErrorMessage())
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


    private void deployWorkflow() {
        log.info("Deploying BPMN... ");

        DeploymentEvent deploymentEvent = zClient.newDeployCommand()
                .addResourceFile("./bpmn/bpmn1.bpmn")
                .send().join();

        log.info("Deployed BPMN: " + deploymentEvent.getKey());
    }

    private void startWorkflowInstance() {
        ZeebeFuture<WorkflowInstanceEvent> workflowInstanceEventFuture = zClient.newCreateInstanceCommand()
                .bpmnProcessId("Process_1")
                .latestVersion()
                .send();

        log.info("Starting workflow instance");

        WorkflowInstanceEvent workflowInstanceEvent = workflowInstanceEventFuture.join();

        log.info("Starting Workflow instance: " + workflowInstanceEvent.getWorkflowInstanceKey());
    }


}
