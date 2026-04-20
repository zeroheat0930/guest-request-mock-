<template>
	<div class="history">
		<div class="page-header">
			<h2 class="page-title">{{ t('history.title') }}</h2>
			<p class="page-sub">{{ t('history.sub') }}</p>
		</div>

		<div class="guest-bar">
			<span class="guest-bar__room">{{ t('guest.room.label', roomNo) }}</span>
		</div>

		<div v-if="loading" class="empty-state">
			<LoadingSpinner :text="t('history.loading')" />
		</div>

		<div v-else-if="items.length === 0" class="empty-state">
			<div class="empty-icon">📋</div>
			<div class="empty-text">{{ t('history.empty') }}</div>
		</div>

		<div v-else class="timeline">
			<div v-for="item in items" :key="item.reqNo" class="timeline-item">
				<div class="timeline-icon">{{ typeIcon(item.type) }}</div>
				<div class="timeline-body">
					<div class="timeline-head">
						<span class="timeline-type">{{ typeLabel(item.type) }}</span>
						<span class="status-badge" :class="statusClass(item.statCd)">{{ statusLabel(item.statCd) }}</span>
					</div>
					<div class="timeline-title">{{ item.title }}</div>
					<div v-if="item.reqDt" class="timeline-date">{{ formatDate(item.reqDt) }}</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { API_BASE } from '../api/client.js';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import { t } from '../i18n/ui.js';

const rsvNo = ref(sessionStorage.getItem('concierge.rsvNo') || '');
const roomNo = ref(sessionStorage.getItem('concierge.roomNo') || '');
const loading = ref(false);
const items = ref([]);

function authHeader() {
	try {
		const token = sessionStorage.getItem('concierge.jwt');
		return token ? { Authorization: `Bearer ${token}` } : {};
	} catch {
		return {};
	}
}

onMounted(async () => {
	loading.value = true;
	try {
		const combined = [];

		// Amenity requests
		try {
			const res = await axios.get(`${API_BASE}/gr/amenity/list`, {
				params: { rsvNo: rsvNo.value },
				headers: authHeader()
			});
			const list = res.data?.list || [];
			for (const row of list) {
				combined.push({
					reqNo: row.reqNo || ('AM' + Math.random()),
					type: 'AMENITY',
					title: row.itemNm || row.memo || t('amenity.title'),
					statCd: row.procStatCd || 'REQ',
					reqDt: row.regDt || row.createdAt || null
				});
			}
		} catch {}

		// Parking registrations
		try {
			const res = await axios.get(`${API_BASE}/gr/parking/list`, {
				params: { rsvNo: rsvNo.value },
				headers: authHeader()
			});
			const list = res.data?.list || [];
			for (const row of list) {
				combined.push({
					reqNo: row.reqNo || ('PK' + Math.random()),
					type: 'PARKING',
					title: row.carNo ? `${row.carNo}${row.carTp ? ' · ' + row.carTp : ''}` : t('park.title'),
					statCd: row.procStatCd || 'REQ',
					reqDt: row.regDt || row.createdAt || null
				});
			}
		} catch {}

		// Sort by date descending (nulls last)
		combined.sort((a, b) => {
			if (!a.reqDt && !b.reqDt) return 0;
			if (!a.reqDt) return 1;
			if (!b.reqDt) return -1;
			return new Date(b.reqDt) - new Date(a.reqDt);
		});

		items.value = combined;
	} finally {
		loading.value = false;
	}
});

function typeIcon(type) {
	const map = { AMENITY: '🛎️', HK: '🧹', PARKING: '🚗', LATE_CO: '⏰' };
	return map[type] || '📋';
}

function typeLabel(type) {
	const map = { AMENITY: t('history.type.amenity'), HK: t('history.type.hk'), PARKING: t('history.type.parking'), LATE_CO: t('history.type.lateco') };
	return map[type] || type;
}

function statusLabel(statCd) {
	const map = { REQ: t('history.stat.req'), IN_PROG: t('history.stat.inprog'), DONE: t('history.stat.done') };
	return map[statCd] || statCd;
}

function statusClass(statCd) {
	if (statCd === 'DONE') return 'badge--done';
	if (statCd === 'IN_PROG') return 'badge--prog';
	return 'badge--req';
}

function formatDate(dt) {
	if (!dt) return '';
	try {
		return new Date(dt).toLocaleString();
	} catch {
		return dt;
	}
}
</script>

<style scoped>
.history {
	max-width: 640px;
}

/* ── Page Header ── */
.page-header {
	margin-bottom: var(--sp-6);
}
.page-title {
	font-family: 'Georgia', 'Times New Roman', serif;
	font-size: var(--fs-2xl);
	font-weight: 400;
	color: var(--c-text);
	letter-spacing: -0.3px;
	line-height: 1.25;
	margin: 0 0 var(--sp-1) 0;
	padding-bottom: var(--sp-3);
	border-bottom: 1px solid var(--c-border-gold);
}
.page-sub {
	font-size: var(--fs-sm);
	color: var(--c-text-soft);
	margin: var(--sp-2) 0 0 0;
	letter-spacing: 0.3px;
}

/* ── Guest Bar ── */
.guest-bar {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: 13px var(--sp-5);
	background: var(--c-cream);
	border: 1px solid var(--c-border-gold);
	border-left: 3px solid var(--c-gold);
	border-radius: var(--r-md);
	margin-bottom: var(--sp-5);
}
.guest-bar__room {
	font-weight: 600;
	font-size: var(--fs-md);
	color: var(--c-text);
	letter-spacing: 0.2px;
}

/* ── Empty State ── */
.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: var(--sp-12) var(--sp-8);
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-radius: var(--r-xl);
	gap: var(--sp-4);
}
.empty-icon { font-size: 40px; }
.empty-text {
	font-size: var(--fs-md);
	color: var(--c-text-soft);
	letter-spacing: 0.2px;
}

/* ── Timeline ── */
.timeline {
	display: flex;
	flex-direction: column;
	gap: var(--sp-3);
}

.timeline-item {
	display: flex;
	align-items: flex-start;
	gap: var(--sp-4);
	padding: var(--sp-5) var(--sp-6);
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-left: 3px solid var(--c-border-gold);
	border-radius: var(--r-xl);
	box-shadow: var(--sh-sm);
	transition: border-color var(--t-fast) var(--ease-out), box-shadow var(--t-fast) var(--ease-out);
}
.timeline-item:hover {
	border-left-color: var(--c-gold);
	box-shadow: var(--sh-md);
}

.timeline-icon {
	font-size: 22px;
	width: 28px;
	text-align: center;
	flex-shrink: 0;
	margin-top: 2px;
}

.timeline-body {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: var(--sp-2);
}

.timeline-head {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	flex-wrap: wrap;
}

.timeline-type {
	font-size: var(--fs-xs);
	font-weight: 600;
	color: var(--c-text-soft);
	letter-spacing: 1.2px;
	text-transform: uppercase;
}

.status-badge {
	display: inline-flex;
	align-items: center;
	padding: 2px 10px;
	border-radius: var(--r-pill);
	font-size: var(--fs-xs);
	font-weight: 700;
	letter-spacing: 0.6px;
}
.badge--req  { background: rgba(201, 169, 110, 0.12); color: var(--c-gold-deep); }
.badge--prog { background: rgba(59, 130, 246, 0.12);  color: #3b82f6; }
.badge--done { background: rgba(34, 197, 94, 0.12);   color: #16a34a; }

.timeline-title {
	font-size: var(--fs-md);
	font-weight: 600;
	color: var(--c-text);
}

.timeline-date {
	font-size: var(--fs-xs);
	color: var(--c-muted);
	letter-spacing: 0.2px;
}

@media (max-width: 480px) {
	.timeline-item { padding: var(--sp-4); }
}
</style>
