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
			<div v-for="t in visibleTasks" :key="t.taskId" class="task-card" @click="selectedTask = t">
				<div class="row-top">
					<div class="title">{{ t.title || '(제목 없음)' }}</div>
					<span :class="['src', sourceClass(t.sourceType)]">{{ sourceLabel(t.sourceType) }}</span>
				</div>
				<div class="memo" v-if="t.memo">{{ t.memo }}</div>
				<div class="row-meta">
					<span v-if="t.rmNo">🏠 {{ t.rmNo }}호</span>
					<span>🕒 {{ fmtTime(t.createdAt) }}</span>
					<span class="st">{{ statusLabel(t.statusCd) }}</span>
				</div>
				<div v-if="t.assigneeId" class="assignee">👤 {{ t.assigneeId }}</div>
				<div class="actions">
					<button
						v-if="!isAdminViewer && t.statusCd === 'REQ'"
						class="primary"
						:disabled="busyId === t.taskId"
						@click="take(t)"
					>내가 받기</button>
					<button
						v-if="!isAdminViewer && t.statusCd === 'IN_PROG'"
						class="primary"
						:disabled="busyId === t.taskId"
						@click="changeStatus(t, 'DONE')"
					>완료</button>
					<button
						v-if="['REQ','IN_PROG'].includes(t.statusCd)"
						class="ghost"
						:disabled="busyId === t.taskId"
						@click="changeStatus(t, 'CANCELED')"
					>취소</button>
					<span v-if="isAdminViewer && t.statusCd === 'REQ' && !t.assigneeId" class="admin-hint">담당자 미배정</span>
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

		<!-- Task Detail Modal -->
		<div v-if="selectedTask" class="modal-overlay" @click.self="selectedTask = null">
			<div class="modal-card">
				<div class="modal-head">
					<h3>{{ selectedTask.title || '(제목 없음)' }}</h3>
					<button @click="selectedTask = null" class="modal-close">✕</button>
				</div>
				<div class="modal-body">
					<div class="detail-row"><span class="detail-label">객실</span><span>{{ selectedTask.rmNo ? selectedTask.rmNo + '호' : '—' }}</span></div>
					<div class="detail-row"><span class="detail-label">유형</span><span :class="['src', sourceClass(selectedTask.sourceType)]">{{ sourceLabel(selectedTask.sourceType) }}</span></div>
					<div class="detail-row"><span class="detail-label">상태</span><span :class="'st-badge st-' + selectedTask.statusCd">{{ statusLabel(selectedTask.statusCd) }}</span></div>
					<div class="detail-row"><span class="detail-label">접수</span><span>{{ fmtTime(selectedTask.createdAt) }}</span></div>
					<div class="detail-row" v-if="selectedTask.updatedAt && selectedTask.statusCd !== 'REQ'"><span class="detail-label">업데이트</span><span>{{ fmtTime(selectedTask.updatedAt) }}</span></div>
					<div class="detail-row" v-if="selectedTask.assigneeId"><span class="detail-label">담당자</span><span>{{ selectedTask.assigneeId }}</span></div>
					<div class="detail-row" v-if="selectedTask.memo"><span class="detail-label">메모</span><span class="detail-memo">{{ selectedTask.memo }}</span></div>
				</div>
				<div class="modal-timeline">
					<div :class="['tl-step', selectedTask.statusCd === 'REQ' || selectedTask.statusCd === 'IN_PROG' || selectedTask.statusCd === 'DONE' ? 'tl-active' : '']">접수</div>
					<div class="tl-line"></div>
					<div :class="['tl-step', selectedTask.statusCd === 'IN_PROG' || selectedTask.statusCd === 'DONE' ? 'tl-active' : '']">진행중</div>
					<div class="tl-line"></div>
					<div :class="['tl-step', selectedTask.statusCd === 'DONE' ? 'tl-done' : selectedTask.statusCd === 'CANCELED' ? 'tl-canceled' : '']">{{ selectedTask.statusCd === 'CANCELED' ? '취소됨' : '완료' }}</div>
				</div>
				<div class="modal-actions">
					<button v-if="!isAdminViewer && selectedTask.statusCd === 'REQ'" class="primary" :disabled="busyId === selectedTask.taskId" @click="take(selectedTask); selectedTask = null;">내가 받기</button>
					<button v-if="!isAdminViewer && selectedTask.statusCd === 'IN_PROG'" class="primary" :disabled="busyId === selectedTask.taskId" @click="changeStatus(selectedTask, 'DONE'); selectedTask = null;">완료</button>
					<button v-if="['REQ','IN_PROG'].includes(selectedTask.statusCd)" class="ghost" :disabled="busyId === selectedTask.taskId" @click="changeStatus(selectedTask, 'CANCELED'); selectedTask = null;">취소</button>
					<button class="ghost" @click="selectedTask = null">닫기</button>
				</div>
			</div>
		</div>
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
	{ key: 'prog',  label: '진행중',  statuses: ['IN_PROG'] },
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
const selectedTask = ref(null);
let pollTimer = null;

// PMS USER_TP 00001~00003: 시스템/프로퍼티/컴플렉스 관리자 — viewer mode
const isAdminViewer = computed(() =>
	['00001', '00002', '00003'].includes(staff.value?.userTp)
);

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
	if (s && s.startsWith('HK')) return 'src-hk';
	return '';
}

function sourceLabel(s) {
	if (!s) return '';
	if (s === 'AMENITY') return '어메니티';
	if (s === 'LATE_CO') return '레이트체크아웃';
	if (s === 'PARKING') return '주차';
	if (s === 'CHAT') return '챗';
	if (s === 'STAFF_REQ') return '내부요청';
	if (s === 'GUEST_REQ') return '게스트요청';
	if (s.startsWith('HK_')) {
		const sub = s.substring(3);
		if (sub === 'MU') return '객실정비요청';
		if (sub === 'DND') return '방해금지';
		if (sub === 'CLR') return '해제';
		return '하우스키핑';
	}
	return s;
}

function statusLabel(s) {
	if (s === 'REQ') return '대기';
	if (s === 'ASSIGNED') return '배정됨';
	if (s === 'IN_PROG') return '진행중';
	if (s === 'DONE') return '완료';
	if (s === 'CANCELED') return '취소됨';
	return s || '';
}

function fmtTime(s) {
	if (!s) return '';
	try {
		const d = new Date(s);
		const mon = d.getMonth() + 1;
		const day = d.getDate();
		const ampm = d.getHours() < 12 ? '오전' : '오후';
		const h = String(d.getHours() % 12 || 12).padStart(2, '0');
		const m = String(d.getMinutes()).padStart(2, '0');
		return `${mon}/${day} ${ampm} ${h}:${m}`;
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
		tasks.value = res?.map?.list || res?.list || [];
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
		if (!token) return;
		connectStomp(token, (client) => {
			// 역할 기반 토픽 선택:
			//  - 00001/00002 (시스템/프로퍼티 관리자) → 프로퍼티 전체
			//  - 00003 (컴플렉스 관리자) → 컴플렉스 전체
			//  - 그 외 → 내 부서만
			const userTp = staff.value?.userTp;
			const propCd = staff.value?.propCd;
			const cmpxCd = staff.value?.cmpxCd;
			const deptCd = staff.value?.deptCd;
			let topic;
			if ((userTp === '00001' || userTp === '00002') && propCd) {
				topic = '/topic/ccs/prop/' + propCd;
			} else if (userTp === '00003' && propCd && cmpxCd) {
				topic = '/topic/ccs/cmpx/' + propCd + '/' + cmpxCd;
			} else if (deptCd) {
				topic = '/topic/ccs/dept/' + deptCd;
			} else {
				return;
			}
			client.subscribe(topic, () => load());
		});
	});
	// WebSocket 주 경로 — 폴링은 연결 실패 시 백업 (30s 간격)
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

.admin-hint {
	font-size: 12px;
	color: #ad6200;
	font-style: italic;
	padding: 6px 0;
}

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

/* ── Task Detail Modal ── */
.modal-overlay {
	position: fixed;
	inset: 0;
	background: rgba(10, 14, 26, 0.72);
	backdrop-filter: blur(3px);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 400;
	padding: 20px;
}
.modal-card {
	background: #fff;
	border-radius: 16px;
	border: 1px solid #c9a96e;
	box-shadow: 0 20px 60px rgba(10, 14, 26, 0.5), 0 0 0 1px rgba(201, 169, 110, 0.2);
	width: 100%;
	max-width: 480px;
	overflow: hidden;
}
.modal-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 18px 20px 14px;
	border-bottom: 1px solid #c9a96e33;
	background: linear-gradient(135deg, #1a3a6e 0%, #253f6e 100%);
}
.modal-head h3 {
	margin: 0;
	font-size: 16px;
	font-weight: 700;
	color: #e8d5a3;
	letter-spacing: -0.2px;
	flex: 1;
	padding-right: 12px;
}
.modal-close {
	width: 30px;
	height: 30px;
	border: 1px solid rgba(201, 169, 110, 0.4);
	background: rgba(201, 169, 110, 0.12);
	color: #c9a96e;
	border-radius: 50%;
	font-size: 13px;
	cursor: pointer;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	transition: background 0.15s;
}
.modal-close:hover { background: rgba(201, 169, 110, 0.25); }

.modal-body {
	padding: 16px 20px;
	display: flex;
	flex-direction: column;
	gap: 10px;
}
.detail-row {
	display: flex;
	align-items: flex-start;
	gap: 12px;
	font-size: 14px;
}
.detail-label {
	min-width: 56px;
	font-size: 11px;
	font-weight: 700;
	color: #8492a6;
	letter-spacing: 0.8px;
	text-transform: uppercase;
	padding-top: 2px;
	flex-shrink: 0;
}
.detail-memo {
	white-space: pre-wrap;
	color: #4a5568;
	line-height: 1.5;
}

.st-badge {
	padding: 2px 10px;
	border-radius: 999px;
	font-size: 12px;
	font-weight: 700;
}
.st-REQ      { background: #fff4e6; color: #ad6200; }
.st-IN_PROG  { background: #e6f0ff; color: #1a3a6e; }
.st-DONE     { background: #e6fff0; color: #276749; }
.st-CANCELED { background: #f7fafc; color: #8492a6; }

.modal-timeline {
	display: flex;
	align-items: center;
	padding: 12px 20px 14px;
	border-top: 1px solid #edf2f7;
	border-bottom: 1px solid #edf2f7;
	background: #f7fafc;
}
.tl-step {
	font-size: 11px;
	font-weight: 700;
	color: #a0aec0;
	padding: 4px 10px;
	border-radius: 999px;
	border: 1.5px solid #e2e8f0;
	background: #fff;
	white-space: nowrap;
}
.tl-step.tl-active {
	color: #1a3a6e;
	border-color: #1a3a6e;
	background: #e6f0ff;
}
.tl-step.tl-done {
	color: #276749;
	border-color: #276749;
	background: #e6fff0;
}
.tl-step.tl-canceled {
	color: #8492a6;
	border-color: #cbd5e0;
	background: #f7fafc;
}
.tl-line {
	flex: 1;
	height: 2px;
	background: #e2e8f0;
	margin: 0 4px;
}

.modal-actions {
	display: flex;
	gap: 8px;
	padding: 14px 20px;
	flex-wrap: wrap;
}
.modal-actions button {
	padding: 9px 16px;
	border-radius: 7px;
	font-size: 13px;
	font-weight: 700;
	cursor: pointer;
	border: 1px solid #cbd5e0;
	background: #f7fafc;
	color: #4a5568;
	transition: background 0.12s;
}
.modal-actions button.primary {
	background: #1a3a6e;
	color: #fff;
	border-color: #1a3a6e;
}
.modal-actions button.primary:hover { background: #152e58; }
.modal-actions button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
