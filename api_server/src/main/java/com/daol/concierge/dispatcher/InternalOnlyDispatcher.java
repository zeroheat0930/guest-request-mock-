package com.daol.concierge.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 내부 전용 no-op 디스패처 (기본값).
 *
 * 외부 PMS 없이 단독 구동할 때 쓰이는 기본 구현. DB 저장은 이미 GrService 에서 끝났으므로
 * 여기서는 디버그 로그만 남긴다.
 */
@Component
@ConditionalOnProperty(name = "concierge.dispatcher", havingValue = "internal", matchIfMissing = true)
public class InternalOnlyDispatcher implements RequestDispatcher {

	private static final Logger log = LoggerFactory.getLogger(InternalOnlyDispatcher.class);

	@Override
	public void dispatch(RequestEvent event) {
		log.debug("[dispatcher:internal] tp={} title={}", event.eventTp(), event.eventTitle());
	}
}
