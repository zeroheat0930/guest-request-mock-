/**
 * 어드민 메뉴 카탈로그 (백엔드 AdminMenu.java 와 1:1 대응).
 *
 * 메뉴 코드 = INV.CCS_ROLE_GRANT.MENU_CD 값.
 * SYS_ADMIN 은 무조건 전체 허용, 그 외 admin 은 me() 응답의 menus 배열로 가시화 결정.
 */
export const ADMIN_MENU = Object.freeze({
	ROUTING:    'ccs.routing',
	FEATURES:   'ccs.features',
	LOSTFOUND:  'ccs.lostfound',
	VOC:        'ccs.voc',
	RENTAL:     'ccs.rental',
	DUTY:       'ccs.duty',
	REPORTS:    'ccs.reports',
	AUDIT:      'ccs.audit',
	QR:         'ccs.qr',
	ROLE_GRANT: 'ccs.role-grant'
});

/**
 * RoleGrant UI 에 노출되는 메뉴 (= SYS_ADMIN 이 다른 admin 에게 부여 가능한 메뉴).
 * ROLE_GRANT 자체는 SYS 전용이라 제외.
 */
export const GRANTABLE_MENUS = Object.freeze([
	{ code: ADMIN_MENU.ROUTING,   labelKey: 'shell.nav.ccs',       icon: '👥' },
	{ code: ADMIN_MENU.FEATURES,  labelKey: 'shell.nav.features',  icon: '⚙️' },
	{ code: ADMIN_MENU.LOSTFOUND, labelKey: 'shell.nav.lostfound', icon: '🔍' },
	{ code: ADMIN_MENU.VOC,       labelKey: 'shell.nav.voc',       icon: '💬' },
	{ code: ADMIN_MENU.RENTAL,    labelKey: 'shell.nav.rental',    icon: '🏷️' },
	{ code: ADMIN_MENU.DUTY,      labelKey: 'shell.nav.duty',      icon: '🗓️' },
	{ code: ADMIN_MENU.REPORTS,   labelKey: 'shell.nav.reports',   icon: '📊' },
	{ code: ADMIN_MENU.AUDIT,     labelKey: 'shell.nav.audit',     icon: '📜' },
	{ code: ADMIN_MENU.QR,        labelKey: 'shell.nav.qr',        icon: '📷' }
]);

/**
 * 메뉴 코드 → 라우터 path 매핑 (StaffShell 에서 router-link to= 채울 때).
 */
export const MENU_TO_PATH = Object.freeze({
	[ADMIN_MENU.ROUTING]:    '/admin/ccs',
	[ADMIN_MENU.FEATURES]:   '/admin/features',
	[ADMIN_MENU.LOSTFOUND]:  '/admin/lostfound',
	[ADMIN_MENU.VOC]:        '/admin/voc',
	[ADMIN_MENU.RENTAL]:     '/admin/rental',
	[ADMIN_MENU.DUTY]:       '/admin/duty',
	[ADMIN_MENU.REPORTS]:    '/admin/reports',
	[ADMIN_MENU.AUDIT]:      '/admin/audit',
	[ADMIN_MENU.QR]:         '/admin/qr',
	[ADMIN_MENU.ROLE_GRANT]: '/admin/role-grant'
});

/**
 * 라우터 path → 메뉴 코드 역매핑 (router beforeEach 에서 권한 체크).
 */
export const PATH_TO_MENU = Object.freeze(
	Object.fromEntries(Object.entries(MENU_TO_PATH).map(([k, v]) => [v, k]))
);
