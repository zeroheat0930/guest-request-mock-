package com.daol.concierge.feature;

import com.daol.concierge.core.api.Responses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * 관리자 — 기능 플래그 조회/저장.
 *
 * 모든 요청은 AdminAuthInterceptor (X-Admin-Token 헤더) 를 통과해야 한다.
 */
@Controller
@ResponseBody
@RequestMapping(value = "/api/concierge/admin/features")
public class FeatureAdminController {

	private static final String APPLICATION_JSON = "application/json;charset=UTF-8";

	@Autowired private FeatureService featureService;

	@RequestMapping(method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse list(@RequestParam String propCd) {
		return Responses.ListResponse.of(featureService.listForAdmin(propCd));
	}

	@RequestMapping(method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public Responses.ListResponse upsert(@RequestParam String propCd,
										 @RequestBody List<Map<String, Object>> rows) {
		return Responses.ListResponse.of(featureService.upsertBulk(propCd, rows));
	}
}
