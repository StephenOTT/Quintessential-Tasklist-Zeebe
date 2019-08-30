package com.github.stephenott;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private EventBus eb;
    private Logger log = LoggerFactory.getLogger(MainVerticle.class);
    private ConfigRetriever appConfigRetriever;
    ApplicationConfiguration appConfig;

    @Override
    public void start() throws Exception {
        Json.mapper.registerModules(new ParameterNamesModule(), new Jdk8Module(), new JavaTimeModule());
        Json.prettyMapper.registerModules(new ParameterNamesModule(), new Jdk8Module(), new JavaTimeModule());

        eb = vertx.eventBus();

        setupAppConfig("zeebe.yml").setHandler(ac -> {
            if (ac.succeeded()) {
                appConfig = ac.result();

                appConfig.getExecutors().forEach(this::deployZeebeWorker);

                appConfig.getZeebe().getClients().forEach(this::deployZeebeClient);

            } else {
                throw new IllegalStateException("Unable to read yml configuration", ac.cause());
            }
        });
    }


    private void deployZeebeWorker(ApplicationConfiguration.ExecutorConfiguration config){
        DeploymentOptions options = new DeploymentOptions();
        options.setConfig(JsonObject.mapFrom(config));

        vertx.deployVerticle(ExecutorVerticle::new, options, vert -> {
            if (vert.succeeded()){
                log.info("ZeebeWorker Verticle " + config.getName() + " has successfully deployed");
            } else {
                log.error("ZeebeWorker Verticle " + config.getName() + " has failed to deploy!", vert.cause());
            }
        });
    }

    private void deployZeebeClient(ApplicationConfiguration.ZeebeClientConfiguration config) {
        DeploymentOptions options = new DeploymentOptions();
        options.setConfig(JsonObject.mapFrom(config));

        vertx.deployVerticle(ZeebeClientVerticle::new, options, vert -> {
            if (vert.succeeded()) {
                log.info("ZeebeClient Verticle " + config.getName() + " has successfully deployed");
            } else {
                log.error("ZeebeClient Verticle " + config.getName() + " has failed to deploy!", vert.cause());
            }
        });
    }


    private Future<ApplicationConfiguration> setupAppConfig(String filePath) {
        Promise<ApplicationConfiguration> promise = Promise.promise();

        ConfigStoreOptions store = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject()
                        .put("path", filePath)
                );

        appConfigRetriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(store));

        appConfigRetriever.getConfig(handler -> {
            if (handler.succeeded()) {
                promise.complete(handler.result().mapTo(ApplicationConfiguration.class));

            } else {
                promise.fail(handler.cause());
            }
        });
        return promise.future();
    }
}
