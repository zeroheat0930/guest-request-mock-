package com.daol.concierge.ccs.common;

import com.daol.concierge.ccs.auth.AdminRoles;
import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.MenuAccess;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.inv.mapper.InvMapper;
import com.daol.concierge.pms.mapper.PmsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 로그인 후 "어느 호텔 관리할지" 선택 단계용 API (PMS 의 property-complex-modal 대응).
 *
 *   SYS_ADMIN  (00001): 전체 프로퍼티 선택 가능
 *   PROP_ADMIN (00002): 본인 propCd 고정 + 하위 컴플렉스 여러 개 중 선택
 *   CMPX_ADMIN (00003): 본인 propCd+cmpxCd 로 고정 (프론트가 선택 단계 스킵)
 *   STAFF      (그 외): 관리 컨텍스트 접근 불가 → 403
 */
@Controller
@RequestMapping("/api/ccs/common")
public class CcsContextController extends BaseController {

	@Autowired private PmsMapper pmsMapper;
	@Autowired private InvMapper invMapper;

	private CcsPrincipal principal() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Object p = auth != null ? auth.getPrincipal() : null;
		if (!(p instanceof CcsPrincipal)) {
			throw new ApiException(ApiStatus.ACCESS_DENIED, "인증 필요");
		}
		return (CcsPrincipal) p;
	}

	private CcsPrincipal requireAdmin() {
		CcsPrincipal cp = principal();
		if (!AdminRoles.isAdmin(cp.userTp())) {
			throw new ApiException(ApiStatus.ACCESS_DENIED, "관리자 권한 필요");
		}
		return cp;
	}

	/**
	 * 로그인 사용자가 접근 가능한 프로퍼티 목록.
	 * SYS_ADMIN 은 전체, 그 외는 본인 propCd 하나만.
	 */
	@ResponseBody
	@GetMapping(value = "/properties", produces = APPLICATION_JSON)
	public ApiResponse properties() {
		CcsPrincipal cp = requireAdmin();
		List<Map<String, Object>> list;
		if (AdminRoles.isSystemAdmin(cp.userTp())) {
			list = pmsMapper.selectPropertyList();
		} else {
			Map<String, Object> mine = pmsMapper.selectProperty(cp.propCd());
			list = mine == null ? Collections.emptyList() : Collections.singletonList(mine);
		}
		return Responses.ListResponse.of(list);
	}

	/**
	 * 특정 프로퍼티의 컴플렉스 목록.
	 * PROP_ADMIN 은 본인 propCd 만, CMPX_ADMIN 은 본인 propCd+cmpxCd 만 필터링하여 반환.
	 */
	@ResponseBody
	@GetMapping(value = "/complexes", produces = APPLICATION_JSON)
	public ApiResponse complexes(@RequestParam("propCd") String propCd) {
		CcsPrincipal cp = requireAdmin();
		if (!AdminRoles.canAccess(cp.userTp(), cp.propCd(), cp.cmpxCd(), propCd, null)) {
			throw new ApiException(ApiStatus.ACCESS_DENIED, "접근 범위를 벗어난 프로퍼티");
		}
		List<Map<String, Object>> list = pmsMapper.selectComplexListByProp(propCd);
		if (AdminRoles.isComplexAdmin(cp.userTp())) {
			final String myCmpx = cp.cmpxCd();
			list = list.stream()
					.filter(c -> String.valueOf(c.get("cmpxCd")).equals(myCmpx))
					.collect(Collectors.toList());
		}
		return Responses.ListResponse.of(list);
	}

	/**
	 * 현재 토큰의 사용자 정보 + 역할.
	 * 프론트가 로그인 직후 CMPX_ADMIN 인지 확인해 선택 단계 스킵할 때 사용.
	 */
	@ResponseBody
	@GetMapping(value = "/me", produces = APPLICATION_JSON)
	public ApiResponse me() {
		CcsPrincipal cp = principal();
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("staffId", cp.staffId());
		out.put("staffNm", cp.staffNm());
		out.put("userTp", cp.userTp());
		out.put("propCd", cp.propCd());
		out.put("cmpxCd", cp.cmpxCd());
		out.put("deptCd", cp.deptCd());
		out.put("role", AdminRoles.label(cp.userTp()));
		out.put("isAdmin", AdminRoles.isAdmin(cp.userTp()));
		// 어드민 메뉴 접근 가능한 코드 목록 — 프론트 StaffShell 이 노출 결정에 사용
		out.put("menus", MenuAccess.menusFor(cp, invMapper));
		return Responses.MapResponse.of(out);
	}
}
