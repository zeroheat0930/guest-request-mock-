package com.daol.concierge.core.api;

/**
 * 비즈 로직 예외 (resCd 포함)
 */
public class BizException extends RuntimeException {

	private final String resCd;

	public BizException(String resCd, String resMsg) {
		super(resMsg);
		this.resCd = resCd;
	}

	public String getResCd() {
		return resCd;
	}
}
