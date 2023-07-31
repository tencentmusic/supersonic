package com.tencent.supersonic.advice;

import com.tencent.supersonic.common.pojo.exception.AccessException;
import com.tencent.supersonic.common.pojo.exception.CommonException;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.common.pojo.ResultData;
import com.tencent.supersonic.common.pojo.ReturnCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    /**
     * default global exception handler
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> exception(Exception e) {
        log.error("default global exception", e);
        return ResultData.fail(ReturnCode.SYSTEM_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(AccessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> accessException(Exception e) {
        log.error("default global exception", e);
        return ResultData.fail(ReturnCode.ACCESS_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(InvalidPermissionException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> invalidPermissionException(Exception e) {
        log.error("default global exception", e);
        return ResultData.fail(ReturnCode.INVALID_PERMISSION.getCode(), e.getMessage());
    }

    @ExceptionHandler(CommonException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> commonException(CommonException e) {
        log.error("default global exception", e);
        return ResultData.fail(e.getCode(), e.getMessage());
    }

}