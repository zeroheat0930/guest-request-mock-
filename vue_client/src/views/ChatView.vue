<template>
	<div class="chat-view">
		<header class="chat-head">
			<div class="title">
				<span class="dot" />
				{{ t(lang, 'chatTitle') }}
				<small v-if="llmEnabled">· LLM</small>
				<small v-else>· Rule</small>
			</div>
			<select v-model="rsvNo" class="rsv-select" @change="switchGuest">
				<option v-for="r in demoReservations" :key="r.rsvNo" :value="r.rsvNo">
					{{ r.label }}
				</option>
			</select>
		</header>

		<div class="chat-log" ref="logRef">
			<div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
				<div class="bubble">{{ m.text }}</div>
			</div>
			<div v-if="busy" class="msg assistant">
				<div class="bubble typing">
					<span /><span /><span />
				</div>
			</div>
		</div>

		<form class="chat-input" @submit.prevent="send">
			<input
				v-model="draft"
				:placeholder="t(lang, 'chatPlaceholder')"
				:disabled="busy"
				ref="inputRef"
			/>
			<button type="submit" :disabled="busy || !draft.trim()">
				{{ t(lang, 'send') }}
			</button>
		</form>
	</div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { t } from '../i18n/messages.js';
import { parseIntent, checkLlmStatus } from '../chat/intent.js';
import {
	fetchReservation,
	requestAmenity,
	updateHousekeeping,
	checkLateCheckout,
	requestLateCheckout
} from '../api/client.js';
import { DEMO_RESERVATIONS, authenticateGuest, clearToken } from '../auth/authBootstrap.js';

const llmEnabled = ref(false);
const demoReservations = DEMO_RESERVATIONS;

const draft = ref('');
const busy = ref(false);
const messages = ref([]);
const logRef = ref(null);
const inputRef = ref(null);

const rsvNo = ref(DEMO_RESERVATIONS[0].rsvNo);
const rsv = ref(null);
const pendingLate = ref(null);

const lang = computed(() => rsv.value?.perUseLang || 'ko_KR');

const HK_NAME_KEY = { MU: 'hkMu', DND: 'hkDnd', CLR: 'hkClr' };

onMounted(async () => {
	try {
		llmEnabled.value = await checkLlmStatus();
	} catch (e) {
		console.warn('[chat] LLM 상태 조회 실패', e);
	}
	await switchGuest();
	nextTick(() => inputRef.value?.focus());
});

async function switchGuest() {
	try {
		clearToken();
		await authenticateGuest(rsvNo.value);
	} catch (e) {
		console.warn('[chat] 인증 실패', e);
		messages.value = [{ role: 'assistant', text: `${t(lang.value, 'failPrefix')}: ${e.message || e}` }];
		return;
	}
	await loadRsv();
}

async function loadRsv() {
	try {
		const res = await fetchReservation(rsvNo.value);
		rsv.value = res.map || null;
		messages.value = [{ role: 'assistant', text: t(lang.value, 'welcome') }];
		pendingLate.value = null;
	} catch (e) {
		console.warn('[chat] 예약 조회 실패', e);
	}
}

async function send() {
	const text = draft.value.trim();
	if (!text || busy.value) return;
	draft.value = '';
	messages.value.push({ role: 'user', text });
	scrollDown();
	busy.value = true;

	try {
		if (pendingLate.value) {
			if (/^(네|예|yes|y|はい|是|确认|好)/i.test(text)) {
				await confirmLateCheckout();
			} else {
				pendingLate.value = null;
				messages.value.push({ role: 'assistant', text: t(lang.value, 'intentChat') });
			}
			return;
		}

		const parsed = await parseIntent(text, rsv.value || { perUseLang: 'ko_KR' });
		await handleIntent(parsed);
	} catch (e) {
		messages.value.push({
			role: 'assistant',
			text: `${t(lang.value, 'failPrefix')}: ${e.message || 'unknown'}`
		});
	} finally {
		busy.value = false;
		scrollDown();
	}
}

async function handleIntent(parsed) {
	const { intent, reply, payload } = parsed;

	if (intent === 'chat') {
		messages.value.push({ role: 'assistant', text: reply || t(lang.value, 'intentChat') });
		return;
	}

	if (intent === 'amenity') {
		const res = await requestAmenity({
			rsvNo: rsv.value.rsvNo,
			roomNo: rsv.value.roomNo,
			itemList: payload.itemList || [],
			reqMemo: payload.reqMemo || ''
		});
		if (res.status === 0) {
			messages.value.push({ role: 'assistant', text: t(lang.value, 'amenitySuccess', { reqNo: res.map?.reqNo }) });
		} else {
			messages.value.push({ role: 'assistant', text: `${t(lang.value, 'failPrefix')} [${res.status}] ${res.message}` });
		}
		return;
	}

	if (intent === 'housekeeping') {
		const res = await updateHousekeeping({
			rsvNo: rsv.value.rsvNo,
			hkStatCd: payload.hkStatCd,
			reqMemo: payload.reqMemo || ''
		});
		if (res.status === 0) {
			const statKey = HK_NAME_KEY[payload.hkStatCd] || 'hkMu';
			messages.value.push({
				role: 'assistant',
				text: t(lang.value, 'hkSuccess', { stat: t(lang.value, statKey), reqNo: res.map?.reqNo })
			});
		} else {
			messages.value.push({ role: 'assistant', text: `${t(lang.value, 'failPrefix')} [${res.status}] ${res.message}` });
		}
		return;
	}

	if (intent === 'late_checkout') {
		const info = await checkLateCheckout(rsv.value.rsvNo, payload.reqOutTm);
		const m = info.map || {};
		if (m.availYn !== 'Y') {
			messages.value.push({ role: 'assistant', text: t(lang.value, 'lateNotAvail') });
			return;
		}
		pendingLate.value = { reqOutTm: payload.reqOutTm, addAmt: m.addAmt };
		messages.value.push({
			role: 'assistant',
			text: t(lang.value, 'lateInfo', { tm: formatHHMM(payload.reqOutTm), amt: m.addAmt.toLocaleString() })
		});
	}
}

async function confirmLateCheckout() {
	const { reqOutTm, addAmt } = pendingLate.value;
	pendingLate.value = null;
	const res = await requestLateCheckout({ rsvNo: rsv.value.rsvNo, reqOutTm, addAmt });
	if (res.status === 0) {
		messages.value.push({
			role: 'assistant',
			text: t(lang.value, 'lateSuccess', { amt: addAmt.toLocaleString(), reqNo: res.map?.reqNo })
		});
	} else {
		messages.value.push({ role: 'assistant', text: `${t(lang.value, 'failPrefix')} [${res.status}] ${res.message}` });
	}
}

function formatHHMM(s) {
	if (!s || s.length < 4) return s;
	return s.slice(0, 2) + ':' + s.slice(2, 4);
}

function scrollDown() {
	nextTick(() => {
		if (logRef.value) logRef.value.scrollTop = logRef.value.scrollHeight;
	});
}
</script>

<style scoped>
.chat-view {
	display: flex;
	flex-direction: column;
	height: 100%;
	background: #fff;
	border-radius: 12px;
	overflow: hidden;
	box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.chat-head {
	background: #1a3a6e;
	color: #fff;
	padding: 14px 18px;
	display: flex;
	align-items: center;
	gap: 10px;
}
.chat-head .title { font-weight: 700; display: flex; align-items: center; gap: 6px; flex: 1; font-size: 15px; }
.chat-head .title small { font-weight: 400; opacity: 0.7; font-size: 11px; }
.chat-head .dot { width: 8px; height: 8px; border-radius: 50%; background: #34d399; display: inline-block; }
.rsv-select {
	background: rgba(255,255,255,0.15);
	color: #fff;
	border: 1px solid rgba(255,255,255,0.3);
	border-radius: 6px;
	padding: 5px 8px;
	font-size: 12px;
}
.rsv-select option { color: #1a202c; }

.chat-log {
	flex: 1;
	overflow-y: auto;
	padding: 20px;
	background: #f7fafc;
	display: flex;
	flex-direction: column;
	gap: 12px;
}
.msg { display: flex; }
.msg.user { justify-content: flex-end; }
.msg .bubble {
	max-width: 75%;
	padding: 12px 16px;
	border-radius: 14px;
	font-size: 14px;
	line-height: 1.5;
	white-space: pre-wrap;
	word-break: break-word;
}
.msg.assistant .bubble { background: #fff; color: #1a202c; border: 1px solid #e2e8f0; border-bottom-left-radius: 4px; }
.msg.user .bubble { background: #2563eb; color: #fff; border-bottom-right-radius: 4px; }
.bubble.typing { display: flex; gap: 4px; padding: 14px; }
.bubble.typing span { width: 6px; height: 6px; border-radius: 50%; background: #cbd5e0; animation: blink 1.2s infinite; }
.bubble.typing span:nth-child(2) { animation-delay: 0.2s; }
.bubble.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink { 0%, 80%, 100% { opacity: 0.3; } 40% { opacity: 1; } }

.chat-input { display: flex; gap: 8px; padding: 14px; background: #fff; border-top: 1px solid #e2e8f0; }
.chat-input input {
	flex: 1;
	padding: 12px 16px;
	border: 1px solid #cbd5e0;
	border-radius: 22px;
	font-size: 14px;
	outline: none;
}
.chat-input input:focus { border-color: #2563eb; }
.chat-input button {
	background: #1a3a6e;
	color: #fff;
	border: none;
	border-radius: 22px;
	padding: 0 22px;
	font-size: 14px;
	font-weight: 700;
	cursor: pointer;
}
.chat-input button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
