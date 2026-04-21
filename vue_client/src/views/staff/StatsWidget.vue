<template>
	<div class="stats-widget">
		<div class="stat-card" v-for="item in cards" :key="item.label">
			<div class="stat-num">{{ item.value }}</div>
			<div class="stat-label">{{ item.label }}</div>
		</div>
	</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { fetchCcsStatsToday } from '../../api/client.js';
import { t } from '../../i18n/ui.js';

const props = defineProps({
	deptCd: {
		type: String,
		default: ''
	}
});

const received = ref('-');
const completed = ref('-');
const avgMinutes = ref('-');

const cards = computed(() => [
	{ label: t('stats.received'), value: received.value },
	{ label: t('stats.completed'), value: completed.value },
	{ label: t('stats.avgMin'), value: avgMinutes.value }
]);

async function loadStats() {
	try {
		const res = await fetchCcsStatsToday(props.deptCd || undefined);
		const m = res?.map ?? res;
		received.value = m.received ?? '-';
		completed.value = m.completed ?? '-';
		avgMinutes.value = m.avgMinutes != null ? Math.round(m.avgMinutes * 10) / 10 : '-';
	} catch {
		// 조용히 실패 — 이전 값 유지
	}
}

let timer = null;

onMounted(() => {
	loadStats();
	timer = setInterval(loadStats, 30000);
});

onUnmounted(() => {
	if (timer) clearInterval(timer);
});
</script>

<style scoped>
.stats-widget {
	display: flex;
	gap: 12px;
	flex-wrap: wrap;
}

.stat-card {
	background: #fff;
	border-radius: 12px;
	padding: 16px 24px;
	flex: 1;
	min-width: 100px;
	text-align: center;
	box-shadow: 0 2px 8px rgba(26, 58, 110, 0.07);
}

.stat-num {
	font-size: 32px;
	font-weight: 700;
	color: #1a3a6e;
	line-height: 1.1;
}

.stat-label {
	font-size: 12px;
	color: #8492a6;
	margin-top: 6px;
}

@media (max-width: 720px) {
	.stats-widget { gap: 8px; }
	.stat-card {
		padding: 12px 8px;
		min-width: 0;
		flex: 1 1 0;
	}
	.stat-num { font-size: 24px; }
	.stat-label { font-size: 10.5px; line-height: 1.3; }
}
</style>
