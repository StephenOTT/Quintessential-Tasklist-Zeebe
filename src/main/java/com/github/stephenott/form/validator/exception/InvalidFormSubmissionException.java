package com.github.stephenott.form.validator.exception;

import com.github.stephenott.form.validator.ValidationRequestResult;

public class InvalidFormSubmissionException extends IllegalArgumentException {
    private ValidationRequestResult.InvalidResult invalidResult;

    public InvalidFormSubmissionException(String s, ValidationRequestResult.InvalidResult invalidResult) {
        super(s);
        this.invalidResult = invalidResult;
    }

    public ValidationRequestResult.InvalidResult getInvalidResult() {
        return invalidResult;
    }
}
