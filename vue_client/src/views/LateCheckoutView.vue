<template>
	<div class="lc">
		<div class="page-header">
			<div class="page-header__icon">
				<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<circle cx="12" cy="12" r="10"/>
					<polyline points="12 6 12 12 16 14"/>
				</svg>
			</div>
			<div>
				<h2 class="page-title">레이트 체크아웃</h2>
				<p class="page-sub">Late Checkout</p>
			</div>
		</div>

		<!-- Step indicator -->
		<div class="steps">
			<div class="step" :class="{ 'step--active': true, 'step--done': !!info }">
				<div class="step-num">1</div>
				<span class="step-label">시간 선택</span>
			</div>
			<div class="step-line" :class="{ 'step-line--done': !!info }" />
			<div class="step" :class="{ 'step--active': !!info, 'step--done': !!result && result.status === 0 }">
				<div class="step-num">2</div>
				<span class="step-label">요금 확인</span>
			</div>
			<div class="step-line" :class="{ 'step-line--done': !!result && result.status === 0 }" />
			<div class="step" :class="{ 'step--active': !!result && result.status === 0 }">
				<div class="step-num">3</div>
				<span class="step-label">신청 완료</span>
			</div>
		</div>

		<div class="form-card">
			<div class="guest-info">
				<span class="guest-room">{{ roomNo }}호 고객님</span>
				
			</div>

			<div class="form-group">
				<label class="field-label">요청 체크아웃 시각</label>
				<div class="time-grid">
					<button
						v-for="t in TIME_OPTIONS"
						:key="t.val"
						class="time-btn"
						:class="{ 'time-btn--active': reqOutTm === t.val }"
						@click="reqOutTm = t.val; info = null"
					>
						{{ t.label }}
					</button>
				</div>
			</div>

			<button class="check-btn" @click="check" :disabled="checking">
				<template v-if="checking">
					<svg class="spin" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>
					확인 중...
				</template>
				<template v-else>
					<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
					가능 여부 확인
				</template>
			</button>

			<Transition name="fade-up">
				<div v-if="info" class="info-card" :class="info.availYn === 'Y' ? 'info-card--avail' : 'info-card--unavail'">
					<div class="info-header">
						<span class="avail-badge" :class="info.availYn === 'Y' ? 'avail-badge--ok' : 'avail-badge--no'">
							{{ info.availYn === 'Y' ? '✓ 가능' : '✗ 불가' }}
						</span>
						<span class="rate-type">{{ info.rateTpNm }}</span>
					</div>
					<div class="fee-display">
						<span class="fee-label">추가 요금</span>
						<span class="fee-amount">
							<span class="fee-currency">{{ info.curCd }}</span>
							{{ Number(info.addAmt).toLocaleString() }}
						</span>
					</div>
					<button v-if="info.availYn === 'Y'" class="apply-btn" @click="apply">
						<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
						레이트 체크아웃 신청하기
					</button>
				</div>
			</Transition>
		</div>

		<Transition name="toast">
			<div v-if="result" class="toast" :class="result.status === 0 ? 'toast--ok' : 'toast--err'">
				<span class="toast-icon">{{ result.status === 0 ? '✓' : '!' }}</span>
				<div>
					<div class="toast-msg">{{ result.message }}</div>
					<div v-if="result.map && result.map.reqNo" class="toast-meta">요청번호: {{ result.map.reqNo }}</div>
				</div>
			</div>
		</Transition>
	</div>
</template>

<script setup>
import { ref } from 'vue';
import { checkLateCheckout, requestLateCheckout } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner.vue';

const TIME_OPTIONS = [
	{ val: '1200', label: '12:00' },
	{ val: '1300', label: '13:00' },
	{ val: '1400', label: '14:00' },
	{ val: '1600', label: '16:00' },
	{ val: '1800', label: '18:00' },
	{ val: '2000', label: '20:00' },
];

const rsvNo = ref(sessionStorage.getItem('concierge.rsvNo') || '');
const roomNo = ref(sessionStorage.getItem('concierge.roomNo') || '');
const guestName = ref(sessionStorage.getItem('concierge.guestName') || '');
const reqOutTm = ref('1300');
const info = ref(null);
const result = ref(null);
const checking = ref(false);

function showResult(data) {
	result.value = data;
	setTimeout(() => { result.value = null; }, 4000);
}

async function check() {
	result.value = null;
	info.value = null;
	checking.value = true;
	try {
		const r = await checkLateCheckout(rsvNo.value, reqOutTm.value);
		info.value = r.map;
	} catch (err) {
		showResult(err);
	} finally {
		checking.value = false;
	}
}

async function apply() {
	try {
		const res = await requestLateCheckout({
			rsvNo: rsvNo.value,
			reqOutTm: reqOutTm.value,
			addAmt: info.value.addAmt
		});
		showResult(res);
	} catch (err) {
		showResult(err);
	}
}
</script>

<style scoped>
.lc { max-width: 640px; }

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

/* ── Steps ── */
.steps {
	display: flex;
	align-items: center;
	margin-bottom: var(--sp-6);
	gap: 0;
}
.step {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: var(--sp-1);
	flex: 0 0 auto;
}
.step-num {
	width: 28px;
	height: 28px;
	border-radius: var(--r-pill);
	background: var(--c-border);
	color: var(--c-muted);
	font-size: var(--fs-sm);
	font-weight: 800;
	display: flex;
	align-items: center;
	justify-content: center;
	transition: background var(--t-norm) var(--ease-out), color var(--t-norm) var(--ease-out);
}
.step--active .step-num { background: var(--c-brand-700); color: #fff; }
.step--done .step-num { background: #065f46; color: #fff; }
.step-label {
	font-size: var(--fs-xs);
	font-weight: 600;
	color: var(--c-muted);
	white-space: nowrap;
	transition: color var(--t-norm);
}
.step--active .step-label { color: var(--c-brand-700); }
.step--done .step-label { color: #065f46; }

.step-line {
	flex: 1;
	height: 2px;
	background: var(--c-border);
	margin: 0 var(--sp-2);
	margin-bottom: 18px;
	border-radius: var(--r-pill);
	transition: background var(--t-norm);
}
.step-line--done { background: #065f46; }

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

/* ── Guest Info ── */
.guest-info {
	display: flex;
	align-items: center;
	gap: 10px;
	padding: 14px 16px;
	background: var(--c-brand-50, #ebf4ff);
	border-radius: var(--r-md, 10px);
	margin-bottom: 16px;
}
.guest-room {
	font-weight: 800;
	font-size: 16px;
	color: var(--c-brand-700, #1a3a6e);
}
.guest-name {
	font-size: 14px;
	color: var(--c-text-soft, #718096);
}

.form-group { display: flex; flex-direction: column; gap: var(--sp-2); }
.field-label {
	font-size: var(--fs-sm);
	font-weight: 700;
	color: var(--c-text-soft);
	letter-spacing: 0.3px;
	text-transform: uppercase;
}

/* ── Time grid ── */
.time-grid {
	display: grid;
	grid-template-columns: repeat(3, 1fr);
	gap: var(--sp-2);
}
.time-btn {
	padding: var(--sp-3) var(--sp-2);
	border: 1.5px solid var(--c-border);
	border-radius: var(--r-md);
	background: var(--c-bg-soft);
	font-size: var(--fs-lg);
	font-weight: 700;
	color: var(--c-text-soft);
	cursor: pointer;
	transition: all var(--t-fast) var(--ease-out);
	text-align: center;
}
.time-btn:hover { border-color: var(--c-brand-400); color: var(--c-brand-700); background: var(--c-brand-50); }
.time-btn--active {
	border-color: var(--c-brand-600);
	background: var(--c-brand-700);
	color: #fff;
	box-shadow: var(--sh-brand);
}

/* ── Check button ── */
.check-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: var(--sp-2);
	width: 100%;
	padding: var(--sp-4);
	background: var(--c-brand-700);
	color: #fff;
	border: none;
	border-radius: var(--r-md);
	font-size: var(--fs-lg);
	font-weight: 800;
	cursor: pointer;
	transition: background var(--t-norm) var(--ease-out), box-shadow var(--t-norm) var(--ease-out), transform var(--t-fast);
}
.check-btn:hover:not(:disabled) {
	background: var(--c-brand-900);
	box-shadow: var(--sh-brand);
	transform: translateY(-1px);
}
.check-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.spin {
	animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Info card ── */
.info-card {
	border-radius: var(--r-lg);
	border: 2px solid var(--c-border);
	overflow: hidden;
}
.info-card--avail { border-color: var(--c-ok-500); }
.info-card--unavail { border-color: var(--c-err-500); }

.info-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: var(--sp-4) var(--sp-5);
	background: var(--c-bg-soft);
	border-bottom: 1px solid var(--c-border);
}
.avail-badge {
	padding: var(--sp-1) var(--sp-4);
	border-radius: var(--r-pill);
	font-size: var(--fs-sm);
	font-weight: 800;
}
.avail-badge--ok { background: var(--c-ok-50); color: #065f46; }
.avail-badge--no { background: var(--c-err-50); color: var(--c-err-600); }
.rate-type { font-size: var(--fs-sm); color: var(--c-muted); font-weight: 600; }

.fee-display {
	display: flex;
	align-items: baseline;
	justify-content: space-between;
	padding: var(--sp-5) var(--sp-5) var(--sp-4);
}
.fee-label {
	font-size: var(--fs-sm);
	color: var(--c-text-soft);
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.3px;
}
.fee-amount {
	font-size: var(--fs-3xl);
	font-weight: 800;
	color: var(--c-brand-900);
	letter-spacing: -0.5px;
}
.fee-currency {
	font-size: var(--fs-lg);
	font-weight: 600;
	color: var(--c-muted);
	margin-right: var(--sp-1);
}

.apply-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: var(--sp-2);
	width: calc(100% - var(--sp-10));
	margin: 0 var(--sp-5) var(--sp-5);
	padding: var(--sp-4);
	background: #065f46;
	color: #fff;
	border: none;
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	font-weight: 800;
	cursor: pointer;
	transition: background var(--t-norm), transform var(--t-fast);
}
.apply-btn:hover { background: #064e3b; transform: translateY(-1px); }

/* ── Toast ── */
.toast {
	position: fixed;
	bottom: var(--sp-8);
	left: 50%;
	transform: translateX(-50%);
	display: flex;
	align-items: center;
	gap: var(--sp-4);
	padding: var(--sp-4) var(--sp-6);
	border-radius: var(--r-xl);
	box-shadow: var(--sh-xl);
	min-width: 280px;
	max-width: 480px;
	z-index: 200;
}
.toast--ok { background: #065f46; color: #fff; }
.toast--err { background: var(--c-err-600); color: #fff; }
.toast-icon {
	width: 28px;
	height: 28px;
	border-radius: var(--r-pill);
	background: rgba(255,255,255,0.2);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: var(--fs-md);
	font-weight: 800;
	flex-shrink: 0;
}
.toast-msg { font-size: var(--fs-md); font-weight: 700; }
.toast-meta { font-size: var(--fs-xs); opacity: 0.75; margin-top: 2px; }

.toast-enter-active, .toast-leave-active {
	transition: opacity var(--t-norm) var(--ease-out), transform var(--t-norm) var(--ease-out);
}
.toast-enter-from { opacity: 0; transform: translateX(-50%) translateY(16px); }
.toast-leave-to   { opacity: 0; transform: translateX(-50%) translateY(16px); }

.fade-up-enter-active { transition: opacity var(--t-slow) var(--ease-out), transform var(--t-slow) var(--ease-out); }
.fade-up-enter-from   { opacity: 0; transform: translateY(12px); }

@media (max-width: 480px) {
	.form-card { padding: var(--sp-5); }
	.time-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
