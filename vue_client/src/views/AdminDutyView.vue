<template>
	<div class="admin-duty">
		<h2 class="title">{{ t('admin.duty.title') }}</h2>

		<div v-if="err" class="err">{{ err }}</div>
		<div v-if="msg" class="success">{{ msg }}</div>

		<!-- Today -->
		<div class="today-card">
			<h3 class="sec-title">Today {{ today }}</h3>
			<div v-if="todayLog.log" class="today-log">
				<div class="row"><span class="lbl">{{ t('admin.duty.shift') }}</span><span>{{ shiftLabel(todayLog.log.shiftCd) }}</span></div>
				<div class="row"><span class="lbl">{{ t('admin.duty.manager') }}</span><span>{{ todayLog.log.managerId }}</span></div>
				<div class="row" v-if="todayLog.log.handoverFrom || todayLog.log.handoverTo">
					<span class="lbl">{{ t('admin.duty.handover') }}</span>
					<span>{{ (todayLog.log.handoverFrom || '—') + ' → ' + (todayLog.log.handoverTo || '—') }}</span>
				</div>
				<div class="row" v-if="todayLog.log.summary"><span class="lbl">{{ t('admin.duty.summary') }}</span><span class="multiline">{{ todayLog.log.summary }}</span></div>
				<div class="row" v-if="todayLog.log.incidents"><span class="lbl">{{ t('admin.duty.incidents') }}</span><span class="multiline">{{ todayLog.log.incidents }}</span></div>

				<div class="today-actions">
					<button class="btn-action" @click="openHandover(todayLog.log)">↪ {{ t('admin.duty.handover') }}</button>
					<button class="btn-action danger" @click="openClose(todayLog.log)">✓ {{ t('admin.duty.close') }}</button>
					<button class="btn-action ghost" @click="deleteLog(todayLog.log)">🗑 {{ t('admin.ccs.delete') }}</button>
				</div>
			</div>
			<div v-else class="dim">{{ t('admin.common.empty') }}</div>
			<div class="pms-audit">
				<strong>{{ t('admin.duty.pmsAudit') }}:</strong>
				<span :class="['pms-badge', audit.auditDoneYn === 'Y' ? 'audit-done' : 'audit-pending']">
					{{ audit.auditDoneYn === 'Y' ? '✓ Done' : '• Pending' }}
				</span>
			</div>
		</div>

		<!-- Quick shift actions -->
		<div class="shift-actions">
			<button class="btn-start" @click="openStart('DAY')">▶ {{ t('admin.duty.day') }} {{ t('admin.duty.start') }}</button>
			<button class="btn-start night" @click="openStart('NIGHT')">▶ {{ t('admin.duty.night') }} {{ t('admin.duty.start') }}</button>
		</div>

		<!-- History -->
		<h3 class="sec-title">History</h3>
		<div v-if="!history.length" class="dim">{{ t('admin.common.empty') }}</div>
		<table v-else class="tbl">
			<thead>
				<tr>
					<th>{{ t('admin.reports.from') }}</th>
					<th>{{ t('admin.duty.shift') }}</th>
					<th>{{ t('admin.duty.manager') }}</th>
					<th>{{ t('admin.duty.handover') }}</th>
					<th>{{ t('admin.duty.summary') }}</th>
					<th>{{ t('admin.ccs.col.action') }}</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="l in history" :key="l.logId">
					<td>{{ l.dutyDate }}</td>
					<td>{{ shiftLabel(l.shiftCd) }}</td>
					<td>{{ l.managerId }}</td>
					<td>{{ (l.handoverFrom || '—') + ' → ' + (l.handoverTo || '—') }}</td>
					<td class="sum" :title="l.summary">{{ l.summary || '—' }}</td>
					<td class="actions">
						<button class="btn-mini" @click="openHandover(l)" :disabled="busy">↪</button>
						<button class="btn-mini" @click="openClose(l)" :disabled="busy">✓</button>
						<button class="btn-mini danger" @click="deleteLog(l)" :disabled="busy">🗑</button>
					</td>
				</tr>
			</tbody>
		</table>

		<!-- Start Modal -->
		<div v-if="startModal.open" class="modal-overlay" @click.self="startModal.open = false">
			<div class="modal-card">
				<div class="modal-head">
					<h3>{{ shiftLabel(startModal.shiftCd) }} {{ t('admin.duty.start') }}</h3>
					<button class="modal-close" @click="startModal.open = false">✕</button>
				</div>
				<div class="modal-body">
					<label><span class="lbl">{{ t('admin.duty.summary') }}</span>
						<textarea v-model="startModal.summary" rows="2" placeholder="시작 시점 인수 사항 / 메모"></textarea>
					</label>
					<label><span class="lbl">{{ t('admin.duty.incidents') }}</span>
						<textarea v-model="startModal.incidents" rows="2" placeholder="진행 중인 사건/이슈"></textarea>
					</label>
				</div>
				<div class="modal-actions">
					<button class="ghost" @click="startModal.open = false">{{ t('admin.ccs.cancel') }}</button>
					<button class="primary" @click="doStart" :disabled="busy">{{ t('admin.duty.start') }}</button>
				</div>
			</div>
		</div>

		<!-- Handover Modal -->
		<div v-if="handoverModal.open" class="modal-overlay" @click.self="handoverModal.open = false">
			<div class="modal-card">
				<div class="modal-head">
					<h3>{{ t('admin.duty.handover') }}</h3>
					<button class="modal-close" @click="handoverModal.open = false">✕</button>
				</div>
				<div class="modal-body">
					<label><span class="lbl">{{ t('admin.duty.handover') }} → 후임자 ID</span>
						<input v-model="handoverModal.handoverTo" placeholder="예) fr2" maxlength="40" />
					</label>
					<label><span class="lbl">{{ t('admin.duty.summary') }}</span>
						<textarea v-model="handoverModal.summary" rows="3" placeholder="현재 상태 요약"></textarea>
					</label>
					<label><span class="lbl">{{ t('admin.duty.incidents') }}</span>
						<textarea v-model="handoverModal.incidents" rows="3" placeholder="미해결 사건/이슈"></textarea>
					</label>
				</div>
				<div class="modal-actions">
					<button class="ghost" @click="handoverModal.open = false">{{ t('admin.ccs.cancel') }}</button>
					<button class="primary" @click="doHandover" :disabled="busy">{{ t('admin.ccs.save') }}</button>
				</div>
			</div>
		</div>

		<!-- Close Modal -->
		<div v-if="closeModal.open" class="modal-overlay" @click.self="closeModal.open = false">
			<div class="modal-card">
				<div class="modal-head">
					<h3>{{ t('admin.duty.close') }}</h3>
					<button class="modal-close" @click="closeModal.open = false">✕</button>
				</div>
				<div class="modal-body">
					<label><span class="lbl">{{ t('admin.duty.summary') }}</span>
						<textarea v-model="closeModal.summary" rows="3" placeholder="근무 종합 요약"></textarea>
					</label>
					<label><span class="lbl">{{ t('admin.duty.incidents') }}</span>
						<textarea v-model="closeModal.incidents" rows="3" placeholder="발생한 사건/이슈 종합"></textarea>
					</label>
					<label class="check-row">
						<input type="checkbox" v-model="closeModal.pmsAuditDone" />
						<span>{{ t('admin.duty.pmsAudit') }} 완료</span>
					</label>
				</div>
				<div class="modal-actions">
					<button class="ghost" @click="closeModal.open = false">{{ t('admin.ccs.cancel') }}</button>
					<button class="primary danger" @click="doClose" :disabled="busy">{{ t('admin.duty.close') }}</button>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import {
	fetchDutyLogToday, fetchDutyLogList, startDutyShift,
	handoverDutyShift, closeDutyShift, deleteDutyShift
} from '../api/client.js';
import { t } from '../i18n/ui.js';

const today = new Date().toISOString().slice(0, 10);
const todayLog = ref({ log: null });
const audit = reactive({ auditDoneYn: 'N' });
const history = ref([]);
const busy = ref(false);
const err = ref('');
const msg = ref('');

const startModal = reactive({ open: false, shiftCd: 'DAY', summary: '', incidents: '' });
const handoverModal = reactive({ open: false, logId: '', handoverTo: '', summary: '', incidents: '' });
const closeModal = reactive({ open: false, logId: '', summary: '', incidents: '', pmsAuditDone: false });

function flashOk(text) { msg.value = text; setTimeout(() => { msg.value = ''; }, 2500); }
function flashErr(e) {
	err.value = e?.message || e?.response?.data?.message || '오류';
	setTimeout(() => { err.value = ''; }, 4000);
}

async function loadToday() {
	const res = await fetchDutyLogToday();
	const m = res?.map || {};
	todayLog.value = { log: m.log || null };
	Object.assign(audit, m.pmsAudit || { auditDoneYn: 'N' });
}
async function loadHistory() {
	const res = await fetchDutyLogList({});
	history.value = res?.map?.list || res?.list || [];
}
async function loadAll() {
	busy.value = true;
	try { await Promise.all([loadToday(), loadHistory()]); }
	finally { busy.value = false; }
}

function openStart(shiftCd) {
	startModal.shiftCd = shiftCd;
	startModal.summary = '';
	startModal.incidents = '';
	startModal.open = true;
}
async function doStart() {
	busy.value = true;
	try {
		const body = { shiftCd: startModal.shiftCd };
		if (startModal.summary) body.summary = startModal.summary;
		if (startModal.incidents) body.incidents = startModal.incidents;
		await startDutyShift(body);
		startModal.open = false;
		flashOk('근무 시작 등록 완료');
		await loadAll();
	} catch (e) { flashErr(e); }
	finally { busy.value = false; }
}

function openHandover(l) {
	handoverModal.logId = l.logId;
	handoverModal.handoverTo = l.handoverTo || '';
	handoverModal.summary = l.summary || '';
	handoverModal.incidents = l.incidents || '';
	handoverModal.open = true;
}
async function doHandover() {
	if (!handoverModal.handoverTo?.trim()) { flashErr({ message: '후임자 ID 누락' }); return; }
	busy.value = true;
	try {
		await handoverDutyShift(handoverModal.logId, {
			handoverTo: handoverModal.handoverTo.trim(),
			summary: handoverModal.summary || null,
			incidents: handoverModal.incidents || null
		});
		handoverModal.open = false;
		flashOk('인수인계 저장 완료');
		await loadAll();
	} catch (e) { flashErr(e); }
	finally { busy.value = false; }
}

function openClose(l) {
	closeModal.logId = l.logId;
	closeModal.summary = l.summary || '';
	closeModal.incidents = l.incidents || '';
	closeModal.pmsAuditDone = (l.pmsAuditDoneYn === 'Y');
	closeModal.open = true;
}
async function doClose() {
	busy.value = true;
	try {
		await closeDutyShift(closeModal.logId, {
			summary: closeModal.summary || null,
			incidents: closeModal.incidents || null,
			pmsAuditDoneYn: closeModal.pmsAuditDone ? 'Y' : 'N'
		});
		closeModal.open = false;
		flashOk('근무 종료 처리 완료');
		await loadAll();
	} catch (e) { flashErr(e); }
	finally { busy.value = false; }
}

async function deleteLog(l) {
	if (!confirm(`${l.dutyDate} ${shiftLabel(l.shiftCd)} 당직 로그를 삭제하시겠습니까?`)) return;
	busy.value = true;
	try {
		await deleteDutyShift(l.logId);
		flashOk('삭제 완료');
		await loadAll();
	} catch (e) { flashErr(e); }
	finally { busy.value = false; }
}

function shiftLabel(s) {
	if (s === 'DAY') return t('admin.duty.day');
	if (s === 'NIGHT') return t('admin.duty.night');
	return s || '';
}

onMounted(loadAll);
</script>

<style scoped>
.admin-duty { max-width: 900px; }
.title { margin: 0 0 16px; color: #1a3a6e; font-size: 22px; font-weight: 800; }
.sec-title { margin: 18px 0 10px; color: #1a3a6e; font-size: 15px; font-weight: 700; }

.err { background: #fff5f5; color: #c53030; padding: 10px 12px; border-radius: 6px; margin-bottom: 12px; font-size: 13px; }
.success { background: #f0fff4; color: #276749; padding: 10px 12px; border-radius: 6px; margin-bottom: 12px; font-size: 13px; border: 1px solid #c6f6d5; }

.today-card { background: #fff; border-radius: 10px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); margin-bottom: 10px; }
.today-log { display: flex; flex-direction: column; gap: 8px; }
.row { display: flex; gap: 12px; font-size: 13px; }
.lbl { min-width: 90px; font-size: 11px; color: #8492a6; font-weight: 700; text-transform: uppercase; }
.multiline { white-space: pre-wrap; }

.today-actions { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; }
.btn-action { padding: 7px 12px; border: 1px solid #cbd5e0; background: #fff; border-radius: 6px; cursor: pointer; font-size: 12px; font-weight: 700; color: #1a3a6e; }
.btn-action.danger { background: #c53030; color: #fff; border-color: #c53030; }
.btn-action.ghost { background: transparent; color: #4a5568; }
.btn-action:disabled { opacity: 0.5; cursor: not-allowed; }

.pms-audit { margin-top: 12px; padding: 8px 12px; background: #f7fafc; border-radius: 6px; font-size: 12px; }
.pms-badge { margin-left: 8px; padding: 2px 8px; border-radius: 999px; font-weight: 700; }
.pms-badge.audit-done    { background: #e6fff0; color: #276749; }
.pms-badge.audit-pending { background: #fff4e6; color: #ad6200; }

.shift-actions { display: flex; gap: 8px; margin-bottom: 20px; }
.btn-start { padding: 10px 18px; border: none; background: #1a3a6e; color: #fff; border-radius: 8px; font-weight: 700; cursor: pointer; font-size: 13px; }
.btn-start.night { background: #1a202c; }

.tbl { width: 100%; border-collapse: collapse; background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.tbl th, .tbl td { padding: 10px 12px; text-align: left; font-size: 13px; border-bottom: 1px solid #edf2f7; }
.tbl th { background: #f7fafc; color: #4a5568; font-weight: 700; }
.sum { color: #4a5568; max-width: 280px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.actions { white-space: nowrap; }
.btn-mini { padding: 4px 8px; border-radius: 5px; border: 1px solid #cbd5e0; background: #f7fafc; font-size: 13px; cursor: pointer; margin-right: 3px; }
.btn-mini.danger { background: #fff5f5; color: #c53030; border-color: #fed7d7; }
.btn-mini:disabled { opacity: 0.5; cursor: not-allowed; }

.dim { padding: 20px; text-align: center; color: #a0aec0; background: #fff; border-radius: 10px; font-size: 13px; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 400; padding: 20px; }
.modal-card { background: #fff; border-radius: 12px; width: 100%; max-width: 480px; overflow: hidden; }
.modal-head { padding: 14px 18px; background: #1a3a6e; color: #fff; display: flex; justify-content: space-between; align-items: center; }
.modal-head h3 { margin: 0; font-size: 15px; font-weight: 700; }
.modal-close { background: transparent; border: none; color: #fff; font-size: 18px; cursor: pointer; }
.modal-body { padding: 14px 18px; display: flex; flex-direction: column; gap: 10px; }
.modal-body label { display: flex; flex-direction: column; gap: 4px; font-size: 12px; }
.modal-body .lbl { font-weight: 700; color: #4a5568; text-transform: uppercase; letter-spacing: 0.5px; font-size: 11px; }
.modal-body input, .modal-body textarea {
	padding: 8px 10px; border: 1px solid #cbd5e0; border-radius: 6px;
	font-size: 13px; font-family: inherit; resize: vertical;
}
.check-row { flex-direction: row !important; align-items: center; gap: 8px !important; font-size: 13px !important; color: #2d3748; }
.check-row input { margin: 0; }
.modal-actions { display: flex; justify-content: flex-end; padding: 12px 18px; border-top: 1px solid #edf2f7; gap: 8px; }
.modal-actions button { padding: 8px 16px; border-radius: 6px; border: 1px solid #cbd5e0; cursor: pointer; font-weight: 700; font-size: 13px; }
.modal-actions button.primary { background: #1a3a6e; color: #fff; border-color: #1a3a6e; }
.modal-actions button.primary.danger { background: #c53030; border-color: #c53030; }
.modal-actions button.ghost { background: #fff; color: #4a5568; }
.modal-actions button:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
