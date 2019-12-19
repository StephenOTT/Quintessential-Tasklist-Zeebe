package com.github.stephenott.qtz.forms.validator.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
import com.github.stephenott.qtz.forms.validator.domain.FormSubmission
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.jackson.annotation.JacksonFeatures
import io.reactivex.Single


@Client("\${formValidatorService.host}")
@JacksonFeatures(
        enabledDeserializationFeatures = [
            FAIL_ON_NULL_FOR_PRIMITIVES,
            FAIL_ON_IGNORED_PROPERTIES
        ]
)
interface FormValidatorServiceClient {

    @Post("/validate")
    fun validate(@Body validationRequest: Single<FormSubmission>): Single<HttpResponse<ValidationResponseValid>>

}

data class ValidationResponseValid(val processed_submission: Map<String, Any?>)

data class ValidationResponseInvalid(@get:JsonProperty("isJoi") @param:JsonProperty("isJoi") val isJoi: Boolean,
                                     val name: String,
                                     val details: List<Map<String, Any>>,
                                     val _object: Map<String, Any>,
                                     val _validated: Map<String, Any>)