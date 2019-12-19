package com.github.stephenott.qtz.forms.repository

import com.github.stephenott.qtz.forms.domain.FormEntity
import com.github.stephenott.qtz.forms.domain.FormSchemaEntity
import io.micronaut.data.annotation.Repository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.reactive.ReactiveStreamsCrudRepository
import io.reactivex.Single
import java.util.*

@Repository
interface FormSchemasRepository : ReactiveStreamsCrudRepository<FormSchemaEntity, UUID> {

    fun findByForm(form: FormEntity, pageable: Pageable): Single<Page<FormSchemaEntity>>
}