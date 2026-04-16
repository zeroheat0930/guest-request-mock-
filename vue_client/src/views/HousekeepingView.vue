<template>
	<div class="hk">
		<div class="page-header">
			<div class="page-header__icon">
				<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
					<polyline points="9 22 9 12 15 12 15 22"/>
				</svg>
			</div>
			<div>
				<h2 class="page-title">객실 정비</h2>
				<p class="page-sub">Housekeeping &amp; DND</p>
			</div>
		</div>

		<div class="form-card">
			<div class="form-group">
				<label class="field-label">예약 정보</label>
				<div class="select-wrap">
					<select v-model="rsvNo" @change="loadStat">
						<option value="R2026041300001">R2026041300001 · 1205호</option>
						<option value="R2026041300002">R2026041300002 · 0807호</option>
					</select>
					<span class="select-arrow">
						<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
					</span>
				</div>
			</div>

			<div class="status-section">
				<div class="status-label">현재 객실 상태</div>
				<LoadingSpinner v-if="loadingStatus" text="상태 확인 중..." />
				<div v-else class="status-display">
					<span class="status-icon">{{ currentIcon }}</span>
					<span class="status-name">{{ curStatNm }}</span>
				</div>
			</div>

			<div class="divider" />

			<div class="actions-section">
				<div class="field-label">상태 변경</div>
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
						<span v-if="curStatCd === action.cd" class="action-badge">현재</span>
					</button>
				</div>
			</div>
		</div>

		<Transition name="toast">
			<div v-if="result" class="toast" :class="result.status === 0 ? 'toast--ok' : 'toast--err'">
				<span class="toast-icon">{{ result.status === 0 ? '✓' : '!' }}</span>
				<span class="toast-msg">{{ result.message }}</span>
			</div>
		</Transition>
	</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { fetchHousekeeping, updateHousekeeping } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner.vue';

const ACTIONS = [
	{ cd: 'MU',  icon: '🛏️', name: '룸 정비',   desc: '객실 청소를 요청합니다',  theme: 'blue' },
	{ cd: 'DND', icon: '🚫', name: '방해 금지',  desc: '입실 중 방문을 삼가해 주세요', theme: 'red' },
	{ cd: 'CLR', icon: '✅', name: '정비 완료',  desc: '현재 상태를 해제합니다',  theme: 'green' },
];

const STAT_ICONS = { MU: '🛏️', DND: '🚫', CLR: '✅' };

const rsvNo = ref('R2026041300001');
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
	display: flex;
	align-items: center;
	gap: var(--sp-4);
	margin-bottom: var(--sp-6);
}
.page-header__icon {
	width: 48px;
	height: 48px;
	background: var(--c-brand-700);
	color: var(--c-brand-300);
	border-radius: var(--r-md);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.page-title {
	font-size: var(--fs-2xl);
	font-weight: 800;
	color: var(--c-brand-900);
	letter-spacing: -0.4px;
	line-height: 1.2;
}
.page-sub {
	font-size: var(--fs-sm);
	color: var(--c-muted);
	letter-spacing: 0.8px;
	text-transform: uppercase;
	margin-top: 2px;
}

/* ── Form Card ── */
.form-card {
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-radius: var(--r-xl);
	padding: var(--sp-8);
	box-shadow: var(--sh-md);
	display: flex;
	flex-direction: column;
	gap: var(--sp-6);
}

/* ── Field ── */
.form-group { display: flex; flex-direction: column; gap: var(--sp-2); }
.field-label {
	font-size: var(--fs-sm);
	font-weight: 700;
	color: var(--c-text-soft);
	letter-spacing: 0.3px;
	text-transform: uppercase;
}

.select-wrap { position: relative; }
.select-wrap select {
	width: 100%;
	padding: var(--sp-3) var(--sp-10) var(--sp-3) var(--sp-4);
	border: 1.5px solid var(--c-border);
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	background: var(--c-bg-soft);
	color: var(--c-text);
	appearance: none;
	-webkit-appearance: none;
	cursor: pointer;
	transition: border-color var(--t-fast), box-shadow var(--t-fast);
}
.select-wrap select:focus {
	outline: none;
	border-color: var(--c-brand-500);
	box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
	background: var(--c-surface);
}
.select-arrow {
	position: absolute;
	right: var(--sp-4);
	top: 50%;
	transform: translateY(-50%);
	color: var(--c-muted);
	pointer-events: none;
	display: flex;
}

/* ── Status ── */
.status-section { display: flex; flex-direction: column; gap: var(--sp-3); }
.status-label {
	font-size: var(--fs-sm);
	font-weight: 700;
	color: var(--c-text-soft);
	letter-spacing: 0.3px;
	text-transform: uppercase;
}
.status-display {
	display: flex;
	align-items: center;
	gap: var(--sp-4);
	background: var(--c-bg-soft);
	border: 1.5px solid var(--c-border);
	border-radius: var(--r-md);
	padding: var(--sp-4) var(--sp-5);
}
.status-icon { font-size: 28px; line-height: 1; }
.status-name { font-size: var(--fs-xl); font-weight: 800; color: var(--c-brand-900); }

.divider { height: 1px; background: var(--c-border); margin: 0 calc(-1 * var(--sp-8)); }

/* ── Actions ── */
.actions-section { display: flex; flex-direction: column; gap: var(--sp-4); }
.action-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--sp-3); }

.action-card {
	position: relative;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: var(--sp-2);
	padding: var(--sp-5) var(--sp-3);
	border-radius: var(--r-lg);
	border: 2px solid var(--c-border);
	background: var(--c-bg-soft);
	cursor: pointer;
	text-align: center;
	transition: border-color var(--t-norm) var(--ease-out), background var(--t-norm) var(--ease-out), box-shadow var(--t-norm) var(--ease-out), transform var(--t-fast) var(--ease-out);
}
.action-card:not(:disabled):hover {
	transform: translateY(-2px);
	box-shadow: var(--sh-md);
}
.action-card:disabled { opacity: 0.5; cursor: not-allowed; }

.action-card--blue:not(:disabled):hover { border-color: var(--c-brand-400); background: var(--c-brand-50); }
.action-card--blue.action-card--active { border-color: var(--c-brand-500); background: var(--c-brand-50); box-shadow: 0 4px 12px rgba(37,99,235,0.18); }

.action-card--red:not(:disabled):hover { border-color: #fc8181; background: #fff5f5; }
.action-card--red.action-card--active { border-color: var(--c-err-500); background: #fff5f5; box-shadow: 0 4px 12px rgba(229,62,62,0.18); }

.action-card--green:not(:disabled):hover { border-color: var(--c-ok-500); background: var(--c-ok-50); }
.action-card--green.action-card--active { border-color: var(--c-ok-500); background: var(--c-ok-50); box-shadow: 0 4px 12px rgba(34,197,94,0.18); }

.action-icon { font-size: 32px; line-height: 1; }
.action-name { font-size: var(--fs-md); font-weight: 800; color: var(--c-text); }
.action-desc { font-size: var(--fs-xs); color: var(--c-muted); line-height: 1.4; }

.action-badge {
	position: absolute;
	top: var(--sp-2);
	right: var(--sp-2);
	background: var(--c-brand-700);
	color: #fff;
	font-size: var(--fs-xs);
	font-weight: 700;
	padding: 2px var(--sp-2);
	border-radius: var(--r-pill);
	letter-spacing: 0.2px;
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
.toast-icon {
	width: 26px;
	height: 26px;
	border-radius: var(--r-pill);
	background: rgba(255,255,255,0.2);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: var(--fs-sm);
	font-weight: 800;
	flex-shrink: 0;
}
.toast-msg { font-size: var(--fs-md); font-weight: 700; }

.toast-enter-active, .toast-leave-active {
	transition: opacity var(--t-norm) var(--ease-out), transform var(--t-norm) var(--ease-out);
}
.toast-enter-from { opacity: 0; transform: translateX(-50%) translateY(16px); }
.toast-leave-to   { opacity: 0; transform: translateX(-50%) translateY(16px); }

@media (max-width: 480px) {
	.form-card { padding: var(--sp-5); }
	.action-grid { grid-template-columns: 1fr; }
	.action-card { flex-direction: row; text-align: left; padding: var(--sp-4); }
	.action-icon { font-size: 24px; }
}
</style>
