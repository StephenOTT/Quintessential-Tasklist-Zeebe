package com.github.stephenott.qtz.forms.validator.exception

import com.github.stephenott.qtz.forms.validator.client.ValidationResponseInvalid

class FormValidationException(val responseBody: ValidationResponseInvalid) : RuntimeException("Form Validation Exception") {}
