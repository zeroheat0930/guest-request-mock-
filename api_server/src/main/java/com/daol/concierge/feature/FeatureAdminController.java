package com.daol.concierge.feature;

import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/api/concierge/admin/features")
public class FeatureAdminController extends BaseController {

	@Autowired private FeatureService featureService;

	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse list(RequestParams requestParams) {
		String propCd = requestParams.getString("propCd");
		String cmpxCd = requestParams.getString("cmpxCd", "00001");
		return Responses.ListResponse.of(featureService.listForAdmin(propCd, cmpxCd));
	}

	@ResponseBody
	@RequestMapping(method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public Responses.ListResponse upsert(RequestParams requestParams,
	                                      @RequestBody List<Map<String, Object>> rows) {
		String propCd = requestParams.getString("propCd");
		String cmpxCd = requestParams.getString("cmpxCd", "00001");
		return Responses.ListResponse.of(featureService.upsertBulk(propCd, cmpxCd, rows));
	}
}
