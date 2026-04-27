package com.daol.concierge.ccs.admin;

import com.daol.concierge.ccs.auth.AdminMenu;
import com.daol.concierge.ccs.auth.AdminRoles;
import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import com.daol.concierge.inv.mapper.InvMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SYS_ADMIN 전용 — 다른 admin (PROP/CMPX_ADMIN) 에게 메뉴 단위 권한 부여 / 회수.
 *
 * 엔드포인트:
 *   GET  /api/concierge/admin/role-grant?userId=...
 *        → { userId, grants: [{menuCd, granted, grantedBy, grantedAt}, ...] }
 *   PUT  /api/concierge/admin/role-grant
 *        → body: { userId, menuCd, granted: 'Y'|'N' } / 단일 toggle
 *   PUT  /api/concierge/admin/role-grant/bulk
 *        → body: { userId, grants: { 'ccs.routing': 'Y', 'ccs.voc': 'N', ... } } / 일괄 저장
 */
@Controller
@RequestMapping("/api/concierge/admin/role-grant")
public class CcsRoleGrantController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(CcsRoleGrantController.class);

	@Autowired private InvMapper invMapper;

	private CcsPrincipal principal() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Object p = auth != null ? auth.getPrincipal() : null;
		if (!(p instanceof CcsPrincipal)) {
			throw new ApiException(ApiStatus.ACCESS_DENIED, "인증 필요");
		}
		return (CcsPrincipal) p;
	}

	/** SYS_ADMIN 만 호출 가능. */
	private CcsPrincipal requireSystemAdmin() {
		CcsPrincipal cp = principal();
		if (!AdminRoles.isSystemAdmin(cp.userTp())) {
			throw new ApiException(ApiStatus.ACCESS_DENIED, "시스템 관리자 전용 기능");
		}
		return cp;
	}

	/** 특정 사용자의 메뉴 권한 현황 조회. 부여된 적 없는 메뉴는 GRANTED='N' 으로 채워서 반환. */
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse list(@RequestParam("userId") String userId) {
		requireSystemAdmin();
		if (userId == null || userId.isBlank()) {
			throw new ApiException(ApiStatus.BAD_REQUEST, "userId 필요");
		}

		List<Map<String, Object>> rows = invMapper.selectRoleGrantsByUser(userId);
		Map<String, Map<String, Object>> byMenu = new HashMap<>();
		for (Map<String, Object> r : rows) {
			byMenu.put(String.valueOf(r.get("menuCd")), r);
		}

		// 카탈로그의 모든 grantable 메뉴를 채워서 반환 (행 부재 = 'N' 으로 표시)
		List<Map<String, Object>> out = new java.util.ArrayList<>();
		for (String code : AdminMenu.GRANTABLE) {
			Map<String, Object> row = new LinkedHashMap<>();
			Map<String, Object> existing = byMenu.get(code);
			row.put("menuCd",    code);
			row.put("granted",   existing != null ? existing.get("granted") : "N");
			row.put("grantedBy", existing != null ? existing.get("grantedBy") : null);
			row.put("grantedAt", existing != null ? existing.get("grantedAt") : null);
			out.add(row);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("userId", userId);
		body.put("grants", out);
		return Responses.MapResponse.of(body);
	}

	/** 단일 메뉴 권한 토글 / 설정. */
	@ResponseBody
	@RequestMapping(method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse upsert(RequestParams requestParams) {
		CcsPrincipal me = requireSystemAdmin();
		String userId  = requestParams.getString("userId");
		String menuCd  = requestParams.getString("menuCd");
		String granted = requestParams.getString("granted");

		if (userId == null || userId.isBlank()) throw new ApiException(ApiStatus.BAD_REQUEST, "userId 필요");
		if (menuCd == null || menuCd.isBlank()) throw new ApiException(ApiStatus.BAD_REQUEST, "menuCd 필요");
		if (!AdminMenu.isGrantable(menuCd))     throw new ApiException(ApiStatus.BAD_REQUEST, "허용되지 않은 메뉴");
		if (!"Y".equals(granted) && !"N".equals(granted))
			throw new ApiException(ApiStatus.BAD_REQUEST, "granted 는 Y 또는 N");

		Map<String, Object> param = new HashMap<>();
		param.put("userId",     userId);
		param.put("menuCd",     menuCd);
		param.put("granted",    granted);
		param.put("grantedBy",  me.staffId());

		invMapper.upsertRoleGrant(param);
		log.info("[role-grant] userId={} menuCd={} granted={} by={}", userId, menuCd, granted, me.staffId());
		return ok("저장 완료");
	}

	/** 일괄 저장 — 한 사용자의 여러 메뉴를 한 번에 토글. */
	@ResponseBody
	@RequestMapping(value = "/bulk", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	@SuppressWarnings("unchecked")
	public ApiResponse bulk(RequestParams requestParams) {
		CcsPrincipal me = requireSystemAdmin();
		String userId = requestParams.getString("userId");
		Object grantsRaw = requestParams.getParams().get("grants");

		if (userId == null || userId.isBlank()) throw new ApiException(ApiStatus.BAD_REQUEST, "userId 필요");
		if (!(grantsRaw instanceof Map)) throw new ApiException(ApiStatus.BAD_REQUEST, "grants 맵 필요");

		Map<String, Object> grants = (Map<String, Object>) grantsRaw;
		int saved = 0;
		for (Map.Entry<String, Object> e : grants.entrySet()) {
			String menuCd = e.getKey();
			String granted = String.valueOf(e.getValue());
			if (!AdminMenu.isGrantable(menuCd)) continue;
			if (!"Y".equals(granted) && !"N".equals(granted)) continue;

			Map<String, Object> param = new HashMap<>();
			param.put("userId",    userId);
			param.put("menuCd",    menuCd);
			param.put("granted",   granted);
			param.put("grantedBy", me.staffId());
			invMapper.upsertRoleGrant(param);
			saved++;
		}
		log.info("[role-grant] bulk userId={} saved={} by={}", userId, saved, me.staffId());
		return ok("저장 완료 (" + saved + ")");
	}
}
