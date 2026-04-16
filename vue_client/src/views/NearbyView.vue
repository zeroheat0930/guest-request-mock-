<template>
	<div class="nearby">
		<div class="page-header">
			<div class="page-header__icon">
				<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
					<circle cx="12" cy="10" r="3"/>
				</svg>
			</div>
			<div>
				<h2 class="page-title">{{ t('nearby.title') }}</h2>
				<p class="page-sub">Nearby Places</p>
			</div>
		</div>

		<div class="tabs">
			<button
				v-for="t in TABS"
				:key="t.cd"
				class="tab"
				:class="{ active: category === t.cd }"
				@click="switchCategory(t.cd)"
			>
				<span class="tab-icon">{{ t.emoji }}</span>
				<span class="tab-label">{{ t.label }}</span>
			</button>
		</div>

		<LoadingSpinner v-if="loading" :text="t('nearby.loading')" />

		<div v-else-if="error" class="state state--error">
			<div class="state-icon">⚠️</div>
			<div class="state-title">{{ t('nearby.retry') }}</div>
			<small class="state-sub">{{ error }}</small>
		</div>

		<div v-else-if="places.length === 0" class="state">
			<div class="state-icon">🗺️</div>
			<div class="state-title">{{ t('nearby.noResult') }}</div>
			<div class="state-sub">{{ t('nearby.noResult.sub') }}</div>
		</div>

		<ul v-else class="card-list">
			<li v-for="(p, idx) in places" :key="idx" class="card">
				<div class="card-main">
					<div class="card-info">
						<div class="row1">
							<strong class="name">{{ p.name }}</strong>
						</div>
						<div class="row2">
							<span class="cat-tag">{{ p.category }}</span>
							<span v-if="p.distanceM != null" class="dist">{{ p.distanceM }}m</span>
						</div>
						<div v-if="p.address" class="addr">{{ p.address }}</div>
					</div>
					<div class="card-badge">
						<span class="walk-badge">
							<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M13 4a1 1 0 1 1-2 0 1 1 0 0 1 2 0zM8 17l1-4 2 2 2-4M6 11l1-3a5 5 0 0 1 4-2h2l2 3h3M6 21l2-4"/></svg>
							{{ t('nearby.walk') }} {{ walkMin(p.distanceM) }}{{ t('nearby.min') }}
						</span>
					</div>
				</div>
				<div class="actions">
					<a v-if="p.phone" class="btn btn--tel" :href="`tel:${p.phone}`">
						<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.64 3.35 2 2 0 0 1 3.61 1h3a2 2 0 0 1 2 1.72c.127.96.36 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.91 8.6a16 16 0 0 0 6 6l.96-.96a2 2 0 0 1 2.11-.45c.907.34 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
						{{ p.phone }}
					</a>
					<a
						v-if="p.mapUrl"
						class="btn btn--map"
						:href="p.mapUrl"
						target="_blank"
						rel="noopener noreferrer"
					>
						<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polygon points="3 11 22 2 13 21 11 13 3 11"/></svg>
						{{ t('nearby.map') }}
					</a>
				</div>
			</li>
		</ul>
	</div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchNearby } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import { t } from '../i18n/ui.js';

const TABS = [
	{ cd: 'food',     get label() { return t('nearby.food'); },     emoji: '🍽️' },
	{ cd: 'cafe',     get label() { return t('nearby.cafe'); },     emoji: '☕' },
	{ cd: 'conv',     get label() { return t('nearby.conv'); },     emoji: '🏪' },
	{ cd: 'tour',     get label() { return t('nearby.tour'); },     emoji: '🗺️' },
	{ cd: 'pharmacy', get label() { return t('nearby.pharmacy'); }, emoji: '💊' }
];

const category = ref('food');
const loading = ref(false);
const error = ref(null);
const places = ref([]);
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
.nearby { max-width: 680px; }

/* ── Page Header ── */
.page-header {
	display: flex;
	align-items: center;
	gap: var(--sp-4);
	margin-bottom: var(--sp-6);
}
.page-header__icon {
	width: 48px;
	height: 48px;
	background: var(--c-brand-700);
	color: var(--c-brand-300);
	border-radius: var(--r-md);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.page-title {
	font-size: var(--fs-2xl);
	font-weight: 800;
	color: var(--c-brand-900);
	letter-spacing: -0.4px;
	line-height: 1.2;
}
.page-sub {
	font-size: var(--fs-sm);
	color: var(--c-muted);
	letter-spacing: 0.8px;
	text-transform: uppercase;
	margin-top: 2px;
}

/* ── Tabs ── */
.tabs {
	display: flex;
	gap: var(--sp-2);
	overflow-x: auto;
	margin-bottom: var(--sp-5);
	padding-bottom: var(--sp-1);
	scrollbar-width: none;
}
.tabs::-webkit-scrollbar { display: none; }

.tab {
	flex: 0 0 auto;
	display: flex;
	align-items: center;
	gap: var(--sp-2);
	padding: var(--sp-2) var(--sp-4);
	background: var(--c-surface);
	border: 1.5px solid var(--c-border);
	border-radius: var(--r-pill);
	font-size: var(--fs-sm);
	font-weight: 700;
	color: var(--c-text-soft);
	cursor: pointer;
	transition: all var(--t-fast) var(--ease-out);
	white-space: nowrap;
}
.tab:hover {
	border-color: var(--c-brand-300);
	color: var(--c-brand-700);
	background: var(--c-brand-50);
}
.tab.active {
	background: var(--c-brand-700);
	border-color: var(--c-brand-700);
	color: #fff;
	box-shadow: 0 4px 12px rgba(26, 58, 110, 0.28);
}
.tab-icon { font-size: 16px; line-height: 1; }
.tab-label { font-size: var(--fs-sm); }

/* ── States ── */
.state {
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	padding: var(--sp-10) var(--sp-8);
	border-radius: var(--r-xl);
	text-align: center;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: var(--sp-2);
	box-shadow: var(--sh-sm);
}
.state--error .state-title { color: var(--c-err-600); }
.state-icon { font-size: 40px; line-height: 1; margin-bottom: var(--sp-2); }
.state-title { font-size: var(--fs-lg); font-weight: 700; color: var(--c-text-soft); }
.state-sub { font-size: var(--fs-sm); color: var(--c-muted); }

/* ── Card list ── */
.card-list {
	list-style: none;
	padding: 0;
	margin: 0;
	display: flex;
	flex-direction: column;
	gap: var(--sp-3);
}

.card {
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-radius: var(--r-lg);
	padding: var(--sp-5);
	box-shadow: var(--sh-sm);
	transition: box-shadow var(--t-norm) var(--ease-out), transform var(--t-fast);
}
.card:hover { box-shadow: var(--sh-md); transform: translateY(-1px); }

.card-main {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: var(--sp-4);
	margin-bottom: var(--sp-4);
}
.card-info { flex: 1; min-width: 0; }

.row1 { margin-bottom: var(--sp-1); }
.name { font-size: var(--fs-lg); font-weight: 800; color: var(--c-text); }

.row2 {
	display: flex;
	align-items: center;
	gap: var(--sp-2);
	margin-bottom: var(--sp-2);
}
.cat-tag {
	font-size: var(--fs-xs);
	font-weight: 700;
	color: var(--c-brand-600);
	background: var(--c-brand-50);
	padding: 2px var(--sp-2);
	border-radius: var(--r-pill);
}
.dist { font-size: var(--fs-xs); color: var(--c-muted); }
.addr { font-size: var(--fs-sm); color: var(--c-text-soft); }

.card-badge { flex-shrink: 0; }
.walk-badge {
	display: flex;
	align-items: center;
	gap: 4px;
	background: var(--c-brand-50);
	color: var(--c-brand-700);
	font-size: var(--fs-xs);
	font-weight: 700;
	padding: var(--sp-1) var(--sp-3);
	border-radius: var(--r-pill);
	white-space: nowrap;
}

.actions {
	display: flex;
	gap: var(--sp-2);
	flex-wrap: wrap;
	border-top: 1px solid var(--c-border);
	padding-top: var(--sp-4);
}
.btn {
	display: inline-flex;
	align-items: center;
	gap: var(--sp-1);
	padding: var(--sp-2) var(--sp-4);
	border-radius: var(--r-md);
	font-size: var(--fs-sm);
	font-weight: 700;
	text-decoration: none;
	transition: all var(--t-fast) var(--ease-out);
}
.btn--tel {
	background: var(--c-ok-50);
	color: #065f46;
	border: 1px solid rgba(34, 197, 94, 0.2);
}
.btn--tel:hover { background: #dcfce7; }
.btn--map {
	background: var(--c-brand-700);
	color: #fff;
	border: 1px solid var(--c-brand-800);
}
.btn--map:hover { background: var(--c-brand-900); }

@media (max-width: 480px) {
	.tab-label { display: none; }
	.tab { padding: var(--sp-2) var(--sp-3); }
	.tab-icon { font-size: 20px; }
}
</style>
