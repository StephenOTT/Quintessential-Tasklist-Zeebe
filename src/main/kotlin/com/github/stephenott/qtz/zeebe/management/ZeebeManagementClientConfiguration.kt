package com.github.stephenott.qtz.zeebe.management

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Value
import java.time.Duration
import java.util.*
import javax.validation.constraints.NotBlank

@ConfigurationProperties("zeebe.management.client")
@Context
class ZeebeManagementClientConfiguration {

    /**
     * The zeebe broker contact point URL
     */
    var brokerContactPoint: String = "localhost:26500"

    /**
     * The name of the Zeebe Cluster.  Used by the User Task DB to track what Zeebe Cluster the user task belongs to.
     */
    var clusterName: String = "zeebe:${UUID.randomUUID()}"

    /**
     * The max duration of the Zeebe long poll for user tasks.
     */
    var longPollTimeout: Duration = Duration.ofMinutes(10)

    /**
     * The max duration of the Zeebe gRPC commands except for the Long poll, which is covered with the longPollTimeout.
     */
    var commandTimeout: Duration = Duration.ofSeconds(30)

    /**
     * The message time to live for Zeebe.
     */
    var messageTimeToLive: Duration = Duration.ofHours(1)
}