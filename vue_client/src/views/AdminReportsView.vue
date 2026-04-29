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
			<button :class="['tab', { active: tab === 'ai' }]" @click="tab = 'ai'">🤖 AI 인사이트</button>
			<button :class="['tab', { active: tab === 'daily' }]" @click="tab = 'daily'">{{ t('admin.reports.daily') }}</button>
			<button :class="['tab', { active: tab === 'sla' }]" @click="tab = 'sla'">{{ t('admin.reports.sla') }}</button>
			<button :class="['tab', { active: tab === 'heatmap' }]" @click="tab = 'heatmap'">{{ t('admin.reports.heatmap') }}</button>
		</div>

		<!-- AI 인사이트 -->
		<div v-if="tab === 'ai'" class="ai-card">
			<div class="ai-head">
				<div class="ai-title">
					<span class="ai-emoji">🤖</span>
					<div>
						<div class="ai-h1">매니저 일일 AI 리포트</div>
						<div class="ai-h2" v-if="aiReport.date">기준일 {{ aiReport.date }} · {{ aiReport.model || '...' }}</div>
					</div>
				</div>
				<div class="ai-controls">
					<input type="date" v-model="aiDate" class="ai-date" />
					<button class="ai-run" :disabled="aiBusy" @click="loadAiReport">{{ aiBusy ? '분석 중…' : '실행' }}</button>
				</div>
			</div>

			<div v-if="aiReport.stats" class="ai-stats">
				<div class="stat-cell"><div class="stat-num">{{ aiReport.stats.totalTasks }}</div><div class="stat-lbl">전체 태스크</div></div>
				<div class="stat-cell"><div class="stat-num">{{ aiReport.stats.doneTasks }}</div><div class="stat-lbl">완료</div></div>
				<div class="stat-cell"><div class="stat-num">{{ aiReport.stats.vocCount }}</div><div class="stat-lbl">VOC</div></div>
				<div class="stat-cell"><div class="stat-num">{{ aiReport.stats.lostFoundCount }}</div><div class="stat-lbl">분실물</div></div>
				<div class="stat-cell"><div class="stat-num">{{ aiReport.stats.dutyCount }}</div><div class="stat-lbl">당직 로그</div></div>
			</div>

			<div v-if="aiBusy" class="ai-loading">
				<span class="dot" /><span class="dot" /><span class="dot" />
				<small>Claude 가 어제 데이터를 읽고 있습니다…</small>
			</div>

			<pre v-else-if="aiReport.summary" class="ai-summary">{{ aiReport.summary }}</pre>
			<div v-else class="dim">실행 버튼을 눌러 AI 분석을 시작하세요.</div>
		</div>

		<!-- Daily -->
		<div v-if="tab === 'daily'">
			<div v-if="!daily.length" class="dim">{{ t('admin.common.empty') }}</div>
			<table v-else class="tbl">
				<thead>
					<tr>
						<th class="sortable" @click="dailySort.sortBy('dateYmd')">Date <span class="sort-ind">{{ dailySort.sortIcon('dateYmd') }}</span></th>
						<th class="sortable" @click="dailySort.sortBy('deptCd')">Dept <span class="sort-ind">{{ dailySort.sortIcon('deptCd') }}</span></th>
						<th class="sortable" @click="dailySort.sortBy('sourceType')">Source <span class="sort-ind">{{ dailySort.sortIcon('sourceType') }}</span></th>
						<th class="sortable" @click="dailySort.sortBy('requestCount')">Req <span class="sort-ind">{{ dailySort.sortIcon('requestCount') }}</span></th>
						<th class="sortable" @click="dailySort.sortBy('doneCount')">Done <span class="sort-ind">{{ dailySort.sortIcon('doneCount') }}</span></th>
						<th class="sortable" @click="dailySort.sortBy('avgElapsedMin')">Avg(m) <span class="sort-ind">{{ dailySort.sortIcon('avgElapsedMin') }}</span></th>
						<th class="sortable" @click="dailySort.sortBy('slaCompliance')">SLA <span class="sort-ind">{{ dailySort.sortIcon('slaCompliance') }}</span></th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="(r, idx) in sortedDaily" :key="idx">
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
import { ref, reactive, computed, onMounted } from 'vue';
import { useSortableTable } from '../composables/useSortableTable.js';
import { fetchReportDaily, fetchReportSla, fetchReportHeatmap, fetchAdminAiReport } from '../api/client.js';
import { t } from '../i18n/ui.js';
import { useAdminContext } from '../composables/useAdminContext.js';

const tab = ref('ai');
const ctx = useAdminContext();
const aiDate = ref(yesterday());
const aiBusy = ref(false);
const aiReport = ref({});

function yesterday() {
	const d = new Date(); d.setDate(d.getDate() - 1);
	return d.toISOString().slice(0, 10);
}

async function loadAiReport() {
	aiBusy.value = true;
	try {
		const r = await fetchAdminAiReport({
			propCd: ctx.propCd.value,
			cmpxCd: ctx.cmpxCd.value,
			date: aiDate.value
		});
		aiReport.value = r?.map || {};
	} catch (e) {
		aiReport.value = { summary: 'AI 리포트 호출 실패: ' + (e?.response?.data?.message || e?.message || '') };
	} finally {
		aiBusy.value = false;
	}
}
const filter = reactive({ from: '', to: '' });
const daily = ref([]);
const sla = ref([]);
const heatmap = ref([]);
const dailySort = useSortableTable('dateYmd', 'desc');
const sortedDaily = computed(() => dailySort.applySort(daily.value));

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

/* AI 인사이트 카드 */
.ai-card { background: linear-gradient(135deg, #fff 0%, #f0f7ff 100%); border-radius: 14px; padding: 22px 24px; box-shadow: 0 2px 8px rgba(26,58,110,0.08); margin-bottom: 16px; border: 1px solid #d9e8ff; }
.ai-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 18px; flex-wrap: wrap; }
.ai-title { display: flex; gap: 12px; align-items: center; }
.ai-emoji { font-size: 28px; }
.ai-h1 { font-size: 16px; font-weight: 800; color: #1a3a6e; }
.ai-h2 { font-size: 11px; color: #8492a6; font-family: ui-monospace, Menlo, monospace; margin-top: 2px; }
.ai-controls { display: flex; gap: 8px; align-items: center; }
.ai-date { padding: 7px 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 13px; background: #fff; }
.ai-run { padding: 8px 18px; background: #1a3a6e; color: #fff; border: none; border-radius: 6px; font-weight: 700; cursor: pointer; font-size: 13px; }
.ai-run:disabled { opacity: 0.5; cursor: not-allowed; }
.ai-stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 10px; margin-bottom: 16px; }
.stat-cell { background: #fff; border: 1px solid #e6efff; border-radius: 10px; padding: 12px 14px; text-align: center; }
.stat-num { font-size: 24px; font-weight: 800; color: #1a3a6e; line-height: 1.1; }
.stat-lbl { font-size: 11px; color: #8492a6; margin-top: 4px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.4px; }
.ai-loading { display: flex; gap: 8px; align-items: center; padding: 24px; color: #4a5568; font-size: 13px; justify-content: center; }
.ai-loading .dot { width: 8px; height: 8px; border-radius: 50%; background: #1a3a6e; opacity: 0.3; animation: dot-pulse 1.2s infinite; }
.ai-loading .dot:nth-child(2) { animation-delay: 0.2s; }
.ai-loading .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes dot-pulse { 0%, 80%, 100% { opacity: 0.3; transform: scale(0.85); } 40% { opacity: 1; transform: scale(1.1); } }
.ai-summary { background: #fff; border: 1px solid #e6efff; border-radius: 10px; padding: 18px 22px; font-size: 13px; line-height: 1.85; color: #2d3748; white-space: pre-wrap; word-break: break-word; font-family: -apple-system, "Pretendard", "Apple SD Gothic Neo", "Segoe UI", sans-serif; }

.heatmap-wrap { overflow-x: auto; }
.heatmap { border-collapse: collapse; background: #fff; font-size: 11px; }
.heatmap th, .heatmap td { padding: 4px 6px; text-align: center; border: 1px solid #edf2f7; min-width: 24px; }
.heatmap th { background: #f7fafc; font-weight: 700; color: #4a5568; }
.heatmap .dow { background: #f7fafc; font-weight: 700; color: #4a5568; }
.heatmap td { color: #fff; font-weight: 600; }
.dim { padding: 32px; text-align: center; color: #a0aec0; background: #fff; border-radius: 10px; }
</style>
