package com.github.stephenott.qtz.forms.validator

import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.reactivex.Single


@Client("\${formValidatorService.host}")
interface FormValidatorServiceClient {

    @Post("/validate")
    fun validate(@Body validationRequest: Single<FormSubmission>): Single<HttpResponse<ValidationResponseValid>>

}


data class ValidationRequest(val submission: FormSubmission) {}

data class ValidationResponse(val response: ValidationResponseType)

interface ValidationResponseType {
    val result: ValidationRequestResult
}

enum class ValidationRequestResult {
    VALID, INVALID, ERROR
}

data class ValidationResponseValid(@JsonUnwrapped val processed_submission: Map<String, Any>)

data class ValidationResponseInvalid(val isJoi: Boolean,
                                     val name: String,
                                     val details: List<Map<String, Any>>,
                                     val _object: Map<String, Any>,
                                     val _validated: Map<String, Any>,
                                     override val result: ValidationRequestResult = ValidationRequestResult.INVALID) : ValidationResponseType

data class ValidationResponseError(val statusCode: HttpStatus,
                                   val errorMessage: String,
                                   override val result: ValidationRequestResult = ValidationRequestResult.ERROR) : ValidationResponseType