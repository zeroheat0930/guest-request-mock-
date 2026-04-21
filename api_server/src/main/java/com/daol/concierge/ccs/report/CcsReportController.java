package com.daol.concierge.ccs.report;

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

@Tag(name = "CCS Reports", description = "관리자 리포트 (Phase E)")
@Controller
@RequestMapping("/api/ccs/reports")
public class CcsReportController extends BaseController {

	@Autowired private CcsReportService service;

	@Operation(summary = "일일 통계", description = "날짜 x 부서 x 소스타입 별 요청/완료 카운트 + SLA 준수 여부")
	@ResponseBody
	@RequestMapping(value = "/daily", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse daily(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> filter = buildFilter(me, requestParams);
		List<Map<String, Object>> list = service.daily(filter);
		return Responses.MapResponse.of(Map.of("list", list));
	}

	@Operation(summary = "SLA 준수율", description = "부서 x 소스타입 별 완료율/평균 처리시간")
	@ResponseBody
	@RequestMapping(value = "/sla", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse sla(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> filter = buildFilter(me, requestParams);
		List<Map<String, Object>> list = service.sla(filter);
		return Responses.MapResponse.of(Map.of("list", list));
	}

	@Operation(summary = "요청 히트맵", description = "요일(1~7) x 시간(0~23) 별 요청 카운트")
	@ResponseBody
	@RequestMapping(value = "/heatmap", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse heatmap(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> filter = buildFilter(me, requestParams);
		List<Map<String, Object>> list = service.heatmap(filter);
		return Responses.MapResponse.of(Map.of("list", list));
	}

	private static Map<String, Object> buildFilter(CcsPrincipal me, RequestParams rp) {
		Map<String, Object> filter = new HashMap<>();
		filter.put("propCd", me.propCd());
		filter.put("cmpxCd", me.cmpxCd());
		filter.put("fromDt", rp.getString("from"));
		filter.put("toDt", rp.getString("to"));
		return filter;
	}
}
