package com.daol.concierge.dispatcher;

/**
 * 요청 전달 추상화.
 *
 * 구현체는 `concierge.dispatcher` 설정(daol | external | internal) 으로 선택.
 * 호출 실패는 게스트 응답과 무관해야 하므로 구현체가 try/catch 로 삼켜야 한다.
 */
public interface RequestDispatcher {

	void dispatch(RequestEvent event);
}
