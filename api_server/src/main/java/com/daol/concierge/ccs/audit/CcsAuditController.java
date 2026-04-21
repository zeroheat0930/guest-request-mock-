package com.daol.concierge.ccs.audit;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "CCS Audit", description = "감사 로그 조회 (Phase E)")
@Controller
@RequestMapping("/api/ccs/audit")
public class CcsAuditController extends BaseController {

	@Autowired private AuditLogService auditLogService;

	@Operation(summary = "감사 로그 조회", description = "domain/action/actorId/날짜 필터 + 최대 500건")
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse list(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> filter = new HashMap<>();
		filter.put("propCd", me.propCd());
		filter.put("cmpxCd", me.cmpxCd());
		filter.put("domain", requestParams.getString("domain"));
		filter.put("action", requestParams.getString("action"));
		filter.put("actorId", requestParams.getString("actorId"));
		filter.put("fromDt", requestParams.getString("from"));
		filter.put("toDt", requestParams.getString("to"));
		List<Map<String, Object>> list = auditLogService.query(filter);
		return Responses.MapResponse.of(Map.of("list", list));
	}
}
