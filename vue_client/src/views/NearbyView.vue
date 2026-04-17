<template>
	<div class="nearby">
		<div class="page-header">
			<h2 class="page-title">{{ t('nearby.title') }}</h2>
			<p class="page-sub">Nearby Places</p>
		</div>

		<div class="tabs">
			<button
				v-for="tab in TABS"
				:key="tab.cd"
				class="tab"
				:class="{ active: category === tab.cd }"
				@click="switchCategory(tab.cd)"
			>
				<span class="tab-icon">{{ tab.emoji }}</span>
				<span class="tab-label">{{ tab.label }}</span>
			</button>
		</div>

		<LoadingSpinner v-if="loading" :text="t('nearby.loading')" />

		<div v-else-if="error" class="state state--error">
			<div class="state-icon-wrap state-icon-wrap--err">
				<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
			</div>
			<div class="state-title">{{ t('nearby.retry') }}</div>
			<div class="state-sub">{{ error }}</div>
		</div>

		<div v-else-if="places.length === 0" class="state">
			<div class="state-icon-wrap">
				<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
			</div>
			<div class="state-title">{{ t('nearby.noResult') }}</div>
			<div class="state-sub">{{ t('nearby.noResult.sub') }}</div>
		</div>

		<ul v-else class="place-list">
			<li v-for="(p, idx) in places" :key="idx" class="place-item">
				<div class="place-item__main">
					<div class="place-item__info">
						<strong class="place-name">{{ p.name }}</strong>
						<div class="place-meta">
							<span class="cat-tag">{{ p.category }}</span>
							<span v-if="p.distanceM != null" class="dist">{{ p.distanceM }}m</span>
						</div>
						<div v-if="p.address" class="place-addr">{{ p.address }}</div>
					</div>
					<div class="place-item__badge">
						<span class="walk-pill">
							<svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M13 4a1 1 0 1 1-2 0 1 1 0 0 1 2 0zM8 17l1-4 2 2 2-4M6 11l1-3a5 5 0 0 1 4-2h2l2 3h3M6 21l2-4"/></svg>
							{{ walkMin(p.distanceM) }}{{ t('nearby.min') }}
						</span>
					</div>
				</div>
				<div v-if="p.phone || p.mapUrl" class="place-item__actions">
					<a v-if="p.phone" class="action-link action-link--tel" :href="`tel:${p.phone}`">
						<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.64 3.35 2 2 0 0 1 3.61 1h3a2 2 0 0 1 2 1.72c.127.96.36 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.91 8.6a16 16 0 0 0 6 6l.96-.96a2 2 0 0 1 2.11-.45c.907.34 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
						{{ p.phone }}
					</a>
					<a
						v-if="p.mapUrl"
						class="action-link action-link--map"
						:href="p.mapUrl"
						target="_blank"
						rel="noopener noreferrer"
					>
						<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polygon points="3 11 22 2 13 21 11 13 3 11"/></svg>
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
.nearby { max-width: 640px; }

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

/* ── Tabs — gold underline style ── */
.tabs {
	display: flex;
	gap: 0;
	overflow-x: auto;
	margin-bottom: var(--sp-6);
	padding-bottom: 0;
	scrollbar-width: none;
	border-bottom: 1px solid var(--c-border);
}
.tabs::-webkit-scrollbar { display: none; }

.tab {
	flex: 0 0 auto;
	display: flex;
	align-items: center;
	gap: 6px;
	padding: 12px var(--sp-5);
	background: transparent;
	border: none;
	border-bottom: 2px solid transparent;
	margin-bottom: -1px;
	font-size: var(--fs-sm);
	font-weight: 500;
	color: var(--c-text-soft);
	cursor: pointer;
	transition: color var(--t-fast) var(--ease-out), border-color var(--t-fast) var(--ease-out);
	white-space: nowrap;
}
.tab:hover {
	color: var(--c-text);
}
.tab.active {
	color: var(--c-gold-deep);
	border-bottom-color: var(--c-gold);
	font-weight: 600;
}
.tab-icon { font-size: 14px; line-height: 1; }
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
.state-icon-wrap {
	width: 52px;
	height: 52px;
	border-radius: var(--r-xl);
	background: var(--c-cream);
	border: 1px solid var(--c-border-gold);
	display: flex;
	align-items: center;
	justify-content: center;
	color: var(--c-gold-light);
	margin-bottom: var(--sp-2);
}
.state-icon-wrap--err { color: var(--c-err-500); border-color: rgba(192, 97, 78, 0.2); background: var(--c-err-50); }
.state-title { font-size: var(--fs-md); font-weight: 600; color: var(--c-text-soft); }
.state-sub { font-size: var(--fs-sm); color: var(--c-muted); }
.state--error .state-title { color: var(--c-err-600); }

/* ── Place list ── */
.place-list {
	list-style: none;
	padding: 0;
	margin: 0;
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-radius: var(--r-xl);
	overflow: hidden;
	box-shadow: var(--sh-sm);
}

.place-item {
	border-bottom: 1px solid var(--c-border);
	padding: var(--sp-5) var(--sp-6);
	transition: background var(--t-fast);
}
.place-item:last-child { border-bottom: none; }
.place-item:hover { background: var(--c-cream); }

.place-item__main {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: var(--sp-4);
	margin-bottom: var(--sp-3);
}
.place-item__info { flex: 1; min-width: 0; }
.place-name {
	font-family: 'Georgia', 'Times New Roman', serif;
	font-size: var(--fs-lg);
	font-weight: 400;
	color: var(--c-text);
	display: block;
	margin-bottom: 6px;
}
.place-meta {
	display: flex;
	align-items: center;
	gap: var(--sp-2);
	margin-bottom: 6px;
}
.cat-tag {
	font-size: var(--fs-xs);
	font-weight: 600;
	color: var(--c-text-soft);
	background: var(--c-cream);
	padding: 2px var(--sp-3);
	border-radius: var(--r-pill);
	border: 1px solid var(--c-border);
	letter-spacing: 0.2px;
}
.dist { font-size: var(--fs-xs); color: var(--c-muted); }
.place-addr { font-size: var(--fs-sm); color: var(--c-text-soft); }

.place-item__badge { flex-shrink: 0; }
.walk-pill {
	display: flex;
	align-items: center;
	gap: 4px;
	background: var(--c-gold-pale);
	color: var(--c-gold-deep);
	font-size: var(--fs-xs);
	font-weight: 600;
	padding: 5px var(--sp-3);
	border-radius: var(--r-pill);
	border: 1px solid var(--c-border-gold);
	white-space: nowrap;
}

.place-item__actions {
	display: flex;
	gap: var(--sp-2);
	flex-wrap: wrap;
}
.action-link {
	display: inline-flex;
	align-items: center;
	gap: 5px;
	padding: 7px var(--sp-4);
	border-radius: var(--r-md);
	font-size: var(--fs-sm);
	font-weight: 600;
	text-decoration: none;
	transition: all var(--t-fast) var(--ease-out);
}
.action-link--tel {
	background: var(--c-cream);
	color: var(--c-text-soft);
	border: 1px solid var(--c-border);
}
.action-link--tel:hover { border-color: var(--c-gold); color: var(--c-text); background: var(--c-gold-pale); }
.action-link--map {
	background: var(--c-midnight);
	color: var(--c-gold-light);
	border: 1px solid rgba(201, 169, 110, 0.15);
}
.action-link--map:hover { background: var(--c-navy); color: var(--c-gold); }

@media (max-width: 480px) {
	.tab-label { display: none; }
	.tab { padding: 10px var(--sp-3); }
	.tab-icon { font-size: 17px; }
	.place-item { padding: var(--sp-4); }
}
</style>
