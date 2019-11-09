package com.github.stephenott.qtz.forms.validator.client

import com.github.stephenott.qtz.forms.validator.FormSchema
import com.github.stephenott.qtz.forms.validator.FormSubmission
import com.github.stephenott.qtz.forms.validator.FormSubmissionObject
import com.github.stephenott.qtz.forms.validator.FormSubmissionOperations
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.annotation.Client
import io.reactivex.Single

@Client("/forms")
interface FormSubmissionClient : FormSubmissionOperations {

    override fun submit(submission: Single<FormSubmission>): Single<HttpResponse<FormSubmission>>

}