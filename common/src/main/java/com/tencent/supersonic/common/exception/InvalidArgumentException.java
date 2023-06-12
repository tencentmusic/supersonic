package com.tencent.supersonic.common.exception;

public class InvalidArgumentException extends RuntimeException {

    private String message;

    public InvalidArgumentException(String message) {
        super(message);
    }
}
