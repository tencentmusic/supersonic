package com.tencent.supersonic.common.pojo.exception;

import java.util.Collections;
import java.util.List;

public class ParamValidationException extends RuntimeException {

    private final List<String> validationErrors;

    public ParamValidationException(String message) {
        super(message);
        this.validationErrors = Collections.singletonList(message);
    }

    public ParamValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors =
                validationErrors != null ? Collections.unmodifiableList(validationErrors)
                        : Collections.emptyList();
    }

    public ParamValidationException(List<String> validationErrors) {
        super(buildMessage(validationErrors));
        this.validationErrors =
                validationErrors != null ? Collections.unmodifiableList(validationErrors)
                        : Collections.emptyList();
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    private static String buildMessage(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Parameter validation failed";
        }
        return "Parameter validation failed: " + String.join("; ", errors);
    }
}
