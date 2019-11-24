package com.github.stephenott.qtz.forms.persistence

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.validation.Validated
import io.reactivex.Single
import java.util.*
import javax.annotation.Nullable
import javax.validation.Valid
import javax.validation.constraints.Pattern
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero


data class FormSaveRequest(
        val name: String,
        val description: String? = null,
        val formKey: String
) {
    fun toFormEntity(): FormEntity {
        return FormEntity(name = name, description = description, formKey = formKey)
    }
}

@Controller("/forms")
open class FormStorageController(val formRepository: FormRepository) : FormStorageOperations {

    @Post()
    override fun saveForm(@Body form: Single<FormSaveRequest>): Single<HttpResponse<FormEntity>> {
        return form.flatMapPublisher {
            formRepository.save(it.toFormEntity())
        }.singleOrError().map { HttpResponse.ok(it) }
    }

    @Get("{?formId}{?formKey}{?pageable*}")
    override fun getForm(formId: UUID?, formKey: String?, pageable: Pageable?): Single<HttpResponse<List<FormEntity>>> {
        formId?.let { uuid ->
            return Single.fromPublisher(formRepository.findById(uuid))
                    .map { HttpResponse.ok(listOf(it)) }
        }
        formKey?.let { key ->
            return formRepository.findByFormKey(key).map { HttpResponse.ok(listOf(it)) }
        }
        return formRepository.findAll(pageable ?: Pageable.from(0, 100))
                .map {
                    HttpResponse.ok(it.content)
                            //@TODO Add headers function helper to set common list headers
                            //@TODO Add .next() support
                            .header("X-Total-Count", it.totalSize.toString())
                            .header("X-Page-Count", it.numberOfElements.toString())
                }
    }
}

@Validated
interface FormStorageOperations {
    fun saveForm(form: Single<FormSaveRequest>): Single<HttpResponse<FormEntity>>

    fun getForm(formId: UUID?, formKey: String?, pageable: Pageable?): Single<HttpResponse<List<FormEntity>>>
}