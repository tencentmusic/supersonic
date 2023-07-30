package com.tencent.supersonic.common.pojo.exception;

public class AccessException extends RuntimeException {

    private String message;

    public AccessException(String message) {
        super(message);
    }
}
