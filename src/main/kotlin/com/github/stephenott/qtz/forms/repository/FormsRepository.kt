package com.github.stephenott.qtz.forms.repository

import com.github.stephenott.qtz.forms.domain.FormEntity
import io.micronaut.data.annotation.Repository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.reactive.ReactiveStreamsCrudRepository
import io.reactivex.Maybe
import io.reactivex.Single
import java.util.*

@Repository
interface FormsRepository : ReactiveStreamsCrudRepository<FormEntity, UUID> {

    fun findByFormKey(formKey: String): Maybe<FormEntity>

    fun findAll(pageable: Pageable): Single<Page<FormEntity>>
}