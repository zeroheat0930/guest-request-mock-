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

@Component
@ConditionalOnProperty(name = "concierge.dispatcher.ccs.enabled", havingValue = "true", matchIfMissing = true)
public class CcsDispatcher implements RequestDispatcher {

	private static final Logger log = LoggerFactory.getLogger(CcsDispatcher.class);

	@Autowired private CcsTaskService taskService;
	@Autowired private CcsRoutingRuleDefault routing;

	@Value("${concierge.tenant.prop-cd:0000000001}") private String propCd;
	@Value("${concierge.tenant.cmpx-cd:00001}") private String cmpxCd;

	@Override
	public void dispatch(RequestEvent event) {
		String deptCd = routing.deptCdFor(event.eventTp());
		if (deptCd == null) return;
		try {
			taskService.createTask(
					event.propCd() != null ? event.propCd() : propCd,
					event.cmpxCd() != null ? event.cmpxCd() : cmpxCd,
					"GUEST_REQ", event.reqNo(), deptCd,
					event.eventTitle(), event.reqMemo(), event.roomNo());
		} catch (Exception e) {
			log.warn("[ccs] task 생성 실패 tp={} reqNo={}: {}", event.eventTp(), event.reqNo(), e.getMessage());
		}
	}
}
