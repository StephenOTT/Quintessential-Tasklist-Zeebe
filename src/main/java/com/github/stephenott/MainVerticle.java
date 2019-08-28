package com.github.stephenott;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private EventBus eb;
    private Logger log = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start() throws Exception {
        Json.mapper.registerModules(new ParameterNamesModule(), new Jdk8Module(), new JavaTimeModule());

        eb = vertx.eventBus();

        log.info("Starting to deploy Worker Verticle");
        vertx.deployVerticle(ZeebeWorkerVerticle::new, new DeploymentOptions());

        log.info("Starting to deploy Client Verticle");
        vertx.deployVerticle(ZeebeClientVerticle::new, new DeploymentOptions().setConfig(config()));


    }
}
