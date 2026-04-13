import { createRouter, createWebHashHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import AmenityView from '../views/AmenityView.vue';
import HousekeepingView from '../views/HousekeepingView.vue';
import LateCheckoutView from '../views/LateCheckoutView.vue';
import DashboardView from '../views/DashboardView.vue';

const routes = [
	{ path: '/',              component: HomeView },
	{ path: '/amenity',       component: AmenityView },
	{ path: '/housekeeping',  component: HousekeepingView },
	{ path: '/late-checkout', component: LateCheckoutView },
	{ path: '/dashboard',     component: DashboardView }
];

export default createRouter({
	history: createWebHashHistory(),
	routes
});
