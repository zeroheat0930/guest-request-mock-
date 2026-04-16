package com.daol.concierge.ccs.admin;

import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import com.daol.concierge.pms.mapper.PmsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/concierge/admin")
public class CcsAdminController extends BaseController {

	@Autowired private PmsMapper pmsMapper;

	@ResponseBody
	@RequestMapping(value = "/departments", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse listDepartments(RequestParams requestParams) {
		String propCd = requestParams.getString("propCd");
		String cmpxCd = requestParams.getString("cmpxCd");
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
		return Responses.MapResponse.of(Map.of("list", depts));
	}

	@ResponseBody
	@RequestMapping(value = "/staff", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse listStaff(RequestParams requestParams) {
		String propCd = requestParams.getString("propCd");
		String cmpxCd = requestParams.getString("cmpxCd");
		String deptCd = requestParams.getString("deptCd");
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
		return Responses.MapResponse.of(Map.of("list", out));
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
