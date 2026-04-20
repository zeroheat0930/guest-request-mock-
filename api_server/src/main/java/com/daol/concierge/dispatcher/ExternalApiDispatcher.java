package com.daol.concierge.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 외부 REST PMS 디스패처 스텁.
 *
 * TODO: 실제 외부 PMS 연동 시 REST shape 결정 후 구현 (auth, retry, outbox 등).
 */
@Component
@ConditionalOnProperty(name = "concierge.dispatcher.external.enabled", havingValue = "true")
public class ExternalApiDispatcher implements RequestDispatcher {

	private static final Logger log = LoggerFactory.getLogger(ExternalApiDispatcher.class);

	@Override
	public void dispatch(RequestEvent event) {
		log.warn("[dispatcher:external] not implemented yet — event dropped: tp={} title={}",
				event.eventTp(), event.eventTitle());
	}
}
