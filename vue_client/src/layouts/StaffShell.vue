<template>
	<div class="ccs-shell">
		<aside class="ccs-lnb">
			<div class="brand">
				<div class="logo">🧑‍🍳</div>
				<div class="text">
					<div class="name">DAOL CCS</div>
					<div class="sub">Communication Center</div>
				</div>
			</div>

			<!-- Lang switcher -->
			<div class="lang-switch">
				<label class="lang-label">{{ t('shell.lang') }}</label>
				<select class="lang-select" :value="currentLang" @change="setLang($event.target.value)">
					<option value="ko_KR">한국어</option>
					<option value="en_US">English</option>
					<option value="ja_JP">日本語</option>
					<option value="zh_CN">中文</option>
				</select>
			</div>

			<!-- 현재 선택된 호텔 (관리자만) -->
			<div v-if="hasAdmin && currentPropCd" class="hotel-bar">
				<div class="hotel-info">
					<div class="hotel-label">🏨 {{ t('shell.currentHotel') }}</div>
					<div class="hotel-name">{{ currentHotelNm || `${currentPropCd} / ${currentCmpxCd}` }}</div>
				</div>
				<button v-if="canChangeHotel" class="hotel-change" @click="goChangeHotel" :title="t('ctx.change')">
					🔄
				</button>
			</div>

			<nav class="nav">
				<div class="group" v-if="hasStaff">
					<div class="group-label">{{ t('shell.group.staff') }}</div>
					<router-link to="/staff" class="item">
						<span class="ic">📋</span><span class="label">{{ t('shell.nav.dashboard') }}</span>
					</router-link>
					<router-link to="/staff/duty" class="item" v-if="hasStaff">
						<span class="ic">🗓️</span><span class="label">{{ t('shell.nav.duty') }}</span>
					</router-link>
				</div>

				<div class="group" v-if="hasAdmin">
					<div class="group-label">{{ t('shell.group.admin') }}</div>
					<router-link to="/admin/features" class="item">
						<span class="ic">⚙️</span><span class="label">{{ t('shell.nav.features') }}</span>
					</router-link>
					<router-link to="/admin/ccs" class="item">
						<span class="ic">👥</span><span class="label">{{ t('shell.nav.ccs') }}</span>
					</router-link>
					<router-link to="/admin/lostfound" class="item">
						<span class="ic">🔍</span><span class="label">{{ t('shell.nav.lostfound') }}</span>
					</router-link>
					<router-link to="/admin/voc" class="item">
						<span class="ic">💬</span><span class="label">{{ t('shell.nav.voc') }}</span>
					</router-link>
					<router-link to="/admin/rental" class="item">
						<span class="ic">🏷️</span><span class="label">{{ t('shell.nav.rental') }}</span>
					</router-link>
					<router-link to="/admin/duty" class="item">
						<span class="ic">🗓️</span><span class="label">{{ t('shell.nav.duty') }}</span>
					</router-link>
					<router-link to="/admin/reports" class="item">
						<span class="ic">📊</span><span class="label">{{ t('shell.nav.reports') }}</span>
					</router-link>
					<router-link to="/admin/audit" class="item">
						<span class="ic">📜</span><span class="label">{{ t('shell.nav.audit') }}</span>
					</router-link>
					<router-link to="/admin/qr" class="item">
						<span class="ic">📷</span><span class="label">{{ t('shell.nav.qr') }}</span>
					</router-link>
				</div>
			</nav>

			<div class="foot">
				<div class="user">
					<div class="avatar">{{ initial }}</div>
					<div class="who">
						<div class="name">{{ displayName }}</div>
						<div class="role">{{ roleLabel }}</div>
					</div>
				</div>
				<button class="logout" @click="logout" :title="t('shell.logout')">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
						stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
						<polyline points="16 17 21 12 16 7" />
						<line x1="21" y1="12" x2="9" y2="12" />
					</svg>
				</button>
			</div>
		</aside>

		<main class="ccs-main">
			<slot />
		</main>
	</div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { t } from '../i18n/ui.js';

const route = useRoute();
const router = useRouter();

const staffToken = ref(null);
const staffInfo = ref({});
// 관리 중인 호텔 컨텍스트. PropertyContextView 에서 sessionStorage 에 쓰고,
// refreshAuth() 가 route 변경 시 다시 읽어 reactive 로 갱신한다.
const ctxData = ref({});
const currentLang = ref('ko_KR');

function readLang() {
	try {
		return sessionStorage.getItem('concierge.staffLang')
			|| sessionStorage.getItem('concierge.perUseLang')
			|| 'ko_KR';
	} catch {
		return 'ko_KR';
	}
}

function setLang(value) {
	try {
		sessionStorage.setItem('concierge.staffLang', value);
		sessionStorage.setItem('concierge.perUseLang', value);
	} catch {}
	currentLang.value = value;
	// 간단한 전환: 페이지 새로고침 — reactive 전역 언어 스토어가 없으므로
	window.location.reload();
}

function refreshAuth() {
	try {
		staffToken.value = sessionStorage.getItem('ccs.token');
		const raw = sessionStorage.getItem('ccs.staff');
		staffInfo.value = raw ? JSON.parse(raw) : {};
		const ctxRaw = sessionStorage.getItem('ccs.context');
		ctxData.value = ctxRaw ? JSON.parse(ctxRaw) : {};
	} catch {
		staffInfo.value = {};
		ctxData.value = {};
	}
}

onMounted(() => {
	refreshAuth();
	currentLang.value = readLang();
});
watch(() => route.fullPath, refreshAuth);

const hasStaff = computed(() => !!staffToken.value);

// 관리자 역할은 PMS_USER_MTR.USER_TP 기반 (00001/00002/00003 중 하나)
const userTp = computed(() => staffInfo.value?.userTp || '');
const isSystemAdmin   = computed(() => userTp.value === '00001');
const isPropertyAdmin = computed(() => userTp.value === '00002');
const isComplexAdmin  = computed(() => userTp.value === '00003');
const hasAdmin = computed(() => isSystemAdmin.value || isPropertyAdmin.value || isComplexAdmin.value);

const displayName = computed(() => staffInfo.value?.staffNm || '—');

const initial = computed(() => {
	const n = displayName.value;
	return n && n !== '—' ? n.slice(0, 1) : '·';
});

const roleLabel = computed(() => {
	if (!hasStaff.value) return '';
	if (isSystemAdmin.value)   return t('role.sysAdmin');
	if (isPropertyAdmin.value) return t('role.propAdmin');
	if (isComplexAdmin.value)  return t('role.cmpxAdmin');
	const d = staffInfo.value?.deptCd;
	return d ? `${d} · ${t('shell.group.staff')}` : t('shell.group.staff');
});

function logout() {
	try {
		sessionStorage.removeItem('ccs.token');
		sessionStorage.removeItem('ccs.staff');
		sessionStorage.removeItem('ccs.context');
		sessionStorage.removeItem('concierge.adminToken'); // 구버전 흔적 제거
	} catch {}
	staffToken.value = null;
	staffInfo.value = {};
	router.replace('/staff/login');
}

function goChangeHotel() {
	router.push('/staff/context');
}

// 현재 선택된 호텔 표시 — ctxData ref 기반이라 refreshAuth 호출 시 reactive 갱신됨
const currentPropCd = computed(() => ctxData.value?.propCd || '');
const currentCmpxCd = computed(() => ctxData.value?.cmpxCd || '');
const currentHotelNm = computed(() => ctxData.value?.cmpxNm || ctxData.value?.propNm || '');
// CMPX_ADMIN 은 호텔 변경 불가
const canChangeHotel = computed(() => isSystemAdmin.value || isPropertyAdmin.value);
</script>

<style scoped>
.ccs-shell {
	display: flex;
	min-height: 100vh;
	background: var(--c-bg);
}

/* ═══ LNB ═══ */
.ccs-lnb {
	width: 248px;
	background: linear-gradient(180deg, var(--c-brand-900), #0b1f3b);
	color: #fff;
	display: flex;
	flex-direction: column;
	flex-shrink: 0;
	box-shadow: 4px 0 24px rgba(11, 31, 59, 0.18);
}

.brand {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: var(--sp-6) var(--sp-5);
	border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.brand .logo {
	font-size: 32px;
	width: 44px;
	height: 44px;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(255, 255, 255, 0.08);
	border-radius: var(--r-md);
}
.brand .text .name { font-size: var(--fs-lg); font-weight: 800; letter-spacing: 0.3px; }
.brand .text .sub {
	font-size: var(--fs-xs);
	opacity: 0.55;
	letter-spacing: 0.6px;
	text-transform: uppercase;
	margin-top: 2px;
}

/* Language switcher */
.lang-switch {
	padding: 10px var(--sp-5);
	border-bottom: 1px solid rgba(255, 255, 255, 0.06);
	display: flex;
	align-items: center;
	gap: 10px;
}
.lang-label {
	font-size: 10px;
	text-transform: uppercase;
	letter-spacing: 1.2px;
	opacity: 0.5;
	flex-shrink: 0;
}
.lang-select {
	flex: 1;
	padding: 6px 8px;
	background: rgba(255,255,255,0.06);
	color: #fff;
	border: 1px solid rgba(255,255,255,0.1);
	border-radius: 4px;
	font-size: 12px;
	cursor: pointer;
}
.lang-select option {
	color: #1a202c;
	background: #fff;
}

/* Current hotel bar (관리자 전용) */
.hotel-bar {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 10px var(--sp-5);
	background: rgba(255, 255, 255, 0.04);
	border-bottom: 1px solid rgba(255,255,255,0.06);
}
.hotel-info { flex: 1; min-width: 0; }
.hotel-label {
	font-size: 10px;
	text-transform: uppercase;
	letter-spacing: 1px;
	opacity: 0.55;
	margin-bottom: 2px;
}
.hotel-name {
	font-size: 13px;
	font-weight: 700;
	color: #fff;
	letter-spacing: 0.2px;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}
.hotel-change {
	background: rgba(255,255,255,0.08);
	color: #fff;
	border: 1px solid rgba(255,255,255,0.12);
	border-radius: 6px;
	width: 32px;
	height: 32px;
	display: flex;
	align-items: center;
	justify-content: center;
	cursor: pointer;
	font-size: 14px;
	transition: background 0.15s;
}
.hotel-change:hover { background: rgba(255,255,255,0.14); }

.nav {
	flex: 1;
	padding: var(--sp-4) var(--sp-3);
	overflow-y: auto;
	display: flex;
	flex-direction: column;
	gap: var(--sp-5);
}
.group { display: flex; flex-direction: column; gap: 4px; }
.group-label {
	font-size: 10px;
	text-transform: uppercase;
	letter-spacing: 1.2px;
	opacity: 0.4;
	padding: 0 var(--sp-3) var(--sp-2);
}
.item {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: 10px 14px;
	border-radius: var(--r-md);
	color: #cfd8dc;
	text-decoration: none;
	font-size: var(--fs-md);
	font-weight: 600;
	transition: background var(--t-fast) var(--ease-out), transform var(--t-fast) var(--ease-out);
}
.item .ic { font-size: 18px; width: 22px; text-align: center; }
.item:hover {
	background: rgba(255, 255, 255, 0.06);
	color: #fff;
}
.item.router-link-active {
	background: linear-gradient(135deg, var(--c-brand-500), var(--c-accent-500));
	color: #fff;
	box-shadow: var(--sh-brand);
}

.foot {
	padding: var(--sp-4) var(--sp-4) var(--sp-5);
	border-top: 1px solid rgba(255, 255, 255, 0.08);
	display: flex;
	align-items: center;
	gap: var(--sp-3);
}
.user { display: flex; align-items: center; gap: var(--sp-3); flex: 1; min-width: 0; }
.avatar {
	width: 36px;
	height: 36px;
	border-radius: var(--r-pill);
	background: linear-gradient(135deg, var(--c-brand-500), var(--c-accent-400));
	color: #fff;
	font-weight: 800;
	font-size: var(--fs-md);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.who { min-width: 0; }
.who .name {
	font-size: var(--fs-sm);
	font-weight: 700;
	color: #fff;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}
.who .role {
	font-size: var(--fs-xs);
	opacity: 0.55;
	margin-top: 2px;
}
.logout {
	width: 36px;
	height: 36px;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(255, 255, 255, 0.06);
	color: #cfd8dc;
	border: none;
	border-radius: var(--r-sm);
	cursor: pointer;
	transition: background var(--t-fast) var(--ease-out);
}
.logout:hover { background: var(--c-err-500); color: #fff; }

/* ═══ Main ═══ */
.ccs-main {
	flex: 1;
	padding: var(--sp-8) var(--sp-8);
	overflow: auto;
	min-width: 0;
}

/* ═══ Mobile (< 720px) — LNB 을 상단 가로 바로 변환 ═══ */
@media (max-width: 720px) {
	.ccs-shell { flex-direction: column; width: 100%; max-width: 100vw; overflow-x: hidden; }
	.ccs-lnb {
		width: 100%;
		max-width: 100%;
		flex-direction: row;
		flex-wrap: wrap;
		box-shadow: 0 4px 12px rgba(11, 31, 59, 0.18);
	}
	.brand { flex: 0 0 100%; padding: var(--sp-4) var(--sp-5); min-width: 0; }
	.lang-switch { flex: 0 0 100%; padding: 6px var(--sp-5); }
	.nav {
		flex: 1 1 100%;
		flex-direction: row;
		overflow-x: auto;
		padding: var(--sp-3);
		gap: var(--sp-2);
		max-width: 100%;
	}
	.group { flex-direction: row; gap: var(--sp-1); }
	.group-label { display: none; }
	.item { padding: 10px 14px; white-space: nowrap; }
	.foot {
		flex: 0 0 100%;
		padding: var(--sp-3) var(--sp-4);
		gap: var(--sp-2);
		justify-content: space-between;
		border-top: 1px solid rgba(255, 255, 255, 0.08);
		min-width: 0;
	}
	.user { min-width: 0; overflow: hidden; }
	.avatar { width: 32px; height: 32px; font-size: var(--fs-sm); }
	.logout { flex-shrink: 0; width: 36px; height: 36px; }
	.ccs-main { padding: var(--sp-4); width: 100%; max-width: 100%; overflow-x: hidden; }
}
</style>
