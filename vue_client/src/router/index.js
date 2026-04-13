import { createRouter, createWebHashHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import AmenityView from '../views/AmenityView.vue';
import HousekeepingView from '../views/HousekeepingView.vue';
import LateCheckoutView from '../views/LateCheckoutView.vue';

const routes = [
	{ path: '/',              component: HomeView },
	{ path: '/amenity',       component: AmenityView },
	{ path: '/housekeeping',  component: HousekeepingView },
	{ path: '/late-checkout', component: LateCheckoutView }
];

export default createRouter({
	history: createWebHashHistory(),
	routes
});
