package com.daol.concierge.ccs.admin;

import com.daol.concierge.ccs.auth.AdminMenu;
import com.daol.concierge.ccs.auth.AdminRoles;
import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.MenuAccess;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import com.daol.concierge.inv.mapper.InvMapper;
import com.daol.concierge.pms.mapper.PmsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
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

	private static final Logger log = LoggerFactory.getLogger(CcsAdminController.class);

	@Autowired private PmsMapper pmsMapper;
	@Autowired private InvMapper invMapper;

	/** 현재 로그인 관리자 principal 얻기. 미인증 시 401. */
	private CcsPrincipal principal() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Object p = auth != null ? auth.getPrincipal() : null;
		if (!(p instanceof CcsPrincipal)) {
			throw new ApiException(ApiStatus.ACCESS_DENIED, "인증 필요");
		}
		return (CcsPrincipal) p;
	}

	/** 부서 목록 — PMS_DIVISION 마스터에서 한국어 DEPT_NM 을 그대로 가져옴. */
	@ResponseBody
	@RequestMapping(value = "/departments", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse listDepartments(RequestParams requestParams) {
		MenuAccess.assertCanAccess(principal(), AdminMenu.ROUTING, invMapper);
		String propCd = requestParams.getString("propCd");
		log.info("[ADMIN] /departments 호출 propCd={}", propCd);
		List<Map<String, Object>> depts = pmsMapper.selectDivisionList(propCd);
		log.info("[ADMIN] /departments 결과 count={}", depts != null ? depts.size() : 0);
		return Responses.MapResponse.of(Map.of("list", depts != null ? depts : new ArrayList<>()));
	}

	/**
	 * 직원 목록.
	 *
	 * 스코프:
	 *   - propCd/cmpxCd 는 쿼리에서 필터
	 *   - 내 역할보다 동급 이상(USER_TP 가 같거나 작은) 사용자는 제외 — 관리 대상 아님
	 *     (SYS→SYS 제외, PROP→SYS+PROP 제외, CMPX→SYS+PROP+CMPX 제외)
	 */
	@ResponseBody
	@RequestMapping(value = "/staff", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse listStaff(RequestParams requestParams) {
		CcsPrincipal me = principal();
		MenuAccess.assertCanAccess(me, AdminMenu.ROUTING, invMapper);
		String propCd = requestParams.getString("propCd");
		String cmpxCd = requestParams.getString("cmpxCd");
		String deptCd = requestParams.getString("deptCd");
		log.info("[ADMIN] /staff 호출 me.userTp={}, propCd={}, cmpxCd={}, deptCd={}",
				me.userTp(), propCd, cmpxCd, deptCd);

		List<Map<String, Object>> users = pmsMapper.selectUsersByDept(
				propCd, cmpxCd, (deptCd != null && !deptCd.isBlank()) ? deptCd : null);
		log.info("[ADMIN] /staff 쿼리 결과 raw count={}", users != null ? users.size() : 0);

		int filteredOut = 0;
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> u : users) {
			String targetTp = str(u.get("userTp"));
			// 역할 위계: 내가 관리할 수 있는 대상만 (나와 같거나 상위는 제외)
			if (!AdminRoles.canManageUser(me.userTp(), targetTp)) { filteredOut++; continue; }

			Map<String, Object> row = new LinkedHashMap<>();
			row.put("staffId",    str(u.get("userId")));
			row.put("loginId",    str(u.get("userId")));
			row.put("staffNm",    str(u.get("userNm")));
			row.put("deptCd",     str(u.get("deptCd")));
			row.put("deptNm",     str(u.get("deptNm")));
			row.put("userTp",     targetTp);
			row.put("userTpLabel", AdminRoles.label(targetTp));
			row.put("userPosLvl", str(u.get("userPosLvl")));
			row.put("userMobile", str(u.get("userMobile")));
			row.put("userWkStat", str(u.get("userWkStat")));
			row.put("useYn",      str(u.get("useYn")));
			out.add(row);
		}
		log.info("[ADMIN] /staff 위계 필터 후 returned count={} (filteredOut={})", out.size(), filteredOut);
		return Responses.MapResponse.of(Map.of("list", out));
	}

	@ResponseBody
	@RequestMapping(value = "/departments", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse createDept(RequestParams requestParams) {
		MenuAccess.assertCanAccess(principal(), AdminMenu.ROUTING, invMapper);
		Map<String, Object> params = requestParams.getParams();
		invMapper.insertDepartment(params);
		return ok("등록 완료");
	}

	@ResponseBody
	@RequestMapping(value = "/departments/{deptCd}", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse updateDept(@PathVariable String deptCd, RequestParams requestParams) {
		MenuAccess.assertCanAccess(principal(), AdminMenu.ROUTING, invMapper);
		Map<String, Object> params = requestParams.getParams();
		params.put("deptCd", deptCd);
		invMapper.updateDepartment(params);
		return ok("수정 완료");
	}

	@ResponseBody
	@RequestMapping(value = "/departments/{deptCd}", method = RequestMethod.DELETE, produces = APPLICATION_JSON)
	public ApiResponse deleteDept(@PathVariable String deptCd, RequestParams requestParams) {
		MenuAccess.assertCanAccess(principal(), AdminMenu.ROUTING, invMapper);
		String propCd = requestParams.getString("propCd");
		String cmpxCd = requestParams.getString("cmpxCd");
		invMapper.deleteDepartment(propCd, cmpxCd, deptCd);
		return ok("삭제 완료");
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
