package com.tencent.supersonic.common.pojo.exception;

public class InvalidArgumentException extends RuntimeException {

    private String message;

    public InvalidArgumentException(String message) {
        super(message);
    }
}
