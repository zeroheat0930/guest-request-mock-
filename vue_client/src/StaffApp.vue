<template>
	<div class="staff-app-root">
		<StaffShell v-if="useShell">
			<router-view v-slot="{ Component }">
				<transition name="page" mode="out-in">
					<component :is="Component" />
				</transition>
			</router-view>
		</StaffShell>
		<div v-else class="plain-shell">
			<router-view v-slot="{ Component }">
				<transition name="page" mode="out-in">
					<component :is="Component" />
				</transition>
			</router-view>
		</div>
	</div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import StaffShell from './layouts/StaffShell.vue';

const route = useRoute();

// 데스크탑 셸을 쓸지 결정.
// - */login : 로그인 화면은 가운데 카드만 필요 → 플레인 셸
// - 그 외(/staff, /admin/*) : 좌측 LNB + 유저 정보 + 로그아웃을 가진 StaffShell
const useShell = computed(() => {
	const p = route.path;
	if (p.endsWith('/login')) return false;
	return true;
});
</script>

<style>
@import './assets/tokens.css';

*, *::before, *::after { box-sizing: border-box; }
html, body, #app {
	height: 100%;
	margin: 0;
	width: 100%;
	max-width: 100vw;
	overflow-x: hidden;
}
body {
	font-family: -apple-system, "SF Pro Display", "Segoe UI", "Noto Sans KR", "Apple SD Gothic Neo", sans-serif;
	background: var(--c-bg);
	color: var(--c-text);
	-webkit-font-smoothing: antialiased;
}
button { font-family: inherit; cursor: pointer; }
button:focus-visible {
	outline: 2px solid var(--c-brand-400);
	outline-offset: 2px;
}
input, select, textarea { font-family: inherit; }
</style>

<style scoped>
.staff-app-root {
	min-height: 100vh;
	width: 100%;
	max-width: 100vw;
	overflow-x: hidden;
	background: var(--c-bg);
}
.plain-shell {
	min-height: 100vh;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: var(--sp-6);
	background:
		radial-gradient(1200px 600px at 20% 0%, rgba(37, 99, 235, 0.12), transparent 60%),
		radial-gradient(900px 500px at 100% 100%, rgba(59, 91, 219, 0.10), transparent 55%),
		var(--c-bg);
}
</style>
