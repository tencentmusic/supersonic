package com.tencent.supersonic.common.exception;

public class InvalidPermissionException extends RuntimeException {

    private String message;

    public InvalidPermissionException(String message) {
        super(message);
    }
}
