package com.daol.concierge.ccs.auth;

import com.daol.concierge.ccs.service.CcsLostFoundServiceTest;
import com.daol.concierge.inv.mapper.InvMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MenuAccess — 메뉴 단위 권한 검사 로직 검증.
 *
 *   - SYS_ADMIN(00001) → 항상 모든 메뉴 허용 (DB 조회 없음)
 *   - PROP/CMPX_ADMIN(00002/00003) → INV.CCS_ROLE_GRANT 의 GRANTED='Y' 행이 있는 메뉴만 허용
 *   - STAFF(그 외) → 어드민 메뉴 전부 거부
 *   - menusFor() 결과는 ROLE_GRANT 메뉴를 비-SYS 사용자에게 노출하지 않아야 함
 */
class MenuAccessTest {

	private CcsPrincipal principal(String userTp, String staffId) {
		return new CcsPrincipal(staffId, staffId, "HK", "0000000010", "00001", "Tester", userTp);
	}

	@Test
	void sysAdminCanAccessEveryMenu_evenWithEmptyGrants() {
		CcsPrincipal sys = principal(AdminRoles.SYS_ADMIN, "sys1");
		InvMapper mapper = new EmptyGrantsStub();
		assertTrue(MenuAccess.canAccess(sys, AdminMenu.ROUTING, mapper));
		assertTrue(MenuAccess.canAccess(sys, AdminMenu.AUDIT, mapper));
		assertTrue(MenuAccess.canAccess(sys, AdminMenu.ROLE_GRANT, mapper));
	}

	@Test
	void propAdminWithoutGrant_isDenied() {
		CcsPrincipal prop = principal(AdminRoles.PROP_ADMIN, "prop1");
		InvMapper mapper = new EmptyGrantsStub();
		assertFalse(MenuAccess.canAccess(prop, AdminMenu.ROUTING, mapper));
		assertFalse(MenuAccess.canAccess(prop, AdminMenu.REPORTS, mapper));
	}

	@Test
	void propAdminWithGrant_isAllowedForGrantedMenusOnly() {
		CcsPrincipal prop = principal(AdminRoles.PROP_ADMIN, "prop1");
		InvMapper mapper = new GrantsStub(Arrays.asList(AdminMenu.ROUTING, AdminMenu.REPORTS));
		assertTrue(MenuAccess.canAccess(prop, AdminMenu.ROUTING, mapper));
		assertTrue(MenuAccess.canAccess(prop, AdminMenu.REPORTS, mapper));
		assertFalse(MenuAccess.canAccess(prop, AdminMenu.AUDIT, mapper));
		assertFalse(MenuAccess.canAccess(prop, AdminMenu.FEATURES, mapper));
	}

	@Test
	void staffUser_isDeniedAllMenus() {
		CcsPrincipal staff = principal("00004", "staff1");
		InvMapper mapper = new GrantsStub(Arrays.asList(AdminMenu.ROUTING));
		assertFalse(MenuAccess.canAccess(staff, AdminMenu.ROUTING, mapper));
	}

	@Test
	void menusForSys_includesAllIncludingRoleGrant() {
		CcsPrincipal sys = principal(AdminRoles.SYS_ADMIN, "sys1");
		List<String> menus = MenuAccess.menusFor(sys, new EmptyGrantsStub());
		assertTrue(menus.contains(AdminMenu.ROUTING));
		assertTrue(menus.contains(AdminMenu.ROLE_GRANT));
	}

	@Test
	void menusForPropAdmin_excludesRoleGrantEvenIfGranted() {
		// 누군가 실수로 PROP_ADMIN 에게 ROLE_GRANT 권한을 부여해도 menusFor 가 필터링.
		CcsPrincipal prop = principal(AdminRoles.PROP_ADMIN, "prop1");
		InvMapper mapper = new GrantsStub(Arrays.asList(AdminMenu.ROUTING, AdminMenu.ROLE_GRANT));
		List<String> menus = MenuAccess.menusFor(prop, mapper);
		assertTrue(menus.contains(AdminMenu.ROUTING));
		assertFalse(menus.contains(AdminMenu.ROLE_GRANT));
	}

	@Test
	void assertCanAccess_throwsWhenDenied() {
		CcsPrincipal prop = principal(AdminRoles.PROP_ADMIN, "prop1");
		InvMapper mapper = new EmptyGrantsStub();
		assertThrows(com.daol.concierge.core.api.ApiException.class,
				() -> MenuAccess.assertCanAccess(prop, AdminMenu.ROUTING, mapper));
	}

	// ── stub helpers — InvMapper 의 selectGrantedMenuCdsByUser 만 의미 있는 동작, 나머지는 no-op ──

	private static class EmptyGrantsStub extends CcsLostFoundServiceTest.InvMapperStub {
		EmptyGrantsStub() { super(new AtomicInteger()); }
		@Override public List<String> selectGrantedMenuCdsByUser(String userId) { return Collections.emptyList(); }
	}

	private static class GrantsStub extends CcsLostFoundServiceTest.InvMapperStub {
		private final List<String> granted;
		GrantsStub(List<String> granted) { super(new AtomicInteger()); this.granted = granted; }
		@Override public List<String> selectGrantedMenuCdsByUser(String userId) { return granted; }
	}
}
