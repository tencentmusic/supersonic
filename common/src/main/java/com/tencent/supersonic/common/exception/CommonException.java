package com.tencent.supersonic.common.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CommonException extends RuntimeException {

    private Integer code;
    private String message;
}
