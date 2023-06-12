package com.tencent.supersonic.common.exception;

public class AccessException extends RuntimeException {

    private String message;

    public AccessException(String message) {
        super(message);
    }
}
