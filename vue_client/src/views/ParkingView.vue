<template>
	<div class="pk">
		<h2>🚗 주차 차량 등록</h2>
		<div class="form">
			<label>예약번호
				<select v-model="rsvNo" @change="loadList">
					<option value="R2026041300001">R2026041300001 / 1205 / HONG GILDONG</option>
					<option value="R2026041300002">R2026041300002 / 0807 / JOHN SMITH</option>
				</select>
			</label>
			<label>차량번호
				<input v-model="carNo" type="text" placeholder="예: 12가 3456" maxlength="20" />
			</label>
			<label>차종
				<select v-model="carTp">
					<option value="SEDAN">세단</option>
					<option value="SUV">SUV</option>
					<option value="FOREIGN">외제차</option>
					<option value="ETC">기타</option>
				</select>
			</label>
			<label>메모
				<textarea v-model="reqMemo" rows="2" placeholder="색상이나 특이사항 (선택)" />
			</label>
			<button class="submit" @click="submit">등록하기</button>
			<div v-if="toast" class="toast" :class="{ ok: toast.ok }">{{ toast.msg }}</div>
		</div>

		<h3 class="list-h">등록된 차량</h3>
		<div v-if="list.length === 0" class="empty">등록된 차량이 없습니다</div>
		<div v-else class="cards">
			<div v-for="row in list" :key="row.reqNo" class="card">
				<div class="car-no">{{ row.carNo }}</div>
				<div class="meta">
					<span class="badge">{{ carTpLabel(row.carTp) }}</span>
					<span class="stat">{{ row.procStatNm }}</span>
					<span class="time">{{ fmtTime(row.reqDt, row.reqTm) }}</span>
				</div>
				<div v-if="row.reqMemo" class="memo">{{ row.reqMemo }}</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { fetchParkingList, requestParking } from '../api/client';

const rsvNo = ref('R2026041300001');
const carNo = ref('');
const carTp = ref('SEDAN');
const reqMemo = ref('');
const list = ref([]);
const toast = ref(null);

const roomNoMap = {
	'R2026041300001': '1205',
	'R2026041300002': '0807'
};

const CAR_TP_LABELS = { SEDAN: '세단', SUV: 'SUV', FOREIGN: '외제차', ETC: '기타' };
function carTpLabel(cd) { return CAR_TP_LABELS[cd] || cd || '-'; }

function fmtTime(d, t) {
	if (!d || !t) return '';
	return `${d.slice(0,4)}-${d.slice(4,6)}-${d.slice(6,8)} ${t.slice(0,2)}:${t.slice(2,4)}`;
}

async function loadList() {
	try {
		const r = await fetchParkingList(rsvNo.value);
		list.value = r.map?.list || [];
	} catch (e) {
		list.value = [];
	}
}

function showToast(msg, ok) {
	toast.value = { msg, ok };
	setTimeout(() => { toast.value = null; }, 3500);
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
			roomNo: roomNoMap[rsvNo.value],
			carNo: trimmed,
			carTp: carTp.value,
			reqMemo: reqMemo.value
		});
		if (res.resCd === '0000') {
			showToast('차량 등록 완료 — 프론트데스크에 전달되었습니다', true);
			carNo.value = '';
			reqMemo.value = '';
			await loadList();
		} else {
			showToast(`[${res.resCd}] ${res.resMsg}`, false);
		}
	} catch (err) {
		showToast(`[${err.resCd}] ${err.resMsg}`, false);
	}
}

onMounted(loadList);
</script>

<style scoped>
.pk h2 { margin-bottom: 20px; }
.form { background: #fff; padding: 24px; border-radius: 12px; max-width: 640px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.form label { display: block; margin-bottom: 16px; font-weight: 600; font-size: 14px; color: #4a5568; }
.form select, .form input[type=text], .form textarea {
	width: 100%; padding: 10px; border: 1px solid #cbd5e0; border-radius: 6px;
	font-size: 16px; margin-top: 6px; box-sizing: border-box;
}
.submit {
	width: 100%; padding: 14px; background: #1a3a6e; color: #fff;
	border: none; border-radius: 8px; font-size: 16px; font-weight: 700;
}
.toast { margin-top: 16px; padding: 12px; border-radius: 6px; background: #fff5f5; color: #c53030; }
.toast.ok { background: #f0fff4; color: #2f855a; }

.list-h { margin: 28px 0 12px; font-size: 16px; color: #2d3748; }
.empty { background: #edf2f7; color: #718096; padding: 24px; border-radius: 12px; text-align: center; max-width: 640px; }
.cards { display: grid; gap: 12px; max-width: 640px; }
.card { background: #fff; padding: 16px 20px; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.car-no { font-family: 'Menlo', 'Consolas', monospace; font-size: 22px; font-weight: 700; color: #1a3a6e; letter-spacing: 1px; }
.meta { display: flex; gap: 10px; align-items: center; margin-top: 8px; flex-wrap: wrap; }
.badge { background: #edf2f7; color: #2d3748; padding: 3px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; }
.stat { color: #2f855a; font-size: 12px; font-weight: 700; }
.time { color: #a0aec0; font-size: 12px; }
.memo { margin-top: 8px; font-size: 13px; color: #4a5568; }
</style>
