package com.github.stephenott.qtz.forms.persistence

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import io.reactivex.Single
import javax.inject.Inject


data class FormSaveModel(
        val name: String,
        val description: String? = null,
        val formKey: String
){
    fun toForm():Form {
        return Form(name = name, description = description, formKey = formKey)
    }
}

@Controller("/forms")
open class FormStorageController() : FormStorageOperations {

    @Inject
    lateinit var formRepository: FormRepository

    @Post("/save")
    override fun saveForm(@Body form: Single<FormSaveModel>): Single<HttpResponse<Form>> {
        return form.flatMap {
            formRepository.persist(it.toForm())
        }.map {
            HttpResponse.ok(it)
        }
    }

}

@Validated
interface FormStorageOperations {
    fun saveForm(@Body form: Single<FormSaveModel>): Single<HttpResponse<Form>>

//    fun saveFormSchema(formUuid: String, @Body form: Single<Schema>): Single<HttpResponse<Unit>>
//
//    fun updateForm(@Body form: Single<Form>): Single<HttpResponse<Form>>
//
//    fun updateFormSchema(formUuid: String, @Body form: Single<Schema>): Single<HttpResponse<Unit>>
//
//    fun deleteFormAndAllRelatedSchemas(formUuid: String): Single<HttpResponse<Unit>>
//
//    fun deleteFormSchema(formUuid: String): Single<HttpResponse<Unit>>
//
//    /**
//     * Defaults to latest schema version (higher version number)
//     */
//    fun getFormWithSchema(schemaVersion: Long?): Single<HttpResponse<Form>>
//
//    fun getForms(): Single<HttpResponse<List<Form>>>
//
//    fun getFormSchemas(formUuid: String, schemaVersion: Long): Single<HttpResponse<List<Schema>>>

}
