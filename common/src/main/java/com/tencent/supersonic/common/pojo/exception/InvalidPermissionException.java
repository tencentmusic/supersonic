package com.tencent.supersonic.common.pojo.exception;

public class InvalidPermissionException extends RuntimeException {

    private String message;

    public InvalidPermissionException(String message) {
        super(message);
    }
}
