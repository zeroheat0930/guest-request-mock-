package com.daol.concierge.ccs.auth;

import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.inv.mapper.InvMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 메뉴별 접근 권한 검사 유틸.
 *
 *   - SYS_ADMIN (USER_TP=00001): 카탈로그의 전 메뉴 항상 허용
 *   - PROP/CMPX_ADMIN (00002/00003): INV.CCS_ROLE_GRANT 에 GRANTED='Y' 행이 있는 메뉴만 허용
 *   - 그 외 (STAFF 등): 어드민 메뉴 전부 거부
 *
 * 본 클래스는 Spring Bean 이 아니므로 호출자가 InvMapper 를 주입 후 정적 호출.
 */
public final class MenuAccess {

	private MenuAccess() {}

	/** 단순 boolean 체크 (예외 없이). */
	public static boolean canAccess(CcsPrincipal cp, String menuCd, InvMapper invMapper) {
		if (cp == null || menuCd == null) return false;
		if (AdminRoles.isSystemAdmin(cp.userTp())) return true;
		if (!AdminRoles.isAdmin(cp.userTp())) return false;
		if (cp.staffId() == null || cp.staffId().isBlank()) return false;
		List<String> granted = invMapper.selectGrantedMenuCdsByUser(cp.staffId());
		return granted != null && granted.contains(menuCd);
	}

	/** 거부 시 ApiException(ACCESS_DENIED) — 컨트롤러 가드 용. */
	public static void assertCanAccess(CcsPrincipal cp, String menuCd, InvMapper invMapper) {
		if (!canAccess(cp, menuCd, invMapper)) {
			throw new ApiException(ApiStatus.ACCESS_DENIED, "메뉴 접근 권한이 없습니다: " + menuCd);
		}
	}

	/**
	 * 사용자가 현재 접근 가능한 모든 어드민 메뉴 코드 목록.
	 * /api/ccs/common/me 응답의 'menus' 필드 채울 때 사용 — 프론트 StaffShell 이 이걸로 메뉴 노출 제어.
	 */
	public static List<String> menusFor(CcsPrincipal cp, InvMapper invMapper) {
		if (cp == null) return Collections.emptyList();
		if (AdminRoles.isSystemAdmin(cp.userTp())) {
			return new ArrayList<>(AdminMenu.ALL);
		}
		if (!AdminRoles.isAdmin(cp.userTp())) return Collections.emptyList();
		if (cp.staffId() == null || cp.staffId().isBlank()) return Collections.emptyList();
		List<String> granted = invMapper.selectGrantedMenuCdsByUser(cp.staffId());
		// ROLE_GRANT 메뉴는 SYS_ADMIN 전용 — 여기서 한 번 더 필터
		if (granted == null) return Collections.emptyList();
		List<String> out = new ArrayList<>(granted.size());
		for (String code : granted) {
			if (!AdminMenu.ROLE_GRANT.equals(code)) out.add(code);
		}
		return out;
	}
}
