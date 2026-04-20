package com.daol.concierge.core.controller;

import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.ApiStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.SQLException;

@ControllerAdvice
public class BaseController {

    protected static final String APPLICATION_JSON = "application/json; charset=UTF-8";
    private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

    public ApiResponse ok() {
        return ApiResponse.of(ApiStatus.SUCCESS, "SUCCESS");
    }

    public ApiResponse ok(String message) {
        return ApiResponse.of(ApiStatus.SUCCESS, message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ApiResponse handleForbidden(Exception e) {
        return ApiResponse.error(ApiStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(TypeMismatchException.class)
    @ResponseBody
    public ApiResponse handleBadRequestException(Exception e) {
        errorLogging(e);
        return ApiResponse.error(ApiStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public ApiResponse handleRequestParameterException(MissingServletRequestParameterException e) {
        errorLogging(e);
        return ApiResponse.error(ApiStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ApiException.class)
    @ResponseBody
    public ApiResponse handleApiException(ApiException apiException) {
        return ApiResponse.error(ApiStatus.getApiStatus(apiException.getCode()), apiException.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    public ApiResponse handleException(Throwable throwable) {
        errorLogging(throwable);
        ApiResponse apiResponse = ApiResponse.error(ApiStatus.SYSTEM_ERROR,
                StringUtils.isEmpty(throwable.getMessage()) ? throwable.toString() : throwable.getMessage());
        Throwable rootCause = ExceptionUtils.getRootCause(throwable);
        if (rootCause instanceof SQLException) {
            apiResponse = ApiResponse.error(ApiStatus.SYSTEM_ERROR,
                    String.format("데이터 처리중 에러가 발생하였습니다.%n시스템 관리자에게 문의하세요."));
        }
        return apiResponse;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Object processValidationError(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldErrors().get(0);
        ApiResponse error = ApiResponse.error(ApiStatus.SYSTEM_ERROR, fieldError.getDefaultMessage());
        error.getError().setRequiredKey(fieldError.getField());
        return error;
    }

    protected void errorLogging(Throwable throwable) {
        if (logger.isErrorEnabled()) {
            Throwable rootCause = ExceptionUtils.getRootCause(throwable);
            if (rootCause != null) throwable = rootCause;
            logger.error(throwable.getMessage() != null ? throwable.getMessage() : "ERROR", throwable);
        }
    }
}
