<template>
	<div class="admin-reports">
		<h2 class="title">{{ t('admin.reports.title') }}</h2>

		<div class="filter-bar">
			<label>
				<span class="lbl">{{ t('admin.reports.from') }}</span>
				<input type="date" v-model="filter.from" @change="reload" />
			</label>
			<label>
				<span class="lbl">{{ t('admin.reports.to') }}</span>
				<input type="date" v-model="filter.to" @change="reload" />
			</label>
			<button class="btn-csv" @click="exportCsv">{{ t('admin.common.export') }}</button>
		</div>

		<div class="tabs">
			<button :class="['tab', { active: tab === 'daily' }]" @click="tab = 'daily'">{{ t('admin.reports.daily') }}</button>
			<button :class="['tab', { active: tab === 'sla' }]" @click="tab = 'sla'">{{ t('admin.reports.sla') }}</button>
			<button :class="['tab', { active: tab === 'heatmap' }]" @click="tab = 'heatmap'">{{ t('admin.reports.heatmap') }}</button>
		</div>

		<!-- Daily -->
		<div v-if="tab === 'daily'">
			<div v-if="!daily.length" class="dim">{{ t('admin.common.empty') }}</div>
			<table v-else class="tbl">
				<thead>
					<tr><th>Date</th><th>Dept</th><th>Source</th><th>Req</th><th>Done</th><th>Avg(m)</th><th>SLA</th></tr>
				</thead>
				<tbody>
					<tr v-for="(r, idx) in daily" :key="idx">
						<td>{{ r.dateYmd }}</td>
						<td>{{ r.deptCd || '—' }}</td>
						<td>{{ r.sourceType || '—' }}</td>
						<td>{{ r.requestCount }}</td>
						<td>{{ r.doneCount }}</td>
						<td>{{ fmtNum(r.avgElapsedMin) }}</td>
						<td><span :class="['sla-badge', r.slaCompliance === 'MET' ? 'sla-met' : 'sla-miss']">{{ r.slaCompliance || '—' }} / {{ r.slaMin }}m</span></td>
					</tr>
				</tbody>
			</table>
		</div>

		<!-- SLA -->
		<div v-if="tab === 'sla'">
			<div v-if="!sla.length" class="dim">{{ t('admin.common.empty') }}</div>
			<div v-else class="sla-grid">
				<div v-for="(r, idx) in sla" :key="idx" class="sla-card">
					<div class="sla-head"><strong>{{ r.deptCd }}</strong> · {{ r.sourceType }}</div>
					<div class="bar-row">
						<div class="bar-track">
							<div class="bar-fill" :style="{ width: (r.completionRate || 0) + '%', background: (r.slaMet ? '#276749' : '#c53030') }"></div>
						</div>
						<span class="rate">{{ r.completionRate }}%</span>
					</div>
					<div class="meta">
						<span>Total: {{ r.totalCount }}</span>
						<span>Avg: {{ fmtNum(r.avgElapsedMin) }}m / SLA: {{ r.slaMin }}m</span>
					</div>
				</div>
			</div>
		</div>

		<!-- Heatmap -->
		<div v-if="tab === 'heatmap'" class="heatmap-wrap">
			<div v-if="!heatmap.length" class="dim">{{ t('admin.common.empty') }}</div>
			<table v-else class="heatmap">
				<thead>
					<tr>
						<th></th>
						<th v-for="h in 24" :key="h">{{ h - 1 }}</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="(d, di) in DOWS" :key="di">
						<td class="dow">{{ d }}</td>
						<td v-for="h in 24" :key="h" :style="{ background: cellColor(di + 1, h - 1) }" :title="cellCount(di + 1, h - 1) + ' requests'">
							{{ cellCount(di + 1, h - 1) || '' }}
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	</div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchReportDaily, fetchReportSla, fetchReportHeatmap } from '../api/client.js';
import { t } from '../i18n/ui.js';

const tab = ref('daily');
const filter = reactive({ from: '', to: '' });
const daily = ref([]);
const sla = ref([]);
const heatmap = ref([]);

const DOWS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

function params() {
	const p = {};
	if (filter.from) p.from = filter.from + ' 00:00:00';
	if (filter.to) p.to = filter.to + ' 23:59:59';
	return p;
}

async function loadDaily() { const r = await fetchReportDaily(params()); daily.value = r?.map?.list || r?.list || []; }
async function loadSla() { const r = await fetchReportSla(params()); sla.value = r?.map?.list || r?.list || []; }
async function loadHeatmap() { const r = await fetchReportHeatmap(params()); heatmap.value = r?.map?.list || r?.list || []; }

async function reload() {
	await loadDaily(); await loadSla(); await loadHeatmap();
}

function cellCount(dow, hour) {
	const r = heatmap.value.find(x => Number(x.dow) === dow && Number(x.hour) === hour);
	return r ? Number(r.cnt) : 0;
}
function cellColor(dow, hour) {
	const c = cellCount(dow, hour);
	if (!c) return '#f7fafc';
	const max = Math.max(...heatmap.value.map(x => Number(x.cnt)), 1);
	const ratio = Math.min(1, c / max);
	const a = 0.15 + ratio * 0.75;
	return `rgba(26, 58, 110, ${a})`;
}

function fmtNum(n) {
	if (n == null) return '—';
	return Math.round(Number(n) * 10) / 10;
}

function exportCsv() {
	const list = tab.value === 'daily' ? daily.value : (tab.value === 'sla' ? sla.value : heatmap.value);
	if (!list.length) return;
	const keys = Object.keys(list[0]);
	const header = keys.join(',');
	const rows = list.map(r => keys.map(k => JSON.stringify(r[k] ?? '')).join(','));
	const csv = [header, ...rows].join('\n');
	const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
	const url = URL.createObjectURL(blob);
	const a = document.createElement('a');
	a.href = url; a.download = `ccs-report-${tab.value}-${new Date().toISOString().slice(0,10)}.csv`;
	a.click();
	URL.revokeObjectURL(url);
}

onMounted(reload);
</script>

<style scoped>
.admin-reports { max-width: 1200px; }
.title { margin: 0 0 16px; color: #1a3a6e; font-size: 22px; font-weight: 800; }

.filter-bar { display: flex; gap: 12px; align-items: flex-end; margin-bottom: 14px; }
.filter-bar label { display: flex; flex-direction: column; gap: 4px; font-size: 12px; color: #4a5568; }
.filter-bar input { padding: 7px 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 13px; }
.btn-csv { padding: 8px 14px; border: 1px solid #1a3a6e; background: #fff; color: #1a3a6e; border-radius: 6px; font-weight: 700; cursor: pointer; font-size: 12px; }
.lbl { font-weight: 700; font-size: 11px; text-transform: uppercase; color: #8492a6; }

.tabs { display: flex; gap: 4px; margin-bottom: 12px; border-bottom: 1px solid #edf2f7; }
.tab { padding: 8px 16px; border: none; background: transparent; color: #8492a6; font-size: 13px; font-weight: 700; cursor: pointer; border-bottom: 2px solid transparent; }
.tab.active { color: #1a3a6e; border-bottom-color: #1a3a6e; }

.tbl { width: 100%; border-collapse: collapse; background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.tbl th, .tbl td { padding: 8px 10px; text-align: left; font-size: 12px; border-bottom: 1px solid #edf2f7; }
.tbl th { background: #f7fafc; font-weight: 700; color: #4a5568; }

.sla-badge { padding: 2px 10px; border-radius: 999px; font-size: 11px; font-weight: 700; }
.sla-met  { background: #e6fff0; color: #276749; }
.sla-miss { background: #fff5f5; color: #c53030; }

.sla-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 12px; }
.sla-card { background: #fff; padding: 14px 16px; border-radius: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.sla-head { font-size: 13px; color: #4a5568; margin-bottom: 10px; }
.bar-row { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.bar-track { flex: 1; height: 12px; background: #edf2f7; border-radius: 999px; overflow: hidden; }
.bar-fill { height: 100%; transition: width 0.3s; }
.rate { font-weight: 700; color: #1a3a6e; }
.meta { display: flex; justify-content: space-between; font-size: 11px; color: #8492a6; }

.heatmap-wrap { overflow-x: auto; }
.heatmap { border-collapse: collapse; background: #fff; font-size: 11px; }
.heatmap th, .heatmap td { padding: 4px 6px; text-align: center; border: 1px solid #edf2f7; min-width: 24px; }
.heatmap th { background: #f7fafc; font-weight: 700; color: #4a5568; }
.heatmap .dow { background: #f7fafc; font-weight: 700; color: #4a5568; }
.heatmap td { color: #fff; font-weight: 600; }
.dim { padding: 32px; text-align: center; color: #a0aec0; background: #fff; border-radius: 10px; }
</style>
