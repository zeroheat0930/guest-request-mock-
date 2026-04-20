<template>
	<NetworkStatus />
	<div v-if="authError && showLnb" class="room-login-shell">
		<div class="room-login-card">
			<div class="room-login-logo">
				<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
					<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
					<polyline points="9 22 9 12 15 12 15 22"/>
				</svg>
			</div>
			<h1 class="room-login-title">{{ t('brand.name') }}</h1>
			<p class="room-login-sub">{{ authError }}</p>
		</div>
	</div>

	<div v-else class="app-shell" :class="{ 'no-lnb': !showLnb }">
		<!-- LNB (좌측 사이드) — 게스트 화면에서만 -->
		<aside v-if="showLnb" class="lnb">
			<div class="brand">
				<div class="brand-logomark">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
						<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
						<polyline points="9 22 9 12 15 12 15 22"/>
					</svg>
				</div>
				<div class="brand-text">
					<div class="brand-name">{{ t('brand.name') }}</div>
					<div class="brand-sub">{{ t('brand.sub') }}</div>
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
					<span class="label">{{ tab.label }}</span>
				</router-link>
			</nav>

			<div class="lnb-foot">
				<div class="guest-card">
					<div class="guest-card__room">
						<div class="guest-card__room-label">ROOM</div>
						<div class="guest-card__room-num">{{ guestRoomNo }}</div>
					</div>
					<div class="guest-card__divider"></div>
					<div class="guest-card__text">
						<div class="guest-card__greet">{{ t('welcome') }}</div>
						<div class="guest-card__sub">{{ t('welcome.sub') }}</div>
					</div>
				</div>
			</div>
		</aside>

		<!-- 본문 — 인증 완료 후에만 렌더 -->
		<main class="app-body">
			<router-view v-if="ready" />
			<LoadingSpinner v-else :text="t('auth.loading')" />
		</main>
	</div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { loadFeatures, enabledSortedFeatures, featuresLoaded } from './features/featureStore.js';
import { getStoredToken, authenticateByRoom } from './auth/authBootstrap.js';
import LoadingSpinner from './components/LoadingSpinner.vue';
import NetworkStatus from './components/NetworkStatus.vue';
import { useRouter, useRoute } from 'vue-router';
import { t } from './i18n/ui.js';

const router = useRouter();
const route = useRoute();
const tabs = computed(() => enabledSortedFeatures());
const showLnb = computed(() => !(route.meta?.admin || route.meta?.staff));

const guestRoomNo = ref('');
const guestName = ref('');
const authError = ref('');
const ready = ref(false);

let tokenCheckTimer = null;

onMounted(async () => {
	const urlRoom = new URLSearchParams(window.location.search).get('room');

	const storedRoom = sessionStorage.getItem('concierge.roomNo') || '';
	if (urlRoom && urlRoom !== storedRoom) {
		try { sessionStorage.clear(); } catch {}
	}

	if (getStoredToken() && (!urlRoom || urlRoom === storedRoom)) {
		guestRoomNo.value = storedRoom ? storedRoom + '호' : '';
		guestName.value = sessionStorage.getItem('concierge.guestName') || '';
	} else if (urlRoom) {
		try {
			const info = await authenticateByRoom(urlRoom);
			guestRoomNo.value = info.roomNo ? `${info.roomNo}호` : '';
			guestName.value = info.perNm || '';
			try { sessionStorage.setItem('concierge.guestName', info.perNm || ''); } catch {}
		} catch (e) {
			authError.value = e?.response?.data?.error?.message || e?.message || '객실 인증에 실패했습니다';
			return;
		}
	} else {
		authError.value = t('auth.scan');
		return;
	}

	await loadFeatures();
	ready.value = true;
	const cur = router.currentRoute.value;
	const cd = cur.meta?.featureCd;
	if (cd && featuresLoaded.value && !tabs.value.some(t => t.featureCd === cd)) {
		if (tabs.value.length) router.replace(tabs.value[0].to);
	}

	tokenCheckTimer = setInterval(() => {
		const token = sessionStorage.getItem('concierge.jwt');
		if (!token) return;
		try {
			const payload = JSON.parse(atob(token.split('.')[1]));
			if (payload.exp * 1000 < Date.now()) {
				sessionStorage.clear();
				authError.value = t('auth.expired');
				ready.value = false;
			}
		} catch {}
	}, 60000);
});

onUnmounted(() => {
	if (tokenCheckTimer) clearInterval(tokenCheckTimer);
});
</script>

<style scoped>
.app-shell { display: flex; height: 100%; background: var(--c-bg); }
.app-shell.no-lnb .app-body { padding: 0; }

/* ═══════════ LNB ═══════════ */
.lnb {
	width: 240px;
	background: var(--c-midnight);
	color: var(--c-text-light);
	display: flex;
	flex-direction: column;
	flex-shrink: 0;
	border-right: 1px solid rgba(201, 169, 110, 0.12);
}

/* ── Brand ── */
.brand {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: var(--sp-6) var(--sp-6);
	border-bottom: 1px solid rgba(201, 169, 110, 0.1);
}
.brand-logomark {
	width: 34px;
	height: 34px;
	background: rgba(201, 169, 110, 0.12);
	border: 1px solid rgba(201, 169, 110, 0.2);
	border-radius: var(--r-md);
	display: flex;
	align-items: center;
	justify-content: center;
	color: var(--c-gold);
	flex-shrink: 0;
}
.brand-name {
	font-family: 'Georgia', 'Times New Roman', serif;
	font-size: 15px;
	font-weight: 400;
	color: var(--c-gold-light);
	letter-spacing: 0.3px;
	line-height: 1.2;
}
.brand-sub {
	font-size: 10px;
	color: rgba(232, 224, 212, 0.35);
	letter-spacing: 1.8px;
	text-transform: uppercase;
	margin-top: 3px;
}

/* ── Nav ── */
.lnb-nav {
	flex: 1;
	padding: var(--sp-4) var(--sp-3);
	display: flex;
	flex-direction: column;
	gap: 2px;
	overflow-y: auto;
}
.tab {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: 13px var(--sp-4);
	border-radius: var(--r-md);
	color: rgba(232, 224, 212, 0.45);
	text-decoration: none;
	transition: background var(--t-fast) var(--ease-out), color var(--t-fast) var(--ease-out);
	position: relative;
	min-height: 50px;
}
.tab .ic {
	font-size: 18px;
	line-height: 1;
	width: 22px;
	text-align: center;
	flex-shrink: 0;
}
.tab .label {
	display: flex;
	flex-direction: column;
	line-height: 1.2;
	font-size: 13.5px;
	font-weight: 500;
	letter-spacing: 0.1px;
}
.tab .label small {
	font-weight: 400;
	opacity: 0.45;
	font-size: 11px;
	margin-top: 2px;
	letter-spacing: 0.2px;
}
.tab:hover {
	background: rgba(201, 169, 110, 0.07);
	color: rgba(232, 224, 212, 0.75);
}
.tab.router-link-active {
	background: rgba(201, 169, 110, 0.1);
	color: var(--c-gold-light);
}
.tab.router-link-active::before {
	content: '';
	position: absolute;
	left: 0;
	top: 18%;
	bottom: 18%;
	width: 2.5px;
	background: var(--c-gold);
	border-radius: 0 var(--r-pill) var(--r-pill) 0;
}

.tab.chat-tab {
	margin-top: var(--sp-3);
	border-top: 1px solid rgba(201, 169, 110, 0.08);
	padding-top: var(--sp-5);
}

/* ── Footer guest card ── */
.lnb-foot {
	padding: var(--sp-4) var(--sp-3);
	border-top: 1px solid rgba(201, 169, 110, 0.08);
}
.guest-card {
	background: rgba(201, 169, 110, 0.06);
	border: 1px solid rgba(201, 169, 110, 0.18);
	border-radius: var(--r-lg);
	padding: var(--sp-4) var(--sp-5);
	display: flex;
	align-items: center;
	gap: var(--sp-4);
}
.guest-card__room {
	display: flex;
	flex-direction: column;
	gap: 2px;
	flex-shrink: 0;
}
.guest-card__room-label {
	font-size: 9px;
	font-weight: 600;
	color: rgba(201, 169, 110, 0.55);
	letter-spacing: 2px;
	text-transform: uppercase;
}
.guest-card__room-num {
	font-family: 'Georgia', 'Times New Roman', serif;
	font-size: 17px;
	font-weight: 400;
	color: var(--c-gold);
	letter-spacing: 0.5px;
	white-space: nowrap;
}
.guest-card__divider {
	width: 1px;
	height: 26px;
	background: rgba(201, 169, 110, 0.15);
	flex-shrink: 0;
}
.guest-card__text {
	display: flex;
	flex-direction: column;
	gap: 2px;
	min-width: 0;
}
.guest-card__greet {
	font-size: 12.5px;
	font-weight: 500;
	color: rgba(232, 224, 212, 0.75);
}
.guest-card__sub {
	font-size: 11px;
	color: rgba(232, 224, 212, 0.3);
	letter-spacing: 0.3px;
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
	.brand { flex: 0 0 100%; padding: var(--sp-4) var(--sp-5); border-bottom: 1px solid rgba(201, 169, 110, 0.08); }
	.lnb-nav {
		flex: 1;
		flex-direction: row;
		overflow-x: auto;
		padding: var(--sp-2) var(--sp-2);
		gap: 2px;
		scrollbar-width: none;
	}
	.lnb-nav::-webkit-scrollbar { display: none; }
	.tab {
		flex: 0 0 auto;
		padding: var(--sp-2) var(--sp-3);
		gap: var(--sp-2);
		min-height: 44px;
	}
	.tab::before { display: none; }
	.tab .label { font-size: 12px; }
	.tab .label small { display: none; }
	.tab.chat-tab { margin-top: 0; border-top: none; padding-top: var(--sp-2); }
	.lnb-foot { display: none; }
	.app-body { padding: var(--sp-5) var(--sp-4); }
}
</style>
