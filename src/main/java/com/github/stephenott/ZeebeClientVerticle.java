package com.github.stephenott;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.ZeebeFuture;
import io.zeebe.client.api.command.ClientException;
import io.zeebe.client.api.command.ClientStatusException;
import io.zeebe.client.api.response.ActivateJobsResponse;
import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ZeebeClientVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger("Client-Verticle");
    private EventBus eb;
    private ZeebeClient zClient;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        eb = vertx.eventBus();

        boolean workerIsActive = true;

        ZeebeClientConfigurationProperties clientConfig =
                new ZeebeClientConfigurationProperties(config().getJsonObject("zeebe_client"));

        zClient = createZeebeClient(clientConfig);

        startPromise.complete();

        deployWorkflow();

        // @TODO Remove
        Thread.sleep(2000);

        startWorkflowInstance();

        //@TODO Remove
        Thread.sleep(2000);

        createJobWorker("job1","horse");

    }


    private ZeebeClient createZeebeClient(ZeebeClientConfigurationProperties properties){
        log.info("Creating ZeebeClient");

        return ZeebeClient.newClientBuilder()
                .brokerContactPoint(properties.getBroker().getContactPoint())
                .defaultMessageTimeToLive(properties.getMessage().getTimeToLive())
                .defaultRequestTimeout(properties.getBroker().getRequestTimeout())
                .build();
    }

    @Override
    public void stop() throws Exception {
        zClient.close();
    }


    private Future<Void> createJobWorker(String jobType, String workerName) {
        Promise<Void> promise = Promise.promise();

        log.info("Starting Activate Jobs Command for: " + jobType + "  " + workerName);

        vertx.<ActivateJobsResponse>executeBlocking(blockProm -> {
            ZeebeFuture<ActivateJobsResponse> jobsResponseFuture = zClient.newActivateJobsCommand().jobType(jobType)
                    .maxJobsToActivate(1)
                    .workerName(workerName)
                    .send();

            log.info("Waiting for jobs");

            try {
                ActivateJobsResponse jobsResponse = jobsResponseFuture.join();
                blockProm.complete(jobsResponse);
            } catch(ClientStatusException e){
                blockProm.fail(e);
            } catch (ClientException e){
                blockProm.fail(e);
            }

        }, false, res -> {
            if (res.succeeded()) {
                log.info("Jobs Found... Count: " + res.result().getJobs().size());

                res.result().getJobs().forEach(job -> {
                    log.info("Handling Job");
                    handleJob(job);

                });
                promise.complete();

            } else {
                //Add circuit breaker
                log.error("Failure of Client", res.cause());
                promise.fail(res.cause());

            }
        });
        return promise.future();
    }

    private void handleJob(ActivatedJob job) {
        DeliveryOptions options = new DeliveryOptions().setSendTimeout(1200);
        // Add message delivery failure support on EB

        eb.<JsonObject>request("job-" + job.getType(), Json.encode(job.toJson()), options, reply -> {
            if (reply.succeeded()) {
                DoneJob doneJob = DoneJob.fromJsonObject(reply.result().body());

                if (doneJob.getResult().equals(DoneJob.Result.COMPLETE)) {
                    reportJobComplete(doneJob);

                } else {
                    reportJobFail(doneJob).setHandler(h -> {
                        if (h.succeeded()) {
                            log.info("Job has been reported as Failure");
                        } else {
                            log.error("Unable to report job as failure");
                        }
                    });
                }
            } else {
                log.error("Job worker was not found: ", reply.cause());
            }
        });
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

            try{
                completeCommandFuture.join();

                log.info("Complete Command was successfully sent");

                blkProm.complete();

            } catch (ClientStatusException e){
                blkProm.fail(e);
            } catch (ClientException e){
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

            try{
                failCommandFuture.join();

                log.info("Fail-Command was successfully sent");

                blkProm.complete();

            } catch (ClientStatusException e){
                blkProm.fail(e);
            } catch (ClientException e){
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


    private void deployWorkflow(){

        ZeebeFuture<DeploymentEvent> deploymentEventFuture = zClient.newDeployCommand()
                .addResourceFile("./bpmn/bpmn1.bpmn")
                .send();

        log.info("Deploying BPMN...");

        DeploymentEvent deploymentEvent = deploymentEventFuture.join();

        log.info("Deployed BPMN: " + deploymentEvent.getKey());
    }

    private void startWorkflowInstance(){
        ZeebeFuture<WorkflowInstanceEvent> workflowInstanceEventFuture = zClient.newCreateInstanceCommand()
                .bpmnProcessId("Process_1")
                .latestVersion()
                .send();

        log.info("Starting workflow instance");

        WorkflowInstanceEvent workflowInstanceEvent = workflowInstanceEventFuture.join();

        log.info("Starting Workflow instance: " + workflowInstanceEvent.getWorkflowInstanceKey());
    }


}
