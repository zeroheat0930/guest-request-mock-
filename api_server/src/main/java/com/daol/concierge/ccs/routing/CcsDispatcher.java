package com.daol.concierge.ccs.routing;

import com.daol.concierge.ccs.service.CcsTaskService;
import com.daol.concierge.dispatcher.RequestDispatcher;
import com.daol.concierge.dispatcher.RequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 게스트 요청 → CCS 작업 자동 라우팅 디스패처.
 *
 * concierge.dispatcher.ccs.enabled=true(기본) 일 때 활성화.
 * CCS 오류는 게스트 요청 결과에 영향을 주지 않도록 삼킨다.
 */
@Component
@ConditionalOnProperty(name = "concierge.dispatcher.ccs.enabled", havingValue = "true", matchIfMissing = true)
public class CcsDispatcher implements RequestDispatcher {

	private static final Logger log = LoggerFactory.getLogger(CcsDispatcher.class);

	@Autowired private CcsTaskService taskService;
	@Autowired private CcsRoutingRuleDefault routing;

	@Value("${pms.prop-cd:0000000010}") private String propCd;
	@Value("${pms.cmpx-cd:00001}") private String cmpxCd;

	@Override
	public void dispatch(RequestEvent event) {
		String deptCd = routing.deptCdFor(event.eventTp());
		if (deptCd == null) return;
		try {
			taskService.createTask(propCd, cmpxCd, "GUEST_REQ",
				event.reqNo(), deptCd, event.eventTitle(),
				event.reqMemo(), event.roomNo());
		} catch (Exception e) {
			log.warn("[ccs] task 생성 실패 tp={} reqNo={}: {}", event.eventTp(), event.reqNo(), e.getMessage());
		}
	}
}
