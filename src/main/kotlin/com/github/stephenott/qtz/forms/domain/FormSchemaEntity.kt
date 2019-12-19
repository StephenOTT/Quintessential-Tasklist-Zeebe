package com.github.stephenott.qtz.forms.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import java.time.Instant
import java.util.*
import javax.persistence.*

@Entity
data class FormSchemaEntity(

        @field:Id
        var id: UUID? = UUID.randomUUID(),

        @field:DateCreated
        var createdAt: Instant? = null,

        @field:DateUpdated
        var updatedAt: Instant? = null,

        var version: Long? = null,

        @field:ManyToOne(fetch = FetchType.EAGER, optional = false)
        @field:JsonIgnore
        var form: FormEntity? = null,

        @field:Column(columnDefinition = "JSON")
        @field:Convert(converter = FormSchemaAttributeConverter::class)
        var schema: FormSchema? = null
)