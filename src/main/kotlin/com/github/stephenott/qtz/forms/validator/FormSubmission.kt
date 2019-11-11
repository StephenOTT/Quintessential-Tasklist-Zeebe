package com.github.stephenott.qtz.forms.validator

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.hateoas.JsonError
import io.micronaut.validation.Validated
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy


@Controller("/forms")
open class FormSubmissionController(private val formValidatorServiceClient: FormValidatorServiceClient) : FormSubmissionOperations {

    override fun submit(submission: Single<FormSubmission>): Single<HttpResponse<FormSubmission>> {
        return submission.map { HttpResponse.ok(it) }
    }

    override fun validate(submission: Single<FormSubmission>): Single<HttpResponse<ValidationResponseValid>> {
            return formValidatorServiceClient.validate(submission).map {
                HttpResponse.ok(it.body()!!)
            }
    }

    @Error
    fun formSubmissionException(request: HttpRequest<*>, exception: HttpClientResponseException): Single<HttpResponse<JsonError>> {
        println(exception.response)
        return Single.just(
                HttpResponse.status<JsonError>(HttpStatus.BAD_REQUEST, "Form Validation Failure")
                            .body(JsonError(exception.response.body.toString()))
        )
    }
}


//    @Error
//    fun formSubmissionException(request: HttpRequest<Any>, exception: Throwable): Single<HttpResponse<JsonError>> {
//        when (exception) {
//            is HttpClientResponseException -> {
//                return if (exception.response.status == HttpStatus.valueOf(400)) {
//                    println(exception.response.body())
//                    Single.just(HttpResponse.status<JsonError>(HttpStatus.BAD_REQUEST, "Form Validation Failure")
//                            .body(JsonError(exception.response.body().toString())))
//                } else {
//                    //@TODO Add logging
//                    exception.printStackTrace()
//                    Single.just(HttpResponse.status<JsonError>(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable"))
//                }
//            }
//            else -> {
//                return Single.just(HttpResponse.status<JsonError>(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable"))
//            }
//        }
//    }
//}

@Validated
interface FormSubmissionOperations {

    @Post(value = "/submit")
    fun submit(@Body submission: Single<FormSubmission>): Single<HttpResponse<FormSubmission>>

    @Post(value = "/validate")
    fun validate(@Body submission: Single<FormSubmission>): Single<HttpResponse<ValidationResponseValid>>
}

//@Introspected
data class FormSubmission(
        val schema: FormSchema,
        val submission: FormSubmissionObject) {
}

//@Introspected
data class FormSubmissionObject(
        val data: Map<String, Any>,
        val metadata: Map<String, Any>?
) {}