package com.github.stephenott.qtz.zeebe

import io.kotlintest.Spec
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import io.micronaut.test.annotation.MicronautTest
import io.zeebe.client.ZeebeClient
import io.zeebe.containers.ZeebePort
import io.zeebe.containers.broker.ZeebeBrokerContainer

@MicronautTest
class ZeebeBroker1Test : StringSpec({

    "zeebe client should be running" {
        val client: ZeebeClient = ZeebeClient.newClientBuilder()
            .brokerContactPoint(broker.getExternalAddress(ZeebePort.GATEWAY))
            .build()
        println("CLIENT->>$client")

        client shouldNotBe null
    }

}) {
    companion object {
        var broker = ZeebeBrokerContainer() //@Todo is a .start needed?
    }

    override fun afterSpec(spec: Spec) {
        broker.stop()
        //@TODO move to common spec abstract class
    }
}
