package com.daol.concierge.ccs.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * DAOL PMS 배포용 동기화 어댑터 (stub).
 *
 * 활성 조건: {@code concierge.pms.sync.enabled=true} (application.yml 또는 env).
 *
 * 실제 동기화 로직 (PMS REST 호출 or PMS DB INSERT) 은 Phase B+ 에서
 * 각 도메인 구현 완료 후 채운다. 현재는 로그만 남기는 placeholder.
 *
 * 동기화 정책:
 *  - CCS 가 로컬 INV 저장 성공 직후 이 어댑터 호출 (fire-and-forget).
 *  - 실패해도 CCS 트랜잭션 롤백하지 않음 (재시도 큐는 별도 future work).
 *  - DAOL PMS 접속정보는 별도 설정 블록(concierge.pms.daol.*) 으로 분리 예정.
 */
@Component
@ConditionalOnProperty(name = "concierge.pms.sync.enabled", havingValue = "true")
public class DaolPmsSyncAdapter implements PmsSyncAdapter {

	private static final Logger log = LoggerFactory.getLogger(DaolPmsSyncAdapter.class);

	public DaolPmsSyncAdapter() {
		log.info("[pms-sync] DaolPmsSyncAdapter 활성 — PMS 로 동기화됨 (DAOL 배포 모드).");
	}

	@Override
	public boolean isEnabled() { return true; }

	@Override
	public void syncLostFound(Map<String, Object> payload) {
		log.info("[pms-sync] syncLostFound (stub) — TODO Phase B: PMS_LOSTFOUND INSERT 또는 /api/ht/hk/lostFoundMgmt 호출");
	}

	@Override
	public void syncVoc(Map<String, Object> payload) {
		log.info("[pms-sync] syncVoc (stub) — TODO Phase B: PMS_CUST_VOC INSERT 또는 /api/gm/voiceOfCustomerMgmt 호출");
	}

	@Override
	public void syncRental(Map<String, Object> payload) {
		log.info("[pms-sync] syncRental (stub) — TODO Phase D: PMS_LOAN INSERT 또는 /api/ht/hk/loanAndRecoveryMgmt 호출");
	}

	@Override
	public void syncDuty(Map<String, Object> payload) {
		log.info("[pms-sync] syncDuty (stub) — TODO Phase D: PMS_DUTY_LOG INSERT 또는 야간감사 API 연동");
	}
}
