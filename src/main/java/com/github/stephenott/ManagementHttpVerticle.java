package com.github.stephenott;

import com.github.stephenott.configuration.ApplicationConfiguration;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.ZeebeFuture;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.Workflow;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

public class ManagementHttpVerticle extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(ManagementHttpVerticle.class);

    private EventBus eb;

    private ZeebeClient zClient;

    private String fileUploadPath;

    private ApplicationConfiguration.ManagementHttpConfiguration managementConfig;

    private String apiRoot;

    @Override
    public void start() throws Exception {
        managementConfig = config().mapTo(ApplicationConfiguration.ManagementHttpConfiguration.class);

        if (!managementConfig.isEnabled()){
            log.error("Stopping Management HTTP Verticle because the provided management config have enables=false");
            stop();
        }

        eb = vertx.eventBus();

        fileUploadPath = managementConfig.getFileUploadPath();
        apiRoot = managementConfig.getApiRoot();

        zClient = createZeebeClient();

        Router mainRouter = Router.router(vertx);
        Router apiRootRouter = Router.router(vertx);

        mainRouter.mountSubRouter("/" + apiRoot, apiRootRouter);

        HttpServer server = vertx.createHttpServer();

        //Generic Failure Handler
        //@TODO setup proper Exceptions with json responses
        //@TODO setup faillback failure handlers (such as Resource not found)
        //@TODO move to method
        mainRouter.route().failureHandler(failure -> {

            int statusCode = failure.statusCode();

            HttpServerResponse response = failure.response();
            response.setStatusCode(statusCode)
                    .end("DOG" + failure.failure().getLocalizedMessage());
        });
        //@TODO move to method
        apiRootRouter.route().failureHandler(failure -> {

            int statusCode = failure.statusCode();

            HttpServerResponse response = failure.response();
            response.setStatusCode(statusCode)
                    .end("DOG" + failure.failure().getLocalizedMessage());
        });

        establishDeployRoute(apiRootRouter);
        establishCreateWorkflowInstanceRoute(apiRootRouter);

        server.requestHandler(mainRouter)
                .listen(managementConfig.getPort());

        log.info("Server deployed at: localhost:{} under apiRoot: {}, CORS is {} ", managementConfig.getPort(), managementConfig.getApiRoot(), (managementConfig.getCorsRegex() != null) ? "enabled with: " + managementConfig.getCorsRegex() : "disabled");

    }

    @Override
    public void stop() throws Exception {

    }

    private void establishDeployRoute(Router router) {
        Route route = router.route(HttpMethod.POST, "/deploy")
                .consumes("multipart/form-data")
                .produces("application/json");

        if (managementConfig.getCorsRegex() != null) {
            route.handler(CorsHandler.create(managementConfig.getCorsRegex()));
        }

        route.handler(BodyHandler.create().setUploadsDirectory(fileUploadPath));  //@TODO Change this

        route.handler(rc -> {
            Set<FileUpload> uploads = rc.fileUploads();

            if (uploads.size() != 1) {
                rc.fail(403, new IllegalArgumentException("Must have only 1 file upload"));

            } else {
                uploads.forEach(upload -> {
                    handleUploadForWorkflowDeployment(upload, rc);
                });

            }
        });
    }

    private void establishCreateWorkflowInstanceRoute(Router router) {
        Route route = router.route(HttpMethod.POST, "/create-instance")
                .consumes("application/json")
                .produces("application/json");

        if (managementConfig.getCorsRegex() != null) {
            route.handler(CorsHandler.create(managementConfig.getCorsRegex()));
        }

        route.handler(BodyHandler.create());

        route.handler(rc -> {
            CreateInstanceConfiguration config = rc.getBodyAsJson().mapTo(CreateInstanceConfiguration.class);

            // Workflow key will take precedent over bpmn version:

            // Create Instance based on workerflow Key

            //@TODO move into its own handle method:
            vertx.<WorkflowInstanceEvent>executeBlocking(code -> {
                if (config.getWorkflowKey() != null) {
                    ZeebeFuture<WorkflowInstanceEvent> workflowInstanceEventFuture =
                            zClient.newCreateInstanceCommand()
                                    .workflowKey(config.getWorkflowKey())
                                    .variables(config.getVariables())
                                    .send();

                    log.info("Starting workflow instance based on key: " + config.getWorkflowKey());
                    try {
                        WorkflowInstanceEvent workflowInstanceEvent = workflowInstanceEventFuture.join();
                        log.info("Workflow({}) instance started: {}", config.getWorkflowKey(), workflowInstanceEvent.getWorkflowInstanceKey());
                        code.complete(workflowInstanceEvent);

                    } catch (Exception e) {
                        log.error("Unable to start workflow instance", e);
                        code.fail(e);
                    }

                    //Create instance based on bpmnProcessId
                } else {
                    ZeebeFuture<WorkflowInstanceEvent> workflowInstanceEventFuture =
                            zClient.newCreateInstanceCommand()
                                    .bpmnProcessId(config.getBpmnProcessId())
                                    .version((config.getBpmnProcessVersion() != null) ? config.getBpmnProcessVersion() : -1)
                                    .variables(config.getVariables())
                                    .send();

                    String humanVersion = (config.getBpmnProcessVersion() != null) ? config.getBpmnProcessVersion().toString() : "latest";

                    log.info("Starting workflow instance based on bpmnProcessId: " + config.getBpmnProcessId() + "and version: " + humanVersion);

                    try {
                        WorkflowInstanceEvent workflowInstanceEvent = workflowInstanceEventFuture.join();
                        log.info("Workflow({}) instance started: {}", config.getWorkflowKey(), workflowInstanceEvent.getWorkflowInstanceKey());
                        code.complete(workflowInstanceEvent);

                    } catch (Exception e) {
                        log.error("Unable to start workflow instance", e);
                        code.fail(e);
                    }
                }

            }, codeResult -> {
                if (codeResult.succeeded()) {
                    rc.response()
                            .setStatusCode(200)
                            .end("Process Instance Started: " + codeResult.result().getWorkflowInstanceKey());

                } else {
                    //Unable to start instance
                    rc.fail(403, codeResult.cause());
                }
            });
        });
    }

    private void handleUploadForWorkflowDeployment(FileUpload upload, RoutingContext rc) {
        readModelFromUpload(upload.uploadedFileName(), result -> {
            if (result.succeeded()) {
                createWorkflowDeployment(upload.name(), result.result()).setHandler(deployResult -> {
                    if (deployResult.succeeded()) {
                        rc.response()
                                .setStatusCode(200)
                                .end("Deployment Success");

                    } else {
                        //Unable to deploy
                        rc.fail(403, deployResult.cause());
                    }
                });

            } else {
                // Could not read file:
                rc.fail(403, result.cause());
            }
        });
    }

    private void readModelFromUpload(String uploadedFileName, Handler<AsyncResult<BpmnModelInstance>> asyncResultHandler) {
        vertx.<BpmnModelInstance>executeBlocking(code->{
            try {
                BpmnModelInstance model = Bpmn.readModelFromFile(new File(uploadedFileName));
                code.complete(model);

            } catch (Exception e) {
                code.fail(e);
            }
        }, codeResult->{
            if (codeResult.succeeded()){
                asyncResultHandler.handle(Future.succeededFuture(codeResult.result()));
            } else{
                asyncResultHandler.handle(Future.failedFuture(codeResult.cause()));
            }
        });
    }


    private Future<DeploymentEvent> createWorkflowDeployment(String workflowName, BpmnModelInstance modelInstance) {
        Promise<DeploymentEvent> promise = Promise.promise();

        if (modelInstance == null || workflowName == null) {
            promise.fail("Must have at least 1 model to deploy");

        } else {
            vertx.<DeploymentEvent>executeBlocking(code -> {
                ZeebeFuture<DeploymentEvent> deploymentEventFuture = zClient.newDeployCommand()
                        .addWorkflowModel(modelInstance, workflowName)
                        .send();

                log.info("Deploying Workflow...");

                try {
                    DeploymentEvent deploymentEvent = deploymentEventFuture.join();

                    //@TODO rebuild this log statement
                    log.info("Deployment Succeeded: Deployment Key:" + deploymentEvent.getKey() + " with workflows: " + Arrays.toString(deploymentEvent.getWorkflows().stream().map(Workflow::getWorkflowKey).toArray()));

                    code.complete(deploymentEvent);

                } catch (Exception e) {
                    code.fail(e);
                }

            }, codeResult -> {
                if (codeResult.succeeded()) {
                    promise.complete(codeResult.result());

                } else {
                    promise.fail(codeResult.cause());
                }
            });
        }

        return promise.future();
    }


    private ZeebeClient createZeebeClient() {
        return ZeebeClient.newClientBuilder()
                .brokerContactPoint(managementConfig.getZeebeClient().getBrokerContactPoint())
                .defaultRequestTimeout(managementConfig.getZeebeClient().getRequestTimeout())
                .usePlaintext() //@TODO remove and replace with cert  /-/SECURITY/-/
                .build();
    }


}
