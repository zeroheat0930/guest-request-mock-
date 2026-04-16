<template>
	<div class="app-shell" :class="{ 'no-lnb': !showLnb }">
		<!-- LNB (좌측 사이드) — 게스트 화면에서만 -->
		<aside v-if="showLnb" class="lnb">
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
				<small>{{ guestRoomNo }}</small>
				<small class="dim">{{ guestName }}</small>
			</div>
		</aside>

		<!-- 본문 -->
		<main class="app-body">
			<router-view />
		</main>
	</div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { loadFeatures, enabledSortedFeatures, featuresLoaded } from './features/featureStore.js';
import { getStoredToken, authenticateGuest, DEMO_RESERVATIONS } from './auth/authBootstrap.js';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();
const tabs = computed(() => enabledSortedFeatures());
const showLnb = computed(() => !(route.meta?.admin || route.meta?.staff));

const guestRoomNo = ref('');
const guestName = ref('');

onMounted(async () => {
	// 데모 부트: 토큰이 없으면 첫 데모 게스트로 자동 인증 (ChatView 에서도 다시 switchGuest 함)
	if (!getStoredToken()) {
		try {
			const info = await authenticateGuest(DEMO_RESERVATIONS[0].rsvNo);
			if (info) {
				guestRoomNo.value = info.roomNo ? `ROOM ${info.roomNo}` : '';
				guestName.value = info.perNm || '';
			}
		} catch {}
	} else {
		// Token already present — derive display from stored rsvNo label
		const stored = sessionStorage.getItem('concierge.rsvNo');
		const demo = DEMO_RESERVATIONS.find(r => r.rsvNo === stored);
		if (demo) {
			const parts = demo.label.split('·');
			guestRoomNo.value = parts[0] ? `ROOM ${parts[0].trim()}` : '';
			guestName.value = parts[1] ? parts[1].trim().split(' ')[0] + ' ' + (parts[1].trim().split(' ').slice(1).join(' ')) : '';
		}
	}
	await loadFeatures();
	// 현재 경로가 비활성 feature 면 첫 활성 탭으로 재배치
	const cur = router.currentRoute.value;
	const cd = cur.meta?.featureCd;
	if (cd && featuresLoaded.value && !tabs.value.some(t => t.featureCd === cd)) {
		if (tabs.value.length) router.replace(tabs.value[0].to);
	}
});
</script>

<style scoped>
.app-shell { display: flex; height: 100%; background: #f5f7fa; }
.app-shell.no-lnb .app-body { padding: 0; }

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
