<template>
	<div class="lc">
		<div class="page-header">
			<h2 class="page-title">{{ t('late.title') }}</h2>
			<p class="page-sub">Late Checkout</p>
		</div>

		<!-- Step indicator -->
		<div class="steps">
			<div class="step" :class="{ 'step--active': true, 'step--done': !!info }">
				<div class="step-num">
					<svg v-if="!!info" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
					<span v-else>1</span>
				</div>
				<span class="step-label">{{ t('late.step1') }}</span>
			</div>
			<div class="step-line" :class="{ 'step-line--done': !!info }" />
			<div class="step" :class="{ 'step--active': !!info, 'step--done': !!result && result.status === 0 }">
				<div class="step-num">
					<svg v-if="!!result && result.status === 0" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
					<span v-else>2</span>
				</div>
				<span class="step-label">{{ t('late.step2') }}</span>
			</div>
			<div class="step-line" :class="{ 'step-line--done': !!result && result.status === 0 }" />
			<div class="step" :class="{ 'step--active': !!result && result.status === 0 }">
				<div class="step-num">3</div>
				<span class="step-label">{{ t('late.step3') }}</span>
			</div>
		</div>

		<div class="form-card">
			<div class="guest-bar">
				<span class="guest-bar__room">{{ t('guest.room.label', roomNo) }}</span>
				<span class="guest-bar__checkout">{{ t('late.stdTime') }}: {{ stdCheckOutH.toString().padStart(2,'0') }}:{{ stdCheckOutMm }}</span>
			</div>

			<div class="form-group">
				<label class="field-label">{{ t('late.selectTime') }}</label>
				<div class="time-grid">
					<button
						v-for="opt in TIME_OPTIONS"
						:key="opt.val"
						class="time-pill"
						:class="{ 'time-pill--active': reqOutTm === opt.val }"
						@click="reqOutTm = opt.val; info = null"
					>
						{{ opt.label }}
					</button>
				</div>
			</div>

			<button class="check-btn" @click="check" :disabled="checking">
				<template v-if="checking">
					<svg class="spin" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>
					{{ t('late.checking') }}
				</template>
				<template v-else>
					{{ t('late.check') }}
				</template>
			</button>

			<Transition name="fade-up">
				<div v-if="info" class="info-card" :class="info.availYn === 'Y' ? 'info-card--avail' : 'info-card--unavail'">
					<div class="info-header">
						<span class="avail-badge" :class="info.availYn === 'Y' ? 'avail-badge--ok' : 'avail-badge--no'">
							{{ info.availYn === 'Y' ? t('late.avail') : t('late.unavail') }}
						</span>
						<span class="rate-type">{{ info.rateTpNm }}</span>
					</div>
					<div class="fee-display">
						<span class="fee-label">{{ t('late.fee') }}</span>
						<span class="fee-amount">
							<span class="fee-currency">{{ info.curCd }}</span>
							{{ Number(info.addAmt).toLocaleString() }}
						</span>
					</div>
					<button v-if="info.availYn === 'Y'" class="apply-btn" @click="apply">
						{{ t('late.submit') }}
					</button>
				</div>
			</Transition>
		</div>

		<Transition name="toast">
			<div v-if="result" class="toast" :class="result.status === 0 ? 'toast--ok' : 'toast--err'">
				<div class="toast-indicator">{{ result.status === 0 ? '✓' : '!' }}</div>
				<div>
					<div class="toast-msg">{{ result.message }}</div>
					<div v-if="result.map && result.map.reqNo" class="toast-meta">요청번호 {{ result.map.reqNo }}</div>
				</div>
			</div>
		</Transition>
	</div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { checkLateCheckout, requestLateCheckout } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import { t } from '../i18n/ui.js';

const rsvNo = ref(sessionStorage.getItem('concierge.rsvNo') || '');
const roomNo = ref(sessionStorage.getItem('concierge.roomNo') || '');
const guestName = ref(sessionStorage.getItem('concierge.guestName') || '');

const stdCheckOutRaw = sessionStorage.getItem('concierge.checkOutTime') || '1100';
const stdCheckOutH = parseInt(stdCheckOutRaw.substring(0, 2), 10);
const stdCheckOutMm = stdCheckOutRaw.length >= 4 ? stdCheckOutRaw.substring(2, 4) : '00';

const TIME_OPTIONS = Array.from({ length: 8 }, (_, i) => {
	const h = stdCheckOutH + i + 1;
	const hh = String(h).padStart(2, '0');
	return { val: `${hh}${stdCheckOutMm}`, label: `${hh}:${stdCheckOutMm}` };
});

const defaultTime = TIME_OPTIONS[1]?.val || TIME_OPTIONS[0]?.val || `${String(stdCheckOutH + 2).padStart(2, '0')}${stdCheckOutMm}`;
const reqOutTm = ref(defaultTime);
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
	margin-bottom: var(--sp-6);
}
.page-title {
	font-family: 'Georgia', 'Times New Roman', serif;
	font-size: var(--fs-2xl);
	font-weight: 400;
	color: var(--c-text);
	letter-spacing: -0.3px;
	line-height: 1.25;
	margin: 0 0 var(--sp-1) 0;
	padding-bottom: var(--sp-3);
	border-bottom: 1px solid var(--c-border-gold);
}
.page-sub {
	font-size: var(--fs-sm);
	color: var(--c-text-soft);
	margin: var(--sp-2) 0 0 0;
	letter-spacing: 0.3px;
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
	font-weight: 700;
	display: flex;
	align-items: center;
	justify-content: center;
	transition: background var(--t-norm) var(--ease-out), color var(--t-norm) var(--ease-out);
	border: 1px solid transparent;
}
.step--active .step-num {
	background: var(--c-gold-pale-md);
	color: var(--c-gold-deep);
	border-color: var(--c-gold);
}
.step--done .step-num {
	background: var(--c-gold);
	color: var(--c-midnight);
	border-color: var(--c-gold-deep);
}
.step-label {
	font-size: var(--fs-xs);
	font-weight: 600;
	color: var(--c-muted);
	white-space: nowrap;
	transition: color var(--t-norm);
	letter-spacing: 0.3px;
}
.step--active .step-label { color: var(--c-gold-deep); }
.step--done .step-label { color: var(--c-gold); }

.step-line {
	flex: 1;
	height: 1px;
	background: var(--c-border);
	margin: 0 var(--sp-2);
	margin-bottom: 18px;
	border-radius: var(--r-pill);
	transition: background var(--t-norm);
}
.step-line--done { background: var(--c-gold); }

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

/* ── Guest Bar ── */
.guest-bar {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 13px var(--sp-5);
	background: var(--c-cream);
	border: 1px solid var(--c-border-gold);
	border-left: 3px solid var(--c-gold);
	border-radius: var(--r-md);
}
.guest-bar__room {
	font-weight: 600;
	font-size: var(--fs-md);
	color: var(--c-text);
}
.guest-bar__checkout {
	font-size: var(--fs-sm);
	color: var(--c-text-soft);
}

.form-group { display: flex; flex-direction: column; gap: var(--sp-3); }
.field-label {
	font-size: 11px;
	font-weight: 600;
	color: var(--c-text-soft);
	letter-spacing: 1.5px;
	text-transform: uppercase;
}

/* ── Time pills ── */
.time-grid {
	display: grid;
	grid-template-columns: repeat(4, 1fr);
	gap: var(--sp-2);
}
.time-pill {
	padding: 14px var(--sp-2);
	border: 1px solid var(--c-border);
	border-radius: var(--r-md);
	background: var(--c-cream);
	font-size: var(--fs-md);
	font-weight: 600;
	color: var(--c-text-soft);
	cursor: pointer;
	transition: all var(--t-fast) var(--ease-out);
	text-align: center;
}
.time-pill:hover {
	border-color: var(--c-gold);
	color: var(--c-gold-deep);
	background: var(--c-gold-pale);
}
.time-pill--active {
	border-color: var(--c-gold);
	background: linear-gradient(135deg, #c9a96e, #d4b896);
	color: var(--c-midnight);
	font-weight: 700;
	box-shadow: var(--sh-gold);
}

/* ── Check button ── */
.check-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: var(--sp-2);
	width: 100%;
	height: var(--touch-lg);
	background: var(--c-midnight);
	color: var(--c-gold-light);
	border: 1px solid rgba(201, 169, 110, 0.2);
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	font-weight: 600;
	cursor: pointer;
	transition: background var(--t-norm) var(--ease-out), transform var(--t-fast), color var(--t-fast);
	letter-spacing: 0.5px;
}
.check-btn:hover:not(:disabled) {
	background: var(--c-navy);
	color: var(--c-gold);
	transform: translateY(-1px);
}
.check-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.spin {
	animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Info card ── */
.info-card {
	border-radius: var(--r-lg);
	border: 1px solid var(--c-border);
	overflow: hidden;
}
.info-card--avail { border-color: var(--c-gold); }
.info-card--unavail { border-color: var(--c-err-500); }

.info-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: var(--sp-4) var(--sp-5);
	background: var(--c-cream);
	border-bottom: 1px solid var(--c-border);
}
.avail-badge {
	padding: 4px var(--sp-4);
	border-radius: var(--r-pill);
	font-size: var(--fs-sm);
	font-weight: 700;
	letter-spacing: 0.5px;
}
.avail-badge--ok {
	background: rgba(201, 169, 110, 0.12);
	color: var(--c-gold-deep);
	border: 1px solid rgba(201, 169, 110, 0.25);
}
.avail-badge--no { background: var(--c-err-50); color: var(--c-err-600); }
.rate-type { font-size: var(--fs-sm); color: var(--c-muted); font-weight: 500; }

.fee-display {
	display: flex;
	align-items: baseline;
	justify-content: space-between;
	padding: var(--sp-6) var(--sp-5) var(--sp-5);
}
.fee-label {
	font-size: 11px;
	color: var(--c-text-soft);
	font-weight: 600;
	letter-spacing: 1.5px;
	text-transform: uppercase;
}
.fee-amount {
	font-family: 'Georgia', 'Times New Roman', serif;
	font-size: var(--fs-3xl);
	font-weight: 400;
	color: var(--c-text);
	letter-spacing: -0.5px;
}
.fee-currency {
	font-family: inherit;
	font-size: var(--fs-lg);
	font-weight: 400;
	color: var(--c-gold-deep);
	margin-right: 4px;
}

.apply-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	width: calc(100% - var(--sp-10));
	margin: 0 var(--sp-5) var(--sp-5);
	height: var(--touch-lg);
	background: linear-gradient(135deg, #c9a96e, #d4b896);
	color: var(--c-midnight);
	border: none;
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	font-weight: 700;
	cursor: pointer;
	transition: opacity var(--t-norm), transform var(--t-fast), box-shadow var(--t-norm);
	letter-spacing: 0.8px;
	text-transform: uppercase;
}
.apply-btn:hover { opacity: 0.88; transform: translateY(-1px); box-shadow: var(--sh-gold); }

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
.toast--ok { background: #1e3a2a; color: #c8e6c9; }
.toast--err { background: #3a1e1a; color: #ffcdd2; }
.toast-indicator {
	width: 28px;
	height: 28px;
	border-radius: var(--r-pill);
	background: rgba(255,255,255,0.12);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 13px;
	font-weight: 800;
	flex-shrink: 0;
}
.toast-msg { font-size: var(--fs-md); font-weight: 600; }
.toast-meta { font-size: var(--fs-xs); opacity: 0.7; margin-top: 2px; }

.toast-enter-active, .toast-leave-active {
	transition: opacity var(--t-norm) var(--ease-out), transform var(--t-norm) var(--ease-out);
}
.toast-enter-from { opacity: 0; transform: translateX(-50%) translateY(16px); }
.toast-leave-to   { opacity: 0; transform: translateX(-50%) translateY(16px); }

.fade-up-enter-active { transition: opacity var(--t-slow) var(--ease-out), transform var(--t-slow) var(--ease-out); }
.fade-up-enter-from   { opacity: 0; transform: translateY(10px); }

@media (max-width: 480px) {
	.form-card { padding: var(--sp-5); }
	.time-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
