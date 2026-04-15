<template>
	<!-- 게스트 영역: LNB + 본문 -->
	<div v-if="isGuestRoute" class="app-shell">
		<aside class="lnb">
			<div class="brand">
				<div class="logo">🏨</div>
				<div class="brand-text">
					<div class="brand-name">다올 컨시어지</div>
					<div class="brand-sub">Guest Request</div>
				</div>
			</div>
			<nav class="lnb-nav">
				<router-link
					v-for="tab in tabs"
					:key="tab.featureCd"
					:to="tab.to"
					class="tab"
					:class="{ 'chat-tab': tab.featureCd === 'CHAT' }"
				>
					<span class="ic">{{ tab.icon }}</span>
					<span class="label">{{ tab.label }}<small>{{ tab.labelEn }}</small></span>
				</router-link>
			</nav>
			<div class="lnb-foot">
				<small>ROOM 1205</small>
				<small class="dim">HONG GILDONG</small>
			</div>
		</aside>
		<main class="app-body">
			<router-view />
		</main>
	</div>

	<!-- 스태프/관리자/로그인 영역: 사이드바 없이 순수 본문만. 각 뷰가 자체 헤더/로그아웃을 가짐 -->
	<div v-else class="plain-shell">
		<router-view />
	</div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { loadFeatures, enabledSortedFeatures, featuresLoaded } from './features/featureStore.js';
import { getStoredToken, authenticateGuest, DEMO_RESERVATIONS } from './auth/authBootstrap.js';

const router = useRouter();
const route = useRoute();
const tabs = computed(() => enabledSortedFeatures());

// 게스트 경로 여부: staff/admin meta 가 없으면 게스트.
// (/staff/*, /admin/*, /staff/login, /admin/login 전부 여기서 걸러짐)
const isGuestRoute = computed(() => !route.meta?.staff && !route.meta?.admin);

// 게스트 부트: 게스트 경로에 처음 진입했을 때만 자동 인증 + 기능 로드.
// 스태프/관리자 라우트에서는 게스트 토큰을 발급하지 않음.
let guestBooted = false;
async function bootGuestIfNeeded() {
	if (guestBooted) return;
	if (!isGuestRoute.value) return;
	guestBooted = true;
	if (!getStoredToken()) {
		try { await authenticateGuest(DEMO_RESERVATIONS[0].rsvNo); } catch {}
	}
	await loadFeatures();
	const cur = router.currentRoute.value;
	const cd = cur.meta?.featureCd;
	if (cd && featuresLoaded.value && !tabs.value.some(t => t.featureCd === cd)) {
		if (tabs.value.length) router.replace(tabs.value[0].to);
	}
}

onMounted(bootGuestIfNeeded);
watch(isGuestRoute, bootGuestIfNeeded);
</script>

<style scoped>
.app-shell { display: flex; height: 100%; background: #f5f7fa; }

/* 스태프/관리자/로그인 — 사이드바 없는 순수 컨테이너.
   각 뷰가 자체 상단 헤더와 로그아웃을 제공함. */
.plain-shell {
	min-height: 100%;
	background: #f5f7fa;
	padding: 24px 28px;
	overflow: auto;
}
@media (max-width: 720px) {
	.plain-shell { padding: 16px; }
}

/* ═══════════ LNB ═══════════ */
.lnb {
	width: 240px;
	background: #0f2748;
	color: #fff;
	display: flex;
	flex-direction: column;
	flex-shrink: 0;
}
.brand {
	display: flex;
	align-items: center;
	gap: 12px;
	padding: 22px 20px;
	border-bottom: 1px solid rgba(255,255,255,0.08);
}
.brand .logo { font-size: 32px; }
.brand-name { font-size: 16px; font-weight: 800; }
.brand-sub { font-size: 11px; opacity: 0.6; letter-spacing: 0.5px; }

.lnb-nav { flex: 1; padding: 16px 12px; display: flex; flex-direction: column; gap: 4px; }
.tab {
	display: flex;
	align-items: center;
	gap: 14px;
	padding: 14px 16px;
	border-radius: 10px;
	color: #cfd8dc;
	text-decoration: none;
	transition: all 0.15s;
}
.tab .ic { font-size: 22px; }
.tab .label { display: flex; flex-direction: column; line-height: 1.2; font-weight: 700; font-size: 14px; }
.tab .label small { font-weight: 400; opacity: 0.55; font-size: 10px; margin-top: 2px; letter-spacing: 0.3px; }
.tab:hover { background: rgba(255,255,255,0.06); color: #fff; }
.tab.router-link-active {
	background: linear-gradient(135deg, #2563eb, #3b5bdb);
	color: #fff;
	box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
}
.tab.chat-tab { margin-top: 8px; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 20px; }

.lnb-foot {
	padding: 16px 20px;
	border-top: 1px solid rgba(255,255,255,0.08);
	display: flex;
	flex-direction: column;
	gap: 2px;
}
.lnb-foot small { font-size: 11px; font-weight: 700; letter-spacing: 0.5px; }
.lnb-foot .dim { font-weight: 400; opacity: 0.5; }

/* ═══════════ BODY ═══════════ */
.app-body {
	flex: 1;
	padding: 24px 28px;
	overflow: auto;
}

/* ═══════════ MOBILE ═══════════ */
@media (max-width: 720px) {
	.app-shell { flex-direction: column; }
	.lnb { width: 100%; flex-direction: row; flex-wrap: wrap; }
	.brand { flex: 0 0 100%; padding: 14px 18px; }
	.lnb-nav { flex: 1; flex-direction: row; overflow-x: auto; padding: 8px; gap: 4px; }
	.tab { flex: 0 0 auto; padding: 10px 14px; }
	.tab .label { font-size: 12px; }
	.tab .label small { display: none; }
	.tab.chat-tab { margin-top: 0; border-top: none; padding-top: 10px; }
	.lnb-foot { display: none; }
	.app-body { padding: 16px; }
}
</style>
