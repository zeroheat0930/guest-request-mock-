<template>
	<div class="hk">
		<h2>🧹 객실 정비 / DND</h2>
		<div class="form">
			<label>예약번호
				<select v-model="rsvNo" @change="loadStat">
					<option value="R2026041300001">R2026041300001 / 1205</option>
					<option value="R2026041300002">R2026041300002 / 0807</option>
				</select>
			</label>
			<LoadingSpinner v-if="loadingStatus" text="상태 확인 중..." />
			<div v-else class="cur">현재 상태: <strong>{{ curStatNm }}</strong></div>
			<div class="btns">
				<button @click="change('MU')"  class="btn btn-mu" :disabled="submitting">🧹 객실 정비 요청</button>
				<button @click="change('DND')" class="btn btn-dnd" :disabled="submitting">🚫 방해 금지</button>
				<button @click="change('CLR')" class="btn btn-clr" :disabled="submitting">✓ 해제</button>
			</div>
			<div v-if="result" class="result" :class="{ ok: result.status === 0 }">
				{{ result.message }}
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { fetchHousekeeping, updateHousekeeping } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner.vue';

const rsvNo = ref('R2026041300001');
const curStatNm = ref('-');
const result = ref(null);
const loadingStatus = ref(false);
const submitting = ref(false);

async function loadStat() {
	loadingStatus.value = true;
	try {
		const r = await fetchHousekeeping(rsvNo.value);
		curStatNm.value = r.map?.hkStatNm || '-';
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
.hk h2 { margin-bottom: 20px; }
.form { background: #fff; padding: 24px; border-radius: 12px; max-width: 640px; }
.form label { display: block; margin-bottom: 16px; font-weight: 600; font-size: 14px; color: #4a5568; }
.form select { width: 100%; padding: 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 16px; margin-top: 6px; }
.cur { margin: 16px 0; padding: 12px; background: #edf2f7; border-radius: 6px; font-size: 16px; }
.btns { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.btn { padding: 20px; border: none; border-radius: 8px; font-size: 15px; font-weight: 700; color: #fff; min-height: 48px; cursor: pointer; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-mu  { background: #3182ce; }
.btn-dnd { background: #c53030; }
.btn-clr { background: #718096; }
.result { padding: 12px; border-radius: 6px; background: #fff5f5; color: #c53030; }
.result.ok { background: #f0fff4; color: #2f855a; }
@media (max-width: 480px) {
	.btns { grid-template-columns: 1fr; gap: 8px; }
}
</style>
