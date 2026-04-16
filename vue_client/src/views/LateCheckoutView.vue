<template>
	<div class="lc">
		<h2>⏰ 레이트 체크아웃</h2>
		<div class="form">
			<label>예약번호
				<select v-model="rsvNo">
					<option value="R2026041300001">R2026041300001 / 1205 / 기본 11:00</option>
					<option value="R2026041300002">R2026041300002 / 0807 / 기본 11:00</option>
				</select>
			</label>
			<label>요청 체크아웃 시각
				<select v-model="reqOutTm">
					<option value="1200">12:00</option>
					<option value="1300">13:00</option>
					<option value="1400">14:00</option>
					<option value="1600">16:00</option>
					<option value="1800">18:00</option>
					<option value="2000">20:00</option>
				</select>
			</label>
			<button class="btn-check" @click="check" :disabled="checking">가능 여부 확인</button>
			<LoadingSpinner v-if="checking" text="가능 여부 확인 중..." />
			<div v-else-if="info" class="info" :class="{ avail: info.availYn === 'Y' }">
				<div>가능 여부: <strong>{{ info.availYn === 'Y' ? '가능' : '불가' }}</strong></div>
				<div>요금 구분: {{ info.rateTpNm }}</div>
				<div>추가 요금: <strong>{{ Number(info.addAmt).toLocaleString() }} {{ info.curCd }}</strong></div>
				<button v-if="info.availYn === 'Y'" class="btn-req" @click="apply">신청하기</button>
			</div>
			<div v-if="result" class="result" :class="{ ok: result.status === 0 }">
				{{ result.message }}
				<template v-if="result.map && result.map.reqNo"> / 요청번호: {{ result.map.reqNo }}</template>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref } from 'vue';
import { checkLateCheckout, requestLateCheckout } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner.vue';

const rsvNo = ref('R2026041300001');
const reqOutTm = ref('1300');
const info = ref(null);
const result = ref(null);
const checking = ref(false);

function showResult(data) {
	result.value = data;
	setTimeout(() => { result.value = null; }, 3000);
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
.lc h2 { margin-bottom: 20px; }
.form { background: #fff; padding: 24px; border-radius: 12px; max-width: 640px; }
.form label { display: block; margin-bottom: 16px; font-weight: 600; font-size: 14px; color: #4a5568; }
.form select { width: 100%; padding: 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 16px; margin-top: 6px; }
.btn-check { width: 100%; padding: 14px; background: #1a3a6e; color: #fff; border: none; border-radius: 8px; font-size: 16px; font-weight: 700; min-height: 48px; }
.btn-check:disabled { opacity: 0.6; cursor: not-allowed; }
.info { margin-top: 16px; padding: 16px; background: #fff5f5; border-radius: 8px; }
.info.avail { background: #f0fff4; }
.info div { margin-bottom: 6px; }
.btn-req { margin-top: 12px; width: 100%; padding: 12px; background: #2f855a; color: #fff; border: none; border-radius: 6px; font-size: 15px; font-weight: 700; min-height: 48px; }
.result { margin-top: 16px; padding: 12px; border-radius: 6px; background: #fff5f5; color: #c53030; }
.result.ok { background: #f0fff4; color: #2f855a; }
</style>
