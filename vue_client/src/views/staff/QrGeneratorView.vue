<template>
	<div class="qr-page">
		<h2 class="page-title">객실 QR 코드 생성</h2>

		<div class="qr-form form-card">
			<div class="form-group">
				<label class="field-label">객실 번호</label>
				<input
					v-model="roomNo"
					class="field-input"
					placeholder="예: 03010"
					@keydown.enter="generate"
				/>
			</div>
			<button class="submit-btn" :disabled="!roomNo.trim()" @click="generate">생성</button>
		</div>

		<div v-if="generated" class="qr-result form-card">
			<canvas ref="qrCanvas" class="qr-canvas"></canvas>
			<p class="qr-url">{{ qrUrl }}</p>
			<button class="print-btn" @click="printQr">인쇄</button>
		</div>
	</div>
</template>

<script setup>
import { ref, nextTick } from 'vue';
import QRCode from 'qrcode';

const roomNo = ref('');
const generated = ref(false);
const qrUrl = ref('');
const qrCanvas = ref(null);

async function generate() {
	if (!roomNo.value.trim()) return;
	qrUrl.value = `${window.location.origin}/?room=${roomNo.value.trim()}`;
	generated.value = true;
	await nextTick();
	await QRCode.toCanvas(qrCanvas.value, qrUrl.value, { width: 280, margin: 2 });
}

function printQr() {
	window.print();
}
</script>

<style scoped>
.qr-page {
	max-width: 480px;
	margin: 0 auto;
}

.page-title {
	font-family: 'Georgia', 'Times New Roman', serif;
	font-size: var(--fs-2xl);
	font-weight: 400;
	color: var(--c-text);
	letter-spacing: -0.3px;
	margin: 0 0 var(--sp-6) 0;
	padding-bottom: var(--sp-3);
	border-bottom: 1px solid var(--c-border-gold);
}

.form-card {
	background: var(--c-surface);
	border: 1px solid var(--c-border);
	border-radius: var(--r-xl);
	padding: var(--sp-8);
	box-shadow: var(--sh-sm);
	display: flex;
	flex-direction: column;
	gap: var(--sp-5);
	margin-bottom: var(--sp-6);
}

.form-group {
	display: flex;
	flex-direction: column;
	gap: var(--sp-2);
}

.field-label {
	font-size: 11px;
	font-weight: 600;
	color: var(--c-text-soft);
	letter-spacing: 1.5px;
	text-transform: uppercase;
}

.field-input {
	padding: 13px var(--sp-4);
	border: 1px solid var(--c-border);
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	background: var(--c-cream);
	color: var(--c-text);
	transition: border-color var(--t-fast) var(--ease-out), box-shadow var(--t-fast) var(--ease-out);
	box-sizing: border-box;
	font-family: inherit;
	width: 100%;
}
.field-input:focus {
	outline: none;
	border-color: var(--c-gold);
	box-shadow: 0 0 0 3px rgba(201, 169, 110, 0.12);
	background: var(--c-surface);
}

.submit-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 100%;
	height: var(--touch-lg);
	background: linear-gradient(135deg, #c9a96e, #d4b896);
	color: var(--c-midnight);
	border: none;
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	font-weight: 700;
	cursor: pointer;
	transition: opacity var(--t-norm) var(--ease-out), box-shadow var(--t-norm) var(--ease-out), transform var(--t-fast) var(--ease-out);
	letter-spacing: 0.8px;
	text-transform: uppercase;
}
.submit-btn:hover:not(:disabled) {
	opacity: 0.9;
	box-shadow: var(--sh-gold);
	transform: translateY(-1px);
}
.submit-btn:active:not(:disabled) { transform: translateY(0); box-shadow: none; }
.submit-btn:disabled { opacity: 0.35; cursor: not-allowed; }

.qr-result {
	align-items: center;
	text-align: center;
}

.qr-canvas {
	border-radius: var(--r-md);
	display: block;
}

.qr-url {
	font-size: var(--fs-sm);
	color: var(--c-text-soft);
	word-break: break-all;
	margin: 0;
	padding: var(--sp-3) var(--sp-4);
	background: var(--c-cream);
	border: 1px solid var(--c-border);
	border-radius: var(--r-md);
	width: 100%;
	box-sizing: border-box;
	text-align: center;
}

.print-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 100%;
	height: var(--touch-lg);
	background: var(--c-surface);
	color: var(--c-text);
	border: 1px solid var(--c-border-gold);
	border-radius: var(--r-md);
	font-size: var(--fs-md);
	font-weight: 600;
	cursor: pointer;
	transition: background var(--t-fast) var(--ease-out), color var(--t-fast) var(--ease-out);
	letter-spacing: 0.5px;
}
.print-btn:hover {
	background: var(--c-gold);
	color: var(--c-midnight);
	border-color: var(--c-gold);
}

@media print {
	.qr-form { display: none; }
	.qr-result {
		box-shadow: none;
		border: none;
		padding: 0;
	}
	.print-btn { display: none; }
}
</style>
