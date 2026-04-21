<template>
	<div class="admin-duty">
		<h2 class="title">{{ t('admin.duty.title') }}</h2>

		<!-- Today -->
		<div class="today-card">
			<h3 class="sec-title">Today {{ today }}</h3>
			<div v-if="todayLog.log" class="today-log">
				<div class="row"><span class="lbl">{{ t('admin.duty.shift') }}</span><span>{{ shiftLabel(todayLog.log.shiftCd) }}</span></div>
				<div class="row"><span class="lbl">{{ t('admin.duty.manager') }}</span><span>{{ todayLog.log.managerId }}</span></div>
				<div class="row" v-if="todayLog.log.summary"><span class="lbl">{{ t('admin.duty.summary') }}</span><span>{{ todayLog.log.summary }}</span></div>
				<div class="row" v-if="todayLog.log.incidents"><span class="lbl">{{ t('admin.duty.incidents') }}</span><span class="multiline">{{ todayLog.log.incidents }}</span></div>
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
			<button class="btn-start" @click="startShift('DAY')">▶ {{ t('admin.duty.day') }}</button>
			<button class="btn-start night" @click="startShift('NIGHT')">▶ {{ t('admin.duty.night') }}</button>
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
				</tr>
			</thead>
			<tbody>
				<tr v-for="l in history" :key="l.logId">
					<td>{{ l.dutyDate }}</td>
					<td>{{ shiftLabel(l.shiftCd) }}</td>
					<td>{{ l.managerId }}</td>
					<td>{{ (l.handoverFrom || '—') + ' → ' + (l.handoverTo || '—') }}</td>
					<td class="sum">{{ l.summary || '—' }}</td>
				</tr>
			</tbody>
		</table>
	</div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchDutyLogToday, fetchDutyLogList, startDutyShift } from '../api/client.js';
import { t } from '../i18n/ui.js';

const today = new Date().toISOString().slice(0, 10);
const todayLog = ref({ log: null });
const audit = reactive({ auditDoneYn: 'N' });
const history = ref([]);

async function loadToday() {
	const res = await fetchDutyLogToday();
	const m = res?.map || {};
	todayLog.value = { log: m.log || null };
	Object.assign(audit, m.pmsAudit || {});
}
async function loadHistory() {
	const res = await fetchDutyLogList({});
	history.value = res?.map?.list || res?.list || [];
}
async function startShift(shiftCd) {
	await startDutyShift({ shiftCd });
	await loadToday();
	await loadHistory();
}

function shiftLabel(s) {
	if (s === 'DAY') return t('admin.duty.day');
	if (s === 'NIGHT') return t('admin.duty.night');
	return s || '';
}

onMounted(async () => { await loadToday(); await loadHistory(); });
</script>

<style scoped>
.admin-duty { max-width: 900px; }
.title { margin: 0 0 16px; color: #1a3a6e; font-size: 22px; font-weight: 800; }
.sec-title { margin: 18px 0 10px; color: #1a3a6e; font-size: 15px; font-weight: 700; }

.today-card { background: #fff; border-radius: 10px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); margin-bottom: 10px; }
.today-log { display: flex; flex-direction: column; gap: 8px; }
.row { display: flex; gap: 12px; font-size: 13px; }
.lbl { min-width: 80px; font-size: 11px; color: #8492a6; font-weight: 700; text-transform: uppercase; }
.multiline { white-space: pre-wrap; }

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
.sum { color: #4a5568; max-width: 300px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.dim { padding: 20px; text-align: center; color: #a0aec0; background: #fff; border-radius: 10px; font-size: 13px; }
</style>
