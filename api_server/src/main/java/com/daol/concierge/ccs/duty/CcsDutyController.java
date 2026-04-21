package com.daol.concierge.ccs.duty;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.service.CcsDutyService;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "CCS Duty", description = "당직 로그 (Phase D)")
@Controller
@RequestMapping("/api/ccs/duty")
public class CcsDutyController extends BaseController {

	@Autowired private CcsDutyService service;

	@Operation(summary = "근무 시작 (교대 기록 생성)")
	@ResponseBody
	@RequestMapping(method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse start(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> body = new HashMap<>(requestParams.getParams());
		body.putIfAbsent("propCd", me.propCd());
		body.putIfAbsent("cmpxCd", me.cmpxCd());
		body.putIfAbsent("managerId", me.staffId());
		Map<String, Object> saved = service.startShift(body);
		return Responses.MapResponse.of(saved);
	}

	@Operation(summary = "당직 로그 목록")
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse list(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> filter = new HashMap<>();
		filter.put("propCd", me.propCd());
		filter.put("cmpxCd", me.cmpxCd());
		filter.put("dutyDateFrom", requestParams.getString("from"));
		filter.put("dutyDateTo", requestParams.getString("to"));
		List<Map<String, Object>> list = service.list(filter);
		return Responses.MapResponse.of(Map.of("list", list));
	}

	@Operation(summary = "오늘자 당직 로그 + PMS 야간감사 상태")
	@ResponseBody
	@RequestMapping(value = "/today", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse today(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> result = service.today(me.propCd(), me.cmpxCd(), requestParams.getString("shift"));
		return Responses.MapResponse.of(result);
	}

	@Operation(summary = "인수인계 기록")
	@ResponseBody
	@RequestMapping(value = "/{logId}/handover", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse handover(@PathVariable String logId, RequestParams requestParams) {
		CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> saved = service.handover(logId, requestParams.getParams());
		return Responses.MapResponse.of(saved);
	}

	@Operation(summary = "근무 종료 (요약/사건/PMS감사 상태 업데이트)")
	@ResponseBody
	@RequestMapping(value = "/{logId}/close", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse close(@PathVariable String logId, RequestParams requestParams) {
		CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> saved = service.close(logId, requestParams.getParams());
		return Responses.MapResponse.of(saved);
	}
}
