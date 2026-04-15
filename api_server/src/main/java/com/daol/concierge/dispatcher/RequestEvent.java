package com.daol.concierge.dispatcher;

/**
 * 디스패처로 넘길 요청 이벤트 DTO (PMS 중립)
 */
public record RequestEvent(
		String propCd,
		String eventTp,
		String eventTitle,
		String roomNo,
		String reqNo,
		String reqMemo
) {}
