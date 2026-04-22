/**
 * 스태프/관리자 번들 전용 라우터.
 * 게스트 라우트(amenity/hk/lc/chat/nearby/parking)는 여기 없음 → 스태프 번들에서
 * 아예 import 자체를 하지 않으므로 Vite 가 게스트 뷰 코드를 스태프 번들에 포함하지 않음.
 */
import { createRouter, createWebHashHistory } from 'vue-router';
import StaffLoginView from '../views/staff/StaffLoginView.vue';
import StaffDashboardView from '../views/staff/StaffDashboardView.vue';
import AdminFeaturesView from '../views/AdminFeaturesView.vue';
import AdminCcsView from '../views/AdminCcsView.vue';

const ADMIN_USER_TPS = ['00001', '00002', '00003']; // SYS/PROP/CMPX

const routes = [
	{ path: '/',               redirect: '/staff' },
	{ path: '/staff/login',    component: StaffLoginView,     meta: { staff: true, public: true } },
	{ path: '/staff/context',  component: () => import('../views/PropertyContextView.vue'), meta: { staff: true, context: true } },
	{ path: '/staff',          component: StaffDashboardView, meta: { staff: true } },
	{ path: '/staff/duty',     component: () => import('../views/staff/DutyLogView.vue'), meta: { staff: true } },
	// 구 /admin/login 은 스태프 로그인으로 통합
	{ path: '/admin/login',    redirect: '/staff/login' },
	{ path: '/admin/features', component: AdminFeaturesView,  meta: { admin: true } },
	{ path: '/admin/ccs',      component: AdminCcsView,       meta: { admin: true } },
	{ path: '/admin/qr',       component: () => import('../views/staff/QrGeneratorView.vue'), meta: { admin: true } },
	{ path: '/admin/lostfound',component: () => import('../views/AdminLostFoundView.vue'), meta: { admin: true } },
	{ path: '/admin/voc',      component: () => import('../views/AdminVocView.vue'),        meta: { admin: true } },
	{ path: '/admin/rental',   component: () => import('../views/AdminRentalView.vue'),     meta: { admin: true } },
	{ path: '/admin/duty',     component: () => import('../views/AdminDutyView.vue'),       meta: { admin: true } },
	{ path: '/admin/reports',  component: () => import('../views/AdminReportsView.vue'),    meta: { admin: true } },
	{ path: '/admin/audit',    component: () => import('../views/AdminAuditView.vue'),      meta: { admin: true } },
	// 스태프 번들로 잘못 들어온 미지 경로는 스태프 로그인으로
	{ path: '/:pathMatch(.*)*', redirect: '/staff/login' }
];

const router = createRouter({
	history: createWebHashHistory(),
	routes
});

function readStaff() {
	try { return JSON.parse(sessionStorage.getItem('ccs.staff') || '{}'); } catch { return {}; }
}
function readCtx() {
	try { return JSON.parse(sessionStorage.getItem('ccs.context') || '{}'); } catch { return {}; }
}
function hasContext() {
	const c = readCtx();
	return !!(c.propCd && c.cmpxCd);
}
function tokenOf() {
	try { return sessionStorage.getItem('ccs.token'); } catch { return null; }
}

router.beforeEach((to) => {
	if (to.meta?.public) return true;

	const tok = tokenOf();
	if (!tok) return '/staff/login';

	const staff = readStaff();
	const isAdmin = ADMIN_USER_TPS.includes(staff.userTp);

	// 호텔 선택 단계 (/staff/context) — 관리자만 허용, 비관리자는 /staff 로 튕김
	if (to.meta?.context) {
		return isAdmin ? true : '/staff';
	}

	// 관리자 전용 경로
	if (to.meta?.admin) {
		if (!isAdmin) return '/staff';
		if (!hasContext()) return '/staff/context';
		return true;
	}

	// 일반 스태프 경로 — 관리자라면 호텔 선택 필수
	if (to.meta?.staff) {
		if (isAdmin && !hasContext()) return '/staff/context';
		return true;
	}

	return true;
});

export default router;
