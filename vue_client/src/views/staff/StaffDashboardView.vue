<template>
	<div class="staff">
		<!-- SLA 초과 에스컬레이션 토스트 (관리자 전용) -->
		<div v-if="escToasts.length" class="esc-toast-stack">
			<div v-for="et in escToasts" :key="et.id" class="esc-toast" @click="escToasts = escToasts.filter(x => x.id !== et.id)">
				<div class="esc-toast-head">
					<span class="esc-toast-icon">🚨</span>
					<span class="esc-toast-title">{{ tt('staff.sla.overdue') }}</span>
					<span class="esc-toast-close">×</span>
				</div>
				<div class="esc-toast-body">
					<div class="esc-toast-main">{{ et.title }}</div>
					<div class="esc-toast-meta">
						<span v-if="et.rmNo">🏠 {{ et.rmNo }}</span>
						<span v-if="et.dept">· {{ et.dept }}</span>
						<span>· {{ et.elapsed }}/{{ et.sla }}m ({{ et.elapsed - et.sla }}m {{ tt('staff.sla.exceed') }})</span>
					</div>
				</div>
			</div>
		</div>

		<header class="page-head">
			<div class="page-head-row">
				<div>
					<h2>🛎️ {{ tt('staff.dashboard.title') }}</h2>
					<p class="page-sub">
						<strong>{{ staff.staffNm || '—' }}</strong> ·
						<span class="dept">{{ staff.deptCd || '-' }}</span>
					</p>
				</div>
				<button class="btn-new-request" @click="showRequestModal = true">{{ tt('staff.dashboard.newRequest') }}</button>
			</div>
		</header>

		<StatsWidget :deptCd="staff?.deptCd" class="stats-widget-wrap" />

		<div class="tabs">
			<button
				v-for="tb in TABS"
				:key="tb.key"
				:class="['tab', { active: tab === tb.key }]"
				@click="tab = tb.key"
			>
				{{ tt(tb.labelKey) }}
				<span class="badge">{{ countByTab(tb.key) }}</span>
			</button>
		</div>

		<div v-if="err" class="err">{{ err }}</div>

		<div v-if="visibleTasks.length" class="list">
			<div
				v-for="tk in visibleTasks"
				:key="tk.taskId"
				:class="['task-card', { overdue: tk.overdue }]"
				@click="selectedTask = tk"
			>
				<div class="row-top">
					<div class="title">{{ tk.title || '—' }}</div>
					<span :class="['src', sourceClass(tk.sourceType)]">{{ sourceLabel(tk.sourceType) }}</span>
				</div>
				<div class="memo" v-if="tk.memo">{{ tk.memo }}</div>
				<div class="row-meta">
					<span v-if="tk.rmNo">🏠 {{ tk.rmNo }}</span>
					<span>🕒 {{ fmtTime(tk.createdAt) }}</span>
					<span v-if="isOpenStatus(tk.statusCd) && tk.slaMin != null"
						:class="['sla', { 'sla-overdue': tk.overdue }]">
						{{ tk.overdue ? '🔥' : '⏱' }} {{ tk.elapsedMin }}/{{ tk.slaMin }}m
					</span>
					<span class="st">{{ statusLabel(tk.statusCd) }}</span>
				</div>
				<div v-if="tk.assigneeId" class="assignee">👤 {{ tk.assigneeId }}</div>
				<div class="actions">
					<button
						v-if="!isAdminViewer && tk.statusCd === 'REQ'"
						class="primary"
						:disabled="busyId === tk.taskId"
						@click.stop="take(tk)"
					>{{ tt('staff.action.assign') }}</button>
					<button
						v-if="!isAdminViewer && tk.statusCd === 'ASSIGNED'"
						class="primary"
						:disabled="busyId === tk.taskId"
						@click.stop="changeStatus(tk, 'IN_PROG')"
					>{{ tt('staff.action.start') }}</button>
					<button
						v-if="!isAdminViewer && tk.statusCd === 'IN_PROG'"
						class="primary"
						:disabled="busyId === tk.taskId"
						@click.stop="changeStatus(tk, 'DONE')"
					>{{ tt('staff.action.complete') }}</button>
					<button
						v-if="['REQ','ASSIGNED','IN_PROG'].includes(tk.statusCd)"
						class="ghost"
						:disabled="busyId === tk.taskId"
						@click.stop="changeStatus(tk, 'CANCELED')"
					>{{ tt('staff.action.cancel') }}</button>
					<span v-if="isAdminViewer && tk.statusCd === 'REQ' && !tk.assigneeId" class="admin-hint">{{ tt('staff.adminHint') }}</span>
				</div>
			</div>
		</div>
		<div v-else class="dim">{{ busy ? tt('staff.loading') : tt('staff.empty') }}</div>

		<details class="dept-load-section" v-if="staff?.deptCd">
			<summary>{{ tt('staff.deptLoad') }}</summary>
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
					<h3>{{ selectedTask.title || '—' }}</h3>
					<button @click="selectedTask = null" class="modal-close">✕</button>
				</div>
				<div class="modal-body">
					<div class="detail-row"><span class="detail-label">{{ tt('staff.detail.room') }}</span><span>{{ selectedTask.rmNo || '—' }}</span></div>
					<div class="detail-row"><span class="detail-label">{{ tt('staff.detail.type') }}</span><span :class="['src', sourceClass(selectedTask.sourceType)]">{{ sourceLabel(selectedTask.sourceType) }}</span></div>
					<div class="detail-row"><span class="detail-label">{{ tt('staff.detail.status') }}</span><span :class="'st-badge st-' + selectedTask.statusCd">{{ statusLabel(selectedTask.statusCd) }}</span></div>
					<div class="detail-row" v-if="isOpenStatus(selectedTask.statusCd) && selectedTask.slaMin != null">
						<span class="detail-label">{{ tt('staff.detail.sla') }}</span>
						<span :class="['sla', { 'sla-overdue': selectedTask.overdue }]">
							{{ selectedTask.overdue ? '🔥' : '⏱' }} {{ selectedTask.elapsedMin }}m / {{ selectedTask.slaMin }}m
							<strong v-if="selectedTask.overdue"> · {{ selectedTask.elapsedMin - selectedTask.slaMin }}m {{ tt('staff.sla.exceed') }}</strong>
						</span>
					</div>
					<div class="detail-row"><span class="detail-label">{{ tt('staff.detail.received') }}</span><span>{{ fmtTime(selectedTask.createdAt) }}</span></div>
					<div class="detail-row" v-if="selectedTask.updatedAt && selectedTask.statusCd !== 'REQ'"><span class="detail-label">{{ tt('staff.detail.updated') }}</span><span>{{ fmtTime(selectedTask.updatedAt) }}</span></div>
					<div class="detail-row" v-if="selectedTask.assigneeId"><span class="detail-label">{{ tt('staff.detail.assignee') }}</span><span>{{ selectedTask.assigneeId }}</span></div>
					<div class="detail-row" v-if="selectedTask.memo"><span class="detail-label">{{ tt('staff.detail.memo') }}</span><span class="detail-memo">{{ selectedTask.memo }}</span></div>
				</div>
				<div class="modal-timeline">
					<div :class="['tl-step', selectedTask.statusCd === 'REQ' || selectedTask.statusCd === 'IN_PROG' || selectedTask.statusCd === 'DONE' ? 'tl-active' : '']">{{ tt('staff.timeline.received') }}</div>
					<div class="tl-line"></div>
					<div :class="['tl-step', selectedTask.statusCd === 'IN_PROG' || selectedTask.statusCd === 'DONE' ? 'tl-active' : '']">{{ tt('staff.timeline.inProg') }}</div>
					<div class="tl-line"></div>
					<div :class="['tl-step', selectedTask.statusCd === 'DONE' ? 'tl-done' : selectedTask.statusCd === 'CANCELED' ? 'tl-canceled' : '']">{{ selectedTask.statusCd === 'CANCELED' ? tt('staff.timeline.canceled') : tt('staff.timeline.complete') }}</div>
				</div>
				<div class="modal-actions">
					<button v-if="!isAdminViewer && selectedTask.statusCd === 'REQ'" class="primary" :disabled="busyId === selectedTask.taskId" @click="take(selectedTask); selectedTask = null;">{{ tt('staff.action.assign') }}</button>
					<button v-if="!isAdminViewer && selectedTask.statusCd === 'ASSIGNED'" class="primary" :disabled="busyId === selectedTask.taskId" @click="changeStatus(selectedTask, 'IN_PROG'); selectedTask = null;">{{ tt('staff.action.start') }}</button>
					<button v-if="!isAdminViewer && selectedTask.statusCd === 'IN_PROG'" class="primary" :disabled="busyId === selectedTask.taskId" @click="changeStatus(selectedTask, 'DONE'); selectedTask = null;">{{ tt('staff.action.complete') }}</button>
					<button v-if="['REQ','ASSIGNED','IN_PROG'].includes(selectedTask.statusCd)" class="ghost" :disabled="busyId === selectedTask.taskId" @click="changeStatus(selectedTask, 'CANCELED'); selectedTask = null;">{{ tt('staff.action.cancel') }}</button>
					<button class="ghost" @click="selectedTask = null">{{ tt('staff.action.close') }}</button>
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
import { t as tt } from '../../i18n/ui.js';
import StatsWidget from './StatsWidget.vue';
import DeptLoadPanel from './DeptLoadPanel.vue';
import StaffRequestModal from './StaffRequestModal.vue';

const TABS = [
	{ key: 'mine',  labelKey: 'staff.tab.mine',  statuses: ['ASSIGNED','IN_PROG'], mine: true },
	{ key: 'wait',  labelKey: 'staff.tab.wait',  statuses: ['REQ'] },
	{ key: 'prog',  labelKey: 'staff.tab.prog',  statuses: ['IN_PROG'] },
	{ key: 'done',  labelKey: 'staff.tab.done',  statuses: ['DONE'] }
];

const router = useRouter();
// 관리자는 '대기' 부터, 일반 사용자는 '내 작업' 부터 보여줌
const initialTab = (() => {
	try {
		const s = JSON.parse(sessionStorage.getItem('ccs.staff') || '{}');
		return ['00001','00002','00003'].includes(s?.userTp) ? 'wait' : 'mine';
	} catch { return 'wait'; }
})();
const tab = ref(initialTab);
const tasks = ref([]);
const err = ref('');
const busy = ref(false);
const busyId = ref('');
const staff = ref({});
const showRequestModal = ref(false);
const selectedTask = ref(null);
const escToasts = ref([]); // SLA 초과 에스컬레이션 토스트
let pollTimer = null;

function pushEscToast(task) {
	const id = task.taskId || ('esc' + Date.now());
	const toast = {
		id,
		title: task.title || '작업 SLA 초과',
		rmNo: task.rmNo,
		elapsed: task.elapsedMin,
		sla: task.slaMin,
		dept: task.deptCd
	};
	// 중복 방지 — 같은 taskId 면 갱신
	const idx = escToasts.value.findIndex(t => t.id === id);
	if (idx >= 0) escToasts.value.splice(idx, 1);
	escToasts.value.unshift(toast);
	if (escToasts.value.length > 4) escToasts.value.pop();
	setTimeout(() => {
		escToasts.value = escToasts.value.filter(t => t.id !== id);
	}, 8000);
}

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

function tabMatch(t, conf) {
	if (!conf.statuses.includes(t.statusCd)) return false;
	if (conf.mine && t.assigneeId !== staff.value?.staffId) return false;
	return true;
}
function countByTab(key) {
	const conf = TABS.find(t => t.key === key);
	if (!conf) return 0;
	return tasks.value.filter(t => tabMatch(t, conf)).length;
}

const visibleTasks = computed(() => {
	const conf = TABS.find(t => t.key === tab.value);
	if (!conf) return [];
	return tasks.value.filter(t => tabMatch(t, conf));
});

function isOpenStatus(s) {
	return s === 'REQ' || s === 'ASSIGNED' || s === 'IN_PROG';
}

function sourceClass(s) {
	if (s === 'CHAT') return 'src-chat';
	if (s === 'AMENITY') return 'src-amenity';
	if (s && s.startsWith('HK')) return 'src-hk';
	return '';
}

function sourceLabel(s) {
	if (!s) return '';
	if (s === 'AMENITY') return tt('staff.source.amenity');
	if (s === 'LATE_CO') return tt('staff.source.lateCheckout');
	if (s === 'PARKING') return tt('staff.source.parking');
	if (s === 'CHAT') return tt('staff.source.chat');
	if (s === 'STAFF_REQ') return tt('staff.source.staffReq');
	if (s === 'LOSTFOUND') return tt('staff.source.lostfound');
	if (s === 'VOC') return tt('staff.source.voc');
	if (s === 'RENTAL') return tt('staff.source.rental');
	if (s === 'GUEST_REQ') return tt('staff.source.staffReq');
	if (s.startsWith('HK_')) {
		const sub = s.substring(3);
		if (sub === 'MU') return tt('staff.source.hkMu');
		if (sub === 'DND') return tt('staff.source.hkDnd');
		if (sub === 'CLR') return tt('staff.source.hkClr');
		return tt('hk.title');
	}
	return s;
}

function statusLabel(s) {
	if (s === 'REQ') return tt('staff.status.req');
	if (s === 'ASSIGNED') return tt('staff.status.assigned');
	if (s === 'IN_PROG') return tt('staff.status.inProg');
	if (s === 'DONE') return tt('staff.status.done');
	if (s === 'CANCELED') return tt('staff.status.canceled');
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
		err.value = e?.message || tt('error.server');
	} finally {
		busy.value = false;
	}
}

async function take(tk) {
	const me = staff.value.staffId;
	if (!me) { err.value = tt('auth.expired'); return; }
	busyId.value = tk.taskId;
	try {
		await assignCcsTask(tk.taskId, me);
		await load();
	} catch (e) {
		err.value = e?.message || tt('error.server');
	} finally {
		busyId.value = '';
	}
}

async function changeStatus(tk, statusCd) {
	busyId.value = tk.taskId;
	try {
		await transitionCcsTask(tk.taskId, statusCd);
		await load();
	} catch (e) {
		err.value = e?.message || tt('error.server');
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
			// SLA 에스컬레이션 — 관리자/컴플렉스 관리자만 구독
			if (isAdminViewer.value && propCd) {
				client.subscribe('/topic/ccs/esc/' + propCd, (msg) => {
					try {
						const task = JSON.parse(msg.body);
						pushEscToast(task);
					} catch {}
					load();
				});
			}
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

/* ═══ SLA overdue ═══ */
.task-card.overdue {
	border: 2px solid #e53e3e;
	box-shadow: 0 0 0 4px rgba(229, 62, 62, 0.1), 0 1px 4px rgba(229, 62, 62, 0.15);
	animation: overdue-pulse 2s ease-in-out infinite;
}
@keyframes overdue-pulse {
	0%, 100% { box-shadow: 0 0 0 4px rgba(229, 62, 62, 0.1), 0 1px 4px rgba(229, 62, 62, 0.15); }
	50%      { box-shadow: 0 0 0 8px rgba(229, 62, 62, 0.18), 0 1px 4px rgba(229, 62, 62, 0.25); }
}
.row-meta .sla {
	color: #4a5568;
	font-weight: 600;
}
.row-meta .sla.sla-overdue {
	color: #c53030;
	font-weight: 700;
}
.detail-row .sla.sla-overdue {
	color: #c53030;
	font-weight: 700;
}

/* ═══ SLA 에스컬레이션 토스트 ═══ */
.esc-toast-stack {
	position: fixed;
	top: 20px;
	right: 20px;
	z-index: 500;
	display: flex;
	flex-direction: column;
	gap: 10px;
	pointer-events: none;
}
.esc-toast {
	pointer-events: auto;
	min-width: 280px;
	max-width: 360px;
	background: linear-gradient(135deg, #742a2a 0%, #9b2c2c 100%);
	color: #fff;
	border-radius: 12px;
	border: 1px solid rgba(229, 62, 62, 0.6);
	box-shadow: 0 8px 24px rgba(197, 48, 48, 0.35);
	padding: 12px 14px;
	cursor: pointer;
	animation: esc-in 0.3s ease-out;
}
@keyframes esc-in {
	from { opacity: 0; transform: translateX(30px); }
	to   { opacity: 1; transform: translateX(0); }
}
.esc-toast-head {
	display: flex;
	align-items: center;
	gap: 6px;
	font-size: 11px;
	font-weight: 700;
	letter-spacing: 0.8px;
	text-transform: uppercase;
	opacity: 0.9;
	margin-bottom: 8px;
}
.esc-toast-icon { font-size: 14px; }
.esc-toast-title { flex: 1; }
.esc-toast-close { font-size: 18px; opacity: 0.7; line-height: 1; }
.esc-toast-main {
	font-size: 14px;
	font-weight: 700;
	margin-bottom: 4px;
	line-height: 1.3;
}
.esc-toast-meta {
	font-size: 12px;
	opacity: 0.8;
	display: flex;
	gap: 6px;
	flex-wrap: wrap;
}
@media (max-width: 720px) {
	.esc-toast-stack { top: 10px; right: 10px; left: 10px; }
	.esc-toast { min-width: 0; max-width: 100%; }
}

/* ═══ Mobile (< 720px) ═══ */
@media (max-width: 720px) {
	.staff { max-width: 100%; }
	.page-head h2 { font-size: 18px; }
	.page-head-row {
		flex-direction: column;
		align-items: stretch;
		gap: 10px;
	}
	.btn-new-request {
		width: 100%;
		padding: 10px 12px;
		font-size: 13px;
	}
	.tabs {
		overflow-x: auto;
		flex-wrap: nowrap;
	}
	.tab {
		padding: 10px 12px;
		font-size: 13px;
		white-space: nowrap;
	}
	.task-card { padding: 12px 14px; }
	.row-top { flex-wrap: wrap; }
	.row-meta { gap: 8px; font-size: 11px; }
	.row-meta .st { margin-left: 0; }

	.modal-overlay { padding: 10px; }
	.modal-card { border-radius: 12px; }
	.modal-head { padding: 14px 16px 12px; }
	.modal-head h3 { font-size: 14px; }
	.modal-body { padding: 12px 16px; }
	.detail-row { font-size: 13px; }
	.detail-label { min-width: 48px; font-size: 10px; }
	.modal-timeline { padding: 10px 16px; }
	.tl-step { padding: 3px 8px; font-size: 10px; }
	.modal-actions { padding: 12px 16px; }
	.modal-actions button {
		padding: 10px 12px;
		font-size: 13px;
		flex: 1 1 calc(50% - 4px);
	}
}
</style>
