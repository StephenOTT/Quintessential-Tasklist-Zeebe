package com.github.stephenott.qtz.forms.persistence

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.stephenott.qtz.forms.FormSchema
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Repository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.reactive.ReactiveStreamsCrudRepository
import io.reactivex.Single
import java.time.Instant
import java.util.*
import javax.persistence.*


@Repository
interface FormRepository : ReactiveStreamsCrudRepository<FormEntity, UUID> {

    fun findByFormKey(formKey: String): Single<FormEntity>

    fun findAll(pageable: Pageable): Single<Page<FormEntity>>
}

@Repository
interface FormSchemaRepository : ReactiveStreamsCrudRepository<FormSchemaEntity, UUID> {

    fun findByForm(form: FormEntity, pageable: Pageable): Single<Page<FormSchemaEntity>>
}

//@Introspected
//data class SchemaDTO(var schema: FormSchema? = null)


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