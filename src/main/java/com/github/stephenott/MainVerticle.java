package com.github.stephenott;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.stephenott.common.EventBusableReplyException;
import com.github.stephenott.common.EventBusableMessageCodec;
import com.github.stephenott.conf.ApplicationConfiguration;
import com.github.stephenott.executors.JobResult;
import com.github.stephenott.executors.polyglot.ExecutorVerticle;
import com.github.stephenott.executors.usertask.UserTaskExecutorVerticle;
import com.github.stephenott.form.validator.FormValidationServerHttpVerticle;
import com.github.stephenott.form.validator.ValidationRequest;
import com.github.stephenott.form.validator.ValidationRequestResult;
import com.github.stephenott.managementserver.ManagementHttpVerticle;
import com.github.stephenott.usertask.*;
import com.github.stephenott.usertask.mongo.MongoManager;
import com.github.stephenott.zeebe.client.ZeebeClientVerticle;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClients;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bson.codecs.configuration.CodecRegistries.*;

public class MainVerticle extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(MainVerticle.class);

    private EventBus eb;

    private ConfigRetriever appConfigRetriever;
    ApplicationConfiguration appConfig;

    @Override
    public void start() throws Exception {
        Json.mapper.registerModules(new ParameterNamesModule(), new Jdk8Module(), new JavaTimeModule());
        Json.prettyMapper.registerModules(new ParameterNamesModule(), new Jdk8Module(), new JavaTimeModule());
        Json.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        Json.prettyMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);


        eb = vertx.eventBus();

        eb.registerDefaultCodec(DbActionResult.FailedDbActionException.class, new EventBusableMessageCodec<>(DbActionResult.FailedDbActionException.class));
        eb.registerDefaultCodec(JobResult.class, new EventBusableMessageCodec<>(JobResult.class));
        eb.registerDefaultCodec(DbActionResult.class, new EventBusableMessageCodec<>(DbActionResult.class));
        eb.registerDefaultCodec(CompletionRequest.class, new EventBusableMessageCodec<>(CompletionRequest.class));
        eb.registerDefaultCodec(GetRequest.class, new EventBusableMessageCodec<>(GetRequest.class));
        eb.registerDefaultCodec(ValidationRequest.class, new EventBusableMessageCodec<>(ValidationRequest.class));
        eb.registerDefaultCodec(ValidationRequestResult.class, new EventBusableMessageCodec<>(ValidationRequestResult.class));

        String configYmlPath = config().getString("configYmlPath");

        retrieveAppConfig(configYmlPath, result -> {
            if (result.succeeded()) {
                appConfig = result.result();

                //Setup Mongo:
                CodecRegistry registry = fromRegistries(
                        MongoClients.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder()
                                .automatic(true)
                                .build())
                        );
                MongoClientSettings mSettings = MongoClientSettings.builder()
                        .codecRegistry(registry)
                        .build();
                MongoManager.setClient(MongoClients.create(mSettings));

                //@TODO refactor this
                vertx.deployVerticle(UserTaskActionsVerticle.class, new DeploymentOptions());
                //@TODO refactor this

                deployUserTaskHttpServer(appConfig.getUserTaskServer());


                appConfig.getExecutors().forEach(this::deployExecutorVerticle);

                appConfig.getUserTaskExecutors().forEach(this::deployUserTaskExecutorVerticle);

                appConfig.getZeebe().getClients().forEach(this::deployZeebeClient);

                if (appConfig.getManagementServer().isEnabled()) {
                    deployManagementClient(appConfig.getManagementServer());
                }

                if (appConfig.getFormValidatorServer().isEnabled()){
                    deployFormValidationServer(appConfig.getFormValidatorServer());
                }

            } else {
                throw new IllegalStateException("Unable to read yml configuration", result.cause());
            }
        });
    }

    private void deployManagementClient(ApplicationConfiguration.ManagementHttpConfiguration config) {
        DeploymentOptions options = new DeploymentOptions()
                .setInstances(config.getInstances())
                .setConfig(JsonObject.mapFrom(config));

        vertx.deployVerticle(ManagementHttpVerticle::new, options, deployResult -> {
            if (deployResult.succeeded()) {
                log.info("Management Client has successfully deployed");
            } else {
                log.error("Management Client failed to deploy", deployResult.cause());
            }
        });
    }

    private void deployUserTaskHttpServer(ApplicationConfiguration.UserTaskHttpServerConfiguration config) {
        DeploymentOptions options = new DeploymentOptions()
                .setInstances(config.getInstances())
                .setConfig(JsonObject.mapFrom(config));

        vertx.deployVerticle(UserTaskHttpServerVerticle::new, options, deployResult -> {
            if (deployResult.succeeded()) {
                log.info("UserTask HTTP Server has successfully deployed");
            } else {
                log.error("UserTask HTTP Server failed to deploy", deployResult.cause());
            }
        });
    }

    private void deployFormValidationServer(ApplicationConfiguration.FormValidationServerConfiguration config) {
        DeploymentOptions options = new DeploymentOptions()
                .setInstances(config.getInstances())
                .setConfig(JsonObject.mapFrom(config));

        vertx.deployVerticle(FormValidationServerHttpVerticle::new, options, deployResult -> {
            if (deployResult.succeeded()) {
                log.info("Form Validation Server has successfully deployed");
            } else {
                log.error("Form Validation Server failed to deploy", deployResult.cause());
            }
        });
    }


    private void deployExecutorVerticle(ApplicationConfiguration.ExecutorConfiguration config) {
        DeploymentOptions options = new DeploymentOptions()
                .setInstances(config.getInstances())
                .setConfig(JsonObject.mapFrom(config));

        vertx.deployVerticle(ExecutorVerticle::new, options, vert -> {
            if (vert.succeeded()) {
                log.info("Executor Verticle " + config.getName() + " has successfully deployed (" + config.getInstances() + " instances)");
            } else {
                log.error("Executor Verticle " + config.getName() + " has failed to deploy!", vert.cause());
            }
        });
    }

    private void deployUserTaskExecutorVerticle(ApplicationConfiguration.UserTaskExecutorConfiguration config) {
        DeploymentOptions options = new DeploymentOptions();
        options.setConfig(JsonObject.mapFrom(config));

        vertx.deployVerticle(UserTaskExecutorVerticle::new, options, vert -> {
            if (vert.succeeded()) {
                log.info("UserTask Executor Verticle " + config.getName() + " has successfully deployed");
            } else {
                log.error("UserTask Executor Verticle " + config.getName() + " has failed to deploy!", vert.cause());
            }
        });
    }

    private void deployZeebeClient(ApplicationConfiguration.ZeebeClientConfiguration config) {
        DeploymentOptions options = new DeploymentOptions();
        options.setConfig(JsonObject.mapFrom(config));

        vertx.deployVerticle(ZeebeClientVerticle::new, options, vert -> {
            if (vert.succeeded()) {
                log.info("Zeebe Client Verticle " + config.getName() + " has successfully deployed");
            } else {
                log.error("Zeebe Client Verticle " + config.getName() + " has failed to deploy!", vert.cause());
            }
        });
    }


    private void retrieveAppConfig(String filePath, Handler<AsyncResult<ApplicationConfiguration>> result) {
        ConfigStoreOptions store = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject()
                        .put("path", filePath)
                );

        appConfigRetriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(store));

        appConfigRetriever.getConfig(retrieverResult -> {
            if (retrieverResult.succeeded()) {
                result.handle(Future.succeededFuture(retrieverResult.result().mapTo(ApplicationConfiguration.class)));

            } else {
                result.handle(Future.failedFuture(retrieverResult.cause()));
            }
        });
    }
}
