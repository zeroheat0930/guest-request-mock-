package com.daol.concierge.ccs.task;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import com.daol.concierge.inv.mapper.InvMapper;
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
@RequestMapping("/api/ccs/dept")
public class CcsDeptLoadController extends BaseController {

	@Autowired private PmsMapper pmsMapper;
	@Autowired private InvMapper invMapper;

	@ResponseBody
	@RequestMapping(value = "/{deptCd}/load", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse load(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		String deptCd = requestParams.getString("deptCd");
		if (!me.deptCd().equals(deptCd)) throw new ApiException(ApiStatus.ACCESS_DENIED, "권한 없음");

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
		return Responses.MapResponse.of(Map.of("list", out));
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
