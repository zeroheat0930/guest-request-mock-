<template>
	<div class="pk">
		<div class="page-header">
			<h2 class="page-title">{{ t('park.title') }}</h2>
			<p class="page-sub">Vehicle Registration</p>
		</div>

		<div class="guest-bar">
			<span class="guest-bar__room">{{ t('guest.room.label', roomNo) }}</span>
		</div>

		<div class="form-card">
			<div class="form-row">
				<div class="form-group" style="flex: 1">
					<label class="field-label">{{ t('park.carNo') }}</label>
					<input v-model="carNo" type="text" class="field-input" :placeholder="t('park.carNo.placeholder')" maxlength="20" />
				</div>
				<div class="form-group" style="flex: 0 0 148px">
					<label class="field-label">{{ t('park.carTp') }}</label>
					<div class="select-wrap">
						<select v-model="carTp" class="field-input">
							<option value="SEDAN">세단</option>
							<option value="SUV">SUV</option>
							<option value="FOREIGN">외제차</option>
							<option value="ETC">기타</option>
						</select>
						<span class="select-arrow">
							<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
						</span>
					</div>
				</div>
			</div>

			<div class="form-group">
				<label class="field-label">{{ t('park.memo') }} <span class="optional">{{ t('park.optional') }}</span></label>
				<textarea v-model="reqMemo" class="field-input" rows="2" :placeholder="t('park.memo.placeholder')" />
			</div>

			<button class="submit-btn" @click="submit">
				{{ t('park.submit') }}
			</button>

			<Transition name="toast">
				<div v-if="toast" class="inline-toast" :class="toast.ok ? 'inline-toast--ok' : 'inline-toast--err'">
					<svg v-if="toast.ok" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
					<svg v-else width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
					<span>{{ toast.msg }}</span>
				</div>
			</Transition>
		</div>

		<div class="list-section">
			<div class="list-header">
				<h3 class="list-title">{{ t('park.list') }}</h3>
				<span v-if="!loadingList && list.length > 0" class="list-count">{{ list.length }}</span>
			</div>

			<LoadingSpinner v-if="loadingList" :text="t('park.loading')" />

			<div v-else-if="list.length === 0" class="empty-state">
				<div class="empty-icon-wrap">
					<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
						<rect x="1" y="3" width="15" height="13" rx="2"/>
						<path d="M16 8h4l3 3v5h-7V8z"/>
						<circle cx="5.5" cy="18.5" r="2.5"/>
						<circle cx="18.5" cy="18.5" r="2.5"/>
					</svg>
				</div>
				<div class="empty-title">{{ t('park.empty') }}</div>
				<div class="empty-sub">{{ t('park.empty.sub') }}</div>
			</div>

			<div v-else class="vehicle-list">
				<div v-for="row in list" :key="row.reqNo" class="vehicle-item">
					<div class="vehicle-item__body">
						<div class="vehicle-item__no">{{ row.carNo }}</div>
						<div class="vehicle-item__meta">
							<span class="badge">{{ carTpLabel(row.carTp) }}</span>
							<span class="stat-badge">{{ row.procStatNm }}</span>
						</div>
						<div v-if="row.reqMemo" class="vehicle-item__memo">{{ row.reqMemo }}</div>
					</div>
					<div class="vehicle-item__time">{{ fmtTime(row.reqDt, row.reqTm) }}</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { fetchParkingList, requestParking } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import { t } from '../i18n/ui.js';

const rsvNo = ref(sessionStorage.getItem('concierge.rsvNo') || '');
const roomNo = ref(sessionStorage.getItem('concierge.roomNo') || '');
const guestName = ref(sessionStorage.getItem('concierge.guestName') || '');
const carNo = ref('');
const carTp = ref('SEDAN');
const reqMemo = ref('');
const list = ref([]);
const toast = ref(null);
const loadingList = ref(false);

const CAR_TP_LABELS = { SEDAN: '세단', SUV: 'SUV', FOREIGN: '외제차', ETC: '기타' };
function carTpLabel(cd) { return CAR_TP_LABELS[cd] || cd || '-'; }

function fmtTime(d, t) {
	if (!d || !t) return '';
	return `${d.slice(0,4)}-${d.slice(4,6)}-${d.slice(6,8)} ${t.slice(0,2)}:${t.slice(2,4)}`;
}

async function loadList() {
	loadingList.value = true;
	try {
		const r = await fetchParkingList(rsvNo.value);
		list.value = r.list || [];
	} catch (e) {
		list.value = [];
	} finally {
		loadingList.value = false;
	}
}

function showToast(msg, ok) {
	toast.value = { msg, ok };
	setTimeout(() => { toast.value = null; }, 3000);
}

async function submit() {
	const trimmed = (carNo.value || '').trim();
	if (!trimmed) {
		showToast(t('park.errEmpty'), false);
		return;
	}
	if (trimmed.length < 4 || trimmed.length > 20) {
		showToast(t('park.errLen'), false);
		return;
	}
	try {
		const res = await requestParking({
			rsvNo: rsvNo.value,
			roomNo: roomNo.value,
			carNo: trimmed,
			carTp: carTp.value,
			reqMemo: reqMemo.value
		});
		if (res.status === 0) {
			showToast(t('park.success'), true);
			carNo.value = '';
			reqMemo.value = '';
			await loadList();
		} else {
			showToast(res.message, false);
		}
	} catch (err) {
		showToast(err.message || t('error'), false);
	}
}

onMounted(loadList);
</script>

<style scoped>
.pk { max-width: 640px; }

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

/* ── Guest Bar ── */
.guest-bar {
	display: flex;
	align-items: center;
	padding: 13px var(--sp-5);
	background: var(--c-cream);
	border: 1px solid var(--c-border-gold);
	border-left: 3px solid var(--c-gold);
	border-radius: var(--r-md);
	margin-bottom: var(--sp-5);
}
.guest-bar__room {
	font-weight: 600;
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
	gap: var(--sp-5);
	margin-bottom: var(--sp-8);
}

.form-row { display: flex; gap: var(--sp-4); }
.form-group { display: flex; flex-direction: column; gap: var(--sp-2); }
.field-label {
	font-size: 11px;
	font-weight: 600;
	color: var(--c-text-soft);
	letter-spacing: 1.5px;
	text-transform: uppercase;
}
.optional {
	font-size: var(--fs-xs);
	font-weight: 400;
	color: var(--c-muted);
	text-transform: none;
	margin-left: 4px;
	letter-spacing: 0;
}

.select-wrap { position: relative; }
.select-wrap select,
.field-input {
	width: 100%;
	height: var(--touch-md);
	padding: 0 var(--sp-4);
	border: 1px solid var(--c-border);
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	background: var(--c-cream);
	color: var(--c-text);
	appearance: none;
	-webkit-appearance: none;
	transition: border-color var(--t-fast), box-shadow var(--t-fast), background var(--t-fast);
	box-sizing: border-box;
	font-family: inherit;
}
.select-wrap select { padding-right: var(--sp-10); cursor: pointer; }
textarea.field-input {
	height: auto;
	padding: 14px var(--sp-4);
	resize: vertical;
	line-height: 1.6;
}
.select-wrap select:focus,
.field-input:focus {
	outline: none;
	border-color: var(--c-gold);
	box-shadow: 0 0 0 3px rgba(201, 169, 110, 0.12);
	background: var(--c-surface);
}
.field-input::placeholder { color: var(--c-muted); }

.select-arrow {
	position: absolute;
	right: var(--sp-4);
	top: 50%;
	transform: translateY(-50%);
	color: var(--c-muted);
	pointer-events: none;
	display: flex;
}

.submit-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 100%;
	height: var(--touch-lg);
	background: linear-gradient(135deg, #c9a96e, #d4b896);
	color: var(--c-midnight);
	border: none;
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	font-weight: 700;
	cursor: pointer;
	transition: opacity var(--t-norm) var(--ease-out), box-shadow var(--t-norm) var(--ease-out), transform var(--t-fast);
	letter-spacing: 0.8px;
	text-transform: uppercase;
}
.submit-btn:hover {
	opacity: 0.9;
	box-shadow: var(--sh-gold);
	transform: translateY(-1px);
}

/* ── Inline toast ── */
.inline-toast {
	display: flex;
	align-items: center;
	gap: var(--sp-2);
	padding: 12px var(--sp-4);
	border-radius: var(--r-md);
	font-size: var(--fs-sm);
	font-weight: 600;
}
.inline-toast--ok {
	background: rgba(201, 169, 110, 0.08);
	color: var(--c-gold-deep);
	border: 1px solid rgba(201, 169, 110, 0.2);
}
.inline-toast--err { background: var(--c-err-50); color: var(--c-err-600); }

.toast-enter-active, .toast-leave-active {
	transition: opacity var(--t-norm) var(--ease-out), transform var(--t-norm) var(--ease-out);
}
.toast-enter-from { opacity: 0; transform: translateY(6px); }
.toast-leave-to   { opacity: 0; transform: translateY(6px); }

/* ── List section ── */
.list-section { display: flex; flex-direction: column; gap: var(--sp-4); }
.list-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
}
.list-title {
	font-family: 'Georgia', 'Times New Roman', serif;
	font-size: var(--fs-lg);
	font-weight: 400;
	color: var(--c-text);
	margin: 0;
}
.list-count {
	background: var(--c-gold-pale);
	color: var(--c-gold-deep);
	border: 1px solid var(--c-border-gold);
	font-size: var(--fs-xs);
	font-weight: 700;
	padding: 2px var(--sp-3);
	border-radius: var(--r-pill);
}

/* ── Empty state ── */
.empty-state {
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-radius: var(--r-xl);
	padding: var(--sp-10) var(--sp-8);
	text-align: center;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: var(--sp-2);
}
.empty-icon-wrap {
	width: 60px;
	height: 60px;
	border-radius: var(--r-xl);
	background: var(--c-cream);
	border: 1px solid var(--c-border-gold);
	display: flex;
	align-items: center;
	justify-content: center;
	color: var(--c-gold-light);
	margin-bottom: var(--sp-2);
}
.empty-title { font-size: var(--fs-md); font-weight: 600; color: var(--c-text-soft); }
.empty-sub { font-size: var(--fs-sm); color: var(--c-muted); }

/* ── Vehicle list ── */
.vehicle-list {
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-radius: var(--r-xl);
	overflow: hidden;
	box-shadow: var(--sh-sm);
}
.vehicle-item {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: var(--sp-4);
	padding: var(--sp-5) var(--sp-6);
	border-bottom: 1px solid var(--c-border);
	transition: background var(--t-fast);
}
.vehicle-item:last-child { border-bottom: none; }
.vehicle-item:hover { background: var(--c-cream); }
.vehicle-item__body { flex: 1; min-width: 0; }
.vehicle-item__no {
	font-size: var(--fs-xl);
	font-weight: 700;
	color: var(--c-text);
	letter-spacing: 0.5px;
	margin-bottom: var(--sp-2);
	font-variant-numeric: tabular-nums;
}
.vehicle-item__meta {
	display: flex;
	gap: var(--sp-2);
	align-items: center;
	flex-wrap: wrap;
}
.badge {
	background: var(--c-cream);
	color: var(--c-text-soft);
	border: 1px solid var(--c-border-gold);
	padding: 2px var(--sp-3);
	border-radius: var(--r-pill);
	font-size: var(--fs-xs);
	font-weight: 600;
}
.stat-badge {
	background: rgba(201, 169, 110, 0.08);
	color: var(--c-gold-deep);
	border: 1px solid rgba(201, 169, 110, 0.2);
	padding: 2px var(--sp-3);
	border-radius: var(--r-pill);
	font-size: var(--fs-xs);
	font-weight: 600;
}
.vehicle-item__memo {
	margin-top: var(--sp-2);
	font-size: var(--fs-sm);
	color: var(--c-text-soft);
}
.vehicle-item__time {
	font-size: var(--fs-xs);
	color: var(--c-muted);
	white-space: nowrap;
	flex-shrink: 0;
}

@media (max-width: 480px) {
	.form-card { padding: var(--sp-5); }
	.form-row { flex-direction: column; }
	.vehicle-item { flex-direction: column; align-items: flex-start; gap: var(--sp-2); }
}
</style>
