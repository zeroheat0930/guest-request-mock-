<template>
	<div class="hk">
		<div class="page-header">
			<h2 class="page-title">{{ t('hk.title') }}</h2>
			<p class="page-sub">Housekeeping &amp; DND</p>
		</div>

		<div class="guest-bar">
			<span class="guest-bar__room">{{ t('guest.room.label', roomNo) }}</span>
		</div>

		<div class="form-card">
			<div class="status-section">
				<div class="status-label">{{ t('hk.current') }}</div>
				<LoadingSpinner v-if="loadingStatus" :text="t('loading')" />
				<div v-else class="status-display">
					<span class="status-icon">{{ currentIcon }}</span>
					<span class="status-name">{{ curStatNm }}</span>
				</div>
			</div>

			<div class="divider" />

			<div class="actions-section">
				<div class="actions-label">{{ t('hk.change') }}</div>
				<div class="action-grid">
					<button
						v-for="action in ACTIONS"
						:key="action.cd"
						class="action-card"
						:class="[`action-card--${action.theme}`, { 'action-card--active': curStatCd === action.cd }]"
						@click="change(action.cd)"
						:disabled="submitting"
					>
						<span class="action-icon">{{ action.icon }}</span>
						<span class="action-name">{{ action.name }}</span>
						<span class="action-desc">{{ action.desc }}</span>
						<span v-if="curStatCd === action.cd" class="action-badge">{{ t('hk.current.badge') }}</span>
					</button>
				</div>
			</div>
		</div>

		<Transition name="toast">
			<div v-if="result" class="toast" :class="result.status === 0 ? 'toast--ok' : 'toast--err'">
				<div class="toast-indicator">{{ result.status === 0 ? '✓' : '!' }}</div>
				<span class="toast-msg">{{ result.message }}</span>
			</div>
		</Transition>
	</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { fetchHousekeeping, updateHousekeeping } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import { t } from '../i18n/ui.js';

const ACTIONS = [
	{ cd: 'MU',  icon: '🛏️', name: t('hk.mu'),  desc: t('hk.mu.desc'),  theme: 'blue' },
	{ cd: 'DND', icon: '🚫', name: t('hk.dnd'), desc: t('hk.dnd.desc'), theme: 'red' },
	{ cd: 'CLR', icon: '✅', name: t('hk.clr'), desc: t('hk.clr.desc'), theme: 'green' },
];

const STAT_ICONS = { MU: '🛏️', DND: '🚫', CLR: '✅' };

const rsvNo = ref(sessionStorage.getItem('concierge.rsvNo') || '');
const roomNo = ref(sessionStorage.getItem('concierge.roomNo') || '');
const guestName = ref(sessionStorage.getItem('concierge.guestName') || '');
const curStatNm = ref('-');
const curStatCd = ref('');
const result = ref(null);
const loadingStatus = ref(false);
const submitting = ref(false);

const currentIcon = computed(() => STAT_ICONS[curStatCd.value] || '•');

async function loadStat() {
	loadingStatus.value = true;
	try {
		const r = await fetchHousekeeping(rsvNo.value);
		curStatNm.value = r.map?.hkStatNm || '-';
		curStatCd.value = r.map?.hkStatCd || '';
	} catch (e) {
		curStatNm.value = '조회 실패';
	} finally {
		loadingStatus.value = false;
	}
}

function showResult(data) {
	result.value = data;
	setTimeout(() => { result.value = null; }, 3000);
}

async function change(cd) {
	submitting.value = true;
	try {
		const res = await updateHousekeeping({ rsvNo: rsvNo.value, hkStatCd: cd });
		showResult(res);
		await loadStat();
	} catch (err) {
		showResult(err);
	} finally {
		submitting.value = false;
	}
}

onMounted(loadStat);
</script>

<style scoped>
.hk { max-width: 640px; }

/* ── Page Header ── */
.page-header {
	margin-bottom: var(--sp-6);
}
.page-title {
	font-size: var(--fs-2xl);
	font-weight: 700;
	color: var(--c-text);
	letter-spacing: -0.5px;
	line-height: 1.25;
	margin: 0 0 var(--sp-1) 0;
}
.page-sub {
	font-size: var(--fs-sm);
	color: var(--c-text-soft);
	margin: 0;
}

/* ── Guest Bar ── */
.guest-bar {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: 14px var(--sp-5);
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-radius: var(--r-md);
	margin-bottom: var(--sp-5);
	box-shadow: var(--sh-xs);
}
.guest-bar__room {
	font-weight: 700;
	font-size: var(--fs-md);
	color: var(--c-text);
}

/* ── Form Card ── */
.form-card {
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-radius: var(--r-xl);
	padding: var(--sp-8);
	box-shadow: var(--sh-sm);
	display: flex;
	flex-direction: column;
	gap: var(--sp-6);
}

/* ── Status ── */
.status-section { display: flex; flex-direction: column; gap: var(--sp-3); }
.status-label {
	font-size: var(--fs-sm);
	font-weight: 600;
	color: var(--c-text-soft);
}
.status-display {
	display: flex;
	align-items: center;
	gap: var(--sp-4);
	background: var(--c-bg);
	border: 1px solid var(--c-border);
	border-radius: var(--r-md);
	padding: var(--sp-5) var(--sp-5);
}
.status-icon { font-size: 26px; line-height: 1; }
.status-name { font-size: var(--fs-xl); font-weight: 700; color: var(--c-text); }

.divider { height: 1px; background: var(--c-border); margin: 0 calc(-1 * var(--sp-8)); }

/* ── Actions ── */
.actions-section { display: flex; flex-direction: column; gap: var(--sp-4); }
.actions-label {
	font-size: var(--fs-sm);
	font-weight: 600;
	color: var(--c-text-soft);
}
.action-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--sp-3); }

.action-card {
	position: relative;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: var(--sp-2);
	padding: var(--sp-7) var(--sp-3);
	border-radius: var(--r-lg);
	border: 1.5px solid var(--c-border);
	background: var(--c-bg);
	cursor: pointer;
	text-align: center;
	min-height: 130px;
	transition: border-color var(--t-norm) var(--ease-out), background var(--t-norm) var(--ease-out), box-shadow var(--t-norm) var(--ease-out), transform var(--t-fast) var(--ease-out);
}
.action-card:not(:disabled):hover {
	transform: translateY(-2px);
	box-shadow: var(--sh-md);
}
.action-card:disabled { opacity: 0.5; cursor: not-allowed; }

.action-card--blue:not(:disabled):hover { border-color: var(--c-brand-400); background: var(--c-brand-50); }
.action-card--blue.action-card--active { border-color: var(--c-brand-400); background: var(--c-brand-50); box-shadow: 0 4px 14px rgba(37,99,235,0.14); }

.action-card--red:not(:disabled):hover { border-color: #fca5a5; background: #fef2f2; }
.action-card--red.action-card--active { border-color: #f87171; background: #fef2f2; box-shadow: 0 4px 14px rgba(239,68,68,0.14); }

.action-card--green:not(:disabled):hover { border-color: #6ee7b7; background: var(--c-ok-50); }
.action-card--green.action-card--active { border-color: var(--c-ok-500); background: var(--c-ok-50); box-shadow: 0 4px 14px rgba(16,185,129,0.14); }

.action-icon { font-size: 30px; line-height: 1; }
.action-name { font-size: var(--fs-md); font-weight: 700; color: var(--c-text); }
.action-desc { font-size: var(--fs-xs); color: var(--c-muted); line-height: 1.4; }

.action-badge {
	position: absolute;
	top: var(--sp-2);
	right: var(--sp-2);
	background: var(--c-brand-500);
	color: #fff;
	font-size: 11px;
	font-weight: 700;
	padding: 2px var(--sp-2);
	border-radius: var(--r-pill);
}

/* ── Toast ── */
.toast {
	position: fixed;
	bottom: var(--sp-8);
	left: 50%;
	transform: translateX(-50%);
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: var(--sp-4) var(--sp-6);
	border-radius: var(--r-xl);
	box-shadow: var(--sh-xl);
	min-width: 240px;
	max-width: 420px;
	z-index: 200;
}
.toast--ok { background: #065f46; color: #fff; }
.toast--err { background: var(--c-err-600); color: #fff; }
.toast-indicator {
	width: 26px;
	height: 26px;
	border-radius: var(--r-pill);
	background: rgba(255,255,255,0.18);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 12px;
	font-weight: 800;
	flex-shrink: 0;
}
.toast-msg { font-size: var(--fs-md); font-weight: 600; }

.toast-enter-active, .toast-leave-active {
	transition: opacity var(--t-norm) var(--ease-out), transform var(--t-norm) var(--ease-out);
}
.toast-enter-from { opacity: 0; transform: translateX(-50%) translateY(16px); }
.toast-leave-to   { opacity: 0; transform: translateX(-50%) translateY(16px); }

@media (max-width: 480px) {
	.form-card { padding: var(--sp-5); }
	.divider { margin: 0 calc(-1 * var(--sp-5)); }
	.action-grid { grid-template-columns: 1fr; }
	.action-card { flex-direction: row; text-align: left; padding: var(--sp-4) var(--sp-5); min-height: 0; }
	.action-icon { font-size: 24px; }
}
</style>
