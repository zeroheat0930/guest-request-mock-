/**
 * 스태프/관리자 번들 전용 라우터.
 * 게스트 라우트(amenity/hk/lc/chat/nearby/parking)는 여기 없음 → 스태프 번들에서
 * 아예 import 자체를 하지 않으므로 Vite 가 게스트 뷰 코드를 스태프 번들에 포함하지 않음.
 */
import { createRouter, createWebHashHistory } from 'vue-router';
import StaffLoginView from '../views/staff/StaffLoginView.vue';
import StaffDashboardView from '../views/staff/StaffDashboardView.vue';
import AdminLoginView from '../views/AdminLoginView.vue';
import AdminFeaturesView from '../views/AdminFeaturesView.vue';
import AdminCcsView from '../views/AdminCcsView.vue';

const routes = [
	{ path: '/',               redirect: '/staff' },
	{ path: '/staff/login',    component: StaffLoginView,     meta: { staff: true, public: true } },
	{ path: '/staff',          component: StaffDashboardView, meta: { staff: true } },
	{ path: '/admin/login',    component: AdminLoginView,     meta: { admin: true, public: true } },
	{ path: '/admin/features', component: AdminFeaturesView,  meta: { admin: true } },
	{ path: '/admin/ccs',      component: AdminCcsView,       meta: { admin: true } },
	{ path: '/admin/qr',       component: () => import('../views/staff/QrGeneratorView.vue'), meta: { admin: true } },
	// 스태프 번들로 잘못 들어온 미지 경로는 스태프 로그인으로
	{ path: '/:pathMatch(.*)*', redirect: '/staff/login' }
];

const router = createRouter({
	history: createWebHashHistory(),
	routes
});

router.beforeEach((to) => {
	if (to.meta?.staff) {
		if (to.meta.public) return true;
		const t = (() => { try { return sessionStorage.getItem('ccs.token'); } catch { return null; } })();
		return t ? true : '/staff/login';
	}
	if (to.meta?.admin) {
		if (to.meta.public) return true;
		const t = (() => { try { return sessionStorage.getItem('concierge.adminToken'); } catch { return null; } })();
		return t ? true : '/admin/login';
	}
	return true;
});

export default router;
