<template>
	<div class="runner-wrap">
		<header class="top-bar">
			<div class="who">
				<span class="staff-nm">{{ staff.staffNm || '—' }}</span>
				<span class="dept-badge">{{ staff.deptCd || '-' }}</span>
			</div>
			<button class="logout-btn" @click="logout" title="로그아웃">
				<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
					<polyline points="16 17 21 12 16 7"/>
					<line x1="21" y1="12" x2="9" y2="12"/>
				</svg>
			</button>
		</header>

		<main class="body">
			<div v-if="err" class="err-bar">{{ err }}</div>

			<div v-if="myTasks.length" class="task-list">
				<div
					v-for="t in myTasks"
					:key="t.taskId"
					class="task-card"
				>
					<div class="card-title">{{ t.title || '(제목 없음)' }}</div>
					<div v-if="t.roomNo" class="room-badge">{{ t.roomNo }}호</div>
					<div v-if="t.memo" class="memo">{{ t.memo }}</div>
					<div class="created-at">{{ fmtTime(t.createdAt) }}</div>
					<div class="card-actions">
						<button
							v-if="t.statusCd === 'ASSIGNED'"
							class="action-btn start-btn"
							:disabled="busyId === t.taskId"
							@click="transition(t, 'IN_PROG')"
						>▶ 시작</button>
						<button
							v-if="t.statusCd === 'IN_PROG'"
							class="action-btn done-btn"
							:disabled="busyId === t.taskId"
							@click="transition(t, 'DONE')"
						>✅ 완료</button>
					</div>
				</div>
			</div>

			<div v-else class="empty-state">
				<div class="empty-icon">📋</div>
				<div class="empty-msg">할당된 작업이 없습니다</div>
			</div>
		</main>
	</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchCcsTasks, transitionCcsTask } from '../../api/client.js';

const router = useRouter();
const tasks = ref([]);
const err = ref('');
const busyId = ref('');
const staff = ref({});
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

function logout() { gotoLogin(); }

const myTasks = computed(() => {
	const myId = staff.value.staffId;
	if (!myId) return [];
	return tasks.value.filter(t =>
		t.assigneeId === myId &&
		(t.statusCd === 'ASSIGNED' || t.statusCd === 'IN_PROG')
	);
});

function fmtTime(s) {
	if (!s) return '';
	try {
		return new Date(s).toLocaleString('ko-KR', { hour12: false });
	} catch {
		return String(s);
	}
}

async function load() {
	if (!sessionStorage.getItem('ccs.token')) { gotoLogin(); return; }
	err.value = '';
	try {
		const res = await fetchCcsTasks();
		tasks.value = res?.map?.list || [];
	} catch (e) {
		if (e?.resCd === '9102') { gotoLogin(); return; }
		err.value = `조회 실패: ${e?.resMsg || '서버 오류'}`;
	}
}

async function transition(t, statusCd) {
	busyId.value = t.taskId;
	try {
		await transitionCcsTask(t.taskId, statusCd);
		await load();
	} catch (e) {
		err.value = `상태 변경 실패: ${e?.resMsg || '서버 오류'}`;
	} finally {
		busyId.value = '';
	}
}

onMounted(() => {
	loadStaff();
	load();
	pollTimer = setInterval(load, 5000);
});

onUnmounted(() => {
	if (pollTimer) clearInterval(pollTimer);
});
</script>

<style scoped>
.runner-wrap {
	min-height: 100dvh;
	background: #f5f7fa;
	display: flex;
	flex-direction: column;
	max-width: 500px;
	margin: 0 auto;
}

/* ── 상단 바 ── */
.top-bar {
	height: 48px;
	background: #0f2748;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 16px;
	position: sticky;
	top: 0;
	z-index: 10;
}

.who {
	display: flex;
	align-items: center;
	gap: 8px;
}

.staff-nm {
	color: #e2e8f0;
	font-size: 13px;
	font-weight: 600;
}

.dept-badge {
	display: inline-block;
	padding: 2px 8px;
	background: #1a3a6e;
	color: #90cdf4;
	border-radius: 999px;
	font-size: 11px;
	font-weight: 600;
}

.logout-btn {
	background: transparent;
	border: none;
	color: #90cdf4;
	cursor: pointer;
	display: flex;
	align-items: center;
	padding: 6px;
	border-radius: 6px;
}
.logout-btn:hover { background: #1a3a6e; }

/* ── 본문 ── */
.body {
	flex: 1;
	padding: 16px 12px;
	overflow-y: auto;
}

.err-bar {
	background: #fff5f5;
	color: #c53030;
	padding: 10px 14px;
	border-radius: 8px;
	margin-bottom: 12px;
	font-size: 13px;
}

/* ── 태스크 리스트 ── */
.task-list {
	display: flex;
	flex-direction: column;
	gap: 12px;
}

.task-card {
	background: #fff;
	border-radius: 12px;
	padding: 16px;
	box-shadow: 0 1px 6px rgba(15, 39, 72, 0.08);
}

.card-title {
	font-size: 18px;
	font-weight: 700;
	color: #1a3a6e;
	margin-bottom: 6px;
	line-height: 1.3;
}

.room-badge {
	display: inline-block;
	padding: 2px 10px;
	background: #e6f0ff;
	color: #1a3a6e;
	border-radius: 999px;
	font-size: 16px;
	font-weight: 600;
	margin-bottom: 8px;
}

.memo {
	font-size: 14px;
	color: #4a5568;
	margin-bottom: 8px;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
	overflow: hidden;
	line-height: 1.5;
}

.created-at {
	font-size: 12px;
	color: #a0aec0;
	margin-bottom: 14px;
}

/* ── 액션 버튼 ── */
.card-actions {
	display: flex;
}

.action-btn {
	width: 100%;
	padding: 14px;
	border: none;
	border-radius: 8px;
	font-size: 16px;
	font-weight: 700;
	cursor: pointer;
	transition: opacity 0.15s;
}
.action-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.start-btn {
	background: #1a3a6e;
	color: #fff;
}
.start-btn:not(:disabled):active { opacity: 0.85; }

.done-btn {
	background: #276749;
	color: #fff;
}
.done-btn:not(:disabled):active { opacity: 0.85; }

/* ── 빈 상태 ── */
.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: 64px 24px;
	gap: 12px;
}

.empty-icon { font-size: 48px; }

.empty-msg {
	font-size: 16px;
	color: #a0aec0;
	font-weight: 500;
}
</style>
