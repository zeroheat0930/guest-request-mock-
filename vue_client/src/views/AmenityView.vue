<template>
	<div class="amenity">
		<div class="page-header">
			<div class="page-header__icon">
				<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M18 8h1a4 4 0 0 1 0 8h-1"/>
					<path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"/>
					<line x1="6" y1="1" x2="6" y2="4"/>
					<line x1="10" y1="1" x2="10" y2="4"/>
					<line x1="14" y1="1" x2="14" y2="4"/>
				</svg>
			</div>
			<div>
				<h2 class="page-title">어메니티 요청</h2>
				<p class="page-sub">Amenity Request</p>
			</div>
		</div>

		<div class="form-card">
			<div class="form-group">
				<label class="field-label">예약 정보</label>
				<div class="select-wrap">
					<select v-model="rsvNo">
						<option value="R2026041300001">R2026041300001 · 1205호 · HONG GILDONG</option>
						<option value="R2026041300002">R2026041300002 · 0807호 · JOHN SMITH</option>
					</select>
					<span class="select-arrow">
						<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
					</span>
				</div>
			</div>

			<LoadingSpinner v-if="loading" text="품목 불러오는 중..." />
			<div v-else class="items">
				<div class="items-header">
					<span class="items-label">품목 선택</span>
					<span class="items-hint">원하는 수량을 입력하세요</span>
				</div>
				<div v-for="item in items" :key="item.itemCd" class="item-card" :class="{ 'item-card--selected': qtyMap[item.itemCd] > 0 }">
					<div class="item-info">
						<span class="item-name">{{ item.itemNm }}</span>
						<span class="item-name-en">{{ item.itemNmEng }}</span>
					</div>
					<div class="item-controls">
						<span class="item-max">최대 {{ item.maxQty }}개</span>
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
				<label class="field-label">요청 메모 <span class="optional">선택사항</span></label>
				<textarea v-model="reqMemo" rows="3" placeholder="예: 밤 10시 이후 가져다 주세요" />
			</div>

			<button class="submit-btn" @click="submit">
				<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
				</svg>
				요청 등록하기
			</button>
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
import { ref, onMounted, reactive } from 'vue';
import { fetchAmenityItems, requestAmenity } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner.vue';

const rsvNo = ref('R2026041300001');
const reqMemo = ref('');
const items = ref([]);
const qtyMap = reactive({});
const result = ref(null);
const loading = ref(false);

const roomNoMap = {
	'R2026041300001': '1205',
	'R2026041300002': '0807'
};

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
		showResult({ status: 401, message: '품목 수량을 입력하세요', map: {} });
		return;
	}

	try {
		const res = await requestAmenity({
			rsvNo: rsvNo.value,
			roomNo: roomNoMap[rsvNo.value],
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
.optional {
	font-size: var(--fs-xs);
	font-weight: 400;
	color: var(--c-muted);
	margin-left: var(--sp-2);
	text-transform: none;
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
	transition: border-color var(--t-fast) var(--ease-out), box-shadow var(--t-fast) var(--ease-out);
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

textarea {
	width: 100%;
	padding: var(--sp-3) var(--sp-4);
	border: 1.5px solid var(--c-border);
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	background: var(--c-bg-soft);
	color: var(--c-text);
	resize: vertical;
	transition: border-color var(--t-fast) var(--ease-out), box-shadow var(--t-fast) var(--ease-out);
}
textarea:focus {
	outline: none;
	border-color: var(--c-brand-500);
	box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
	background: var(--c-surface);
}
textarea::placeholder { color: var(--c-muted); }

/* ── Items ── */
.items-header {
	display: flex;
	justify-content: space-between;
	align-items: baseline;
	margin-bottom: var(--sp-3);
}
.items-label {
	font-size: var(--fs-sm);
	font-weight: 700;
	color: var(--c-text-soft);
	letter-spacing: 0.3px;
	text-transform: uppercase;
}
.items-hint { font-size: var(--fs-xs); color: var(--c-muted); }

.item-card {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: var(--sp-4);
	padding: var(--sp-4) var(--sp-5);
	border: 1.5px solid var(--c-border);
	border-radius: var(--r-md);
	background: var(--c-bg-soft);
	margin-bottom: var(--sp-2);
	transition: border-color var(--t-norm) var(--ease-out), background var(--t-norm) var(--ease-out), box-shadow var(--t-norm) var(--ease-out);
}
.item-card:last-child { margin-bottom: 0; }
.item-card--selected {
	border-color: var(--c-brand-400);
	background: var(--c-brand-50);
	box-shadow: var(--sh-sm);
}
.item-info { display: flex; flex-direction: column; gap: 2px; }
.item-name { font-size: var(--fs-md); font-weight: 700; color: var(--c-text); }
.item-name-en { font-size: var(--fs-xs); color: var(--c-muted); }

.item-controls { display: flex; align-items: center; gap: var(--sp-4); flex-shrink: 0; }
.item-max { font-size: var(--fs-xs); color: var(--c-muted); }
.qty-wrap {
	display: flex;
	align-items: center;
	gap: var(--sp-2);
	background: var(--c-surface);
	border: 1.5px solid var(--c-border);
	border-radius: var(--r-pill);
	padding: 4px;
}
.qty-btn {
	width: 30px;
	height: 30px;
	border: none;
	background: transparent;
	color: var(--c-text-soft);
	border-radius: var(--r-pill);
	display: flex;
	align-items: center;
	justify-content: center;
	transition: background var(--t-fast), color var(--t-fast);
}
.qty-btn:not(:disabled):hover {
	background: var(--c-brand-700);
	color: #fff;
}
.qty-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.qty-val {
	min-width: 28px;
	text-align: center;
	font-size: var(--fs-md);
	font-weight: 700;
	color: var(--c-text);
}

/* ── Submit ── */
.submit-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: var(--sp-2);
	width: 100%;
	padding: var(--sp-4) var(--sp-6);
	background: var(--c-brand-700);
	color: #fff;
	border: none;
	border-radius: var(--r-md);
	font-size: var(--fs-lg);
	font-weight: 800;
	letter-spacing: 0.2px;
	cursor: pointer;
	transition: background var(--t-norm) var(--ease-out), box-shadow var(--t-norm) var(--ease-out), transform var(--t-fast) var(--ease-out);
}
.submit-btn:hover {
	background: var(--c-brand-900);
	box-shadow: var(--sh-brand);
	transform: translateY(-1px);
}
.submit-btn:active { transform: translateY(0); }

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

@media (max-width: 480px) {
	.form-card { padding: var(--sp-5); }
	.item-card { flex-direction: column; align-items: flex-start; }
	.item-controls { width: 100%; justify-content: space-between; }
}
</style>
