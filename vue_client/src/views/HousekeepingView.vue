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
			<div class="cur">현재 상태: <strong>{{ curStatNm }}</strong></div>
			<div class="btns">
				<button @click="change('MU')"  class="btn btn-mu">🧹 객실 정비 요청</button>
				<button @click="change('DND')" class="btn btn-dnd">🚫 방해 금지</button>
				<button @click="change('CLR')" class="btn btn-clr">✓ 해제</button>
			</div>
			<div v-if="result" class="result" :class="{ ok: result.resCd === '0000' }">
				[{{ result.resCd }}] {{ result.resMsg }}
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { fetchHousekeeping, updateHousekeeping } from '../api/client';

const rsvNo = ref('R2026041300001');
const curStatNm = ref('-');
const result = ref(null);

async function loadStat() {
	try {
		const r = await fetchHousekeeping(rsvNo.value);
		curStatNm.value = r.map?.hkStatNm || '-';
	} catch (e) {
		curStatNm.value = '조회 실패';
	}
}

async function change(cd) {
	try {
		result.value = await updateHousekeeping({ rsvNo: rsvNo.value, hkStatCd: cd });
		await loadStat();
	} catch (err) {
		result.value = err;
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
.btn { padding: 20px; border: none; border-radius: 8px; font-size: 15px; font-weight: 700; color: #fff; }
.btn-mu  { background: #3182ce; }
.btn-dnd { background: #c53030; }
.btn-clr { background: #718096; }
.result { padding: 12px; border-radius: 6px; background: #fff5f5; color: #c53030; }
.result.ok { background: #f0fff4; color: #2f855a; }
</style>
