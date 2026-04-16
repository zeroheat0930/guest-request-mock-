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

@Controller
@ResponseBody
@RequestMapping(value = "/api/concierge/admin/features")
public class FeatureAdminController {

	@Autowired private FeatureService featureService;

	@RequestMapping(method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public Responses.ListResponse list(@RequestParam String propCd,
	                                    @RequestParam(defaultValue = "00001") String cmpxCd) {
		return Responses.ListResponse.of(featureService.listForAdmin(propCd, cmpxCd));
	}

	@RequestMapping(method = RequestMethod.PUT, produces = "application/json;charset=UTF-8")
	public Responses.ListResponse upsert(@RequestParam String propCd,
	                                      @RequestParam(defaultValue = "00001") String cmpxCd,
	                                      @RequestBody List<Map<String, Object>> rows) {
		return Responses.ListResponse.of(featureService.upsertBulk(propCd, cmpxCd, rows));
	}
}
