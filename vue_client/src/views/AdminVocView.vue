<template>
	<div class="admin-voc">
		<h2 class="title">{{ t('admin.voc.title') }}</h2>

		<div class="filter-row">
			<select v-model="filter.statusCd" @change="load">
				<option value="">{{ t('admin.common.all') }} · {{ t('admin.voc.status') }}</option>
				<option value="OPEN">{{ t('admin.voc.st.open') }}</option>
				<option value="IN_PROG">{{ t('admin.voc.st.inProg') }}</option>
				<option value="RESOLVED">{{ t('admin.voc.st.resolved') }}</option>
				<option value="CLOSED">{{ t('admin.voc.st.closed') }}</option>
			</select>
			<select v-model="filter.severity" @change="load">
				<option value="">{{ t('admin.common.all') }} · {{ t('admin.voc.severity') }}</option>
				<option value="URGENT">{{ t('voc.sev.urgent') }}</option>
				<option value="HIGH">{{ t('voc.sev.high') }}</option>
				<option value="NORMAL">{{ t('voc.sev.normal') }}</option>
				<option value="LOW">{{ t('voc.sev.low') }}</option>
			</select>
			<select v-model="filter.category" @change="load">
				<option value="">{{ t('admin.common.all') }} · {{ t('admin.voc.category') }}</option>
				<option value="FACILITY">{{ t('voc.cat.facility') }}</option>
				<option value="CLEAN">{{ t('voc.cat.clean') }}</option>
				<option value="SERVICE">{{ t('voc.cat.service') }}</option>
				<option value="NOISE">{{ t('voc.cat.noise') }}</option>
				<option value="BILLING">{{ t('voc.cat.billing') }}</option>
				<option value="ETC">{{ t('voc.cat.etc') }}</option>
			</select>
			<button class="btn-refresh" @click="load">↻</button>
		</div>

		<div v-if="err" class="err">{{ err }}</div>
		<div v-if="!rows.length && !busy" class="dim">{{ t('admin.common.empty') }}</div>

		<div class="voc-list">
			<div v-for="r in rows" :key="r.vocId" :class="['voc-card', 'sev-' + (r.severity || '').toLowerCase()]">
				<div class="voc-head">
					<span :class="['sev-badge', 'sev-' + (r.severity || 'normal').toLowerCase()]">{{ sevLabel(r.severity) }}</span>
					<span class="cat">{{ catLabel(r.category) }}</span>
					<span :class="['status-pill', 'st-' + (r.statusCd || '').toLowerCase()]">{{ stLabel(r.statusCd) }}</span>
					<span class="time">{{ fmt(r.createdAt) }}</span>
				</div>
				<h4 class="voc-title">{{ r.title }}</h4>
				<p class="voc-content">{{ r.content }}</p>
				<div v-if="r.resolution" class="voc-resolution">
					<span class="res-label">✓ {{ t('admin.voc.action.resolve') }}</span>
					{{ r.resolution }}
				</div>
				<div class="voc-actions">
					<button v-if="r.statusCd === 'OPEN'" @click="transition(r, 'IN_PROG')">{{ t('staff.action.start') }}</button>
					<button v-if="r.statusCd === 'OPEN' || r.statusCd === 'IN_PROG'" class="primary" @click="openResolve(r)">{{ t('admin.voc.action.resolve') }}</button>
					<button v-if="r.statusCd === 'RESOLVED'" class="ghost" @click="transition(r, 'CLOSED')">{{ t('admin.voc.st.closed') }}</button>
				</div>
			</div>
		</div>

		<div v-if="resolving" class="modal-overlay" @click.self="resolving = null">
			<div class="modal-card">
				<div class="modal-head">
					<h3>{{ resolving.title }}</h3>
					<button class="modal-close" @click="resolving = null">✕</button>
				</div>
				<div class="modal-body">
					<textarea v-model="resolutionText" :placeholder="t('admin.voc.resolution.placeholder')" rows="5"></textarea>
				</div>
				<div class="modal-actions">
					<button class="ghost" @click="resolving = null">{{ t('staff.action.cancel') }}</button>
					<button class="primary" @click="doResolve" :disabled="!resolutionText.trim()">{{ t('admin.voc.action.resolve') }}</button>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchVocList, updateVocStatus, resolveVoc } from '../api/client.js';
import { t } from '../i18n/ui.js';

const rows = ref([]);
const filter = reactive({ statusCd: '', severity: '', category: '' });
const busy = ref(false);
const err = ref('');
const resolving = ref(null);
const resolutionText = ref('');

async function load() {
	busy.value = true; err.value = '';
	try {
		const params = {};
		if (filter.statusCd) params.statusCd = filter.statusCd;
		if (filter.severity) params.severity = filter.severity;
		if (filter.category) params.category = filter.category;
		const res = await fetchVocList(params);
		rows.value = res?.map?.list || res?.list || [];
	} catch (e) { err.value = e?.message || 'load error'; }
	finally { busy.value = false; }
}

async function transition(row, statusCd) {
	try { await updateVocStatus(row.vocId, { statusCd }); await load(); }
	catch (e) { err.value = e?.message || 'update error'; }
}

function openResolve(row) {
	resolving.value = row;
	resolutionText.value = '';
}

async function doResolve() {
	if (!resolving.value) return;
	try {
		await resolveVoc(resolving.value.vocId, { resolution: resolutionText.value.trim() });
		resolving.value = null;
		resolutionText.value = '';
		await load();
	} catch (e) { err.value = e?.message || 'resolve error'; }
}

function catLabel(c) {
	const map = {
		FACILITY: 'voc.cat.facility', CLEAN: 'voc.cat.clean', SERVICE: 'voc.cat.service',
		NOISE: 'voc.cat.noise', BILLING: 'voc.cat.billing', ETC: 'voc.cat.etc'
	};
	return map[c] ? t(map[c]) : c;
}
function sevLabel(s) {
	const map = { LOW: 'voc.sev.low', NORMAL: 'voc.sev.normal', HIGH: 'voc.sev.high', URGENT: 'voc.sev.urgent' };
	return map[s] ? t(map[s]) : (s || '');
}
function stLabel(s) {
	const map = {
		OPEN: 'admin.voc.st.open', IN_PROG: 'admin.voc.st.inProg',
		RESOLVED: 'admin.voc.st.resolved', CLOSED: 'admin.voc.st.closed'
	};
	return map[s] ? t(map[s]) : (s || '');
}
function fmt(d) {
	if (!d) return '';
	try {
		const x = new Date(d);
		return `${x.getMonth() + 1}/${x.getDate()} ${String(x.getHours()).padStart(2,'0')}:${String(x.getMinutes()).padStart(2,'0')}`;
	} catch { return String(d); }
}

onMounted(load);
</script>

<style scoped>
.admin-voc { max-width: 900px; }
.title { margin: 0 0 16px; color: #1a3a6e; font-size: 22px; font-weight: 800; }

.filter-row { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; align-items: center; }
.filter-row select {
	padding: 8px 12px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 13px; background: #fff;
}
.btn-refresh { padding: 8px 14px; border: 1px solid #cbd5e0; background: #fff; border-radius: 6px; cursor: pointer; }
.err { background: #fff5f5; color: #c53030; padding: 10px 12px; border-radius: 6px; }
.dim { color: #a0aec0; padding: 32px; text-align: center; background: #fff; border-radius: 10px; }

.voc-list { display: flex; flex-direction: column; gap: 10px; }
.voc-card {
	background: #fff;
	border-radius: 10px;
	padding: 14px 16px;
	box-shadow: 0 1px 3px rgba(0,0,0,0.06);
	border-left: 4px solid #cbd5e0;
}
.voc-card.sev-urgent { border-left-color: #e53e3e; background: #fff5f5; }
.voc-card.sev-high   { border-left-color: #ed8936; }
.voc-card.sev-low    { border-left-color: #a0aec0; }

.voc-head { display: flex; gap: 8px; align-items: center; margin-bottom: 6px; flex-wrap: wrap; }
.sev-badge {
	padding: 2px 10px;
	border-radius: 999px;
	font-size: 11px;
	font-weight: 700;
	background: #edf2f7;
	color: #4a5568;
}
.sev-badge.sev-urgent { background: #fed7d7; color: #c53030; }
.sev-badge.sev-high   { background: #feebc8; color: #c05621; }
.sev-badge.sev-low    { background: #edf2f7; color: #718096; }

.cat { padding: 2px 8px; border-radius: 999px; font-size: 11px; background: #e6f0ff; color: #1a3a6e; font-weight: 600; }
.status-pill { padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 700; }
.status-pill.st-open     { background: #fff4e6; color: #ad6200; }
.status-pill.st-in_prog  { background: #e6f0ff; color: #1a3a6e; }
.status-pill.st-resolved { background: #e6fff0; color: #276749; }
.status-pill.st-closed   { background: #f7fafc; color: #8492a6; }
.time { margin-left: auto; font-size: 11px; color: #8492a6; }

.voc-title { margin: 4px 0; font-size: 15px; color: #1a3a6e; font-weight: 700; }
.voc-content { margin: 4px 0 8px; font-size: 13px; color: #4a5568; white-space: pre-wrap; }
.voc-resolution { padding: 8px 10px; background: #f0fff4; border-radius: 6px; font-size: 12px; color: #276749; margin-top: 6px; }
.res-label { display: block; font-weight: 700; margin-bottom: 3px; }

.voc-actions { display: flex; gap: 6px; margin-top: 10px; }
.voc-actions button {
	padding: 6px 12px;
	border-radius: 6px;
	border: 1px solid #cbd5e0;
	background: #f7fafc;
	color: #4a5568;
	font-size: 12px;
	font-weight: 700;
	cursor: pointer;
}
.voc-actions button.primary { background: #1a3a6e; color: #fff; border-color: #1a3a6e; }
.voc-actions button.ghost   { background: #fff; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 400; padding: 20px; }
.modal-card { background: #fff; border-radius: 12px; width: 100%; max-width: 520px; overflow: hidden; }
.modal-head { padding: 16px 20px; background: #1a3a6e; color: #fff; display: flex; justify-content: space-between; }
.modal-head h3 { margin: 0; font-size: 15px; }
.modal-close { background: transparent; border: none; color: #fff; font-size: 18px; cursor: pointer; }
.modal-body { padding: 16px 20px; }
.modal-body textarea { width: 100%; padding: 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-family: inherit; font-size: 13px; box-sizing: border-box; }
.modal-actions { display: flex; justify-content: flex-end; padding: 12px 20px; border-top: 1px solid #edf2f7; gap: 8px; }
.modal-actions button {
	padding: 7px 14px; border-radius: 6px; border: 1px solid #cbd5e0; cursor: pointer; font-weight: 700; font-size: 13px;
}
.modal-actions button.primary { background: #1a3a6e; color: #fff; border-color: #1a3a6e; }
.modal-actions button.primary:disabled { opacity: 0.4; cursor: not-allowed; }
.modal-actions button.ghost { background: #fff; color: #4a5568; }
</style>
