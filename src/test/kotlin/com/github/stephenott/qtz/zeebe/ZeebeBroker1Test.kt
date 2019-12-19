package com.github.stephenott.qtz.zeebe

import io.kotlintest.Spec
import io.kotlintest.specs.StringSpec
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.test.annotation.MicronautTest
import java.io.File

@MicronautTest
class ZeebeBroker1Test : StringSpec({

//    "zeebe client should be running" {
//        val client: ZeebeClient = ZeebeClient.newClientBuilder()
//                .brokerContactPoint(broker.getExternalAddress(ZeebePort.GATEWAY))
//                .build()
//        println("CLIENT is RUNNING -->>$client")
//
//        client shouldNotBe null
//    }

    "deploy a workflow"{

        val file = File("test1.bpmn")

        val requestBody = MultipartBody.builder()
                .addPart("data",
                        file.name,
                        MediaType.TEXT_PLAIN_TYPE,
                        file).build()

        val dog = HttpRequest.POST("localhost:8080/zeebe/management/deployment", requestBody)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)


    }

}) {
    companion object {
//        var broker = ZeebeBrokerContainer() //@Todo is a .start needed?
    }

    override fun afterSpec(spec: Spec) {
//        broker.stop()
        //@TODO move to common spec abstract class
    }
}
