package com.github.stephenott.qtz.forms.validator.controller

import com.github.stephenott.qtz.forms.validator.domain.FormSubmission
import com.github.stephenott.qtz.forms.validator.exception.FormValidationException
import com.github.stephenott.qtz.forms.validator.client.FormValidatorServiceClient
import com.github.stephenott.qtz.forms.validator.client.ValidationResponseInvalid
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
class FormsController(
        private val formValidatorServiceClient: FormValidatorServiceClient
) : FormsOperations {

    @Post(value = "/validate")
    override fun validate(@Body submission: Single<FormSubmission>): Single<HttpResponse<Map<String, Any?>>> {
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
                    HttpResponse.ok(it.body()!!.processed_submission)
                }
    }

    @Error
    fun formValidationError(request: HttpRequest<*>, exception: FormValidationException): HttpResponse<ValidationResponseInvalid> {
        return HttpResponse.badRequest(exception.responseBody)
    }
}

@Validated
interface FormsOperations {
    fun validate(submission: Single<FormSubmission>): Single<HttpResponse<Map<String, Any?>>>
}