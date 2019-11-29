package com.github.stephenott.qtz.forms.submission.domain

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class SubmissionEntity(

        @field:Id
        var id: UUID? = UUID.randomUUID(),

        @field:DateCreated
        var createdAt: Instant? = null,

        @field:DateUpdated
        var updatedAt: Instant? = null,

        var submitter: String? = null,

        @field:Column(columnDefinition = "JSON")
        @field:Convert(converter = SubmissionObjectAttributeConverter::class)
        var submission: SubmissionObject? = null,

        var formKeyAndVersion: String? = null,

        var destinationSystem: String? = null,

        var transferState: String? = null
)

data class SubmissionObject(val submission: Map<String, Any?>?)