<template>
	<div class="dashboard">
		<header class="head">
			<h2>📊 프론트 데스크 대시보드</h2>
			<div class="ctrl">
				<label>예약
					<select v-model="rsvNo">
						<option value="">전체</option>
						<option v-for="r in rsvList" :key="r.rsvNo" :value="r.rsvNo">
							{{ r.roomNo }} · {{ r.perNm }}
						</option>
					</select>
				</label>
				<label>폴링
					<select v-model.number="intervalSec">
						<option :value="3">3초</option>
						<option :value="5">5초</option>
						<option :value="10">10초</option>
						<option :value="0">중지</option>
					</select>
				</label>
				<span class="last">마지막 갱신: {{ lastUpdated || '-' }}</span>
			</div>
		</header>

		<section class="card">
			<h3>🛎️ 어메니티 요청 ({{ amenityList.length }})</h3>
			<div v-if="amenityList.length === 0" class="empty">아직 요청이 없습니다</div>
			<table v-else>
				<thead>
					<tr><th>요청번호</th><th>객실</th><th>품목</th><th>메모</th><th>접수</th></tr>
				</thead>
				<tbody>
					<tr v-for="r in amenityList" :key="r.reqNo" :class="{ fresh: isFresh(r) }">
						<td>{{ r.reqNo }}</td>
						<td>{{ r.roomNo }}</td>
						<td>{{ formatItems(r.itemList) }}</td>
						<td class="memo">{{ r.reqMemo || '-' }}</td>
						<td>{{ r.reqDt }} {{ r.reqTm }}</td>
					</tr>
				</tbody>
			</table>
		</section>
	</div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { fetchAmenityList, fetchReservationList } from '../api/client.js';

const rsvNo = ref('');
const rsvList = ref([]);
const amenityList = ref([]);
const intervalSec = ref(5);
const lastUpdated = ref('');
let timer = null;
const seenReqNos = new Set();

async function poll() {
	try {
		const res = await fetchAmenityList(rsvNo.value);
		const list = res.map?.list || [];
		// 신규 요청 표시: 처음 폴링 후에만 fresh 깜빡임
		list.forEach(r => {
			if (!seenReqNos.has(r.reqNo)) {
				r._fresh = seenReqNos.size > 0; // 첫 폴링은 fresh 아님
				seenReqNos.add(r.reqNo);
			}
		});
		amenityList.value = list.slice().reverse();
		lastUpdated.value = new Date().toLocaleTimeString();
	} catch (e) {
		console.warn('[dashboard] 폴링 실패', e);
	}
}

function isFresh(r) {
	return !!r._fresh;
}

function formatItems(itemList) {
	if (!Array.isArray(itemList)) return '-';
	return itemList.map(it => `${it.itemCd}×${it.qty}`).join(', ');
}

function startTimer() {
	stopTimer();
	if (intervalSec.value > 0) {
		timer = setInterval(poll, intervalSec.value * 1000);
	}
}
function stopTimer() {
	if (timer) { clearInterval(timer); timer = null; }
}

onMounted(async () => {
	try {
		const list = await fetchReservationList();
		rsvList.value = list.map?.list || [];
	} catch (e) { /* ignore */ }
	await poll();
	startTimer();
});

onUnmounted(stopTimer);
watch(intervalSec, startTimer);
watch(rsvNo, poll);
</script>

<style scoped>
.dashboard { max-width: 980px; }
.head {
	display: flex;
	justify-content: space-between;
	align-items: center;
	flex-wrap: wrap;
	gap: 12px;
	margin-bottom: 20px;
}
.head h2 { font-size: 22px; }
.ctrl {
	display: flex;
	gap: 12px;
	align-items: center;
	flex-wrap: wrap;
	font-size: 13px;
	color: #4a5568;
}
.ctrl select {
	margin-left: 6px;
	padding: 4px 8px;
	border: 1px solid #cbd5e0;
	border-radius: 6px;
}
.last { color: #8492a6; font-size: 12px; }

.card {
	background: #fff;
	padding: 20px 24px;
	border-radius: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.card h3 { margin-bottom: 14px; font-size: 16px; color: #1a3a6e; }
.empty { color: #a0aec0; padding: 20px 0; text-align: center; }

table { width: 100%; border-collapse: collapse; font-size: 13px; }
thead { background: #f7fafc; }
th, td { text-align: left; padding: 10px 8px; border-bottom: 1px solid #edf2f7; }
th { font-weight: 600; color: #4a5568; font-size: 12px; }
td.memo { color: #718096; max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
tr.fresh td { background: #fefce8; animation: flash 1.5s ease-out; }
@keyframes flash {
	from { background: #fde68a; }
	to   { background: #fefce8; }
}
</style>
