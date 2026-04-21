<template>
	<div class="rental">
		<div class="page-header">
			<h2 class="page-title">{{ t('rental.title') }}</h2>
			<p class="page-sub">{{ t('rental.sub') }}</p>
		</div>

		<div class="guest-bar">
			<span class="guest-bar__room">{{ t('guest.room.label', roomNo) }}</span>
		</div>

		<LoadingSpinner v-if="loading" :text="t('rental.loading')" />
		<div v-else-if="!items.length" class="empty">{{ t('rental.empty') }}</div>

		<div v-else class="catalog">
			<div v-for="item in items" :key="item.itemId" class="item-card">
				<div class="item-head">
					<div class="item-ic">{{ iconFor(item.category) }}</div>
					<div class="item-name">{{ item.name }}</div>
					<span :class="['stock-badge', item.stockAvailable > 0 ? '' : 'stock-empty']">
						{{ t('rental.stock') }}: {{ item.stockAvailable }}/{{ item.stockTotal }}
					</span>
				</div>
				<p class="item-desc" v-if="item.description">{{ item.description }}</p>
				<div class="item-foot">
					<div class="qty-wrap">
						<button class="qty-btn" :disabled="(qtyMap[item.itemId]||1) <= 1" @click="qtyMap[item.itemId] = (qtyMap[item.itemId]||1) - 1">−</button>
						<span class="qty-val">{{ qtyMap[item.itemId] || 1 }}</span>
						<button class="qty-btn" :disabled="(qtyMap[item.itemId]||1) >= item.stockAvailable" @click="qtyMap[item.itemId] = (qtyMap[item.itemId]||1) + 1">+</button>
					</div>
					<button class="order-btn" :disabled="item.stockAvailable <= 0 || busyId === item.itemId" @click="order(item)">
						{{ t('rental.submit') }}
					</button>
				</div>
			</div>
		</div>

		<Transition name="toast">
			<div v-if="toastMsg" class="toast" :class="toastMsg.ok ? 'toast--ok' : 'toast--err'">
				<div class="toast-indicator">{{ toastMsg.ok ? '✓' : '!' }}</div>
				<div><div class="toast-msg">{{ toastMsg.text }}</div></div>
			</div>
		</Transition>
	</div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchRentalItems, submitRental } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import { t } from '../i18n/ui.js';

const rsvNo = ref(sessionStorage.getItem('concierge.rsvNo') || '');
const roomNo = ref(sessionStorage.getItem('concierge.roomNo') || '');
const items = ref([]);
const qtyMap = reactive({});
const loading = ref(false);
const busyId = ref('');
const toastMsg = ref(null);

const ICONS = {
	UMBRELLA: '☂️', CHARGER: '🔌', ADAPTER: '🔄', IRON: '🔥', ETC: '📦'
};
function iconFor(cat) { return ICONS[cat] || '📦'; }

function showToast(text, ok) {
	toastMsg.value = { text, ok };
	setTimeout(() => { toastMsg.value = null; }, 3500);
}

onMounted(async () => {
	loading.value = true;
	try {
		const res = await fetchRentalItems();
		items.value = res?.list || [];
	} catch (e) {
		// silently fail — empty state
	} finally {
		loading.value = false;
	}
});

async function order(item) {
	busyId.value = item.itemId;
	try {
		await submitRental({
			rsvNo: rsvNo.value,
			itemId: item.itemId,
			qty: qtyMap[item.itemId] || 1
		});
		showToast(t('rental.success'), true);
		// refresh catalog to show updated stock
		const res = await fetchRentalItems();
		items.value = res?.list || [];
	} catch (e) {
		showToast(e?.message || t('error'), false);
	} finally {
		busyId.value = '';
	}
}
</script>

<style scoped>
.rental { max-width: 640px; }
.page-header { margin-bottom: var(--sp-6); }
.page-title {
	font-family: 'Georgia', 'Times New Roman', serif;
	font-size: var(--fs-2xl);
	font-weight: 400;
	color: var(--c-text);
	margin: 0 0 var(--sp-1) 0;
	padding-bottom: var(--sp-3);
	border-bottom: 1px solid var(--c-border-gold);
}
.page-sub { font-size: var(--fs-sm); color: var(--c-text-soft); margin-top: var(--sp-2); }
.guest-bar {
	display: flex; align-items: center; gap: var(--sp-3);
	padding: 13px var(--sp-5);
	background: var(--c-cream);
	border: 1px solid var(--c-border-gold);
	border-left: 3px solid var(--c-gold);
	border-radius: var(--r-md);
	margin-bottom: var(--sp-5);
}
.guest-bar__room { font-weight: 600; font-size: var(--fs-md); color: var(--c-text); }

.empty { padding: 48px 20px; text-align: center; color: var(--c-muted); background: var(--c-surface); border-radius: var(--r-md); }

.catalog { display: flex; flex-direction: column; gap: var(--sp-3); }
.item-card {
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-radius: var(--r-md);
	padding: var(--sp-5);
	box-shadow: var(--sh-sm);
}
.item-head { display: flex; align-items: center; gap: var(--sp-3); margin-bottom: 6px; }
.item-ic { font-size: 28px; width: 40px; text-align: center; }
.item-name { flex: 1; font-weight: 600; font-size: var(--fs-lg); color: var(--c-text); }
.stock-badge {
	padding: 3px 10px;
	border-radius: 999px;
	font-size: 11px;
	font-weight: 700;
	background: var(--c-gold-pale);
	color: var(--c-midnight);
}
.stock-badge.stock-empty { background: #fed7d7; color: #c53030; }

.item-desc { margin: 0 0 var(--sp-3) 0; color: var(--c-text-soft); font-size: var(--fs-sm); }

.item-foot { display: flex; align-items: center; justify-content: space-between; gap: var(--sp-3); }

.qty-wrap { display: flex; align-items: center; gap: var(--sp-2); }
.qty-btn {
	width: 34px; height: 34px;
	border: 1px solid var(--c-border-gold);
	background: var(--c-surface);
	border-radius: var(--r-pill);
	font-size: 18px;
	font-weight: 700;
	cursor: pointer;
}
.qty-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.qty-val { min-width: 32px; text-align: center; font-weight: 700; font-size: var(--fs-md); }

.order-btn {
	padding: 10px 20px;
	background: linear-gradient(135deg, #c9a96e, #d4b896);
	color: var(--c-midnight);
	border: none;
	border-radius: var(--r-md);
	font-size: var(--fs-sm);
	font-weight: 700;
	cursor: pointer;
	letter-spacing: 0.5px;
	text-transform: uppercase;
}
.order-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.toast { position: fixed; bottom: var(--sp-8); left: 50%; transform: translateX(-50%); display: flex; align-items: center; gap: var(--sp-4); padding: var(--sp-4) var(--sp-6); border-radius: var(--r-xl); box-shadow: var(--sh-xl); min-width: 280px; z-index: 200; }
.toast--ok { background: #1e3a2a; color: #c8e6c9; }
.toast--err { background: #3a1e1a; color: #ffcdd2; }
.toast-indicator { width: 28px; height: 28px; border-radius: var(--r-pill); background: rgba(255,255,255,0.12); display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 800; }
.toast-msg { font-size: var(--fs-md); font-weight: 600; }
.toast-enter-active, .toast-leave-active { transition: opacity var(--t-norm), transform var(--t-norm); }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(16px); }
</style>
