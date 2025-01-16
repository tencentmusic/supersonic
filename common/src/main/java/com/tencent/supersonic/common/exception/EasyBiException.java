package com.tencent.supersonic.common.exception;

/**
 * @program: data-asset
 * @description: 元数据业务异常
 * @author: roylin
 * @create: 2021-04-29 17:00
 **/

public class EasyBiException extends RuntimeException {
    private IErrorCode errorCode;

    public EasyBiException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public EasyBiException(String message) {
        super(message);
    }

    public IErrorCode getErrorCode() {
        return errorCode;
    }
}
