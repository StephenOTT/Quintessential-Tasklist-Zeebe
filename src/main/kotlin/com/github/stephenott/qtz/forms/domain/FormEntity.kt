package com.github.stephenott.qtz.forms.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import java.time.Instant
import java.util.*
import javax.persistence.*

@Entity
data class FormEntity(

        @field:Id
        var id: UUID? = UUID.randomUUID(),

        @field:DateCreated
        var createdAt: Instant? = null,

        @field:DateUpdated
        var updatedAt: Instant? = null,

        var name: String? = null,

        var description: String? = null,

        var formKey: String? = null,

        @field:OneToMany(mappedBy = "form", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
        @get:JsonIgnore
        var schemas: Set<FormSchemaEntity>? = null
)