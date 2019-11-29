package com.github.stephenott.qtz.forms.validator

import com.github.stephenott.qtz.forms.FormSchema
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.validation.Validated
import io.reactivex.Single


@Controller("/forms")
class FormSubmissionController(private val formValidatorServiceClient: FormValidatorServiceClient) : FormSubmissionOperations {

    @Post(value = "/submit")
    override fun submit(submission: Single<FormSubmission>): Single<HttpResponse<FormSubmission>> {
        return submission.map { HttpResponse.ok(it) }
    }

    @Post(value = "/validate")
    override fun validate(submission: Single<FormSubmission>): Single<HttpResponse<ValidationResponseValid>> {
        return formValidatorServiceClient.validate(submission)
                .onErrorResumeNext {
                    // @TODO Can eventually be replaced once micronaut-core fixes a issue where the response body is not passed to @Error handler when it catches the HttpClientResponseException
                    if (it is HttpClientResponseException) {
                        val body = it.response.getBody(ValidationResponseInvalid::class.java)
                        if (body.isPresent) {
                            Single.error(FormValidationException(body.get()))
                        } else {
                            Single.error(IllegalStateException("Invalid Response Received", it))
                        }
                    } else {
                        Single.error(IllegalStateException("Unexpected Error received from Form Validation request.", it))
                    }
                }.map {
                    HttpResponse.ok(it.body()!!)
                }
    }

    @Error
    fun formValidationError(request: HttpRequest<*>, exception: FormValidationException): HttpResponse<ValidationResponseInvalid> {
        return HttpResponse.badRequest(exception.responseBody)
    }
}

@Validated
interface FormSubmissionOperations {
    fun submit(@Body submission: Single<FormSubmission>): Single<HttpResponse<FormSubmission>>

    fun validate(@Body submission: Single<FormSubmission>): Single<HttpResponse<ValidationResponseValid>>
}

//@Introspected
data class FormSubmission(
        val schema: FormSchema,
        val submission: FormSubmissionData) {}

//@Introspected
data class FormSubmissionData(
        val data: Map<String, Any>,
        val metadata: Map<String, Any>?) {}

class FormValidationException(val responseBody: ValidationResponseInvalid) : RuntimeException("Form Validation Exception") {}
