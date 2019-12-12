package com.github.stephenott.qtz.zeebe

import java.time.Duration

interface ZeebeClientConfiguration{
    var brokerContactPoint: String
    var clusterName: String
    var longPollTimeout: Duration
    var commandTimeout: Duration
    var messageTimeToLive: Duration
}