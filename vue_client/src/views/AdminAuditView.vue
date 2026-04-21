<template>
	<div class="admin-audit">
		<h2 class="title">{{ t('admin.audit.title') }}</h2>

		<div class="filter-bar">
			<select v-model="filter.domain" @change="load">
				<option value="">{{ t('admin.common.all') }} · {{ t('admin.audit.domain') }}</option>
				<option value="TASK">TASK</option>
				<option value="LOSTFOUND">LOSTFOUND</option>
				<option value="VOC">VOC</option>
				<option value="RENTAL">RENTAL</option>
				<option value="DUTY">DUTY</option>
				<option value="FEATURE">FEATURE</option>
			</select>
			<select v-model="filter.action" @change="load">
				<option value="">{{ t('admin.common.all') }} · {{ t('admin.audit.action') }}</option>
				<option value="CREATE">CREATE</option>
				<option value="UPDATE">UPDATE</option>
				<option value="DELETE">DELETE</option>
				<option value="STATUS_CHANGE">STATUS_CHANGE</option>
				<option value="LOGIN">LOGIN</option>
			</select>
			<input type="text" v-model="filter.actorId" @keyup.enter="load" :placeholder="t('admin.audit.actor')" />
			<button class="btn-refresh" @click="load">↻</button>
		</div>

		<div v-if="!rows.length" class="dim">{{ t('admin.common.empty') }}</div>
		<div v-else class="audit-list">
			<div v-for="r in rows" :key="r.auditId" class="audit-row">
				<div class="row-head">
					<span :class="['action-pill', 'act-' + (r.action || '').toLowerCase().replace('_','-')]">{{ r.action }}</span>
					<span class="domain">{{ r.domain }}</span>
					<span class="entity" v-if="r.entityId">#{{ r.entityId }}</span>
					<span class="actor">{{ r.actorType }}: {{ r.actorId || '—' }}</span>
					<span class="time">{{ fmt(r.createdAt) }}</span>
					<button class="expand" @click="toggle(r.auditId)">{{ open[r.auditId] ? '▼' : '▶' }}</button>
				</div>
				<div v-if="open[r.auditId]" class="diff">
					<div class="diff-col">
						<h4>{{ t('admin.audit.before') }}</h4>
						<pre>{{ pretty(r.beforeJson) }}</pre>
					</div>
					<div class="diff-col">
						<h4>{{ t('admin.audit.after') }}</h4>
						<pre>{{ pretty(r.afterJson) }}</pre>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchAuditLog } from '../api/client.js';
import { t } from '../i18n/ui.js';

const rows = ref([]);
const filter = reactive({ domain: '', action: '', actorId: '' });
const open = reactive({});

function toggle(id) { open[id] = !open[id]; }

async function load() {
	const params = {};
	if (filter.domain) params.domain = filter.domain;
	if (filter.action) params.action = filter.action;
	if (filter.actorId) params.actorId = filter.actorId;
	const r = await fetchAuditLog(params);
	rows.value = r?.map?.list || r?.list || [];
}

function pretty(json) {
	if (!json) return '—';
	try { return JSON.stringify(JSON.parse(json), null, 2); }
	catch { return String(json); }
}

function fmt(d) {
	if (!d) return '';
	try { const x = new Date(d); return `${x.getFullYear()}-${String(x.getMonth()+1).padStart(2,'0')}-${String(x.getDate()).padStart(2,'0')} ${String(x.getHours()).padStart(2,'0')}:${String(x.getMinutes()).padStart(2,'0')}:${String(x.getSeconds()).padStart(2,'0')}`; } catch { return String(d); }
}

onMounted(load);
</script>

<style scoped>
.admin-audit { max-width: 1100px; }
.title { margin: 0 0 16px; color: #1a3a6e; font-size: 22px; font-weight: 800; }
.filter-bar { display: flex; gap: 8px; margin-bottom: 12px; align-items: center; flex-wrap: wrap; }
.filter-bar select, .filter-bar input { padding: 7px 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 13px; background: #fff; }
.btn-refresh { padding: 7px 12px; border: 1px solid #cbd5e0; background: #fff; border-radius: 6px; cursor: pointer; }

.audit-list { display: flex; flex-direction: column; gap: 6px; }
.audit-row { background: #fff; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); overflow: hidden; }
.row-head { display: flex; align-items: center; gap: 12px; padding: 10px 14px; font-size: 12px; }
.action-pill { padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 700; }
.act-create        { background: #e6fff0; color: #276749; }
.act-update        { background: #e6f0ff; color: #1a3a6e; }
.act-delete        { background: #fff5f5; color: #c53030; }
.act-status-change { background: #fff4e6; color: #ad6200; }
.act-login         { background: #f7fafc; color: #4a5568; }
.domain { font-weight: 700; color: #1a3a6e; }
.entity { color: #8492a6; font-family: ui-monospace, monospace; }
.actor { color: #4a5568; }
.time { margin-left: auto; color: #8492a6; }
.expand { background: transparent; border: none; color: #8492a6; font-size: 14px; cursor: pointer; }

.diff { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding: 10px 14px 14px; border-top: 1px solid #edf2f7; }
.diff-col h4 { margin: 0 0 6px; font-size: 11px; color: #8492a6; text-transform: uppercase; font-weight: 700; letter-spacing: 0.8px; }
.diff pre { margin: 0; padding: 8px 10px; background: #f7fafc; border-radius: 6px; font-size: 11px; overflow: auto; max-height: 300px; white-space: pre-wrap; word-break: break-all; }

.dim { padding: 32px; text-align: center; color: #a0aec0; background: #fff; border-radius: 10px; }
</style>
