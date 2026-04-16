package com.daol.concierge.ccs.task;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.util.CcsResponse;
import com.daol.concierge.core.api.BizException;
import com.daol.concierge.inv.mapper.InvMapper;
import com.daol.concierge.pms.mapper.PmsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ccs/dept")
public class CcsDeptLoadController {

	@Autowired private PmsMapper pmsMapper;
	@Autowired private InvMapper invMapper;

	@GetMapping("/{deptCd}/load")
	public Map<String, Object> load(@PathVariable String deptCd) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		if (!me.deptCd().equals(deptCd)) throw new BizException("9102", "권한 없음");

		List<Map<String, Object>> members = pmsMapper.selectUsersByDept(me.propCd(), me.cmpxCd(), deptCd);

		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> m : members) {
			String userId = str(m.get("userId"));
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("staffId", userId);
			row.put("staffNm", str(m.get("userNm")));
			row.put("assignedCount", invMapper.selectTaskCountByStatus(userId, "ASSIGNED"));
			row.put("inProgCount", invMapper.selectTaskCountByStatus(userId, "IN_PROG"));
			out.add(row);
		}
		return CcsResponse.ok(Map.of("list", out));
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
