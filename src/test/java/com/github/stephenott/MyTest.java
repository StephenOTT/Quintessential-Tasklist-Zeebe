package com.github.stephenott;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.zeebe.test.ZeebeTestRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class MyTest {

    @Rule public final ZeebeTestRule testRule = new ZeebeTestRule();
    private MongodExecutable mongodExecutable;

    @Test
    public void test() throws Exception {

        MongodStarter runtime = MongodStarter.getDefaultInstance();
        IMongodConfig mongodConfig = new MongodConfigBuilder().version(Version.Main.PRODUCTION)
                .net(new Net("localhost", 27017, Network.localhostIsIPv6()))
                .build();

        MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(mongodConfig);
        mongodExecutable.start();

        try {
            Thread.sleep(2000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        client
//                .newDeployCommand()
//                .addResourceFromClasspath("process.bpmn")
//                .send()
//                .join();
//
//        final WorkflowInstanceEvent workflowInstance =
//                client
//                        .newCreateInstanceCommand()
//                        .bpmnProcessId("process")
//                        .latestVersion()
//                        .send()
//                        .join();
    }
}