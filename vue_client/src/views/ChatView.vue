<template>
	<div class="chat-view">
		<header class="chat-head">
			<div class="chat-head__title">
				<span class="live-dot" />
				{{ t(lang, 'chatTitle') }}
			</div>
			<div class="chat-head__meta">
				<small class="llm-badge" :title="aiTitle">{{ aiBadge }}</small>
				<span class="room-chip">{{ guestRoom }}호</span>
			</div>
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
				autocomplete="off"
			/>
			<button type="submit" :disabled="busy || !draft.trim()" aria-label="전송">
				<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
				</svg>
			</button>
		</form>
	</div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { t } from '../i18n/messages.js';
import { parseAgent, checkLlmStatus } from '../chat/intent.js';
import {
	fetchReservation,
	requestAmenity,
	updateHousekeeping,
	checkLateCheckout,
	requestLateCheckout,
	submitLostFound,
	submitVoc,
	submitRental,
	requestParking
} from '../api/client.js';

const llmEnabled = ref(false);
const agentEnabled = ref(false);
const llmModel = ref('');
const aiBadge = computed(() => agentEnabled.value ? 'Agent' : (llmEnabled.value ? 'AI' : 'Bot'));
const aiTitle = computed(() => {
	if (agentEnabled.value) return 'Claude Tool Use 자율 에이전트 (' + llmModel.value + ')';
	if (llmEnabled.value)   return 'Claude (' + llmModel.value + ')';
	return '룰 기반 의도 파서';
});

const draft = ref('');
const busy = ref(false);
const messages = ref([]);
const logRef = ref(null);
const inputRef = ref(null);

const rsvNo = ref(sessionStorage.getItem('concierge.rsvNo') || '');
const guestRoom = ref(sessionStorage.getItem('concierge.roomNo') || '');
const guestName = ref(sessionStorage.getItem('concierge.guestName') || '');
const rsv = ref(null);
const pendingLate = ref(null);

const lang = computed(() => rsv.value?.perUseLang || 'ko_KR');

const HK_NAME_KEY = { MU: 'hkMu', DND: 'hkDnd', CLR: 'hkClr' };

onMounted(async () => {
	try {
		const s = await checkLlmStatus();
		// 호환: 구버전(boolean) / 신버전(object)
		if (typeof s === 'object' && s) {
			llmEnabled.value = !!s.llm;
			agentEnabled.value = !!s.agent;
			llmModel.value = s.agentModel || s.model || '';
		} else {
			llmEnabled.value = !!s;
		}
	} catch (e) {
		console.warn('[chat] LLM 상태 조회 실패', e);
	}
	await loadRsv();
	nextTick(() => inputRef.value?.focus());
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

		const r = await parseAgent(text, rsv.value || { perUseLang: 'ko_KR' });
		// 에이전트 reply (다중 액션 요약 멘트) 가 있으면 먼저 표시
		if (r.reply) {
			messages.value.push({ role: 'assistant', text: r.reply });
			scrollDown();
		}
		const actions = r.actions && r.actions.length ? r.actions : [{ intent: 'chat', payload: {} }];
		for (const action of actions) {
			await handleIntent({ intent: action.intent, payload: action.payload || {}, reply: r.reply });
			scrollDown();
		}
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
		// reply 가 있으면 위 단계에서 이미 표시. 추가 안내는 생략.
		if (!reply) messages.value.push({ role: 'assistant', text: t(lang.value, 'intentChat') });
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
		return;
	}

	if (intent === 'lostfound') {
		try {
			const res = await submitLostFound({
				rsvNo: rsv.value.rsvNo,
				roomNo: rsv.value.roomNo,
				category: payload.category || 'OTHER',
				itemName: payload.itemName || '',
				description: payload.description || payload.reqMemo || '',
				locationHint: payload.locationHint || ''
			});
			messages.value.push({
				role: 'assistant',
				text: `🔍 분실물 신고 접수 — ${payload.itemName || '항목'}${res?.map?.lfId ? ' (' + res.map.lfId + ')' : ''}`
			});
		} catch (e) {
			messages.value.push({ role: 'assistant', text: `${t(lang.value, 'failPrefix')} ${e.message || ''}` });
		}
		return;
	}

	if (intent === 'voc') {
		try {
			const res = await submitVoc({
				rsvNo: rsv.value.rsvNo,
				roomNo: rsv.value.roomNo,
				category: payload.category || 'OTHER',
				severity: payload.severity || 'NORMAL',
				title: payload.title || (payload.content || '').slice(0, 30),
				content: payload.content || payload.reqMemo || ''
			});
			const sev = (payload.severity || 'NORMAL').toUpperCase();
			const sevTag = sev === 'URGENT' ? '🚨' : (sev === 'HIGH' ? '⚠️' : '💬');
			messages.value.push({
				role: 'assistant',
				text: `${sevTag} 불편사항 접수 (${sev})${res?.map?.vocId ? ' — ' + res.map.vocId : ''}`
			});
		} catch (e) {
			messages.value.push({ role: 'assistant', text: `${t(lang.value, 'failPrefix')} ${e.message || ''}` });
		}
		return;
	}

	if (intent === 'rental') {
		try {
			const res = await submitRental({
				rsvNo: rsv.value.rsvNo,
				itemId: payload.itemId || null,
				itemName: payload.itemName || payload.note || '',
				qty: payload.qty || 1,
				note: payload.note || payload.itemName || ''
			});
			messages.value.push({
				role: 'assistant',
				text: `🏷️ 대여 요청 — ${payload.itemName || '품목'} × ${payload.qty || 1}${res?.map?.orderId ? ' (' + res.map.orderId + ')' : ''}`
			});
		} catch (e) {
			messages.value.push({ role: 'assistant', text: `${t(lang.value, 'failPrefix')} ${e.message || ''}` });
		}
		return;
	}

	if (intent === 'parking') {
		try {
			const res = await requestParking({
				rsvNo: rsv.value.rsvNo,
				roomNo: rsv.value.roomNo,
				carNo: payload.carNo || '',
				carTp: payload.carTp || '',
				memo: payload.memo || ''
			});
			messages.value.push({
				role: 'assistant',
				text: `🚗 차량 등록 — ${payload.carNo || ''}${res?.map?.reqNo ? ' (' + res.map.reqNo + ')' : ''}`
			});
		} catch (e) {
			messages.value.push({ role: 'assistant', text: `${t(lang.value, 'failPrefix')} ${e.message || ''}` });
		}
		return;
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
	background: var(--c-surface);
	border-radius: var(--r-xl);
	overflow: hidden;
	box-shadow: var(--sh-md);
	border: 1px solid var(--c-border);
}

/* ── Header ── */
.chat-head {
	background: var(--c-midnight);
	color: var(--c-text-light);
	padding: 18px var(--sp-6);
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: var(--sp-4);
	flex-shrink: 0;
	border-bottom: 1px solid rgba(201, 169, 110, 0.12);
}
.chat-head__title {
	font-family: 'Georgia', 'Times New Roman', serif;
	font-weight: 400;
	font-size: var(--fs-md);
	display: flex;
	align-items: center;
	gap: 10px;
	color: var(--c-gold-light);
	letter-spacing: 0.2px;
}
.chat-head__meta {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
}
.llm-badge {
	font-size: 10px;
	font-weight: 700;
	color: rgba(201, 169, 110, 0.45);
	letter-spacing: 1.2px;
	text-transform: uppercase;
}
.live-dot {
	width: 6px;
	height: 6px;
	border-radius: 50%;
	background: #5a8a6a;
	display: inline-block;
	flex-shrink: 0;
	box-shadow: 0 0 0 2px rgba(90, 138, 106, 0.25);
}
.room-chip {
	background: rgba(201, 169, 110, 0.1);
	border: 1px solid rgba(201, 169, 110, 0.2);
	border-radius: var(--r-pill);
	padding: 4px 12px;
	font-size: var(--fs-sm);
	font-weight: 600;
	color: var(--c-gold-light);
}

/* ── Chat log ── */
.chat-log {
	flex: 1;
	overflow-y: auto;
	padding: var(--sp-6);
	background: var(--c-ivory);
	display: flex;
	flex-direction: column;
	gap: var(--sp-3);
}
.msg { display: flex; }
.msg.user { justify-content: flex-end; }
.msg .bubble {
	max-width: 72%;
	padding: 13px var(--sp-5);
	border-radius: 18px;
	font-size: var(--fs-md);
	line-height: 1.65;
	white-space: pre-wrap;
	word-break: break-word;
}
.msg.assistant .bubble {
	background: var(--c-surface);
	color: var(--c-text);
	border: 1px solid var(--c-border);
	border-bottom-left-radius: 5px;
	box-shadow: var(--sh-xs);
}
.msg.user .bubble {
	background: rgba(201, 169, 110, 0.12);
	color: var(--c-text);
	border: 1px solid rgba(201, 169, 110, 0.22);
	border-bottom-right-radius: 5px;
}

/* typing indicator */
.bubble.typing {
	display: flex;
	gap: 5px;
	padding: 16px var(--sp-5);
	align-items: center;
}
.bubble.typing span {
	width: 6px;
	height: 6px;
	border-radius: 50%;
	background: var(--c-gold-light);
	animation: blink 1.2s infinite;
}
.bubble.typing span:nth-child(2) { animation-delay: 0.2s; }
.bubble.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
	0%, 80%, 100% { opacity: 0.2; transform: scale(0.85); }
	40% { opacity: 1; transform: scale(1); }
}

/* ── Input area ── */
.chat-input {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: var(--sp-4) var(--sp-5);
	background: var(--c-surface);
	border-top: 1px solid var(--c-border);
	flex-shrink: 0;
}
.chat-input input {
	flex: 1;
	height: var(--touch-md);
	padding: 0 var(--sp-5);
	border: 1px solid var(--c-border);
	border-radius: var(--r-pill);
	font-size: var(--fs-md);
	background: var(--c-cream);
	color: var(--c-text);
	outline: none;
	transition: border-color var(--t-fast), box-shadow var(--t-fast), background var(--t-fast);
	font-family: inherit;
}
.chat-input input:focus {
	border-color: var(--c-gold);
	box-shadow: 0 0 0 3px rgba(201, 169, 110, 0.12);
	background: var(--c-surface);
}
.chat-input input::placeholder { color: var(--c-muted); }
.chat-input input:disabled { opacity: 0.55; }
.chat-input button {
	width: var(--touch-md);
	height: var(--touch-md);
	background: var(--c-midnight);
	color: var(--c-gold);
	border: 1px solid rgba(201, 169, 110, 0.2);
	border-radius: var(--r-pill);
	display: flex;
	align-items: center;
	justify-content: center;
	cursor: pointer;
	flex-shrink: 0;
	transition: background var(--t-fast), transform var(--t-fast), opacity var(--t-fast), color var(--t-fast);
}
.chat-input button:hover:not(:disabled) {
	background: var(--c-navy);
	color: var(--c-gold-light);
	transform: scale(1.04);
}
.chat-input button:disabled {
	opacity: 0.35;
	cursor: not-allowed;
	transform: none;
}
</style>
