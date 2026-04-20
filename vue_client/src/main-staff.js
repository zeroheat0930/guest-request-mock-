/**
 * 스태프/관리자 번들 엔트리 (staff.html → /src/main-staff.js).
 * 게스트 번들(main.js)과 완전히 분리된 Vue 앱 인스턴스.
 */
import { createApp } from 'vue';
import StaffApp from './StaffApp.vue';
import router from './router/staffRouter.js';

createApp(StaffApp).use(router).mount('#app');
