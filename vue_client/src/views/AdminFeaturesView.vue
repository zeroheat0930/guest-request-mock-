<template>
	<div class="admin">
		<h2>⚙️ 컨시어지 기능 관리</h2>
		<div class="bar">
			<label>propCd
				<input v-model="propCd" @change="load" />
			</label>
			<button @click="load">새로고침</button>
			<button class="primary" @click="save" :disabled="busy">저장</button>
			<button class="ghost" @click="resetToken">토큰 재입력</button>
		</div>
		<div v-if="err" class="err">{{ err }}</div>

		<table v-if="rows.length">
			<thead>
				<tr>
					<th>featureCd</th>
					<th>이름</th>
					<th>사용</th>
					<th>sortOrd</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="r in rows" :key="r.featureCd">
					<td><code>{{ r.featureCd }}</code></td>
					<td>{{ META[r.featureCd]?.label || r.featureCd }}</td>
					<td><input type="checkbox" :checked="r.useYn === 'Y'" @change="r.useYn = $event.target.checked ? 'Y' : 'N'" /></td>
					<td><input type="number" v-model.number="r.sortOrd" style="width: 80px;" /></td>
				</tr>
			</tbody>
		</table>
		<div v-else class="dim">데이터 없음</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { API_BASE } from '../api/client.js';
import { FEATURE_META } from '../features/featureStore.js';

const META = FEATURE_META;

const propCd = ref('HQ');
const rows = ref([]);
const busy = ref(false);
const err = ref('');

const TOKEN_KEY = 'concierge.adminToken';

function getToken() {
	let t = sessionStorage.getItem(TOKEN_KEY);
	if (!t) {
		t = window.prompt('관리자 토큰을 입력하세요 (X-Admin-Token)') || '';
		if (t) sessionStorage.setItem(TOKEN_KEY, t);
	}
	return t;
}

function resetToken() {
	sessionStorage.removeItem(TOKEN_KEY);
	getToken();
}

async function load() {
	err.value = '';
	const t = getToken();
	if (!t) { err.value = '토큰 필요'; return; }
	try {
		const res = await axios.get(`${API_BASE}/concierge/admin/features`, {
			params: { propCd: propCd.value },
			headers: { 'X-Admin-Token': t },
			timeout: 8000
		});
		rows.value = res.data?.map?.list || [];
	} catch (e) {
		err.value = `조회 실패: ${e.response?.data?.resMsg || e.message}`;
	}
}

async function save() {
	err.value = '';
	const t = getToken();
	if (!t) { err.value = '토큰 필요'; return; }
	busy.value = true;
	try {
		const res = await axios.put(`${API_BASE}/concierge/admin/features`, rows.value, {
			params: { propCd: propCd.value },
			headers: { 'X-Admin-Token': t, 'Content-Type': 'application/json' },
			timeout: 8000
		});
		rows.value = res.data?.map?.list || rows.value;
		alert('저장 완료');
	} catch (e) {
		err.value = `저장 실패: ${e.response?.data?.resMsg || e.message}`;
	} finally {
		busy.value = false;
	}
}

onMounted(load);
</script>

<style scoped>
.admin { background: #fff; padding: 24px; border-radius: 12px; max-width: 720px; }
.admin h2 { margin-bottom: 16px; }
.bar { display: flex; gap: 8px; align-items: center; margin-bottom: 16px; flex-wrap: wrap; }
.bar label { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #4a5568; }
.bar input[type=text], .bar input:not([type]) { padding: 6px 10px; border: 1px solid #cbd5e0; border-radius: 6px; }
.bar button { padding: 8px 14px; border: 1px solid #cbd5e0; background: #f7fafc; border-radius: 6px; cursor: pointer; }
.bar button.primary { background: #1a3a6e; color: #fff; border-color: #1a3a6e; }
.bar button.ghost { background: transparent; }
table { width: 100%; border-collapse: collapse; }
th, td { text-align: left; padding: 10px 8px; border-bottom: 1px solid #edf2f7; font-size: 14px; }
th { background: #f7fafc; font-weight: 700; color: #4a5568; }
code { background: #edf2f7; padding: 2px 6px; border-radius: 4px; font-size: 12px; }
.err { background: #fff5f5; color: #c53030; padding: 10px; border-radius: 6px; margin-bottom: 12px; }
.dim { color: #a0aec0; }
</style>
