package com.daol.concierge.ccs.stats;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/ccs/stats")
public class CcsStatsController extends BaseController {

	@Autowired private InvMapper invMapper;

	@ResponseBody
	@RequestMapping(value = "/today", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse today(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		String deptCd = requestParams.getString("deptCd");
		String dept = (deptCd != null && !deptCd.isBlank()) ? deptCd : me.deptCd();

		Map<String, Object> stats = invMapper.selectTodayStats(me.propCd(), me.cmpxCd(), dept);

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("received", stats != null ? stats.get("totalCnt") : 0);
		map.put("completed", stats != null ? stats.get("doneCnt") : 0);
		map.put("avgMinutes", stats != null ? stats.get("avgMinutes") : null);
		return Responses.MapResponse.of(map);
	}
}
