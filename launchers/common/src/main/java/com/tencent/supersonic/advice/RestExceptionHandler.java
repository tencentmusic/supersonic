package com.tencent.supersonic.advice;

import com.tencent.supersonic.common.pojo.ResultData;
import com.tencent.supersonic.common.pojo.enums.ReturnCode;
import com.tencent.supersonic.common.pojo.exception.AccessException;
import com.tencent.supersonic.common.pojo.exception.CommonException;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.common.pojo.exception.QuotaExceededException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    /**
     * Handle client abort exceptions (connection reset, broken pipe). These are normal when clients
     * disconnect before response completes.
     */
    @ExceptionHandler(ClientAbortException.class)
    @ResponseStatus(HttpStatus.OK)
    public void clientAbortException(ClientAbortException e) {
        // Log at debug level only - this is normal client behavior
        if (log.isDebugEnabled()) {
            log.debug("Client disconnected: {}", e.getMessage());
        }
    }

    /** @Valid on @RequestBody — field-level validation failures */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("request validation failed: {}", message);
        return ResultData.fail(ReturnCode.INVALID_REQUEST.getCode(), message);
    }

    /** @Validated on path/query params — constraint violations */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> constraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("constraint violation: {}", message);
        return ResultData.fail(ReturnCode.INVALID_REQUEST.getCode(), message);
    }

    /** Malformed or missing request body */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> httpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("request body not readable: {}", e.getMessage());
        return ResultData.fail(ReturnCode.INVALID_REQUEST.getCode(),
                "request body is missing or malformed");
    }

    /** Missing required query/path parameter */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> missingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        log.warn("missing request parameter: {}", e.getMessage());
        return ResultData.fail(ReturnCode.INVALID_REQUEST.getCode(), e.getMessage());
    }

    /** Wrong type for query/path parameter */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> methodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        String message = "parameter '" + e.getName() + "' must be of type "
                + (e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");
        log.warn("argument type mismatch: {}", message);
        return ResultData.fail(ReturnCode.INVALID_REQUEST.getCode(), message);
    }

    /** Static resource not found (e.g. browser hitting root path) */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResultData<String> noResourceFoundException(NoResourceFoundException e) {
        log.debug("resource not found: {}", e.getMessage());
        return ResultData.fail(ReturnCode.SYSTEM_ERROR.getCode(), "resource not found");
    }

    @ExceptionHandler(QuotaExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ResultData<String> quotaExceededException(QuotaExceededException e) {
        log.warn("quota exceeded for resource [{}]: {}", e.getResource(), e.getMessage());
        return ResultData.fail(ReturnCode.QUOTA_EXCEEDED.getCode(), e.getMessage());
    }

    /** default global exception handler */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> exception(Exception e) {
        // Check for client abort exceptions that may be wrapped
        if (isClientAbortException(e)) {
            if (log.isDebugEnabled()) {
                log.debug("Client disconnected: {}", e.getMessage());
            }
            return null;
        }
        log.error("default global exception", e);
        return ResultData.fail(ReturnCode.SYSTEM_ERROR.getCode(), e.getMessage());
    }

    /**
     * Check if the exception is caused by client disconnection.
     */
    private boolean isClientAbortException(Throwable e) {
        if (e == null) {
            return false;
        }
        if (e instanceof ClientAbortException) {
            return true;
        }
        String message = e.getMessage();
        if (message != null && (message.contains("Connection reset")
                || message.contains("Broken pipe") || message.contains("connection was aborted"))) {
            return true;
        }
        return isClientAbortException(e.getCause());
    }

    @ExceptionHandler(AccessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> accessException(Exception e) {
        return ResultData.fail(ReturnCode.ACCESS_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(InvalidPermissionException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> invalidPermissionException(Exception e) {
        log.error("default global exception", e);
        return ResultData.fail(ReturnCode.INVALID_PERMISSION.getCode(), e.getMessage());
    }

    @ExceptionHandler(InvalidArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> invalidArgumentException(Exception e) {
        log.error("default global exception", e);
        return ResultData.fail(ReturnCode.INVALID_REQUEST.getCode(), e.getMessage());
    }

    @ExceptionHandler(CommonException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> commonException(CommonException e) {
        log.error("default global exception", e);
        return ResultData.fail(e.getCode(), e.getMessage());
    }
}
