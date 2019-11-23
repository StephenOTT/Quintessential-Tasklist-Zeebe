package com.github.stephenott.qtz.forms.persistence

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.stephenott.qtz.forms.FormSchema
import io.micronaut.data.annotation.*

import io.micronaut.data.model.DataType
import io.micronaut.data.repository.reactive.ReactiveStreamsCrudRepository
import io.reactivex.Single
import java.time.Instant
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id


@Repository
interface FormRepository : ReactiveStreamsCrudRepository<Form, UUID> {
        fun persist(entity: Form): Single<Form>
}

@Entity
data class Form(
        @Id
//        @AutoPopulated // @TODO Does not currently work...
        var uuid: UUID? = UUID.randomUUID(),

        @DateCreated
        var createdAt: Instant? = null,

        @DateUpdated
        var updatedAt: Instant? = null,

        var name: String,
        var description: String? = null,
        var formKey: String
)

data class Schema(
        @Id
        @AutoPopulated
        val uuid: UUID,

        @Relation(value = Relation.Kind.ONE_TO_ONE)
        val form: Form,

        @DateCreated
        val createdAt: Instant,

        @DateUpdated
        val updatedAt: Instant,

        val version: Long,

        @TypeDef(type = DataType.JSON)
        val schema: FormSchema
)