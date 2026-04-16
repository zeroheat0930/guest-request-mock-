<template>
	<div class="staff">
		<header class="page-head">
			<div class="page-head-row">
				<div>
					<h2>🛎️ CCS 대시보드</h2>
					<p class="page-sub">
						<strong>{{ staff.staffNm || '—' }}</strong> ·
						<span class="dept">{{ staff.deptCd || '-' }} 부서</span>
					</p>
				</div>
				<button class="btn-new-request" @click="showRequestModal = true">+ 새 요청</button>
			</div>
		</header>

		<StatsWidget :deptCd="staff?.deptCd" class="stats-widget-wrap" />

		<div class="tabs">
			<button
				v-for="t in TABS"
				:key="t.key"
				:class="['tab', { active: tab === t.key }]"
				@click="tab = t.key"
			>
				{{ t.label }}
				<span class="badge">{{ countByTab(t.key) }}</span>
			</button>
		</div>

		<div v-if="err" class="err">{{ err }}</div>

		<div v-if="visibleTasks.length" class="list">
			<div v-for="t in visibleTasks" :key="t.taskId" class="task-card">
				<div class="row-top">
					<div class="title">{{ t.title || '(제목 없음)' }}</div>
					<span :class="['src', sourceClass(t.sourceType)]">{{ t.sourceType }}</span>
				</div>
				<div class="memo" v-if="t.memo">{{ t.memo }}</div>
				<div class="row-meta">
					<span v-if="t.roomNo">🏠 {{ t.roomNo }}호</span>
					<span>🕒 {{ fmtTime(t.createdAt) }}</span>
					<span class="st">{{ t.statusCd }}</span>
				</div>
				<div class="actions">
					<button
						v-if="t.statusCd === 'REQ'"
						class="primary"
						:disabled="busyId === t.taskId"
						@click="take(t)"
					>내가 받기</button>
					<button
						v-if="t.statusCd === 'ASSIGNED'"
						class="primary"
						:disabled="busyId === t.taskId"
						@click="changeStatus(t, 'IN_PROG')"
					>시작</button>
					<button
						v-if="t.statusCd === 'IN_PROG'"
						class="primary"
						:disabled="busyId === t.taskId"
						@click="changeStatus(t, 'DONE')"
					>완료</button>
					<button
						v-if="['REQ','ASSIGNED','IN_PROG'].includes(t.statusCd)"
						class="ghost"
						:disabled="busyId === t.taskId"
						@click="changeStatus(t, 'CANCELED')"
					>취소</button>
				</div>
			</div>
		</div>
		<div v-else class="dim">{{ busy ? '불러오는 중…' : '표시할 작업이 없습니다' }}</div>

		<details class="dept-load-section" v-if="staff?.deptCd">
			<summary>부서원 로드 현황</summary>
			<DeptLoadPanel :deptCd="staff.deptCd" />
		</details>

		<StaffRequestModal
			:open="showRequestModal"
			:onClose="() => { showRequestModal = false; }"
			:onSuccess="() => { load(); showRequestModal = false; }"
		/>
	</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchCcsTasks, assignCcsTask, transitionCcsTask } from '../../api/client.js';
import { connectStomp, disconnectStomp } from '../../api/websocket.js';
import StatsWidget from './StatsWidget.vue';
import DeptLoadPanel from './DeptLoadPanel.vue';
import StaffRequestModal from './StaffRequestModal.vue';

const TABS = [
	{ key: 'wait',  label: '대기',    statuses: ['REQ'] },
	{ key: 'prog',  label: '진행중',  statuses: ['ASSIGNED', 'IN_PROG'] },
	{ key: 'done',  label: '완료',    statuses: ['DONE'] }
];

const router = useRouter();
const tab = ref('wait');
const tasks = ref([]);
const err = ref('');
const busy = ref(false);
const busyId = ref('');
const staff = ref({});
const showRequestModal = ref(false);
let pollTimer = null;

function loadStaff() {
	try {
		const raw = sessionStorage.getItem('ccs.staff');
		staff.value = raw ? JSON.parse(raw) : {};
	} catch {
		staff.value = {};
	}
}

function gotoLogin() {
	try {
		sessionStorage.removeItem('ccs.token');
		sessionStorage.removeItem('ccs.staff');
	} catch {}
	router.replace('/staff/login');
}

function countByTab(key) {
	const conf = TABS.find(t => t.key === key);
	if (!conf) return 0;
	return tasks.value.filter(t => conf.statuses.includes(t.statusCd)).length;
}

const visibleTasks = computed(() => {
	const conf = TABS.find(t => t.key === tab.value);
	if (!conf) return [];
	return tasks.value.filter(t => conf.statuses.includes(t.statusCd));
});

function sourceClass(s) {
	if (s === 'CHAT') return 'src-chat';
	if (s === 'AMENITY') return 'src-amenity';
	if (s === 'HK') return 'src-hk';
	return '';
}

function fmtTime(s) {
	if (!s) return '';
	try {
		const d = new Date(s);
		return d.toLocaleString('ko-KR', { hour12: false });
	} catch {
		return String(s);
	}
}

async function load() {
	if (!sessionStorage.getItem('ccs.token')) { gotoLogin(); return; }
	err.value = '';
	busy.value = true;
	try {
		const res = await fetchCcsTasks();
		tasks.value = res?.list || [];
	} catch (e) {
		if (e?.status === -30) { gotoLogin(); return; }
		err.value = `조회 실패: ${e?.message || '서버 오류'}`;
	} finally {
		busy.value = false;
	}
}

async function take(t) {
	const me = staff.value.staffId;
	if (!me) { err.value = '스태프 정보 없음 — 재로그인 필요'; return; }
	busyId.value = t.taskId;
	try {
		await assignCcsTask(t.taskId, me);
		await load();
	} catch (e) {
		err.value = `작업 배정 실패: ${e?.message || '서버 오류'}`;
	} finally {
		busyId.value = '';
	}
}

async function changeStatus(t, statusCd) {
	busyId.value = t.taskId;
	try {
		await transitionCcsTask(t.taskId, statusCd);
		await load();
	} catch (e) {
		err.value = `상태 변경 실패: ${e?.message || '서버 오류'}`;
	} finally {
		busyId.value = '';
	}
}

onMounted(() => {
	loadStaff();
	load().then(() => {
		const token = sessionStorage.getItem('ccs.token');
		if (token && staff.value?.deptCd) {
			connectStomp(token, (client) => {
				client.subscribe('/topic/ccs/dept/' + staff.value.deptCd, () => {
					load();
				});
			});
		}
	});
	pollTimer = setInterval(load, 30000);
});

onUnmounted(() => {
	if (pollTimer) clearInterval(pollTimer);
	disconnectStomp();
});
</script>

<style scoped>
.staff { max-width: 860px; }

.page-head {
	margin-bottom: var(--sp-6, 20px);
}
.page-head h2 {
	margin: 0 0 6px;
	color: var(--c-brand-700, #1a3a6e);
	font-size: 24px;
	font-weight: 800;
	letter-spacing: -0.3px;
}
.page-sub {
	margin: 0;
	font-size: 13px;
	color: var(--c-text-soft, #4a5568);
}
.page-sub strong { color: var(--c-brand-700, #1a3a6e); font-weight: 700; }
.page-sub .dept { color: var(--c-text-dim, #8492a6); }

.tabs {
	display: flex;
	gap: 6px;
	margin-bottom: 16px;
	border-bottom: 1px solid #edf2f7;
}
.tab {
	padding: 10px 16px;
	border: none;
	background: transparent;
	color: #8492a6;
	font-size: 14px;
	font-weight: 600;
	cursor: pointer;
	border-bottom: 2px solid transparent;
}
.tab.active {
	color: #1a3a6e;
	border-bottom-color: #1a3a6e;
}
.tab .badge {
	display: inline-block;
	margin-left: 6px;
	padding: 1px 8px;
	background: #edf2f7;
	color: #4a5568;
	border-radius: 999px;
	font-size: 11px;
	font-weight: 700;
}
.tab.active .badge { background: #1a3a6e; color: #fff; }

.err {
	background: #fff5f5;
	color: #c53030;
	padding: 10px 12px;
	border-radius: 6px;
	margin-bottom: 12px;
	font-size: 13px;
}
.dim {
	color: #a0aec0;
	padding: 32px;
	background: #fff;
	border-radius: 12px;
	text-align: center;
}

.list { display: flex; flex-direction: column; gap: 12px; }
.task-card {
	background: #fff;
	border-radius: 12px;
	padding: 16px 18px;
	box-shadow: 0 1px 4px rgba(26, 58, 110, 0.06);
}
.row-top {
	display: flex;
	justify-content: space-between;
	align-items: center;
	gap: 8px;
	margin-bottom: 6px;
}
.title { font-weight: 700; color: #1a3a6e; font-size: 15px; }
.src {
	padding: 2px 8px;
	background: #edf2f7;
	color: #4a5568;
	border-radius: 999px;
	font-size: 11px;
	font-weight: 700;
}
.src-chat    { background: #e6f0ff; color: #1a3a6e; }
.src-amenity { background: #fff4e6; color: #ad6200; }
.src-hk      { background: #e6fff0; color: #276749; }

.memo {
	margin: 4px 0 8px;
	font-size: 13px;
	color: #4a5568;
	white-space: pre-wrap;
}
.row-meta {
	display: flex;
	gap: 12px;
	flex-wrap: wrap;
	font-size: 12px;
	color: #8492a6;
	margin-bottom: 10px;
}
.row-meta .st {
	margin-left: auto;
	padding: 2px 8px;
	background: #edf2f7;
	color: #4a5568;
	border-radius: 999px;
	font-weight: 700;
}

.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.actions button {
	padding: 8px 14px;
	border-radius: 6px;
	font-size: 13px;
	font-weight: 700;
	cursor: pointer;
	border: 1px solid #cbd5e0;
	background: #f7fafc;
	color: #4a5568;
}
.actions button.primary {
	background: #1a3a6e;
	color: #fff;
	border-color: #1a3a6e;
}
.actions button:disabled { opacity: 0.5; cursor: not-allowed; }

.page-head-row {
	display: flex;
	justify-content: space-between;
	align-items: flex-start;
}

.btn-new-request {
	padding: 10px 18px;
	background: #1a3a6e;
	color: #fff;
	border: none;
	border-radius: 8px;
	font-size: 14px;
	font-weight: 700;
	cursor: pointer;
	white-space: nowrap;
	flex-shrink: 0;
}
.btn-new-request:hover { background: #152e58; }

.stats-widget-wrap { margin-bottom: 16px; }

.dept-load-section {
	margin-top: 20px;
	border: 1px solid #edf2f7;
	border-radius: 10px;
	overflow: hidden;
}
.dept-load-section > summary {
	padding: 12px 16px;
	font-size: 14px;
	font-weight: 700;
	color: #1a3a6e;
	background: #f7fafc;
	cursor: pointer;
	user-select: none;
	list-style: none;
}
.dept-load-section > summary::-webkit-details-marker { display: none; }
.dept-load-section > summary::before {
	content: '▶ ';
	font-size: 11px;
	margin-right: 4px;
	color: #8492a6;
}
.dept-load-section[open] > summary::before { content: '▼ '; }
.dept-load-section > :not(summary) { padding: 12px; }
</style>
