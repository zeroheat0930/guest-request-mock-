<template>
	<div class="amenity">
		<h2>🛎️ 어메니티 요청</h2>
		<div class="form">
			<label>예약번호
				<select v-model="rsvNo">
					<option value="R2026041300001">R2026041300001 / 1205 / HONG GILDONG</option>
					<option value="R2026041300002">R2026041300002 / 0807 / JOHN SMITH</option>
				</select>
			</label>
			<div class="items">
				<div v-for="item in items" :key="item.itemCd" class="item-row">
					<span class="nm">{{ item.itemNm }} <small>({{ item.itemNmEng }})</small></span>
					<span class="max">최대 {{ item.maxQty }}</span>
					<input type="number" min="0" :max="item.maxQty" v-model.number="qtyMap[item.itemCd]" />
				</div>
			</div>
			<label>요청 메모
				<textarea v-model="reqMemo" rows="2" placeholder="예: 밤 10시 이후 가져다 주세요" />
			</label>
			<button class="submit" @click="submit">요청 등록</button>
			<div v-if="result" class="result" :class="{ ok: result.resCd === '0000' }">
				[{{ result.resCd }}] {{ result.resMsg }}
				<template v-if="result.map && result.map.reqNo"> / 요청번호: {{ result.map.reqNo }}</template>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { fetchAmenityItems, requestAmenity } from '../api/client';

const rsvNo = ref('R2026041300001');
const reqMemo = ref('');
const items = ref([]);
const qtyMap = reactive({});
const result = ref(null);

const roomNoMap = {
	'R2026041300001': '1205',
	'R2026041300002': '0807'
};

onMounted(async () => {
	const res = await fetchAmenityItems();
	items.value = res.map.list || [];
	items.value.forEach(it => { qtyMap[it.itemCd] = 0; });
});

async function submit() {
	const itemList = items.value
		.filter(it => qtyMap[it.itemCd] > 0)
		.map(it => ({ itemCd: it.itemCd, qty: qtyMap[it.itemCd] }));

	if (itemList.length === 0) {
		result.value = { resCd: '9001', resMsg: '품목 수량을 입력하세요', map: {} };
		return;
	}

	try {
		result.value = await requestAmenity({
			rsvNo: rsvNo.value,
			roomNo: roomNoMap[rsvNo.value],
			itemList,
			reqMemo: reqMemo.value
		});
	} catch (err) {
		result.value = err;
	}
}
</script>

<style scoped>
.amenity h2 { margin-bottom: 20px; }
.form { background: #fff; padding: 24px; border-radius: 12px; max-width: 640px; }
.form label { display: block; margin-bottom: 16px; font-weight: 600; font-size: 14px; color: #4a5568; }
.form select, .form textarea, .form input[type=number] {
	width: 100%; padding: 10px; border: 1px solid #cbd5e0; border-radius: 6px;
	font-size: 16px; margin-top: 6px;
}
.items { margin-bottom: 16px; }
.item-row {
	display: grid; grid-template-columns: 1fr auto 90px; gap: 12px; align-items: center;
	padding: 10px 0; border-bottom: 1px solid #edf2f7;
}
.item-row .nm { font-size: 15px; }
.item-row .nm small { color: #8492a6; }
.item-row .max { font-size: 12px; color: #a0aec0; }
.item-row input { margin: 0; padding: 6px; text-align: center; }
.submit {
	width: 100%; padding: 14px; background: #1a3a6e; color: #fff;
	border: none; border-radius: 8px; font-size: 16px; font-weight: 700;
}
.result { margin-top: 16px; padding: 12px; border-radius: 6px; background: #fff5f5; color: #c53030; }
.result.ok { background: #f0fff4; color: #2f855a; }
</style>
