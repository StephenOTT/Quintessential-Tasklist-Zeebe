package com.github.stephenott.usertask;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserTaskHttpServer extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(UserTaskHttpServer.class);

    EventBus eb;

    @Override
    public void start() throws Exception {

        eb = vertx.eventBus();


    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    private void establishCompleteActionRoute(){
        //@TODO
    }

    private void establishGetTaskRoute(){
        //@TODO
    }

    private void establishGetTasksRoute(){
        //@TODO
    }

    private void establishDeleteTaskRoute(){
        //@TODO
    }

    private void establishClaimTaskRoute(){
        //@TODO
    }

    private void establishUnClaimTaskRoute(){
        //@TODO
    }

    private void establishAssignTaskRoute(){
        //@TODO
    }

    private void establishCreateCustomTaskRoute(){
        //@TODO
        // Will use a custom BPMN that allows a custom single step task to be created.
        // Create a config for this so the BPMN Process ID can be set in the YAML config
    }
}
