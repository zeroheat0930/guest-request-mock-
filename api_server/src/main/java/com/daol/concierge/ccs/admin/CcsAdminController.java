package com.daol.concierge.ccs.admin;

import com.daol.concierge.ccs.util.CcsResponse;
import com.daol.concierge.pms.mapper.PmsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/concierge/admin")
public class CcsAdminController {

	@Autowired private PmsMapper pmsMapper;

	@GetMapping("/departments")
	public Map<String, Object> listDepartments(
			@RequestParam String propCd,
			@RequestParam String cmpxCd) {
		// PMS_USER_MTR 의 DEPT_CD 그룹핑으로 부서 목록 추출
		List<Map<String, Object>> allUsers = pmsMapper.selectUsersByDept(propCd, cmpxCd, null);
		Map<String, String> deptMap = new LinkedHashMap<>();
		for (Map<String, Object> u : allUsers) {
			String deptCd = str(u.get("deptCd"));
			if (deptCd != null && !deptMap.containsKey(deptCd)) {
				deptMap.put(deptCd, deptCd);
			}
		}
		List<Map<String, Object>> depts = new ArrayList<>();
		for (String dc : deptMap.keySet()) {
			depts.add(Map.of("deptCd", dc, "deptNm", dc, "useYn", "Y"));
		}
		return CcsResponse.ok(Map.of("list", depts));
	}

	@GetMapping("/staff")
	public Map<String, Object> listStaff(
			@RequestParam String propCd,
			@RequestParam String cmpxCd,
			@RequestParam(required = false) String deptCd) {
		List<Map<String, Object>> users;
		if (deptCd != null && !deptCd.isBlank()) {
			users = pmsMapper.selectUsersByDept(propCd, cmpxCd, deptCd);
		} else {
			users = pmsMapper.selectUsersByDept(propCd, cmpxCd, null);
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> u : users) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("staffId", str(u.get("userId")));
			row.put("loginId", str(u.get("userId")));
			row.put("staffNm", str(u.get("userNm")));
			row.put("deptCd", str(u.get("deptCd")));
			row.put("useYn", str(u.get("useYn")));
			out.add(row);
		}
		return CcsResponse.ok(Map.of("list", out));
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
