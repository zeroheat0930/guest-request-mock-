package com.daol.concierge.ccs.auth;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 어드민 메뉴 카탈로그.
 *
 * SYS_ADMIN 이 PROP/CMPX_ADMIN 에게 권한 부여하는 단위.
 * 프론트엔드 라우터/StaffShell 메뉴와 1:1 대응.
 *
 *   - SYS_ADMIN 은 본 카탈로그를 우회하여 무조건 전 메뉴 허용
 *   - 그 외 admin 은 INV.CCS_ROLE_GRANT(USER_ID, MENU_CD, GRANTED='Y') 행이 있어야 허용
 *   - ROLE_GRANT 메뉴 자체는 SYS_ADMIN 전용 (다른 admin 에게 부여 불가)
 */
public final class AdminMenu {

	public static final String ROUTING    = "ccs.routing";
	public static final String FEATURES   = "ccs.features";
	public static final String LOSTFOUND  = "ccs.lostfound";
	public static final String VOC        = "ccs.voc";
	public static final String RENTAL     = "ccs.rental";
	public static final String DUTY       = "ccs.duty";
	public static final String REPORTS    = "ccs.reports";
	public static final String AUDIT      = "ccs.audit";
	public static final String QR         = "ccs.qr";
	public static final String ROLE_GRANT = "ccs.role-grant";

	/** 권한 부여 가능한 메뉴 (= UI 그리드에 노출되는 항목). ROLE_GRANT 자체는 SYS 전용이라 제외. */
	public static final List<String> GRANTABLE = Collections.unmodifiableList(Arrays.asList(
			ROUTING, FEATURES, LOSTFOUND, VOC, RENTAL, DUTY, REPORTS, AUDIT, QR
	));

	/** 모든 메뉴 (me() 응답에서 SYS_ADMIN 의 menus 필드 채울 때 사용). */
	public static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
			ROUTING, FEATURES, LOSTFOUND, VOC, RENTAL, DUTY, REPORTS, AUDIT, QR, ROLE_GRANT
	));

	/** 메뉴 코드 → i18n key 매핑 (프론트가 그대로 t() 에 넘김). */
	public static final Map<String, String> I18N_KEY;
	static {
		Map<String, String> m = new LinkedHashMap<>();
		m.put(ROUTING,    "shell.nav.ccs");
		m.put(FEATURES,   "shell.nav.features");
		m.put(LOSTFOUND,  "shell.nav.lostfound");
		m.put(VOC,        "shell.nav.voc");
		m.put(RENTAL,     "shell.nav.rental");
		m.put(DUTY,       "shell.nav.duty");
		m.put(REPORTS,    "shell.nav.reports");
		m.put(AUDIT,      "shell.nav.audit");
		m.put(QR,         "shell.nav.qr");
		m.put(ROLE_GRANT, "shell.nav.roleGrant");
		I18N_KEY = Collections.unmodifiableMap(m);
	}

	private AdminMenu() {}

	public static boolean isGrantable(String menuCd) {
		return menuCd != null && GRANTABLE.contains(menuCd);
	}
}
