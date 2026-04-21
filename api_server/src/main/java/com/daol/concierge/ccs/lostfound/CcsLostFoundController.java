package com.daol.concierge.ccs.lostfound;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.service.CcsLostFoundService;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.ApiStatus;
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

@Tag(name = "CCS LostFound", description = "분실물/습득물 관리 (Phase B)")
@Controller
@RequestMapping("/api/ccs/lostfound")
public class CcsLostFoundController extends BaseController {

	@Autowired private CcsLostFoundService service;

	@Operation(summary = "분실물 신고/등록", description = "게스트 분실 신고 또는 스태프 습득 등록")
	@ResponseBody
	@RequestMapping(method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse create(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> body = new HashMap<>(requestParams.getParams());
		body.putIfAbsent("propCd", me.propCd());
		body.putIfAbsent("cmpxCd", me.cmpxCd());
		body.putIfAbsent("reporterType", "STAFF");
		body.putIfAbsent("reporterRef", me.staffId());
		Map<String, Object> saved = service.createReport(body);
		return Responses.MapResponse.of(saved);
	}

	@Operation(summary = "분실물 목록 조회", description = "status/category 필터 지원")
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse list(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> filter = new HashMap<>();
		filter.put("propCd", me.propCd());
		filter.put("cmpxCd", me.cmpxCd());
		filter.put("statusCd", requestParams.getString("statusCd"));
		filter.put("category", requestParams.getString("category"));
		filter.put("reporterType", requestParams.getString("reporterType"));
		List<Map<String, Object>> list = service.list(filter);
		return Responses.MapResponse.of(Map.of("list", list));
	}

	@Operation(summary = "분실물 상태 변경", description = "FOUND/MATCHED/RETURNED/DISPOSED")
	@ResponseBody
	@RequestMapping(value = "/{lfId}/status", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse updateStatus(@PathVariable String lfId, RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		String statusCd = requestParams.getString("statusCd");
		if (statusCd == null || statusCd.isBlank())
			throw new ApiException(ApiStatus.BAD_REQUEST, "statusCd 필수");
		Map<String, Object> saved = service.updateStatus(lfId, statusCd, me.staffId(),
				requestParams.getString("note"));
		return Responses.MapResponse.of(saved);
	}

	@Operation(summary = "분실물-습득 매칭", description = "GUEST 신고와 STAFF 습득 연결")
	@ResponseBody
	@RequestMapping(value = "/{lfId}/match", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse match(@PathVariable String lfId, RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		String matchedLfId = requestParams.getString("matchedLfId");
		if (matchedLfId == null || matchedLfId.isBlank())
			throw new ApiException(ApiStatus.BAD_REQUEST, "matchedLfId 필수");
		Map<String, Object> saved = service.match(lfId, matchedLfId, me.staffId());
		return Responses.MapResponse.of(saved);
	}
}
