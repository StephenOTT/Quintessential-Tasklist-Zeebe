package com.github.stephenott.qtz.forms.controller

import com.github.stephenott.qtz.forms.domain.FormSchema
import com.github.stephenott.qtz.forms.domain.FormEntity
import com.github.stephenott.qtz.forms.domain.FormSchemaEntity
import com.github.stephenott.qtz.forms.repository.FormsRepository
import com.github.stephenott.qtz.forms.repository.FormSchemasRepository
import com.github.stephenott.qtz.forms.validator.exception.FormValidationException
import com.github.stephenott.qtz.forms.validator.client.FormValidatorServiceClient
import com.github.stephenott.qtz.forms.validator.client.ValidationResponseInvalid
import com.github.stephenott.qtz.tasks.service.UserTasksService
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Sort
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.validation.Validated
import io.reactivex.Single
import java.util.*
import javax.inject.Inject


@Controller("/forms")
class FormsController(
        private val formValidatorServiceClient: FormValidatorServiceClient
) : FormsOperations {

    @Inject
    lateinit var formsRepository: FormsRepository

    @Inject
    lateinit var formSchemasRepository: FormSchemasRepository


    @Post()
    override fun saveForm(@Body form: Single<FormSaveRequest>): Single<HttpResponse<FormEntity>> {
        return form.flatMapPublisher {
            formsRepository.save(it.toFormEntity())
        }.singleOrError().map {
            HttpResponse.ok(it)
        }
    }

    @Get("{?formId}{?formKey}")
    override fun getForm(formId: UUID?, formKey: String?, pageable: Pageable?): Single<HttpResponse<List<FormEntity>>> {
        formId?.let { uuid ->
            return Single.fromPublisher(formsRepository.findById(uuid)).map { HttpResponse.ok(listOf(it)) }
        }
        formKey?.let { key ->
            return formsRepository.findByFormKey(key)
                    .switchIfEmpty(Single.error(UserTasksService.FormKeyNotFoundException(key)))
                    .map { HttpResponse.ok(listOf(it)) }
        }
        return formsRepository.findAll(pageable ?: Pageable.from(0, 10)) //@TODO Refactor to a configurable default
                .map {
                    HttpResponse.ok(it.content)
                            //@TODO Add headers function helper to set common list headers
                            //@TODO Add .next() support
                            .header("X-Total-Count", it.totalSize.toString())
                            .header("X-Page-Count", it.numberOfElements.toString())
                }
    }

    @Post("/{formId}/schemas")
    override fun addSchema(formId: UUID, @Body schema: Single<FormSchemaSaveRequest>): Single<HttpResponse<FormSchemaEntity>> {

        return schema.flatMapPublisher {
            val entity = it.toFormSchemaEntity(FormEntity(id = formId))
            formSchemasRepository.save(entity)
        }.singleOrError().map {
            HttpResponse.ok(it)
        }
    }

    @Get("/{formId}/schemas")
    override fun getSchemasByFormId(formId: UUID, pageable: Pageable?): Single<HttpResponse<List<FormSchemaEntity>>> {
        //@TODO Refactor to make pageable a configurable option
        return formSchemasRepository.findByForm(
                FormEntity(id = formId),
                pageable
                        ?: Pageable.from(0, 10, Sort.of(Sort.Order.desc("version")))
        ).map { page ->
            HttpResponse.ok(page.content) //@TODO Refactor to use projections on DB level
                    .header("X-Total-Count", page.totalSize.toString())
                    .header("X-Page-Count", page.numberOfElements.toString())
        }
    }

    @Error
    fun formValidationError(request: HttpRequest<*>, exception: FormValidationException): HttpResponse<ValidationResponseInvalid> {
        return HttpResponse.badRequest(exception.responseBody)
    }
}

@Validated
interface FormsOperations {

    fun saveForm(form: Single<FormSaveRequest>): Single<HttpResponse<FormEntity>>

    fun getForm(formId: UUID?, formKey: String?, pageable: Pageable?): Single<HttpResponse<List<FormEntity>>>

    fun addSchema(formId: UUID, schema: Single<FormSchemaSaveRequest>): Single<HttpResponse<FormSchemaEntity>>

    fun getSchemasByFormId(formId: UUID, pageable: Pageable?): Single<HttpResponse<List<FormSchemaEntity>>>
}


data class FormSaveRequest(
        val name: String,
        val description: String? = null,
        val formKey: String
) {
    fun toFormEntity(): FormEntity {
        return FormEntity(name = name, description = description, formKey = formKey)
    }
}

data class FormSchemaSaveRequest(
        val schema: FormSchema
) {
    fun toFormSchemaEntity(formEntity: FormEntity): FormSchemaEntity {
        return FormSchemaEntity(schema = schema, form = formEntity)
    }
}