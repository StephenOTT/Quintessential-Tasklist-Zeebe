package com.github.stephenott.qtz.forms.validator.domain

import com.github.stephenott.qtz.forms.domain.FormSchema

data class FormSubmission(
        val schema: FormSchema,
        val submission: FormSubmissionData) {}

data class FormSubmissionData(
        val data: Map<String, Any?>,
        val metadata: Map<String, Any?>?) {}

