package com.github.stephenott;

import io.zeebe.client.ZeebeClient;
import io.zeebe.test.ZeebeTestRule;
import org.junit.Rule;
import org.junit.Test;

public class MyTest {

    @Rule public final ZeebeTestRule testRule = new ZeebeTestRule();

    private ZeebeClient client;

    @Test
    public void test() {
        client = testRule.getClient();
        System.out.println(client.getConfiguration().getBrokerContactPoint());
        try {
            Thread.sleep(1000000);
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