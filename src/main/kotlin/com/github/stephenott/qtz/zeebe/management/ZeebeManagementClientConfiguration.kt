package com.github.stephenott.qtz.zeebe.management

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Value
import javax.validation.constraints.NotBlank

@ConfigurationProperties("zeebe.management.client")
@Context
class ZeebeManagementClientConfiguration {
    var broker: String = "0.0.0.0:26500"
}