package com.github.stephenott.qtz.forms.validator

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.DeserializationFeature.*
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.jackson.annotation.JacksonFeatures
import io.reactivex.Single


@Client("\${formValidatorService.host}")
@JacksonFeatures(
        enabledDeserializationFeatures = [FAIL_ON_NULL_FOR_PRIMITIVES, FAIL_ON_IGNORED_PROPERTIES]
)
interface FormValidatorServiceClient {

    @Post("/validate")
    fun validate(@Body validationRequest: Single<FormSubmission>): Single<HttpResponse<ValidationResponseValid>>

}

data class ValidationResponseValid(@JsonUnwrapped val processed_submission: Map<String, Any>)

data class ValidationResponseInvalid(@get:JsonProperty("isJoi") @param:JsonProperty("isJoi") val isJoi: Boolean,
                                     val name: String,
                                     val details: List<Map<String, Any>>,
                                     val _object: Map<String, Any>,
                                     val _validated: Map<String, Any>)