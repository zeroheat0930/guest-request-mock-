<template>
	<div class="lostfound">
		<div class="page-header">
			<h2 class="page-title">{{ t('lostfound.title') }}</h2>
			<p class="page-sub">{{ t('lostfound.sub') }}</p>
		</div>

		<div class="guest-bar">
			<span class="guest-bar__room">{{ t('guest.room.label', roomNo) }}</span>
		</div>

		<div class="form-card">
			<div class="form-group">
				<label class="field-label">{{ t('lostfound.category') }}</label>
				<div class="cat-grid">
					<button
						v-for="c in CATS"
						:key="c.code"
						type="button"
						:class="['cat-chip', { 'cat-chip--active': form.category === c.code }]"
						@click="form.category = c.code"
					>
						<span class="cat-ic">{{ c.icon }}</span>
						<span>{{ t(c.labelKey) }}</span>
					</button>
				</div>
			</div>

			<div class="form-group">
				<label class="field-label">{{ t('lostfound.item') }}</label>
				<input type="text" v-model="form.itemName" :placeholder="t('lostfound.item.placeholder')" maxlength="120" />
			</div>

			<div class="form-group">
				<label class="field-label">{{ t('lostfound.location') }}</label>
				<input type="text" v-model="form.locationHint" :placeholder="t('lostfound.location.placeholder')" maxlength="200" />
			</div>

			<div class="form-group">
				<label class="field-label">{{ t('lostfound.description') }}</label>
				<textarea v-model="form.description" rows="3" maxlength="500" />
			</div>

			<button class="submit-btn" @click="submit" :disabled="busy || !form.category || !form.itemName">
				{{ t('lostfound.submit') }}
			</button>
		</div>

		<Transition name="toast">
			<div v-if="toastMsg" class="toast" :class="toastMsg.ok ? 'toast--ok' : 'toast--err'">
				<div class="toast-indicator">{{ toastMsg.ok ? '✓' : '!' }}</div>
				<div>
					<div class="toast-msg">{{ toastMsg.text }}</div>
				</div>
			</div>
		</Transition>
	</div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { submitLostFound } from '../api/client';
import { t } from '../i18n/ui.js';

const rsvNo = ref(sessionStorage.getItem('concierge.rsvNo') || '');
const roomNo = ref(sessionStorage.getItem('concierge.roomNo') || '');
const form = reactive({
	category: '',
	itemName: '',
	description: '',
	locationHint: ''
});
const toastMsg = ref(null);
const busy = ref(false);

const CATS = [
	{ code: 'WALLET',    icon: '👛', labelKey: 'lostfound.cat.wallet' },
	{ code: 'PHONE',     icon: '📱', labelKey: 'lostfound.cat.phone' },
	{ code: 'CLOTHING',  icon: '👕', labelKey: 'lostfound.cat.clothing' },
	{ code: 'ACCESSORY', icon: '💍', labelKey: 'lostfound.cat.accessory' },
	{ code: 'DOCUMENT',  icon: '📄', labelKey: 'lostfound.cat.document' },
	{ code: 'ETC',       icon: '📦', labelKey: 'lostfound.cat.etc' }
];

function showToast(text, ok) {
	toastMsg.value = { text, ok };
	setTimeout(() => { toastMsg.value = null; }, 3500);
}

async function submit() {
	if (busy.value) return;
	if (!form.category || !form.itemName) return;
	busy.value = true;
	try {
		const body = {
			rsvNo: rsvNo.value,
			rmNo: roomNo.value,
			category: form.category,
			itemName: form.itemName,
			description: form.description,
			locationHint: form.locationHint
		};
		await submitLostFound(body);
		showToast(t('lostfound.success'), true);
		form.category = '';
		form.itemName = '';
		form.description = '';
		form.locationHint = '';
	} catch (err) {
		showToast(err?.message || t('error'), false);
	} finally {
		busy.value = false;
	}
}
</script>

<style scoped>
.lostfound { max-width: 640px; }

.page-header { margin-bottom: var(--sp-6); }
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

.form-card {
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-radius: var(--r-xl);
	padding: var(--sp-8);
	box-shadow: var(--sh-sm);
	display: flex;
	flex-direction: column;
	gap: var(--sp-6);
}

.form-group { display: flex; flex-direction: column; gap: var(--sp-2); }
.field-label {
	font-size: 11px;
	font-weight: 600;
	color: var(--c-text-soft);
	letter-spacing: 1.5px;
	text-transform: uppercase;
}

input[type='text'], textarea {
	width: 100%;
	padding: 14px var(--sp-4);
	border: 1px solid var(--c-border);
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	background: var(--c-cream);
	color: var(--c-text);
	box-sizing: border-box;
	font-family: inherit;
	line-height: 1.6;
}
textarea { resize: vertical; }
input[type='text']:focus, textarea:focus {
	outline: none;
	border-color: var(--c-gold);
	box-shadow: 0 0 0 3px rgba(201, 169, 110, 0.12);
	background: var(--c-surface);
}

.cat-grid {
	display: grid;
	grid-template-columns: repeat(3, 1fr);
	gap: 8px;
}
.cat-chip {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 6px;
	padding: 14px 8px;
	border: 1px solid var(--c-border);
	border-radius: var(--r-md);
	background: var(--c-surface);
	color: var(--c-text);
	font-size: var(--fs-sm);
	font-weight: 600;
	cursor: pointer;
	transition: border-color var(--t-fast), background var(--t-fast);
}
.cat-chip:hover { border-color: var(--c-gold); }
.cat-chip--active {
	border-color: var(--c-gold);
	background: var(--c-gold-pale);
	box-shadow: var(--sh-sm);
}
.cat-ic { font-size: 22px; }

.submit-btn {
	width: 100%;
	height: var(--touch-lg);
	background: linear-gradient(135deg, #c9a96e, #d4b896);
	color: var(--c-midnight);
	border: none;
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	font-weight: 700;
	cursor: pointer;
	letter-spacing: 0.8px;
	text-transform: uppercase;
	transition: opacity var(--t-norm), transform var(--t-fast);
}
.submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.submit-btn:not(:disabled):hover { opacity: 0.9; transform: translateY(-1px); }

.toast {
	position: fixed;
	bottom: var(--sp-8);
	left: 50%;
	transform: translateX(-50%);
	display: flex;
	align-items: center;
	gap: var(--sp-4);
	padding: var(--sp-4) var(--sp-6);
	border-radius: var(--r-xl);
	box-shadow: var(--sh-xl);
	min-width: 280px;
	max-width: 480px;
	z-index: 200;
}
.toast--ok { background: #1e3a2a; color: #c8e6c9; }
.toast--err { background: #3a1e1a; color: #ffcdd2; }
.toast-indicator {
	width: 28px; height: 28px;
	border-radius: var(--r-pill);
	background: rgba(255,255,255,0.12);
	display: flex; align-items: center; justify-content: center;
	font-size: 13px; font-weight: 800;
}
.toast-msg { font-size: var(--fs-md); font-weight: 600; }
.toast-enter-active, .toast-leave-active { transition: opacity var(--t-norm), transform var(--t-norm); }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(16px); }

@media (max-width: 480px) {
	.form-card { padding: var(--sp-5); }
	.cat-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
