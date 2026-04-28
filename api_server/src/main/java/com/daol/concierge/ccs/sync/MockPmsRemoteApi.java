package com.daol.concierge.ccs.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PMS REST API 가 비활성/미배포 상태에서 동작하는 mock.
 *
 * 시연/개발 환경에서 어드민이 PMS 데이터 변경을 시도하면
 * 메모리 캐시에 오버라이드를 저장하고, {@code applyUserOverrides()} 가
 * 직원 조회 결과에 적용해 화면에서 변경이 즉시 반영된 것처럼 보이게 한다.
 *
 * - 서버 재시작 시 초기화됨 (PMS 마스터 원본 그대로 복구)
 * - PMS_USER_MTR 등 PMS 테이블은 절대 만지지 않음
 * - {@link DaolPmsRemoteApi} 가 활성화되면 이 빈은 등록되지 않음
 */
@Component
@ConditionalOnMissingBean(DaolPmsRemoteApi.class)
public class MockPmsRemoteApi implements PmsRemoteApi {

	private static final Logger log = LoggerFactory.getLogger(MockPmsRemoteApi.class);

	private final Map<String, String> useYnOverride = new ConcurrentHashMap<>();
	private final Map<String, String> deptOverride  = new ConcurrentHashMap<>();

	public MockPmsRemoteApi() {
		log.info("[pms-api] MockPmsRemoteApi 활성 — 메모리 오버라이드 모드 (PMS 테이블 직접 변경 없음).");
	}

	@Override
	public boolean isEnabled() { return false; }

	@Override
	public void updateUserUseYn(String userId, String useYn, String actor) {
		useYnOverride.put(userId, useYn);
		log.info("[pms-api MOCK] updateUserUseYn userId={} useYn={} by={} (메모리 오버라이드)", userId, useYn, actor);
	}

	@Override
	public void updateUserDept(String userId, String deptCd, String actor) {
		deptOverride.put(userId, deptCd);
		log.info("[pms-api MOCK] updateUserDept userId={} deptCd={} by={} (메모리 오버라이드)", userId, deptCd, actor);
	}

	@Override
	public void applyUserOverrides(List<Map<String, Object>> users) {
		if (users == null || users.isEmpty()) return;
		if (useYnOverride.isEmpty() && deptOverride.isEmpty()) return;
		for (Map<String, Object> u : users) {
			String userId = String.valueOf(u.get("userId"));
			String useYn = useYnOverride.get(userId);
			if (useYn != null) u.put("useYn", useYn);
			String dept = deptOverride.get(userId);
			if (dept != null) u.put("deptCd", dept);
		}
	}

	/** 시연 종료 후 캐시 초기화용 (테스트 / 어드민 endpoint 에서 호출 가능). */
	public void reset() {
		useYnOverride.clear();
		deptOverride.clear();
		log.info("[pms-api MOCK] 오버라이드 캐시 초기화");
	}
}
