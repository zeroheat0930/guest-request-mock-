<template>
	<div class="admin">
		<div class="head">
			<h2>🧑‍🍳 CCS 스태프 관리</h2>
			<div class="bar">
				<label>propCd
					<input v-model="propCd" @change="load" />
				</label>
				<label>cmpxCd
					<input v-model="cmpxCd" @change="load" />
				</label>
				<button @click="load" :disabled="busy">새로고침</button>
				<button class="ghost" @click="goBack">뒤로</button>
			</div>
		</div>

		<div v-if="err" class="err">{{ err }}</div>

		<section class="section">
			<h3>부서 목록</h3>
			<div class="table-wrap">
				<table>
					<thead>
						<tr>
							<th>deptCd</th>
							<th>deptNm</th>
							<th>sortOrd</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="d in depts" :key="d.deptCd">
							<td>{{ d.deptCd }}</td>
							<td>{{ d.deptNm }}</td>
							<td>{{ d.sortOrd }}</td>
						</tr>
						<tr v-if="!depts.length">
							<td colspan="3" class="dim">데이터 없음</td>
						</tr>
					</tbody>
				</table>
			</div>
		</section>

		<section class="section">
			<h3>직원 목록</h3>
			<div class="table-wrap">
				<table>
					<thead>
						<tr>
							<th>loginId</th>
							<th>staffNm</th>
							<th>deptCd</th>
							<th>positionCd</th>
							<th>useYn</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="s in staffList" :key="s.staffId">
							<td>{{ s.loginId }}</td>
							<td>{{ s.staffNm }}</td>
							<td>{{ s.deptCd }}</td>
							<td>{{ s.positionCd }}</td>
							<td>{{ s.useYn }}</td>
						</tr>
						<tr v-if="!staffList.length">
							<td colspan="5" class="dim">데이터 없음</td>
						</tr>
					</tbody>
				</table>
			</div>
		</section>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import { API_BASE } from '../api/client.js';

const TOKEN_KEY = 'concierge.adminToken';

const router = useRouter();
const propCd = ref('0000000010');
const cmpxCd = ref('00001');
const depts = ref([]);
const staffList = ref([]);
const busy = ref(false);
const err = ref('');

function getToken() {
	try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; }
}

function gotoLogin() {
	try { sessionStorage.removeItem(TOKEN_KEY); } catch {}
	router.replace('/admin/login');
}

function goBack() {
	router.push('/admin/features');
}

async function load() {
	const t = getToken();
	if (!t) { gotoLogin(); return; }
	err.value = '';
	busy.value = true;
	try {
		const headers = { 'X-Admin-Token': t };
		const params = { propCd: propCd.value, cmpxCd: cmpxCd.value };

		const [deptRes, staffRes] = await Promise.all([
			axios.get(`${API_BASE}/concierge/admin/departments`, { params, headers, timeout: 8000 }),
			axios.get(`${API_BASE}/concierge/admin/staff`, { params, headers, timeout: 8000 })
		]);

		depts.value = deptRes.data?.list ?? [];
		staffList.value = staffRes.data?.list ?? [];
	} catch (e) {
		if (e.response?.status === 401) { gotoLogin(); return; }
		err.value = `조회 실패: ${e.response?.data?.message || e.message}`;
	} finally {
		busy.value = false;
	}
}

onMounted(() => {
	if (!getToken()) { gotoLogin(); return; }
	load();
});
</script>

<style scoped>
.admin { max-width: 900px; }
.head { margin-bottom: 16px; }
.admin h2 { margin: 0 0 12px; color: #1a3a6e; }
.bar { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.bar label { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #4a5568; }
.bar input { padding: 6px 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 14px; width: 120px; }
.bar button { padding: 8px 14px; border: 1px solid #cbd5e0; background: #f7fafc; border-radius: 6px; cursor: pointer; font-size: 13px; }
.bar button:disabled { opacity: 0.5; cursor: not-allowed; }
.bar button.ghost { background: transparent; color: #4a5568; }

.err { background: #fff5f5; color: #c53030; padding: 10px 12px; border-radius: 6px; margin-bottom: 12px; font-size: 13px; }

.section { margin-bottom: 24px; }
.section h3 { font-size: 14px; font-weight: 700; color: #1a3a6e; margin: 0 0 10px; }

.table-wrap {
	background: #fff;
	border-radius: 12px;
	overflow: hidden;
	box-shadow: 0 2px 8px rgba(26, 58, 110, 0.06);
}

table { width: 100%; border-collapse: collapse; font-size: 13px; }
thead { background: #edf2f7; }
thead th { padding: 10px 14px; text-align: left; font-weight: 600; color: #4a5568; }
tbody td { padding: 10px 14px; border-top: 1px solid #f0f4f8; color: #2d3748; }
tbody tr:hover td { background: #f7fafc; }

.dim { text-align: center; color: #a0aec0; padding: 24px; }
</style>
