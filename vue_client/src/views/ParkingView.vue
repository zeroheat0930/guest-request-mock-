<template>
	<div class="pk">
		<div class="page-header">
			<div class="page-header__icon">
				<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<rect x="1" y="3" width="15" height="13" rx="2"/>
					<path d="M16 8h4l3 3v5h-7V8z"/>
					<circle cx="5.5" cy="18.5" r="2.5"/>
					<circle cx="18.5" cy="18.5" r="2.5"/>
				</svg>
			</div>
			<div>
				<h2 class="page-title">주차 차량 등록</h2>
				<p class="page-sub">Vehicle Registration</p>
			</div>
		</div>

		<div class="form-card">
			<div class="guest-info">
				<span class="guest-room">{{ roomNo }}호 고객님</span>
				
			</div>

			<div class="form-row">
				<div class="form-group" style="flex: 1">
					<label class="field-label">차량번호</label>
					<input v-model="carNo" type="text" class="field-input" placeholder="예: 12가 3456" maxlength="20" />
				</div>
				<div class="form-group" style="flex: 0 0 160px">
					<label class="field-label">차종</label>
					<div class="select-wrap">
						<select v-model="carTp" class="field-input">
							<option value="SEDAN">세단</option>
							<option value="SUV">SUV</option>
							<option value="FOREIGN">외제차</option>
							<option value="ETC">기타</option>
						</select>
						<span class="select-arrow">
							<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
						</span>
					</div>
				</div>
			</div>

			<div class="form-group">
				<label class="field-label">메모 <span class="optional">선택사항</span></label>
				<textarea v-model="reqMemo" class="field-input" rows="2" placeholder="색상이나 특이사항을 입력하세요" />
			</div>

			<button class="submit-btn" @click="submit">
				<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.64 3.35 2 2 0 0 1 3.61 1h3a2 2 0 0 1 2 1.72c.127.96.36 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.91 8.6a16 16 0 0 0 6 6l.96-.96a2 2 0 0 1 2.11-.45c.907.34 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
				차량 등록하기
			</button>

			<Transition name="toast">
				<div v-if="toast" class="inline-toast" :class="toast.ok ? 'inline-toast--ok' : 'inline-toast--err'">
					<span class="toast-icon">{{ toast.ok ? '✓' : '!' }}</span>
					<span>{{ toast.msg }}</span>
				</div>
			</Transition>
		</div>

		<div class="list-section">
			<div class="list-header">
				<h3 class="list-title">등록된 차량</h3>
				<span v-if="!loadingList && list.length > 0" class="list-count">{{ list.length }}대</span>
			</div>

			<LoadingSpinner v-if="loadingList" text="차량 목록 불러오는 중..." />

			<div v-else-if="list.length === 0" class="empty-state">
				<div class="empty-icon">🚗</div>
				<div class="empty-title">등록된 차량이 없습니다</div>
				<div class="empty-sub">위 양식으로 차량을 등록해 주세요</div>
			</div>

			<div v-else class="cards">
				<div v-for="row in list" :key="row.reqNo" class="vehicle-card">
					<div class="vehicle-icon">🚗</div>
					<div class="vehicle-body">
						<div class="vehicle-no">{{ row.carNo }}</div>
						<div class="vehicle-meta">
							<span class="badge">{{ carTpLabel(row.carTp) }}</span>
							<span class="stat-badge">{{ row.procStatNm }}</span>
							<span class="time">{{ fmtTime(row.reqDt, row.reqTm) }}</span>
						</div>
						<div v-if="row.reqMemo" class="vehicle-memo">{{ row.reqMemo }}</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { fetchParkingList, requestParking } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner.vue';

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
		showToast('차량번호를 입력하세요', false);
		return;
	}
	if (trimmed.length < 4 || trimmed.length > 20) {
		showToast('차량번호 형식 오류 (4~20자)', false);
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
			showToast('차량 등록 완료 — 프론트데스크에 전달되었습니다', true);
			carNo.value = '';
			reqMemo.value = '';
			await loadList();
		} else {
			showToast(res.message, false);
		}
	} catch (err) {
		showToast(err.message || '요청 처리 중 오류가 발생했습니다', false);
	}
}

onMounted(loadList);
</script>

<style scoped>
.pk { max-width: 640px; }

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
	gap: var(--sp-5);
	margin-bottom: var(--sp-8);
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

.form-row { display: flex; gap: var(--sp-4); }

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
	text-transform: none;
	margin-left: var(--sp-2);
}

.select-wrap { position: relative; }
.select-wrap select,
.field-input {
	width: 100%;
	padding: var(--sp-3) var(--sp-4);
	border: 1.5px solid var(--c-border);
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	background: var(--c-bg-soft);
	color: var(--c-text);
	appearance: none;
	-webkit-appearance: none;
	transition: border-color var(--t-fast), box-shadow var(--t-fast), background var(--t-fast);
	box-sizing: border-box;
}
.select-wrap select { padding-right: var(--sp-10); cursor: pointer; }
.select-wrap select:focus,
.field-input:focus {
	outline: none;
	border-color: var(--c-brand-500);
	box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
	background: var(--c-surface);
}
.field-input::placeholder { color: var(--c-muted); }
textarea.field-input { resize: vertical; }

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
	gap: var(--sp-2);
	width: 100%;
	padding: var(--sp-4) var(--sp-6);
	background: var(--c-brand-700);
	color: #fff;
	border: none;
	border-radius: var(--r-md);
	font-size: var(--fs-lg);
	font-weight: 800;
	cursor: pointer;
	transition: background var(--t-norm) var(--ease-out), box-shadow var(--t-norm) var(--ease-out), transform var(--t-fast);
}
.submit-btn:hover {
	background: var(--c-brand-900);
	box-shadow: var(--sh-brand);
	transform: translateY(-1px);
}

/* ── Inline toast ── */
.inline-toast {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: var(--sp-3) var(--sp-4);
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	font-weight: 600;
}
.inline-toast--ok { background: var(--c-ok-50); color: #065f46; }
.inline-toast--err { background: var(--c-err-50); color: var(--c-err-600); }
.toast-icon {
	width: 22px;
	height: 22px;
	border-radius: var(--r-pill);
	background: currentColor;
	color: #fff;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: var(--fs-xs);
	font-weight: 900;
	flex-shrink: 0;
}
.inline-toast--ok .toast-icon { background: #065f46; }
.inline-toast--err .toast-icon { background: var(--c-err-600); }

.toast-enter-active, .toast-leave-active {
	transition: opacity var(--t-norm) var(--ease-out), transform var(--t-norm) var(--ease-out);
}
.toast-enter-from { opacity: 0; transform: translateY(8px); }
.toast-leave-to   { opacity: 0; transform: translateY(8px); }

/* ── List section ── */
.list-section { display: flex; flex-direction: column; gap: var(--sp-4); }
.list-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
}
.list-title {
	font-size: var(--fs-lg);
	font-weight: 800;
	color: var(--c-brand-900);
}
.list-count {
	background: var(--c-brand-50);
	color: var(--c-brand-700);
	font-size: var(--fs-sm);
	font-weight: 700;
	padding: 2px var(--sp-3);
	border-radius: var(--r-pill);
}

/* ── Empty state ── */
.empty-state {
	background: var(--c-surface);
	border: 1.5px dashed var(--c-border);
	border-radius: var(--r-xl);
	padding: var(--sp-10) var(--sp-8);
	text-align: center;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: var(--sp-2);
}
.empty-icon { font-size: 48px; line-height: 1; margin-bottom: var(--sp-2); }
.empty-title { font-size: var(--fs-lg); font-weight: 700; color: var(--c-text-soft); }
.empty-sub { font-size: var(--fs-sm); color: var(--c-muted); }

/* ── Vehicle cards ── */
.cards { display: flex; flex-direction: column; gap: var(--sp-3); }
.vehicle-card {
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-radius: var(--r-lg);
	padding: var(--sp-4) var(--sp-5);
	box-shadow: var(--sh-sm);
	display: flex;
	align-items: flex-start;
	gap: var(--sp-4);
	transition: box-shadow var(--t-norm) var(--ease-out), transform var(--t-fast);
}
.vehicle-card:hover { box-shadow: var(--sh-md); transform: translateY(-1px); }

.vehicle-icon {
	font-size: 28px;
	line-height: 1;
	background: var(--c-brand-50);
	width: 48px;
	height: 48px;
	border-radius: var(--r-md);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.vehicle-body { flex: 1; min-width: 0; }
.vehicle-no {
	font-family: 'Menlo', 'Consolas', monospace;
	font-size: var(--fs-xl);
	font-weight: 700;
	color: var(--c-brand-900);
	letter-spacing: 1px;
	margin-bottom: var(--sp-2);
}
.vehicle-meta {
	display: flex;
	gap: var(--sp-2);
	align-items: center;
	flex-wrap: wrap;
}
.badge {
	background: var(--c-bg-soft);
	color: var(--c-text-soft);
	border: 1px solid var(--c-border);
	padding: 2px var(--sp-3);
	border-radius: var(--r-pill);
	font-size: var(--fs-xs);
	font-weight: 700;
}
.stat-badge {
	background: var(--c-ok-50);
	color: #065f46;
	padding: 2px var(--sp-3);
	border-radius: var(--r-pill);
	font-size: var(--fs-xs);
	font-weight: 700;
}
.time { font-size: var(--fs-xs); color: var(--c-muted); }
.vehicle-memo {
	margin-top: var(--sp-2);
	font-size: var(--fs-sm);
	color: var(--c-text-soft);
}

@media (max-width: 480px) {
	.form-card { padding: var(--sp-5); }
	.form-row { flex-direction: column; }
}
</style>
