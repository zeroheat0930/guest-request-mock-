package com.daol.concierge.feature;

import com.daol.concierge.ccs.auth.AdminMenu;
import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.MenuAccess;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/api/concierge/admin/features")
public class FeatureAdminController extends BaseController {

	@Autowired private FeatureService featureService;
	@Autowired private InvMapper invMapper;

	private CcsPrincipal principal() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Object p = auth != null ? auth.getPrincipal() : null;
		if (!(p instanceof CcsPrincipal)) {
			throw new ApiException(ApiStatus.ACCESS_DENIED, "인증 필요");
		}
		return (CcsPrincipal) p;
	}

	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse list(RequestParams requestParams) {
		MenuAccess.assertCanAccess(principal(), AdminMenu.FEATURES, invMapper);
		String propCd = requestParams.getString("propCd");
		String cmpxCd = requestParams.getString("cmpxCd", "00001");
		return Responses.ListResponse.of(featureService.listForAdmin(propCd, cmpxCd));
	}

	@ResponseBody
	@RequestMapping(method = RequestMethod.PUT, produces = APPLICATION_JSON)
	@SuppressWarnings("unchecked")
	public Responses.ListResponse upsert(RequestParams requestParams) {
		// RequestParamsArgumentResolver 가 body 의 InputStream 을 한번 소비하므로
		// @RequestBody 와 같은 컨트롤러에 공존시키면 IllegalStateException 발생.
		// 따라서 본문은 { "rows": [...] } 형태의 Map 으로 받아 RequestParams 에서 추출.
		MenuAccess.assertCanAccess(principal(), AdminMenu.FEATURES, invMapper);
		String propCd = requestParams.getString("propCd");
		String cmpxCd = requestParams.getString("cmpxCd", "00001");
		Object rowsRaw = requestParams.getParams().get("rows");
		List<Map<String, Object>> rows = Collections.emptyList();
		if (rowsRaw instanceof List<?>) {
			rows = (List<Map<String, Object>>) rowsRaw;
		}
		if (rows.isEmpty()) {
			throw new ApiException(ApiStatus.BAD_REQUEST, "rows 파라미터 필수 (List<Map>)");
		}
		return Responses.ListResponse.of(featureService.upsertBulk(propCd, cmpxCd, rows));
	}
}
