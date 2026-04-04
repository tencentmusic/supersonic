package com.tencent.supersonic.common.pojo.exception;

import lombok.Getter;

@Getter
public class QuotaExceededException extends RuntimeException {
    private final String resource;

    public QuotaExceededException(String resource, String message) {
        super(message);
        this.resource = resource;
    }
}
