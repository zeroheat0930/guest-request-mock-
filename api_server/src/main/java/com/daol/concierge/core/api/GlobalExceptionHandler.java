package com.daol.concierge.core.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BizException.class)
	public ApiResponse handleBiz(BizException e) {
		return Responses.fail(e.getResCd(), e.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ApiResponse handleAny(Exception e) {
		log.error("Unhandled error", e);
		return Responses.fail("9999", e.getMessage());
	}
}
