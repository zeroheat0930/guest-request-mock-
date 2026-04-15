import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'node:path';

/**
 * 멀티 엔트리 빌드.
 * - index.html  → 게스트 번들  (/src/main.js, 게스트 라우트만)
 * - staff.html  → 스태프 번들  (/src/main-staff.js, 스태프+관리자 라우트만)
 * 공용 모듈(vue, vue-router, api/client 등)은 Vite 가 자동으로 shared chunk 로 분리.
 */
export default defineConfig({
	plugins: [vue()],
	server: {
		port: 5173,
		host: true
	},
	build: {
		rollupOptions: {
			input: {
				main: resolve(__dirname, 'index.html'),
				staff: resolve(__dirname, 'staff.html')
			}
		}
	}
});
