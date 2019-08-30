package com.github.stephenott;

import io.zeebe.client.impl.ZeebeObjectMapper;
import io.zeebe.client.impl.response.ActivatedJobImpl;
import io.zeebe.gateway.protocol.GatewayOuterClass;

public class ActivatedJob extends ActivatedJobImpl {

    public ActivatedJob(ZeebeObjectMapper objectMapper, GatewayOuterClass.ActivatedJob job) {
        super(objectMapper, job);
    }

    public ActivatedJob(long key){

    }
}
