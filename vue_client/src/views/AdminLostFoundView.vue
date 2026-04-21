<template>
	<div class="admin-lf">
		<h2 class="title">{{ t('admin.lf.title') }}</h2>

		<div class="filter-row">
			<select v-model="filter.statusCd" @change="load">
				<option value="">{{ t('admin.common.all') }} · {{ t('admin.lf.status') }}</option>
				<option value="REPORTED">{{ t('admin.lf.st.reported') }}</option>
				<option value="FOUND">{{ t('admin.lf.st.found') }}</option>
				<option value="MATCHED">{{ t('admin.lf.st.matched') }}</option>
				<option value="RETURNED">{{ t('admin.lf.st.returned') }}</option>
				<option value="DISPOSED">{{ t('admin.lf.st.disposed') }}</option>
			</select>
			<select v-model="filter.category" @change="load">
				<option value="">{{ t('admin.common.all') }} · {{ t('admin.lf.category') }}</option>
				<option value="WALLET">{{ t('lostfound.cat.wallet') }}</option>
				<option value="PHONE">{{ t('lostfound.cat.phone') }}</option>
				<option value="CLOTHING">{{ t('lostfound.cat.clothing') }}</option>
				<option value="ACCESSORY">{{ t('lostfound.cat.accessory') }}</option>
				<option value="DOCUMENT">{{ t('lostfound.cat.document') }}</option>
				<option value="ETC">{{ t('lostfound.cat.etc') }}</option>
			</select>
			<button class="btn-refresh" @click="load">↻</button>
		</div>

		<div v-if="err" class="err">{{ err }}</div>
		<div v-if="!rows.length && !busy" class="dim">{{ t('admin.common.empty') }}</div>

		<table v-if="rows.length" class="lf-table">
			<thead>
				<tr>
					<th>{{ t('admin.lf.reportedAt') }}</th>
					<th>{{ t('admin.lf.category') }}</th>
					<th>{{ t('admin.lf.itemName') }}</th>
					<th>{{ t('admin.lf.location') }}</th>
					<th>{{ t('admin.lf.reporter') }}</th>
					<th>{{ t('admin.lf.status') }}</th>
					<th></th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="r in rows" :key="r.lfId">
					<td>{{ fmt(r.createdAt) }}</td>
					<td>{{ catLabel(r.category) }}</td>
					<td class="item-cell" @click="detail = r">
						<strong>{{ r.itemName }}</strong>
						<span class="desc" v-if="r.description">{{ r.description }}</span>
					</td>
					<td>{{ r.locationHint || '—' }}</td>
					<td>{{ (r.reporterType || '-') + (r.reporterRef ? ' · ' + r.reporterRef : '') }}</td>
					<td><span :class="['pill', 'st-' + (r.statusCd || '').toLowerCase()]">{{ stLabel(r.statusCd) }}</span></td>
					<td class="actions">
						<button v-if="r.statusCd === 'REPORTED'" @click="transition(r, 'FOUND')">{{ t('admin.lf.action.found') }}</button>
						<button v-if="r.statusCd === 'FOUND' || r.statusCd === 'MATCHED'" @click="transition(r, 'RETURNED')">{{ t('admin.lf.action.return') }}</button>
						<button class="warn" v-if="r.statusCd !== 'DISPOSED' && r.statusCd !== 'RETURNED'" @click="transition(r, 'DISPOSED')">{{ t('admin.lf.action.dispose') }}</button>
					</td>
				</tr>
			</tbody>
		</table>

		<div v-if="detail" class="modal-overlay" @click.self="detail = null">
			<div class="modal-card">
				<div class="modal-head">
					<h3>{{ detail.itemName }}</h3>
					<button class="modal-close" @click="detail = null">✕</button>
				</div>
				<div class="modal-body">
					<div class="row"><span class="lbl">{{ t('admin.lf.category') }}</span><span>{{ catLabel(detail.category) }}</span></div>
					<div class="row"><span class="lbl">{{ t('admin.lf.status') }}</span><span :class="['pill', 'st-' + (detail.statusCd || '').toLowerCase()]">{{ stLabel(detail.statusCd) }}</span></div>
					<div class="row"><span class="lbl">{{ t('admin.lf.location') }}</span><span>{{ detail.locationHint || '—' }}</span></div>
					<div class="row" v-if="detail.description"><span class="lbl">{{ t('lostfound.description') }}</span><span>{{ detail.description }}</span></div>
					<div class="row" v-if="detail.reporterType"><span class="lbl">{{ t('admin.lf.reporter') }}</span><span>{{ detail.reporterType }} · {{ detail.reporterRef || '—' }}</span></div>
					<div class="row" v-if="detail.rmNo"><span class="lbl">{{ t('staff.detail.room') }}</span><span>{{ detail.rmNo }}</span></div>
					<div class="row"><span class="lbl">{{ t('admin.lf.reportedAt') }}</span><span>{{ fmt(detail.createdAt) }}</span></div>
					<div class="row" v-if="detail.handlerId"><span class="lbl">{{ t('admin.lf.handler') }}</span><span>{{ detail.handlerId }}</span></div>
				</div>
				<div class="modal-actions">
					<button class="ghost" @click="detail = null">{{ t('staff.action.close') }}</button>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchLostFoundList, updateLostFoundStatus } from '../api/client.js';
import { t } from '../i18n/ui.js';

const rows = ref([]);
const filter = reactive({ statusCd: '', category: '' });
const busy = ref(false);
const err = ref('');
const detail = ref(null);

async function load() {
	busy.value = true; err.value = '';
	try {
		const res = await fetchLostFoundList({
			statusCd: filter.statusCd || undefined,
			category: filter.category || undefined
		});
		rows.value = res?.map?.list || res?.list || [];
	} catch (e) {
		err.value = e?.message || 'load error';
	} finally {
		busy.value = false;
	}
}

async function transition(row, statusCd) {
	try {
		await updateLostFoundStatus(row.lfId, { statusCd });
		await load();
	} catch (e) {
		err.value = e?.message || 'update error';
	}
}

function catLabel(c) {
	const map = {
		WALLET: 'lostfound.cat.wallet', PHONE: 'lostfound.cat.phone',
		CLOTHING: 'lostfound.cat.clothing', ACCESSORY: 'lostfound.cat.accessory',
		DOCUMENT: 'lostfound.cat.document', ETC: 'lostfound.cat.etc'
	};
	return map[c] ? t(map[c]) : c;
}

function stLabel(s) {
	const map = {
		REPORTED: 'admin.lf.st.reported', FOUND: 'admin.lf.st.found',
		MATCHED: 'admin.lf.st.matched', RETURNED: 'admin.lf.st.returned',
		DISPOSED: 'admin.lf.st.disposed'
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
.admin-lf { max-width: 1100px; }
.title { margin: 0 0 16px; color: #1a3a6e; font-size: 22px; font-weight: 800; }
.filter-row { display: flex; gap: 8px; margin-bottom: 12px; align-items: center; }
.filter-row select {
	padding: 8px 12px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 13px; background: #fff;
}
.btn-refresh {
	padding: 8px 14px; border: 1px solid #cbd5e0; background: #fff; border-radius: 6px; cursor: pointer;
}
.err { background: #fff5f5; color: #c53030; padding: 10px 12px; border-radius: 6px; margin-bottom: 10px; }
.dim { color: #a0aec0; padding: 32px; text-align: center; background: #fff; border-radius: 10px; }

.lf-table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.lf-table th, .lf-table td { padding: 10px 12px; text-align: left; font-size: 13px; border-bottom: 1px solid #edf2f7; }
.lf-table th { background: #f7fafc; color: #4a5568; font-weight: 700; }
.item-cell { cursor: pointer; }
.item-cell strong { color: #1a3a6e; display: block; }
.item-cell .desc { color: #8492a6; font-size: 11px; display: block; margin-top: 2px; }
.actions { display: flex; gap: 6px; }
.actions button { padding: 5px 10px; border-radius: 5px; border: 1px solid #cbd5e0; background: #f7fafc; font-size: 12px; font-weight: 700; cursor: pointer; }
.actions button.warn { color: #c53030; border-color: #feb2b2; }

.pill { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 11px; font-weight: 700; }
.pill.st-reported  { background: #fff4e6; color: #ad6200; }
.pill.st-found     { background: #e6f0ff; color: #1a3a6e; }
.pill.st-matched   { background: #e6fff0; color: #276749; }
.pill.st-returned  { background: #ebf4ff; color: #1a3a6e; }
.pill.st-disposed  { background: #f7fafc; color: #8492a6; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 400; padding: 20px; }
.modal-card { background: #fff; border-radius: 12px; width: 100%; max-width: 480px; overflow: hidden; }
.modal-head { padding: 16px 20px; background: #1a3a6e; color: #fff; display: flex; justify-content: space-between; align-items: center; }
.modal-head h3 { margin: 0; font-size: 15px; }
.modal-close { background: transparent; border: none; color: #fff; font-size: 18px; cursor: pointer; }
.modal-body { padding: 16px 20px; display: flex; flex-direction: column; gap: 10px; }
.row { display: flex; gap: 12px; font-size: 13px; }
.lbl { min-width: 72px; color: #8492a6; font-size: 11px; font-weight: 700; text-transform: uppercase; padding-top: 2px; }
.modal-actions { display: flex; justify-content: flex-end; padding: 12px 20px; border-top: 1px solid #edf2f7; gap: 8px; }
.modal-actions button.ghost { padding: 7px 14px; border: 1px solid #cbd5e0; background: #fff; border-radius: 6px; cursor: pointer; }

@media (max-width: 720px) {
	.lf-table th, .lf-table td { padding: 8px; font-size: 12px; }
	.lf-table th:nth-child(3), .lf-table td:nth-child(3) { min-width: 120px; }
}
</style>
