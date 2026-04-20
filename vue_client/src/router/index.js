/**
 * 게스트 번들 전용 라우터.
 * staff/admin 라우트는 별도 번들(staffRouter.js)로 분리됨 — 여기에 import 하지 않으므로
 * 게스트 번들에 스태프/관리자 소스가 포함되지 않음(Vite 트리쉐이킹 + 멀티 엔트리).
 */
import { createRouter, createWebHashHistory } from 'vue-router';
import AmenityView from '../views/AmenityView.vue';
import HousekeepingView from '../views/HousekeepingView.vue';
import LateCheckoutView from '../views/LateCheckoutView.vue';
import ChatView from '../views/ChatView.vue';
import NearbyView from '../views/NearbyView.vue';
import ParkingView from '../views/ParkingView.vue';
import { featuresLoaded, isFeatureEnabled, firstEnabledPath } from '../features/featureStore.js';

const routes = [
	{ path: '/',              redirect: '/amenity' },
	{ path: '/amenity',       component: AmenityView,      meta: { featureCd: 'AMENITY' } },
	{ path: '/housekeeping',  component: HousekeepingView, meta: { featureCd: 'HK' } },
	{ path: '/late-checkout', component: LateCheckoutView, meta: { featureCd: 'LATE_CO' } },
	{ path: '/chat',          component: ChatView,         meta: { featureCd: 'CHAT' } },
	{ path: '/nearby',        component: NearbyView,       meta: { featureCd: 'NEARBY' } },
	{ path: '/parking',       component: ParkingView,      meta: { featureCd: 'PARKING' } },
	{ path: '/history',       component: () => import('../views/HistoryView.vue'), meta: { featureCd: 'HISTORY' } },
	// 게스트 번들로 잘못 들어온 스태프/관리자 URL 은 홈으로 돌려보냄
	{ path: '/:pathMatch(.*)*', redirect: '/' }
];

const router = createRouter({
	history: createWebHashHistory(),
	routes
});

router.beforeEach((to) => {
	if (!featuresLoaded.value) return true;
	const cd = to.meta?.featureCd;
	if (cd && !isFeatureEnabled(cd)) {
		return firstEnabledPath();
	}
	return true;
});

export default router;
