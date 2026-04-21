<template>
	<div class="voc">
		<div class="page-header">
			<h2 class="page-title">{{ t('voc.title') }}</h2>
			<p class="page-sub">{{ t('voc.sub') }}</p>
		</div>

		<div class="guest-bar">
			<span class="guest-bar__room">{{ t('guest.room.label', roomNo) }}</span>
		</div>

		<div class="form-card">
			<div class="form-group">
				<label class="field-label">{{ t('voc.category') }}</label>
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
				<label class="field-label">{{ t('voc.severity') }}</label>
				<div class="sev-row">
					<label
						v-for="s in SEVS"
						:key="s.code"
						:class="['sev-pill', 'sev-' + s.code.toLowerCase(), { 'sev-pill--active': form.severity === s.code }]"
					>
						<input type="radio" v-model="form.severity" :value="s.code" />
						<span>{{ t(s.labelKey) }}</span>
					</label>
				</div>
			</div>

			<div class="form-group">
				<label class="field-label">{{ t('voc.voctitle') }}</label>
				<input type="text" v-model="form.title" :placeholder="t('voc.title.placeholder')" maxlength="200" />
			</div>

			<div class="form-group">
				<label class="field-label">{{ t('voc.content') }}</label>
				<textarea v-model="form.content" rows="5" maxlength="2000" :placeholder="t('voc.content.placeholder')" />
			</div>

			<button class="submit-btn" @click="submit" :disabled="busy || !form.category || !form.title || !form.content">
				{{ t('voc.submit') }}
			</button>
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
import { ref, reactive } from 'vue';
import { submitVoc } from '../api/client';
import { t } from '../i18n/ui.js';

const rsvNo = ref(sessionStorage.getItem('concierge.rsvNo') || '');
const roomNo = ref(sessionStorage.getItem('concierge.roomNo') || '');

const form = reactive({
	category: '',
	severity: 'NORMAL',
	title: '',
	content: ''
});
const toastMsg = ref(null);
const busy = ref(false);

const CATS = [
	{ code: 'FACILITY', icon: '🏢', labelKey: 'voc.cat.facility' },
	{ code: 'CLEAN',    icon: '🧹', labelKey: 'voc.cat.clean' },
	{ code: 'SERVICE',  icon: '🛎️', labelKey: 'voc.cat.service' },
	{ code: 'NOISE',    icon: '🔊', labelKey: 'voc.cat.noise' },
	{ code: 'BILLING',  icon: '💳', labelKey: 'voc.cat.billing' },
	{ code: 'ETC',      icon: '📝', labelKey: 'voc.cat.etc' }
];

const SEVS = [
	{ code: 'LOW',    labelKey: 'voc.sev.low' },
	{ code: 'NORMAL', labelKey: 'voc.sev.normal' },
	{ code: 'HIGH',   labelKey: 'voc.sev.high' },
	{ code: 'URGENT', labelKey: 'voc.sev.urgent' }
];

function showToast(text, ok) {
	toastMsg.value = { text, ok };
	setTimeout(() => { toastMsg.value = null; }, 3500);
}

async function submit() {
	if (busy.value) return;
	busy.value = true;
	try {
		const body = {
			rsvNo: rsvNo.value,
			rmNo: roomNo.value,
			category: form.category,
			severity: form.severity,
			title: form.title,
			content: form.content
		};
		await submitVoc(body);
		showToast(t('voc.success'), true);
		form.category = '';
		form.severity = 'NORMAL';
		form.title = '';
		form.content = '';
	} catch (err) {
		showToast(err?.message || t('error'), false);
	} finally {
		busy.value = false;
	}
}
</script>

<style scoped>
.voc { max-width: 640px; }

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
}
.cat-chip:hover { border-color: var(--c-gold); }
.cat-chip--active {
	border-color: var(--c-gold);
	background: var(--c-gold-pale);
}
.cat-ic { font-size: 22px; }

.sev-row { display: flex; gap: 8px; flex-wrap: wrap; }
.sev-pill {
	position: relative;
	display: inline-flex;
	align-items: center;
	padding: 9px 16px;
	border-radius: 999px;
	border: 1.5px solid #cbd5e0;
	font-size: 13px;
	font-weight: 700;
	cursor: pointer;
	background: #fff;
}
.sev-pill input { position: absolute; opacity: 0; pointer-events: none; }
.sev-pill--active { background: var(--c-gold-pale); border-color: var(--c-gold); color: var(--c-midnight); }
.sev-pill.sev-urgent.sev-pill--active { background: #fff5f5; border-color: #e53e3e; color: #c53030; }
.sev-pill.sev-high.sev-pill--active   { background: #fff4e6; border-color: #ed8936; color: #ad6200; }
.sev-pill.sev-low.sev-pill--active    { background: #f7fafc; border-color: #a0aec0; color: #4a5568; }

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
}
.submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.submit-btn:not(:disabled):hover { opacity: 0.9; transform: translateY(-1px); }

.toast {
	position: fixed; bottom: var(--sp-8); left: 50%; transform: translateX(-50%);
	display: flex; align-items: center; gap: var(--sp-4);
	padding: var(--sp-4) var(--sp-6);
	border-radius: var(--r-xl);
	box-shadow: var(--sh-xl);
	min-width: 280px;
	z-index: 200;
}
.toast--ok { background: #1e3a2a; color: #c8e6c9; }
.toast--err { background: #3a1e1a; color: #ffcdd2; }
.toast-indicator {
	width: 28px; height: 28px; border-radius: var(--r-pill);
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
