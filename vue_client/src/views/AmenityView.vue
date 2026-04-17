<template>
	<div class="amenity">
		<div class="page-header">
			<h2 class="page-title">{{ t('amenity.title') }}</h2>
			<p class="page-sub">{{ t('amenity.sub') }}</p>
		</div>

		<div class="guest-bar">
			<span class="guest-bar__room">{{ t('guest.room.label', roomNo) }}</span>
		</div>

		<div class="form-card">
			<LoadingSpinner v-if="loading" :text="t('amenity.loading')" />
			<div v-else class="items">
				<div class="items-header">
					<span class="items-label">{{ t('amenity.select') }}</span>
					<span class="items-hint">{{ t('amenity.hint') }}</span>
				</div>
				<div v-for="item in items" :key="item.itemCd" class="item-card" :class="{ 'item-card--selected': qtyMap[item.itemCd] > 0 }">
					<div class="item-info">
						<span class="item-name">{{ getLang() !== 'ko' && item.itemNmEng ? item.itemNmEng : item.itemNm }}</span>
						<span class="item-name-en">{{ getLang() === 'ko' ? item.itemNmEng : item.itemNm }}</span>
					</div>
					<div class="item-controls">
						<span class="item-max">max {{ item.maxQty }}</span>
						<div class="qty-wrap">
							<button class="qty-btn" @click="decQty(item)" :disabled="qtyMap[item.itemCd] <= 0" aria-label="감소">
								<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/></svg>
							</button>
							<span class="qty-val">{{ qtyMap[item.itemCd] }}</span>
							<button class="qty-btn" @click="incQty(item)" :disabled="qtyMap[item.itemCd] >= item.maxQty" aria-label="증가">
								<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
							</button>
						</div>
					</div>
				</div>
			</div>

			<div class="form-group">
				<label class="field-label">{{ t('amenity.memo') }}</label>
				<textarea v-model="reqMemo" rows="3" :placeholder="t('amenity.memo')" />
			</div>

			<button class="submit-btn" @click="submit">
				{{ t('amenity.submit') }}
			</button>
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
import { ref, onMounted, reactive } from 'vue';
import { fetchAmenityItems, requestAmenity } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import { t, getLang } from '../i18n/ui.js';

const rsvNo = ref(sessionStorage.getItem('concierge.rsvNo') || '');
const roomNo = ref(sessionStorage.getItem('concierge.roomNo') || '');
const guestName = ref(sessionStorage.getItem('concierge.guestName') || '');
const reqMemo = ref('');
const items = ref([]);
const qtyMap = reactive({});
const result = ref(null);
const loading = ref(false);

onMounted(async () => {
	loading.value = true;
	try {
		const res = await fetchAmenityItems();
		items.value = res.list || [];
		items.value.forEach(it => { qtyMap[it.itemCd] = 0; });
	} finally {
		loading.value = false;
	}
});

function incQty(item) {
	if (qtyMap[item.itemCd] < item.maxQty) qtyMap[item.itemCd]++;
}
function decQty(item) {
	if (qtyMap[item.itemCd] > 0) qtyMap[item.itemCd]--;
}

function showResult(data) {
	result.value = data;
	setTimeout(() => { result.value = null; }, 3500);
}

async function submit() {
	const itemList = items.value
		.filter(it => qtyMap[it.itemCd] > 0)
		.map(it => ({ itemCd: it.itemCd, qty: qtyMap[it.itemCd] }));

	if (itemList.length === 0) {
		showResult({ status: 401, message: t('amenity.noitem'), map: {} });
		return;
	}

	try {
		const res = await requestAmenity({
			rsvNo: rsvNo.value,
			roomNo: roomNo.value,
			itemList,
			reqMemo: reqMemo.value
		});
		showResult(res);
	} catch (err) {
		showResult(err);
	}
}
</script>

<style scoped>
.amenity {
	max-width: 640px;
}

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
	gap: var(--sp-3);
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
	letter-spacing: 0.2px;
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

/* ── Field ── */
.form-group { display: flex; flex-direction: column; gap: var(--sp-2); }
.field-label {
	font-size: 11px;
	font-weight: 600;
	color: var(--c-text-soft);
	letter-spacing: 1.5px;
	text-transform: uppercase;
}

textarea {
	width: 100%;
	padding: 14px var(--sp-4);
	border: 1px solid var(--c-border);
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	background: var(--c-cream);
	color: var(--c-text);
	resize: vertical;
	transition: border-color var(--t-fast) var(--ease-out), box-shadow var(--t-fast) var(--ease-out), background var(--t-fast);
	box-sizing: border-box;
	line-height: 1.6;
	font-family: inherit;
}
textarea:focus {
	outline: none;
	border-color: var(--c-gold);
	box-shadow: 0 0 0 3px rgba(201, 169, 110, 0.12);
	background: var(--c-surface);
}
textarea::placeholder { color: var(--c-muted); }

/* ── Items ── */
.items-header {
	display: flex;
	justify-content: space-between;
	align-items: baseline;
	margin-bottom: var(--sp-4);
}
.items-label {
	font-size: 11px;
	font-weight: 600;
	color: var(--c-text-soft);
	letter-spacing: 1.5px;
	text-transform: uppercase;
}
.items-hint { font-size: var(--fs-xs); color: var(--c-muted); }

.item-card {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: var(--sp-4);
	padding: var(--sp-5) var(--sp-5);
	border: 1px solid var(--c-border);
	border-bottom: 2px solid rgba(201, 169, 110, 0.1);
	border-radius: var(--r-md);
	background: var(--c-surface);
	margin-bottom: var(--sp-3);
	transition: border-color var(--t-norm) var(--ease-out), background var(--t-norm) var(--ease-out), box-shadow var(--t-norm) var(--ease-out);
}
.item-card:last-child { margin-bottom: 0; }
.item-card--selected {
	border-color: var(--c-gold);
	border-bottom-color: var(--c-gold-deep);
	background: var(--c-gold-pale);
	box-shadow: var(--sh-sm);
}
.item-info { display: flex; flex-direction: column; gap: 3px; }
.item-name {
	font-size: var(--fs-lg);
	font-weight: 600;
	color: var(--c-text);
}
.item-name-en { font-size: var(--fs-xs); color: var(--c-muted); letter-spacing: 0.3px; }

.item-controls { display: flex; align-items: center; gap: var(--sp-4); flex-shrink: 0; }
.item-max { font-size: var(--fs-xs); color: var(--c-muted); }

.qty-wrap {
	display: flex;
	align-items: center;
	gap: var(--sp-2);
}
.qty-btn {
	width: 38px;
	height: 38px;
	border: 1px solid var(--c-border-gold);
	background: var(--c-surface);
	color: var(--c-text-soft);
	border-radius: var(--r-pill);
	display: flex;
	align-items: center;
	justify-content: center;
	cursor: pointer;
	transition: background var(--t-fast), border-color var(--t-fast), color var(--t-fast);
	flex-shrink: 0;
}
.qty-btn:not(:disabled):hover {
	background: var(--c-gold);
	border-color: var(--c-gold);
	color: var(--c-midnight);
}
.qty-btn:not(:disabled):active {
	background: var(--c-gold-deep);
	border-color: var(--c-gold-deep);
}
.qty-btn:disabled { opacity: 0.28; cursor: not-allowed; }
.qty-val {
	min-width: 30px;
	text-align: center;
	font-size: var(--fs-lg);
	font-weight: 700;
	color: var(--c-text);
}

/* ── Submit ── */
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
	transition: opacity var(--t-norm) var(--ease-out), box-shadow var(--t-norm) var(--ease-out), transform var(--t-fast) var(--ease-out);
	letter-spacing: 0.8px;
	text-transform: uppercase;
}
.submit-btn:hover {
	opacity: 0.9;
	box-shadow: var(--sh-gold);
	transform: translateY(-1px);
}
.submit-btn:active { transform: translateY(0); box-shadow: none; }

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

@media (max-width: 480px) {
	.form-card { padding: var(--sp-5); }
	.item-card { flex-direction: column; align-items: flex-start; gap: var(--sp-3); }
	.item-controls { width: 100%; justify-content: space-between; }
}
</style>
