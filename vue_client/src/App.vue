<template>
	<div class="app-shell" :class="{ 'no-lnb': !showLnb }">
		<!-- LNB (좌측 사이드) — 게스트 화면에서만 -->
		<aside v-if="showLnb" class="lnb">
			<div class="brand">
				<div class="brand-logomark">
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
						<polyline points="9 22 9 12 15 12 15 22"/>
					</svg>
				</div>
				<div class="brand-text">
					<div class="brand-name">다올 컨시어지</div>
					<div class="brand-sub">Guest Services</div>
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
					<span class="label">
						{{ tab.label }}
						<small>{{ tab.labelEn }}</small>
					</span>
				</router-link>
			</nav>

			<div class="lnb-foot">
				<div class="guest-card">
					<div class="guest-card__room">
						<span class="room-badge">{{ guestRoomNo }}</span>
					</div>
					<div class="guest-card__name">{{ guestName }}</div>
				</div>
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
	if (!getStoredToken()) {
		try {
			const info = await authenticateGuest(DEMO_RESERVATIONS[0].rsvNo);
			if (info) {
				guestRoomNo.value = info.roomNo ? `${info.roomNo}호` : '';
				guestName.value = info.perNm || '';
			}
		} catch {}
	} else {
		const stored = sessionStorage.getItem('concierge.rsvNo');
		const demo = DEMO_RESERVATIONS.find(r => r.rsvNo === stored);
		if (demo) {
			const parts = demo.label.split('·');
			guestRoomNo.value = parts[0] ? `${parts[0].trim()}호` : '';
			guestName.value = parts[1] ? parts[1].trim().split(' ')[0] + ' ' + (parts[1].trim().split(' ').slice(1).join(' ')) : '';
		}
	}
	await loadFeatures();
	const cur = router.currentRoute.value;
	const cd = cur.meta?.featureCd;
	if (cd && featuresLoaded.value && !tabs.value.some(t => t.featureCd === cd)) {
		if (tabs.value.length) router.replace(tabs.value[0].to);
	}
});
</script>

<style scoped>
.app-shell { display: flex; height: 100%; background: var(--c-bg); }
.app-shell.no-lnb .app-body { padding: 0; }

/* ═══════════ LNB ═══════════ */
.lnb {
	width: 248px;
	background: var(--c-brand-900);
	color: #fff;
	display: flex;
	flex-direction: column;
	flex-shrink: 0;
	border-right: 1px solid rgba(255,255,255,0.04);
}

/* ── Brand ── */
.brand {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: var(--sp-6) var(--sp-5);
	border-bottom: 1px solid rgba(255,255,255,0.07);
}
.brand-logomark {
	width: 38px;
	height: 38px;
	background: var(--c-brand-700);
	border-radius: var(--r-md);
	display: flex;
	align-items: center;
	justify-content: center;
	color: var(--c-brand-300);
	flex-shrink: 0;
	box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.brand-name {
	font-size: var(--fs-md);
	font-weight: 800;
	color: #fff;
	letter-spacing: -0.2px;
	line-height: 1.2;
}
.brand-sub {
	font-size: var(--fs-xs);
	color: rgba(255,255,255,0.4);
	letter-spacing: 0.8px;
	text-transform: uppercase;
	margin-top: 2px;
}

/* ── Nav ── */
.lnb-nav {
	flex: 1;
	padding: var(--sp-4) var(--sp-3);
	display: flex;
	flex-direction: column;
	gap: var(--sp-1);
	overflow-y: auto;
}
.tab {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: var(--sp-3) var(--sp-4);
	border-radius: var(--r-md);
	color: rgba(255,255,255,0.55);
	text-decoration: none;
	transition: background var(--t-fast) var(--ease-out), color var(--t-fast) var(--ease-out);
	position: relative;
}
.tab .ic {
	font-size: 20px;
	line-height: 1;
	width: 26px;
	text-align: center;
	flex-shrink: 0;
}
.tab .label {
	display: flex;
	flex-direction: column;
	line-height: 1.2;
	font-size: var(--fs-md);
	font-weight: 700;
}
.tab .label small {
	font-weight: 400;
	opacity: 0.5;
	font-size: var(--fs-xs);
	margin-top: 2px;
	letter-spacing: 0.3px;
}
.tab:hover {
	background: rgba(255,255,255,0.06);
	color: rgba(255,255,255,0.85);
}
.tab.router-link-active {
	background: rgba(37, 99, 235, 0.22);
	color: #fff;
}
.tab.router-link-active::before {
	content: '';
	position: absolute;
	left: 0;
	top: 25%;
	bottom: 25%;
	width: 3px;
	background: var(--c-brand-400);
	border-radius: 0 var(--r-pill) var(--r-pill) 0;
}

.tab.chat-tab {
	margin-top: var(--sp-4);
	border-top: 1px solid rgba(255,255,255,0.07);
	padding-top: var(--sp-5);
}

/* ── Footer guest card ── */
.lnb-foot {
	padding: var(--sp-4) var(--sp-3);
	border-top: 1px solid rgba(255,255,255,0.07);
}
.guest-card {
	background: rgba(255,255,255,0.05);
	border: 1px solid rgba(255,255,255,0.08);
	border-radius: var(--r-md);
	padding: var(--sp-3) var(--sp-4);
	display: flex;
	align-items: center;
	gap: var(--sp-3);
}
.guest-card__room { flex-shrink: 0; }
.room-badge {
	display: inline-block;
	background: var(--c-brand-600);
	color: #fff;
	font-size: var(--fs-sm);
	font-weight: 800;
	padding: 3px var(--sp-3);
	border-radius: var(--r-pill);
	letter-spacing: 0.3px;
}
.guest-card__name {
	font-size: var(--fs-sm);
	color: rgba(255,255,255,0.6);
	font-weight: 500;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

/* ═══════════ BODY ═══════════ */
.app-body {
	flex: 1;
	padding: var(--sp-8) var(--sp-8);
	overflow: auto;
}

/* ═══════════ MOBILE ═══════════ */
@media (max-width: 720px) {
	.app-shell { flex-direction: column; }
	.lnb { width: 100%; flex-direction: row; flex-wrap: wrap; }
	.brand { flex: 0 0 100%; padding: var(--sp-4) var(--sp-5); border-bottom: 1px solid rgba(255,255,255,0.07); }
	.lnb-nav {
		flex: 1;
		flex-direction: row;
		overflow-x: auto;
		padding: var(--sp-2) var(--sp-2);
		gap: var(--sp-1);
		scrollbar-width: none;
	}
	.lnb-nav::-webkit-scrollbar { display: none; }
	.tab {
		flex: 0 0 auto;
		padding: var(--sp-2) var(--sp-3);
		gap: var(--sp-2);
	}
	.tab::before { display: none; }
	.tab .label { font-size: var(--fs-sm); }
	.tab .label small { display: none; }
	.tab.chat-tab { margin-top: 0; border-top: none; padding-top: var(--sp-2); }
	.lnb-foot { display: none; }
	.app-body { padding: var(--sp-5) var(--sp-4); }
}
</style>
