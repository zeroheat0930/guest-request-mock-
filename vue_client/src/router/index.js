import { createRouter, createWebHashHistory } from 'vue-router';
import AmenityView from '../views/AmenityView.vue';
import HousekeepingView from '../views/HousekeepingView.vue';
import LateCheckoutView from '../views/LateCheckoutView.vue';
import ChatView from '../views/ChatView.vue';
import NearbyView from '../views/NearbyView.vue';
import ParkingView from '../views/ParkingView.vue';
import AdminFeaturesView from '../views/AdminFeaturesView.vue';
import { featuresLoaded, isFeatureEnabled, firstEnabledPath } from '../features/featureStore.js';

const routes = [
	{ path: '/',              redirect: '/amenity' },
	{ path: '/amenity',       component: AmenityView,      meta: { featureCd: 'AMENITY' } },
	{ path: '/housekeeping',  component: HousekeepingView, meta: { featureCd: 'HK' } },
	{ path: '/late-checkout', component: LateCheckoutView, meta: { featureCd: 'LATE_CO' } },
	{ path: '/chat',          component: ChatView,         meta: { featureCd: 'CHAT' } },
	{ path: '/nearby',        component: NearbyView,       meta: { featureCd: 'NEARBY' } },
	{ path: '/parking',       component: ParkingView,      meta: { featureCd: 'PARKING' } },
	{ path: '/admin/features', component: AdminFeaturesView, meta: { admin: true } }
];

const router = createRouter({
	history: createWebHashHistory(),
	routes
});

router.beforeEach((to) => {
	if (to.meta?.admin) return true;
	if (!featuresLoaded.value) return true;
	const cd = to.meta?.featureCd;
	if (cd && !isFeatureEnabled(cd)) {
		return firstEnabledPath();
	}
	return true;
});

export default router;
