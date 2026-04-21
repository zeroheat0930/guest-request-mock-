<template>
	<div class="duty-staff">
		<h2 class="title">{{ t('shell.nav.duty') }}</h2>

		<div class="actions">
			<button class="btn-start" @click="startShift('DAY')">{{ t('admin.duty.start') }} · {{ t('admin.duty.day') }}</button>
			<button class="btn-start night" @click="startShift('NIGHT')">{{ t('admin.duty.start') }} · {{ t('admin.duty.night') }}</button>
		</div>

		<div v-if="currentLog" class="log-card">
			<h3>{{ t('admin.duty.shift') }}: {{ shiftLabel(currentLog.shiftCd) }}</h3>

			<label><span class="lb">{{ t('admin.duty.summary') }}</span>
				<textarea v-model="form.summary" rows="3"></textarea>
			</label>

			<label><span class="lb">{{ t('admin.duty.incidents') }}</span>
				<textarea v-model="form.incidents" rows="5" placeholder="timeline entries..."></textarea>
			</label>

			<label><span class="lb">{{ t('admin.duty.handover') }} → To</span>
				<input v-model="form.handoverTo" type="text" />
			</label>

			<div class="btn-row">
				<button class="primary" @click="saveHandover">{{ t('admin.duty.handover') }}</button>
				<button class="ghost" @click="closeShift">{{ t('admin.duty.close') }}</button>
			</div>
		</div>
		<div v-else class="dim">{{ t('admin.common.empty') }}</div>
	</div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchDutyLogToday, startDutyShift, handoverDutyShift, closeDutyShift } from '../../api/client.js';
import { t } from '../../i18n/ui.js';

const currentLog = ref(null);
const form = reactive({ summary: '', incidents: '', handoverTo: '' });

async function load() {
	const res = await fetchDutyLogToday();
	const m = res?.map || {};
	currentLog.value = m.log || null;
	if (m.log) {
		form.summary = m.log.summary || '';
		form.incidents = m.log.incidents || '';
		form.handoverTo = m.log.handoverTo || '';
	}
}

async function startShift(shiftCd) {
	await startDutyShift({ shiftCd });
	await load();
}
async function saveHandover() {
	if (!currentLog.value) return;
	await handoverDutyShift(currentLog.value.logId, {
		handoverTo: form.handoverTo,
		summary: form.summary,
		incidents: form.incidents
	});
	await load();
}
async function closeShift() {
	if (!currentLog.value) return;
	await closeDutyShift(currentLog.value.logId, {
		summary: form.summary,
		incidents: form.incidents,
		pmsAuditDoneYn: 'Y'
	});
	await load();
}

function shiftLabel(s) { return s === 'DAY' ? t('admin.duty.day') : (s === 'NIGHT' ? t('admin.duty.night') : s); }

onMounted(load);
</script>

<style scoped>
.duty-staff { max-width: 720px; }
.title { margin: 0 0 16px; color: #1a3a6e; font-size: 22px; font-weight: 800; }
.actions { display: flex; gap: 8px; margin-bottom: 16px; }
.btn-start { padding: 10px 18px; border: none; background: #1a3a6e; color: #fff; border-radius: 8px; font-weight: 700; cursor: pointer; font-size: 13px; }
.btn-start.night { background: #1a202c; }

.log-card { background: #fff; border-radius: 10px; padding: 18px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); display: flex; flex-direction: column; gap: 12px; }
.log-card h3 { margin: 0; font-size: 16px; color: #1a3a6e; }
.log-card label { display: flex; flex-direction: column; gap: 4px; }
.lb { font-size: 11px; color: #4a5568; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
.log-card textarea, .log-card input { padding: 9px 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 13px; font-family: inherit; }

.btn-row { display: flex; gap: 8px; margin-top: 6px; }
.btn-row button { padding: 9px 16px; border: 1px solid #cbd5e0; border-radius: 6px; cursor: pointer; font-weight: 700; font-size: 13px; }
.btn-row button.primary { background: #1a3a6e; color: #fff; border-color: #1a3a6e; }
.btn-row button.ghost { background: #fff; color: #4a5568; }

.dim { padding: 32px; text-align: center; color: #a0aec0; background: #fff; border-radius: 10px; }
</style>
