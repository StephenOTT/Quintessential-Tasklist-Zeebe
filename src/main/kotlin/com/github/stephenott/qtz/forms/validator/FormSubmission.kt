package com.github.stephenott.qtz.forms.validator

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import io.reactivex.Single


@Controller("/forms")
open class FormSubmissionController : FormSubmissionOperations {

    override fun submit(submission: Single<FormSubmission>): Single<HttpResponse<FormSubmission>> {
        return submission.map { HttpResponse.ok(it) }
    }
}


@Validated
interface FormSubmissionOperations {
    @Post(value = "/submit",
            consumes = [MediaType.APPLICATION_JSON],
            produces = [MediaType.APPLICATION_JSON])

    fun submit(@Body submission: Single<FormSubmission>): Single<HttpResponse<FormSubmission>>
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