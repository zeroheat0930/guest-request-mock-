package com.daol.concierge.ccs.stats;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.util.CcsResponse;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ccs/stats")
public class CcsStatsController {

	@Autowired private InvMapper invMapper;

	@GetMapping("/today")
	public Map<String, Object> today(@RequestParam(required = false) String deptCd) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		String dept = (deptCd != null && !deptCd.isBlank()) ? deptCd : me.deptCd();

		Map<String, Object> stats = invMapper.selectTodayStats(me.propCd(), me.cmpxCd(), dept);

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("received", stats != null ? stats.get("totalCnt") : 0);
		map.put("completed", stats != null ? stats.get("doneCnt") : 0);
		map.put("avgMinutes", stats != null ? stats.get("avgMinutes") : null);
		return CcsResponse.ok(map);
	}
}
