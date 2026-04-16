package com.daol.concierge.dispatcher;

public record RequestEvent(
		String propCd,
		String cmpxCd,
		String eventTp,
		String eventTitle,
		String roomNo,
		String reqNo,
		String reqMemo
) {}
