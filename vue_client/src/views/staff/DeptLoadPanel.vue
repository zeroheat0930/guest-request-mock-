<template>
	<div class="dept-load-panel">
		<div class="panel-head">
			<h3 class="panel-title">부서 로드</h3>
			<button class="ghost" :disabled="loading" @click="load">새로고침</button>
		</div>

		<div v-if="err" class="err">{{ err }}</div>

		<div v-if="loading && !members.length" class="dim">불러오는 중…</div>

		<table v-else-if="members.length" class="load-table">
			<thead>
				<tr>
					<th>직원명</th>
					<th>대기 (ASSIGNED)</th>
					<th>진행중 (IN_PROG)</th>
					<th>합계</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="m in members" :key="m.staffId">
					<td class="nm">{{ m.staffNm }}</td>
					<td>
						<span :class="['badge', m.assignedCount > 0 ? 'badge-wait' : 'badge-zero']">
							{{ m.assignedCount }}
						</span>
					</td>
					<td>
						<span :class="['badge', m.inProgCount > 0 ? 'badge-prog' : 'badge-zero']">
							{{ m.inProgCount }}
						</span>
					</td>
					<td>
						<span class="total">{{ m.assignedCount + m.inProgCount }}</span>
					</td>
				</tr>
			</tbody>
		</table>

		<div v-else class="dim">부서 멤버가 없습니다</div>
	</div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { fetchCcsDeptLoad } from '../../api/client.js';

const props = defineProps({
	deptCd: { type: String, required: true }
});

const members = ref([]);
const loading = ref(false);
const err = ref('');
let pollTimer = null;

async function load() {
	if (!sessionStorage.getItem('ccs.token')) return;
	loading.value = true;
	err.value = '';
	try {
		const res = await fetchCcsDeptLoad(props.deptCd);
		members.value = res?.list || [];
	} catch (e) {
		err.value = `로드 조회 실패: ${e?.message || '서버 오류'}`;
	} finally {
		loading.value = false;
	}
}

onMounted(() => {
	load();
	pollTimer = setInterval(load, 10000);
});

onUnmounted(() => {
	if (pollTimer) clearInterval(pollTimer);
});
</script>

<style scoped>
.dept-load-panel {
	background: #fff;
	border-radius: 12px;
	padding: 16px 18px;
	box-shadow: 0 1px 4px rgba(26, 58, 110, 0.06);
	margin-bottom: 16px;
}

.panel-head {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.panel-title {
	margin: 0;
	font-size: 15px;
	font-weight: 700;
	color: #1a3a6e;
}

.ghost {
	padding: 6px 12px;
	border: 1px solid #cbd5e0;
	background: transparent;
	color: #4a5568;
	border-radius: 6px;
	cursor: pointer;
	font-size: 12px;
}
.ghost:disabled { opacity: 0.5; cursor: not-allowed; }

.err {
	background: #fff5f5;
	color: #c53030;
	padding: 8px 12px;
	border-radius: 6px;
	margin-bottom: 10px;
	font-size: 13px;
}
.dim {
	color: #a0aec0;
	padding: 20px;
	text-align: center;
	font-size: 13px;
}

.load-table {
	width: 100%;
	border-collapse: collapse;
	font-size: 13px;
}
.load-table th {
	text-align: left;
	padding: 6px 10px;
	font-weight: 700;
	color: #4a5568;
	background: #f7fafc;
	border-bottom: 1px solid #edf2f7;
}
.load-table td {
	padding: 8px 10px;
	border-bottom: 1px solid #edf2f7;
	color: #2d3748;
}
.load-table tbody tr:last-child td { border-bottom: none; }
.nm { font-weight: 600; color: #1a3a6e; }

.badge {
	display: inline-block;
	min-width: 28px;
	text-align: center;
	padding: 2px 8px;
	border-radius: 999px;
	font-size: 12px;
	font-weight: 700;
}
.badge-wait { background: #e6f0ff; color: #1a3a6e; }
.badge-prog { background: #fff4e6; color: #ad6200; }
.badge-zero { background: #edf2f7; color: #a0aec0; }

.total {
	font-weight: 700;
	color: #2d3748;
}
</style>
