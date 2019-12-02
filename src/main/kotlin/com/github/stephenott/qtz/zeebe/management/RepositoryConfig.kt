package com.github.stephenott.qtz.zeebe.management

data class RepositoryConfig(
        val brokerContactPoint: String = "\${zeebe.management.client.brokerContactPoint}"
)