<template>
	<div v-if="authError && showLnb" class="room-login-shell">
		<div class="room-login-card">
			<div class="room-login-logo">
				<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
					<div class="guest-card__room-num">{{ guestRoomNo }}</div>
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
import { computed, onMounted, ref } from 'vue';
import { loadFeatures, enabledSortedFeatures, featuresLoaded } from './features/featureStore.js';
import { getStoredToken, authenticateByRoom } from './auth/authBootstrap.js';
import LoadingSpinner from './components/LoadingSpinner.vue';
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

/**
 * 방 번호 기반 자동 인증
 * URL: ?room=00304 (QR 코드 or 태블릿 북마크)
 * 토큰 있으면 복원, 없으면 room 파라미터로 자동 발급
 */
onMounted(async () => {
	const urlRoom = new URLSearchParams(window.location.search).get('room');

	const storedRoom = sessionStorage.getItem('concierge.roomNo') || '';
	// URL room이 바뀌었으면 기존 세션 무시하고 재인증
	if (urlRoom && urlRoom !== storedRoom) {
		try { sessionStorage.clear(); } catch {}
	}

	if (getStoredToken() && (!urlRoom || urlRoom === storedRoom)) {
		// 이미 인증된 상태 — 저장된 정보 복원
		guestRoomNo.value = storedRoom ? storedRoom + '호' : '';
		guestName.value = sessionStorage.getItem('concierge.guestName') || '';
	} else if (urlRoom) {
		// QR/태블릿 — URL에 room 파라미터 있으면 자동 인증
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
		// room 파라미터 없음 — QR 스캔 안내
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
	background: linear-gradient(135deg, rgba(255,255,255,0.08), rgba(255,255,255,0.03));
	border: 1px solid rgba(255,255,255,0.1);
	border-radius: var(--r-lg);
	padding: var(--sp-4) var(--sp-5);
	display: flex;
	align-items: center;
	gap: var(--sp-4);
}
.guest-card__room-num {
	font-size: 20px;
	font-weight: 900;
	color: var(--c-brand-300);
	letter-spacing: 1px;
	white-space: nowrap;
}
.guest-card__divider {
	width: 1px;
	height: 32px;
	background: rgba(255,255,255,0.15);
	flex-shrink: 0;
}
.guest-card__text {
	display: flex;
	flex-direction: column;
	gap: 2px;
}
.guest-card__greet {
	font-size: var(--fs-md);
	font-weight: 700;
	color: rgba(255,255,255,0.85);
	letter-spacing: 0.3px;
}
.guest-card__sub {
	font-size: var(--fs-xs);
	color: rgba(255,255,255,0.4);
	letter-spacing: 0.5px;
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
