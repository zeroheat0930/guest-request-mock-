package com.daol.concierge.ccs.voc;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.service.CcsVocService;
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

@Tag(name = "CCS VOC", description = "고객 불만 관리 (Phase B)")
@Controller
@RequestMapping("/api/ccs/voc")
public class CcsVocController extends BaseController {

	@Autowired private CcsVocService service;

	@Operation(summary = "VOC 등록", description = "스태프/관리자 접수")
	@ResponseBody
	@RequestMapping(method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse create(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> body = new HashMap<>(requestParams.getParams());
		body.putIfAbsent("propCd", me.propCd());
		body.putIfAbsent("cmpxCd", me.cmpxCd());
		Map<String, Object> saved = service.createReport(body);
		return Responses.MapResponse.of(saved);
	}

	@Operation(summary = "VOC 목록 조회")
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse list(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> filter = new HashMap<>();
		filter.put("propCd", me.propCd());
		filter.put("cmpxCd", me.cmpxCd());
		filter.put("statusCd", requestParams.getString("statusCd"));
		filter.put("category", requestParams.getString("category"));
		filter.put("severity", requestParams.getString("severity"));
		List<Map<String, Object>> list = service.list(filter);
		return Responses.MapResponse.of(Map.of("list", list));
	}

	@Operation(summary = "VOC 상태 변경")
	@ResponseBody
	@RequestMapping(value = "/{vocId}/status", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse updateStatus(@PathVariable String vocId, RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		String statusCd = requestParams.getString("statusCd");
		if (statusCd == null || statusCd.isBlank())
			throw new ApiException(ApiStatus.BAD_REQUEST, "statusCd 필수");
		Map<String, Object> saved = service.updateStatus(vocId, statusCd, me.staffId());
		return Responses.MapResponse.of(saved);
	}

	@Operation(summary = "VOC 해결 처리 (RESOLUTION 저장)")
	@ResponseBody
	@RequestMapping(value = "/{vocId}/resolve", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse resolve(@PathVariable String vocId, RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		String resolution = requestParams.getString("resolution");
		if (resolution == null || resolution.isBlank())
			throw new ApiException(ApiStatus.BAD_REQUEST, "resolution 필수");
		Map<String, Object> saved = service.resolve(vocId, resolution, me.staffId());
		return Responses.MapResponse.of(saved);
	}

	@Operation(summary = "VOC 만족도 평가 (1~5)", description = "게스트 피드백")
	@ResponseBody
	@RequestMapping(value = "/{vocId}/satisfaction", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse satisfaction(@PathVariable String vocId, RequestParams requestParams) {
		int rating = requestParams.getInt("rating", 0);
		Map<String, Object> saved = service.satisfaction(vocId, rating);
		return Responses.MapResponse.of(saved);
	}
}
