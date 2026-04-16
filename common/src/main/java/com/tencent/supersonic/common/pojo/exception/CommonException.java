package com.tencent.supersonic.common.pojo.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class CommonException extends RuntimeException {

    private Integer code;
    private String message;
}
