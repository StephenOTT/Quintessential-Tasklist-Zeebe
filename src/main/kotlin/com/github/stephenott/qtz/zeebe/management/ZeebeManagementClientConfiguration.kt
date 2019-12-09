package com.github.stephenott.qtz.zeebe.management

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Value
import java.time.Duration
import java.util.*
import javax.validation.constraints.NotBlank

interface ZeebeClientConfiguration{
    var brokerContactPoint: String
    var clusterName: String
    var longPollTimeout: Duration
    var commandTimeout: Duration
    var messageTimeToLive: Duration
}

@ConfigurationProperties("zeebe.management.client")
@Context
class ZeebeManagementClientConfiguration: ZeebeClientConfiguration {

    /**
     * The zeebe broker contact point URL
     */
    override var brokerContactPoint: String = "localhost:26500"

    /**
     * The name of the Zeebe Cluster.  Used by the User Task DB to track what Zeebe Cluster the user task belongs to.
     */
    override var clusterName: String = "zeebe:${UUID.randomUUID()}"

    /**
     * The max duration of the Zeebe long poll for user tasks.
     */
    override var longPollTimeout: Duration = Duration.ofMinutes(10)

    /**
     * The max duration of the Zeebe gRPC commands except for the Long poll, which is covered with the longPollTimeout.
     */
    override var commandTimeout: Duration = Duration.ofSeconds(30)

    /**
     * The message time to live for Zeebe.
     */
    override var messageTimeToLive: Duration = Duration.ofHours(1)
}