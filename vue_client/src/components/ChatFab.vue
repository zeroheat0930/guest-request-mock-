<template>
	<div class="chat-fab">
		<!-- 플로팅 버튼 -->
		<button v-if="!open" class="fab-btn" @click="toggle" :title="t(lang, 'chatTitle')">
			<span class="fab-icon">💬</span>
			<span class="fab-badge" v-if="llmEnabled">AI</span>
		</button>

		<!-- 채팅창 -->
		<div v-if="open" class="chat-panel">
			<header class="chat-head">
				<div class="title">
					<span class="dot" />
					{{ t(lang, 'chatTitle') }}
					<small v-if="llmEnabled">· LLM</small>
					<small v-else>· Rule</small>
				</div>
				<select v-model="rsvNo" class="rsv-select" @change="loadRsv">
					<option v-for="r in rsvList" :key="r.rsvNo" :value="r.rsvNo">
						{{ r.roomNo }} · {{ r.perNm }} ({{ r.perUseLang }})
					</option>
				</select>
				<button class="close-btn" @click="toggle">×</button>
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
	</div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { t } from '../i18n/messages.js';
import { parseIntent, llmEnabled } from '../chat/intent.js';
import {
	fetchReservation,
	fetchReservationList,
	requestAmenity,
	updateHousekeeping,
	checkLateCheckout,
	requestLateCheckout
} from '../api/client.js';

const open = ref(false);
const draft = ref('');
const busy = ref(false);
const messages = ref([]);
const logRef = ref(null);
const inputRef = ref(null);

const rsvList = ref([]);
const rsvNo = ref('R2026041300001');
const rsv = ref(null);
const pendingLate = ref(null); // 레이트 체크아웃 확인 단계 보관

const lang = computed(() => rsv.value?.perUseLang || 'ko_KR');

const HK_NAME_KEY = { MU: 'hkMu', DND: 'hkDnd', CLR: 'hkClr' };

onMounted(async () => {
	try {
		const list = await fetchReservationList();
		rsvList.value = list.map?.list || [];
		if (rsvList.value.length > 0) {
			rsvNo.value = rsvList.value[0].rsvNo;
			await loadRsv();
		}
	} catch (e) {
		console.warn('[chat] 예약 목록 로드 실패', e);
	}
});

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

function toggle() {
	open.value = !open.value;
	if (open.value && messages.value.length === 0) {
		messages.value = [{ role: 'assistant', text: t(lang.value, 'welcome') }];
	}
	if (open.value) {
		nextTick(() => inputRef.value?.focus());
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
		// 레이트 체크아웃 확인 단계 처리
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
			text: `${t(lang.value, 'failPrefix')}: ${e.message || e.resMsg || 'unknown'}`
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
		if (res.resCd === '0000') {
			messages.value.push({
				role: 'assistant',
				text: t(lang.value, 'amenitySuccess', { reqNo: res.map?.reqNo })
			});
		} else {
			messages.value.push({
				role: 'assistant',
				text: `${t(lang.value, 'failPrefix')} [${res.resCd}] ${res.resMsg}`
			});
		}
		return;
	}

	if (intent === 'housekeeping') {
		const res = await updateHousekeeping({
			rsvNo: rsv.value.rsvNo,
			hkStatCd: payload.hkStatCd,
			reqMemo: payload.reqMemo || ''
		});
		if (res.resCd === '0000') {
			const statKey = HK_NAME_KEY[payload.hkStatCd] || 'hkMu';
			messages.value.push({
				role: 'assistant',
				text: t(lang.value, 'hkSuccess', {
					stat: t(lang.value, statKey),
					reqNo: res.map?.reqNo
				})
			});
		} else {
			messages.value.push({
				role: 'assistant',
				text: `${t(lang.value, 'failPrefix')} [${res.resCd}] ${res.resMsg}`
			});
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
		pendingLate.value = {
			reqOutTm: payload.reqOutTm,
			addAmt: m.addAmt
		};
		messages.value.push({
			role: 'assistant',
			text: t(lang.value, 'lateInfo', {
				tm: formatHHMM(payload.reqOutTm),
				amt: m.addAmt.toLocaleString()
			})
		});
	}
}

async function confirmLateCheckout() {
	const { reqOutTm, addAmt } = pendingLate.value;
	pendingLate.value = null;
	const res = await requestLateCheckout({
		rsvNo: rsv.value.rsvNo,
		reqOutTm,
		addAmt
	});
	if (res.resCd === '0000') {
		messages.value.push({
			role: 'assistant',
			text: t(lang.value, 'lateSuccess', {
				amt: addAmt.toLocaleString(),
				reqNo: res.map?.reqNo
			})
		});
	} else {
		messages.value.push({
			role: 'assistant',
			text: `${t(lang.value, 'failPrefix')} [${res.resCd}] ${res.resMsg}`
		});
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

watch(open, v => { if (v) scrollDown(); });
</script>

<style scoped>
.chat-fab {
	position: fixed;
	right: 24px;
	bottom: 24px;
	z-index: 9999;
	font-family: inherit;
}
.fab-btn {
	width: 64px;
	height: 64px;
	border-radius: 50%;
	border: none;
	background: linear-gradient(135deg, #1a3a6e, #2563eb);
	color: #fff;
	box-shadow: 0 8px 24px rgba(26, 58, 110, 0.4);
	cursor: pointer;
	display: flex;
	align-items: center;
	justify-content: center;
	transition: transform 0.15s;
	position: relative;
}
.fab-btn:hover { transform: scale(1.05); }
.fab-icon { font-size: 28px; }
.fab-badge {
	position: absolute;
	top: -2px;
	right: -2px;
	background: #ef4444;
	color: #fff;
	font-size: 10px;
	font-weight: 700;
	padding: 2px 6px;
	border-radius: 10px;
}

.chat-panel {
	width: 380px;
	max-width: calc(100vw - 32px);
	height: 540px;
	max-height: calc(100vh - 80px);
	background: #fff;
	border-radius: 16px;
	box-shadow: 0 24px 64px rgba(0,0,0,0.25);
	display: flex;
	flex-direction: column;
	overflow: hidden;
}
.chat-head {
	background: #1a3a6e;
	color: #fff;
	padding: 12px 16px;
	display: flex;
	align-items: center;
	gap: 8px;
}
.chat-head .title {
	font-weight: 700;
	display: flex;
	align-items: center;
	gap: 6px;
	flex: 1;
}
.chat-head .title small { font-weight: 400; opacity: 0.7; font-size: 11px; }
.chat-head .dot {
	width: 8px;
	height: 8px;
	border-radius: 50%;
	background: #34d399;
	display: inline-block;
}
.rsv-select {
	background: rgba(255,255,255,0.15);
	color: #fff;
	border: 1px solid rgba(255,255,255,0.3);
	border-radius: 6px;
	padding: 4px 6px;
	font-size: 11px;
	max-width: 130px;
}
.rsv-select option { color: #1a202c; }
.close-btn {
	background: transparent;
	border: none;
	color: #fff;
	font-size: 22px;
	cursor: pointer;
	padding: 0 4px;
}

.chat-log {
	flex: 1;
	overflow-y: auto;
	padding: 16px;
	background: #f7fafc;
	display: flex;
	flex-direction: column;
	gap: 10px;
}
.msg { display: flex; }
.msg.user { justify-content: flex-end; }
.msg .bubble {
	max-width: 80%;
	padding: 10px 14px;
	border-radius: 14px;
	font-size: 14px;
	line-height: 1.4;
	white-space: pre-wrap;
	word-break: break-word;
}
.msg.assistant .bubble {
	background: #fff;
	color: #1a202c;
	border: 1px solid #e2e8f0;
	border-bottom-left-radius: 4px;
}
.msg.user .bubble {
	background: #2563eb;
	color: #fff;
	border-bottom-right-radius: 4px;
}
.bubble.typing { display: flex; gap: 4px; padding: 14px; }
.bubble.typing span {
	width: 6px;
	height: 6px;
	border-radius: 50%;
	background: #cbd5e0;
	animation: blink 1.2s infinite;
}
.bubble.typing span:nth-child(2) { animation-delay: 0.2s; }
.bubble.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
	0%, 80%, 100% { opacity: 0.3; }
	40% { opacity: 1; }
}

.chat-input {
	display: flex;
	gap: 8px;
	padding: 12px;
	background: #fff;
	border-top: 1px solid #e2e8f0;
}
.chat-input input {
	flex: 1;
	padding: 10px 12px;
	border: 1px solid #cbd5e0;
	border-radius: 20px;
	font-size: 14px;
	outline: none;
}
.chat-input input:focus { border-color: #2563eb; }
.chat-input button {
	background: #1a3a6e;
	color: #fff;
	border: none;
	border-radius: 20px;
	padding: 0 18px;
	font-size: 14px;
	font-weight: 700;
	cursor: pointer;
}
.chat-input button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
