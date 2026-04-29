<template>
	<div class="admin-rental">
		<h2 class="title">{{ t('admin.rental.title') }}</h2>

		<div class="tabs">
			<button :class="['tab', { active: tab === 'orders' }]" @click="tab = 'orders'">{{ t('admin.rental.orders') }}</button>
			<button :class="['tab', { active: tab === 'catalog' }]" @click="tab = 'catalog'">{{ t('admin.rental.catalog') }}</button>
		</div>

		<!-- Orders -->
		<div v-if="tab === 'orders'">
			<div class="filter-row">
				<select v-model="orderFilter.statusCd" @change="loadOrders">
					<option value="">{{ t('admin.common.all') }}</option>
					<option value="REQUESTED">{{ t('rental.status.requested') }}</option>
					<option value="LOANED">{{ t('rental.status.loaned') }}</option>
					<option value="RETURNED">{{ t('rental.status.returned') }}</option>
				</select>
				<button class="btn-refresh" @click="loadOrders">↻</button>
			</div>

			<div v-if="!orders.length" class="dim">{{ t('admin.common.empty') }}</div>
			<table v-else class="tbl">
				<thead>
					<tr>
						<th class="sortable" @click="orderSort.sortBy('requestedAt')">
							{{ t('admin.lf.reportedAt') }} <span class="sort-ind">{{ orderSort.sortIcon('requestedAt') }}</span>
						</th>
						<th class="sortable" @click="orderSort.sortBy('itemId')">
							{{ t('admin.lf.itemName') }} <span class="sort-ind">{{ orderSort.sortIcon('itemId') }}</span>
						</th>
						<th class="sortable" @click="orderSort.sortBy('qty')">
							{{ t('admin.rental.qty') }} <span class="sort-ind">{{ orderSort.sortIcon('qty') }}</span>
						</th>
						<th class="sortable" @click="orderSort.sortBy('statusCd')">
							{{ t('admin.voc.status') }} <span class="sort-ind">{{ orderSort.sortIcon('statusCd') }}</span>
						</th>
						<th></th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="o in sortedOrders" :key="o.orderId">
						<td>{{ fmt(o.requestedAt) }}</td>
						<td>{{ nameOfItem(o.itemId) }}</td>
						<td>{{ o.qty }}</td>
						<td><span :class="['pill', 'st-' + (o.statusCd || '').toLowerCase()]">{{ stLabel(o.statusCd) }}</span></td>
						<td class="actions">
							<button v-if="o.statusCd === 'REQUESTED'" @click="doLoan(o)">{{ t('admin.rental.loan') }}</button>
							<button v-if="o.statusCd === 'LOANED'" @click="doReturn(o)">{{ t('admin.rental.return') }}</button>
						</td>
					</tr>
				</tbody>
			</table>
		</div>

		<!-- Catalog -->
		<div v-else>
			<div class="filter-row">
				<button class="btn-refresh" @click="loadCatalog">↻</button>
				<button class="btn-add" @click="openNew">+ {{ t('admin.common.add') }}</button>
			</div>
			<div v-if="!catalog.length" class="dim">{{ t('admin.common.empty') }}</div>
			<table v-else class="tbl">
				<thead>
					<tr>
						<th class="sortable" @click="catSort.sortBy('name')">
							{{ t('admin.lf.itemName') }} <span class="sort-ind">{{ catSort.sortIcon('name') }}</span>
						</th>
						<th class="sortable" @click="catSort.sortBy('category')">
							{{ t('admin.lf.category') }} <span class="sort-ind">{{ catSort.sortIcon('category') }}</span>
						</th>
						<th class="sortable" @click="catSort.sortBy('stockAvailable')">
							{{ t('admin.rental.stock') }} <span class="sort-ind">{{ catSort.sortIcon('stockAvailable') }}</span>
						</th>
						<th class="sortable" @click="catSort.sortBy('useYn')">
							{{ t('admin.ccs.col.useYn') }} <span class="sort-ind">{{ catSort.sortIcon('useYn') }}</span>
						</th>
						<th></th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="i in sortedCatalog" :key="i.itemId" :class="{ 'row-disabled': i.useYn === 'N' }">
						<td>{{ i.name }}</td>
						<td>{{ catLabel(i.category) }}</td>
						<td>{{ i.stockAvailable }} / {{ i.stockTotal }}</td>
						<td>
							<label class="switch" :title="i.useYn === 'Y' ? t('admin.ccs.on') : t('admin.ccs.off')">
								<input type="checkbox" :checked="i.useYn !== 'N'" :disabled="busy" @change="toggleActive(i, $event.target.checked)" />
								<span class="slider"></span>
							</label>
						</td>
						<td class="actions"><button @click="openEdit(i)">{{ t('admin.common.edit') }}</button></td>
					</tr>
				</tbody>
			</table>
		</div>

		<!-- Edit modal -->
		<div v-if="editing" class="modal-overlay" @click.self="editing = null">
			<div class="modal-card">
				<div class="modal-head">
					<h3>{{ editing.itemId ? t('admin.common.edit') : t('admin.common.add') }}</h3>
					<button class="modal-close" @click="editing = null">✕</button>
				</div>
				<div class="modal-body">
					<label><span class="lbl">{{ t('admin.lf.itemName') }}</span><input v-model="editing.name" /></label>
					<label><span class="lbl">{{ t('admin.lf.category') }}</span>
						<select v-model="editing.category">
							<option value="UMBRELLA">{{ t('rental.cat.umbrella') }}</option>
							<option value="CHARGER">{{ t('rental.cat.charger') }}</option>
							<option value="ADAPTER">{{ t('rental.cat.adapter') }}</option>
							<option value="IRON">{{ t('rental.cat.iron') }}</option>
							<option value="ETC">{{ t('rental.cat.etc') }}</option>
						</select>
					</label>
					<label><span class="lbl">{{ t('lostfound.description') }}</span><textarea v-model="editing.description" rows="2"></textarea></label>
					<label><span class="lbl">{{ t('admin.rental.stock') }} (total)</span><input type="number" v-model.number="editing.stockTotal" /></label>
					<label v-if="!editing.itemId"><span class="lbl">{{ t('admin.rental.stock') }} (avail)</span><input type="number" v-model.number="editing.stockAvailable" /></label>
					<label v-if="editing.itemId" class="check-row">
						<input type="checkbox" :checked="editing.useYn !== 'N'" @change="editing.useYn = ($event.target.checked ? 'Y' : 'N')" />
						<span>{{ t('admin.ccs.on') }} ({{ t('admin.ccs.col.useYn') }})</span>
					</label>
				</div>
				<div class="modal-actions">
					<button class="ghost" @click="editing = null">{{ t('staff.action.cancel') }}</button>
					<button class="primary" @click="save">{{ t('admin.common.save') }}</button>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import {
	fetchRentalCatalog, upsertRentalItem, updateRentalItem,
	fetchRentalOrders, loanRentalOrder, returnRentalOrder
} from '../api/client.js';
import { t } from '../i18n/ui.js';
import { useSortableTable } from '../composables/useSortableTable.js';

const tab = ref('orders');
const catalog = ref([]);
const orders = ref([]);

const orderSort = useSortableTable('requestedAt', 'desc');
const catSort = useSortableTable('name', 'asc');
const sortedOrders = computed(() => orderSort.applySort(orders.value));
const sortedCatalog = computed(() => catSort.applySort(catalog.value));
const orderFilter = reactive({ statusCd: '' });
const editing = ref(null);
const busy = ref(false);

async function loadCatalog() {
	const res = await fetchRentalCatalog();
	catalog.value = res?.map?.list || res?.list || [];
}

async function toggleActive(item, checked) {
	const useYn = checked ? 'Y' : 'N';
	busy.value = true;
	try {
		await updateRentalItem(item.itemId, {
			itemId: item.itemId,
			name: item.name,
			description: item.description,
			category: item.category,
			stockTotal: item.stockTotal,
			useYn
		});
		item.useYn = useYn;
	} catch (e) {
		alert('변경 실패: ' + (e?.message || ''));
		await loadCatalog();
	} finally { busy.value = false; }
}
async function loadOrders() {
	const params = {};
	if (orderFilter.statusCd) params.statusCd = orderFilter.statusCd;
	const res = await fetchRentalOrders(params);
	orders.value = res?.map?.list || res?.list || [];
}

function openNew() { editing.value = { name: '', category: 'UMBRELLA', description: '', stockTotal: 1, stockAvailable: 1, useYn: 'Y' }; }
function openEdit(i) { editing.value = { ...i, useYn: i.useYn || 'Y' }; }

async function save() {
	if (!editing.value) return;
	const body = { ...editing.value };
	if (body.itemId) await updateRentalItem(body.itemId, body);
	else await upsertRentalItem(body);
	editing.value = null;
	await loadCatalog();
}

async function doLoan(o) { await loanRentalOrder(o.orderId); await loadOrders(); }
async function doReturn(o) { await returnRentalOrder(o.orderId); await loadOrders(); await loadCatalog(); }

function nameOfItem(itemId) {
	const i = catalog.value.find(x => x.itemId === itemId);
	return i ? i.name : itemId;
}
function catLabel(c) {
	const map = { UMBRELLA: 'rental.cat.umbrella', CHARGER: 'rental.cat.charger', ADAPTER: 'rental.cat.adapter', IRON: 'rental.cat.iron', ETC: 'rental.cat.etc' };
	return map[c] ? t(map[c]) : c;
}
function stLabel(s) {
	const map = { REQUESTED: 'rental.status.requested', LOANED: 'rental.status.loaned', RETURNED: 'rental.status.returned', LOST: 'rental.status.lost' };
	return map[s] ? t(map[s]) : (s || '');
}
function fmt(d) {
	if (!d) return '';
	try { const x = new Date(d); return `${x.getMonth() + 1}/${x.getDate()} ${String(x.getHours()).padStart(2,'0')}:${String(x.getMinutes()).padStart(2,'0')}`; } catch { return String(d); }
}

onMounted(async () => { await loadCatalog(); await loadOrders(); });
</script>

<style scoped>
.admin-rental { max-width: 1100px; }
.title { margin: 0 0 16px; color: #1a3a6e; font-size: 22px; font-weight: 800; }

.tabs { display: flex; gap: 4px; margin-bottom: 16px; border-bottom: 1px solid #edf2f7; }
.tab { padding: 8px 16px; border: none; background: transparent; color: #8492a6; font-size: 13px; font-weight: 700; cursor: pointer; border-bottom: 2px solid transparent; }
.tab.active { color: #1a3a6e; border-bottom-color: #1a3a6e; }

.filter-row { display: flex; gap: 8px; margin-bottom: 10px; }
.filter-row select { padding: 7px 12px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 13px; background: #fff; }
.btn-refresh, .btn-add { padding: 7px 14px; border: 1px solid #cbd5e0; background: #fff; border-radius: 6px; cursor: pointer; font-size: 13px; }
.btn-add { background: #1a3a6e; color: #fff; border-color: #1a3a6e; }

.tbl { width: 100%; border-collapse: collapse; background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.tbl th, .tbl td { padding: 10px 12px; text-align: left; font-size: 13px; border-bottom: 1px solid #edf2f7; }
.tbl th { background: #f7fafc; color: #4a5568; font-weight: 700; }
.actions { display: flex; gap: 6px; }
.actions button { padding: 5px 10px; border-radius: 5px; border: 1px solid #cbd5e0; background: #f7fafc; font-size: 12px; font-weight: 700; cursor: pointer; }
.dim { padding: 32px; text-align: center; color: #a0aec0; background: #fff; border-radius: 10px; }

.tbl tbody tr.row-disabled td { background: #fafafa; color: #a0aec0; }
.tbl tbody tr.row-disabled td:not(:nth-last-child(-n+2)) { text-decoration: line-through; }

/* iOS 토글 */
.switch { position: relative; display: inline-block; width: 38px; height: 20px; vertical-align: middle; }
.switch input { opacity: 0; width: 0; height: 0; }
.slider { position: absolute; cursor: pointer; inset: 0; background: #cbd5e0; border-radius: 20px; transition: background 0.2s; }
.slider::before { content: ''; position: absolute; height: 14px; width: 14px; left: 3px; bottom: 3px; background: #fff; border-radius: 50%; transition: transform 0.2s; }
.switch input:checked + .slider { background: #48bb78; }
.switch input:checked + .slider::before { transform: translateX(18px); }
.switch input:disabled + .slider { opacity: 0.5; cursor: not-allowed; }

.check-row { flex-direction: row !important; align-items: center; gap: 8px !important; font-size: 13px !important; }
.check-row input { margin: 0; }

.pill { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 11px; font-weight: 700; }
.pill.st-requested { background: #fff4e6; color: #ad6200; }
.pill.st-loaned    { background: #e6f0ff; color: #1a3a6e; }
.pill.st-returned  { background: #e6fff0; color: #276749; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 400; padding: 20px; }
.modal-card { background: #fff; border-radius: 12px; width: 100%; max-width: 480px; overflow: hidden; }
.modal-head { padding: 14px 18px; background: #1a3a6e; color: #fff; display: flex; justify-content: space-between; }
.modal-head h3 { margin: 0; font-size: 15px; }
.modal-close { background: transparent; border: none; color: #fff; font-size: 18px; cursor: pointer; }
.modal-body { padding: 14px 18px; display: flex; flex-direction: column; gap: 10px; }
.modal-body label { display: flex; flex-direction: column; gap: 4px; font-size: 12px; }
.lbl { font-weight: 700; color: #4a5568; text-transform: uppercase; letter-spacing: 0.5px; font-size: 11px; }
.modal-body input, .modal-body select, .modal-body textarea { padding: 8px 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 13px; font-family: inherit; }
.modal-actions { display: flex; justify-content: flex-end; padding: 12px 18px; border-top: 1px solid #edf2f7; gap: 8px; }
.modal-actions button { padding: 7px 14px; border-radius: 6px; border: 1px solid #cbd5e0; cursor: pointer; font-weight: 700; font-size: 13px; }
.modal-actions button.primary { background: #1a3a6e; color: #fff; border-color: #1a3a6e; }
.modal-actions button.ghost { background: #fff; color: #4a5568; }
</style>
