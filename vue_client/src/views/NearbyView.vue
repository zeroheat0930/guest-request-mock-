<template>
	<div class="nearby">
		<h2>📍 주변 안내</h2>

		<div class="tabs">
			<button
				v-for="t in TABS"
				:key="t.cd"
				class="tab"
				:class="{ active: category === t.cd }"
				@click="switchCategory(t.cd)"
			>
				<span class="emoji">{{ t.emoji }}</span>
				<span>{{ t.label }}</span>
			</button>
		</div>

		<div v-if="loading" class="state">불러오는 중…</div>
		<div v-else-if="error" class="state error">
			잠시 후 다시 시도해주세요.<br />
			<small>{{ error }}</small>
		</div>
		<div v-else-if="places.length === 0" class="state">근처에 결과가 없어요.</div>

		<ul v-else class="card-list">
			<li v-for="(p, idx) in places" :key="idx" class="card">
				<div class="row1">
					<strong class="name">{{ p.name }}</strong>
					<span class="walk">도보 {{ walkMin(p.distanceM) }}분</span>
				</div>
				<div class="row2">
					<span class="cat">{{ p.category }}</span>
					<span v-if="p.distanceM != null" class="dist">· {{ p.distanceM }}m</span>
				</div>
				<div v-if="p.address" class="addr">{{ p.address }}</div>
				<div class="actions">
					<a v-if="p.phone" class="btn tel" :href="`tel:${p.phone}`">📞 {{ p.phone }}</a>
					<a
						v-if="p.mapUrl"
						class="btn map"
						:href="p.mapUrl"
						target="_blank"
						rel="noopener noreferrer"
					>카카오맵 열기</a>
				</div>
			</li>
		</ul>
	</div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchNearby } from '../api/client';

const TABS = [
	{ cd: 'food',     label: '음식점', emoji: '🍽️' },
	{ cd: 'cafe',     label: '카페',   emoji: '☕' },
	{ cd: 'conv',     label: '편의점', emoji: '🏪' },
	{ cd: 'tour',     label: '관광지', emoji: '🗺️' },
	{ cd: 'pharmacy', label: '약국',   emoji: '💊' }
];

const category = ref('food');
const loading = ref(false);
const error = ref(null);
const places = ref([]);
// 카테고리별 결과 캐시 — 탭 전환 왕복을 즉시 렌더
const cache = reactive({});

function walkMin(distanceM) {
	if (distanceM == null) return '-';
	return Math.max(1, Math.round(distanceM / 67));
}

async function load(cat) {
	if (cache[cat]) {
		places.value = cache[cat];
		return;
	}
	loading.value = true;
	error.value = null;
	try {
		const res = await fetchNearby(cat);
		const list = (res && res.map && res.map.places) || [];
		cache[cat] = list;
		places.value = list;
	} catch (e) {
		error.value = (e && e.message) || '네트워크 오류';
		places.value = [];
	} finally {
		loading.value = false;
	}
}

function switchCategory(cat) {
	if (category.value === cat) return;
	category.value = cat;
	load(cat);
}

onMounted(() => load(category.value));
</script>

<style scoped>
.nearby h2 { margin-bottom: 20px; }

.tabs {
	display: flex;
	gap: 8px;
	overflow-x: auto;
	margin-bottom: 16px;
	padding-bottom: 4px;
}
.tab {
	flex: 0 0 auto;
	display: flex;
	align-items: center;
	gap: 6px;
	padding: 10px 14px;
	background: #fff;
	border: 1px solid #e2e8f0;
	border-radius: 999px;
	font-size: 14px;
	font-weight: 600;
	color: #4a5568;
	cursor: pointer;
}
.tab.active {
	background: #1a3a6e;
	border-color: #1a3a6e;
	color: #fff;
}
.tab .emoji { font-size: 16px; }

.state {
	background: #fff;
	padding: 32px;
	border-radius: 12px;
	text-align: center;
	color: #718096;
	box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.state.error { color: #c53030; }
.state small { color: #a0aec0; }

.card-list {
	list-style: none;
	padding: 0;
	margin: 0;
	display: flex;
	flex-direction: column;
	gap: 12px;
}
.card {
	background: #fff;
	padding: 16px;
	border-radius: 12px;
	box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.row1 {
	display: flex;
	justify-content: space-between;
	align-items: baseline;
	gap: 12px;
	margin-bottom: 4px;
}
.name { font-size: 16px; font-weight: 700; color: #1a202c; }
.walk {
	flex: 0 0 auto;
	font-size: 13px;
	font-weight: 600;
	color: #1a3a6e;
	background: #ebf4ff;
	padding: 4px 10px;
	border-radius: 999px;
}
.row2 { font-size: 12px; color: #718096; margin-bottom: 6px; }
.row2 .cat { font-weight: 600; }
.addr { font-size: 13px; color: #4a5568; margin-bottom: 10px; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.btn {
	display: inline-block;
	padding: 8px 12px;
	border-radius: 6px;
	font-size: 13px;
	font-weight: 600;
	text-decoration: none;
}
.btn.tel { background: #f0fff4; color: #2f855a; }
.btn.map { background: #1a3a6e; color: #fff; }
</style>
