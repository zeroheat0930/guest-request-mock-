package com.daol.concierge.ccs.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * PMS 동기화 OFF 기본 어댑터.
 *
 * {@link DaolPmsSyncAdapter} 가 활성화되지 않으면 이 어댑터가 바인딩돼
 * 도메인 서비스가 {@code syncXxx()} 를 호출해도 no-op 이 된다.
 * 타 호텔(PMS 없음) 배포에서 이 경로가 정상.
 */
@Component
@ConditionalOnMissingBean(DaolPmsSyncAdapter.class)
public class NoopPmsSyncAdapter implements PmsSyncAdapter {

	private static final Logger log = LoggerFactory.getLogger(NoopPmsSyncAdapter.class);

	public NoopPmsSyncAdapter() {
		log.info("[pms-sync] NoopPmsSyncAdapter 활성 — PMS 동기화 비활성화 상태 (CCS 독립 운영).");
	}

	@Override
	public boolean isEnabled() { return false; }
}
