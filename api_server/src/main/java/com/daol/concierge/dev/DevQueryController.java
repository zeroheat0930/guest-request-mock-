package com.daol.concierge.dev;

import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import com.daol.concierge.pms.mapper.PmsMapper;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/dev")
@Profile("dev")
public class DevQueryController extends BaseController {

	@Autowired private PmsMapper pmsMapper;
	@Autowired private InvMapper invMapper;

	@ResponseBody
	@RequestMapping(value = "/reservations", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse reservations(RequestParams requestParams) {
		String propCd = requestParams.getString("propCd", "0000000001");
		String cmpxCd = requestParams.getString("cmpxCd", "00001");
		return Responses.ListResponse.of(pmsMapper.selectReservationList(propCd, cmpxCd));
	}

	@ResponseBody
	@RequestMapping(value = "/reservation", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse reservation(RequestParams requestParams) {
		String propCd = requestParams.getString("propCd", "0000000001");
		String cmpxCd = requestParams.getString("cmpxCd", "00001");
		String resvNo = requestParams.getString("resvNo");
		Map<String, Object> r = pmsMapper.selectReservation(propCd, cmpxCd, resvNo);
		return Responses.MapResponse.of(r != null ? r : Map.of("error", "not found"));
	}

	@ResponseBody
	@RequestMapping(value = "/features", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse features(RequestParams requestParams) {
		String propCd = requestParams.getString("propCd", "0000000001");
		String cmpxCd = requestParams.getString("cmpxCd", "00001");
		return Responses.ListResponse.of(invMapper.selectFeatures(propCd, cmpxCd));
	}

	@ResponseBody
	@RequestMapping(value = "/amenity-items", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse amenityItems(RequestParams requestParams) {
		String propCd = requestParams.getString("propCd", "0000000001");
		String cmpxCd = requestParams.getString("cmpxCd", "00001");
		return Responses.ListResponse.of(invMapper.selectAmenityItems(propCd, cmpxCd));
	}

	@ResponseBody
	@RequestMapping(value = "/users", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse users(RequestParams requestParams) {
		String propCd = requestParams.getString("propCd", "0000000001");
		String cmpxCd = requestParams.getString("cmpxCd", "00001");
		String deptCd = requestParams.getString("deptCd");
		return Responses.ListResponse.of(pmsMapper.selectUsersByDept(propCd, cmpxCd, deptCd));
	}
}
