package com.github.stephenott;

import io.zeebe.test.ZeebeTestRule;
import org.junit.Rule;
import org.junit.Test;

public class MyTest {

    @Rule public final ZeebeTestRule testRule = new ZeebeTestRule();

    @Test
    public void test() {
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