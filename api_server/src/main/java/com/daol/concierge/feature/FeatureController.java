package com.daol.concierge.feature;

import com.daol.concierge.auth.SecurityContextUtil;
import com.daol.concierge.core.api.Responses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 게스트 — 본인 프로퍼티에 활성화된 기능 목록.
 * propCd 는 JWT principal 에서 가져오므로 프론트는 파라미터를 보낼 필요가 없다.
 */
@Controller
@ResponseBody
@RequestMapping(value = "/api/concierge/features")
public class FeatureController {

	private static final String APPLICATION_JSON = "application/json;charset=UTF-8";

	@Autowired private FeatureService featureService;

	@RequestMapping(method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse list() {
		String propCd = SecurityContextUtil.requirePrincipal().propCd();
		return Responses.ListResponse.of(featureService.listForGuest(propCd));
	}
}
