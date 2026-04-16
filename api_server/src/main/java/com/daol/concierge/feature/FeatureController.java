package com.daol.concierge.feature;

import com.daol.concierge.auth.GuestPrincipal;
import com.daol.concierge.auth.SecurityContextUtil;
import com.daol.concierge.core.api.Responses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
@RequestMapping(value = "/api/concierge/features")
public class FeatureController {

	@Autowired private FeatureService featureService;

	@RequestMapping(method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public Responses.ListResponse list() {
		GuestPrincipal p = SecurityContextUtil.requirePrincipal();
		return Responses.ListResponse.of(featureService.listForGuest(p.propCd(), p.cmpxCd()));
	}
}
