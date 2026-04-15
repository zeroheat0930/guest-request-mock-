package com.daol.concierge.ccs.task;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.domain.CcsStaff;
import com.daol.concierge.ccs.repo.CcsStaffRepository;
import com.daol.concierge.ccs.repo.CcsTaskRepository;
import com.daol.concierge.ccs.util.CcsResponse;
import com.daol.concierge.core.api.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CCS 부서 로드 조회 — US-011
 *
 * GET /api/ccs/dept/{deptCd}/load
 * 응답: { resCd, resMsg, map: { list: [ { staffId, staffNm, assignedCount, inProgCount } ] } }
 */
@RestController
@RequestMapping("/api/ccs/dept")
public class CcsDeptLoadController {

	@Autowired
	private CcsStaffRepository staffRepo;

	@Autowired
	private CcsTaskRepository taskRepo;

	@GetMapping("/{deptCd}/load")
	public Map<String, Object> load(@PathVariable String deptCd) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();

		// 자기 부서만 조회 가능
		if (!me.deptCd().equals(deptCd)) {
			throw new BizException("9102", "권한 없음");
		}

		List<CcsStaff> members = staffRepo.findByPropCdAndCmpxCdAndDeptCdOrderByStaffNm(
				me.propCd(), me.cmpxCd(), deptCd);

		List<Map<String, Object>> out = new ArrayList<>();
		for (CcsStaff s : members) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("staffId", s.getStaffId());
			row.put("staffNm", s.getStaffNm());
			row.put("assignedCount", taskRepo.countByAssigneeIdAndStatusCd(s.getStaffId(), "ASSIGNED"));
			row.put("inProgCount",   taskRepo.countByAssigneeIdAndStatusCd(s.getStaffId(), "IN_PROG"));
			out.add(row);
		}

		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("list", out);

		return CcsResponse.ok(inner);
	}
}
