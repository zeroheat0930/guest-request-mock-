/**
 * 자연어 → {intent, reply, payload} 의도 파서.
 *
 * 두 가지 모드:
 * 1. **Rule mode** (기본): 키워드 룰 기반. 오프라인 동작, 데모 안전.
 * 2. **LLM mode**: Spring Boot 프록시(`POST /api/ai/chat`) 경유로 Claude API 호출.
 *    실패 시 자동으로 rule 모드로 폴백. API 키는 서버에만 존재(프론트 번들에 절대 노출 안 됨).
 *
 * intent: 'amenity' | 'housekeeping' | 'late_checkout' | 'chat'
 */
import { t } from '../i18n/messages.js';
import { postAiChat, getAiStatus } from '../api/client.js';

// ─────────────────────────────────────────────
// 룰 기반 키워드 사전 (4개 언어)
// ─────────────────────────────────────────────
const KW = {
	towel:     [/수건|타월/i, /towel/i, /タオル/, /毛巾/],
	water:     [/생수|물(\s*병|병)?(?!\s*자)/i, /water|bottle/i, /水|ミネラルウォーター/, /水|矿泉水/],
	soap:      [/비누/i, /soap/i, /石(けん|鹸)/, /香皂|肥皂/],
	shampoo:   [/샴푸/i, /shampoo/i, /シャンプー/, /洗发(水|液)|香波/],
	toothbrush:[/칫솔|치솔/i, /toothbrush|tooth\s*brush/i, /歯ブラシ/, /牙刷/],

	hkMu:      [/청소|정비|치워|치우|메이크\s*업/i, /clean|make\s*up\s*room|housekeep/i, /(掃除|清掃|メイクアップ)/, /(清扫|打扫|清洁)/],
	hkDnd:     [/방해\s*금지|건드리지|들어오지/i, /do\s*not\s*disturb|dnd|don.?t\s*disturb/i, /(起こさないで|お休み|ドント\s*ディスターブ)/, /(请勿打扰|不要打扰)/],
	hkClr:     [/해제|취소|풀어/i, /clear|cancel\s*dnd|remove/i, /(解除|キャンセル)/, /(取消|解除)/],

	late:      [/(레이트|늦게|체크\s*아웃|연장|더\s*있)/i, /(late|extend|stay\s*longer|check\s*out)/i, /(レイト|延長|遅く|チェックアウト)/, /(延迟|延长|退房)/]
};

const ITEM_MAP = {
	towel: 'AM001',
	water: 'AM002',
	soap: 'AM003',
	shampoo: 'AM004',
	toothbrush: 'AM005'
};

function matchAny(text, patterns) {
	return patterns.some(p => p.test(text));
}

function extractQty(text) {
	// "수건 2개", "two towels", "タオル2枚", "两瓶水" 등
	const m = text.match(/(\d+)/);
	if (m) return parseInt(m[1], 10);
	// 한글 수사
	if (/하나|한\s*개/.test(text)) return 1;
	if (/둘|두\s*개/.test(text)) return 2;
	if (/셋|세\s*개/.test(text)) return 3;
	if (/넷|네\s*개/.test(text)) return 4;
	// 영어
	if (/\bone\b/i.test(text)) return 1;
	if (/\btwo\b/i.test(text)) return 2;
	if (/\bthree\b/i.test(text)) return 3;
	// 일본어/중국어
	if (/[一]/.test(text)) return 1;
	if (/[二两]/.test(text)) return 2;
	if (/[三]/.test(text)) return 3;
	return 1; // 기본 1개
}

function extractHourOffset(text) {
	// "+2시간 / 2 hours / 2時間 / 2小时"
	const m = text.match(/(\d+)\s*(?:시간|hours?|時間|小时)/i);
	return m ? parseInt(m[1], 10) : null;
}

function extractHour(text) {
	// "오후 2시", "2pm", "14時", "14点", "14:00"
	// "시간/時間" 문맥은 hour-offset 이므로 여기서 제외
	if (/시간|時間|hours?|小时/i.test(text)) return null;
	const m = text.match(/(\d{1,2})\s*(?:시(?!간)|点|時(?!間)|:00|pm|am|h\b)/i);
	if (m) {
		let h = parseInt(m[1], 10);
		if (/pm/i.test(text) && h < 12) h += 12;
		return h;
	}
	return null;
}

// ─────────────────────────────────────────────
// 룰 기반 파서 (기본/폴백)
// ─────────────────────────────────────────────
export function parseIntentRule(text, ctx) {
	const lang = ctx?.perUseLang || 'ko_KR';

	// 1) 어메니티
	const items = [];
	for (const [key, code] of Object.entries(ITEM_MAP)) {
		if (matchAny(text, KW[key])) {
			items.push({ itemCd: code, qty: extractQty(text) });
		}
	}
	if (items.length > 0) {
		return {
			intent: 'amenity',
			reply: t(lang, 'thinking'),
			payload: { itemList: items, reqMemo: text }
		};
	}

	// 2) 하우스키핑
	if (matchAny(text, KW.hkClr)) {
		return {
			intent: 'housekeeping',
			reply: t(lang, 'thinking'),
			payload: { hkStatCd: 'CLR', reqMemo: text }
		};
	}
	if (matchAny(text, KW.hkDnd)) {
		return {
			intent: 'housekeeping',
			reply: t(lang, 'thinking'),
			payload: { hkStatCd: 'DND', reqMemo: text }
		};
	}
	if (matchAny(text, KW.hkMu)) {
		return {
			intent: 'housekeeping',
			reply: t(lang, 'thinking'),
			payload: { hkStatCd: 'MU', reqMemo: text }
		};
	}

	// 3) 레이트 체크아웃
	if (matchAny(text, KW.late)) {
		const baseHour = parseInt((ctx?.chkOutTm || '1100').slice(0, 2), 10);
		let reqHour = extractHour(text);
		if (reqHour == null) {
			const offset = extractHourOffset(text);
			if (offset != null) reqHour = baseHour + offset;
		}
		if (reqHour == null) reqHour = baseHour + 2; // 기본 2시간 연장
		const reqOutTm = String(reqHour).padStart(2, '0') + '00';
		return {
			intent: 'late_checkout',
			reply: t(lang, 'thinking'),
			payload: { reqOutTm }
		};
	}

	// 4) 일반 채팅
	return {
		intent: 'chat',
		reply: t(lang, 'intentChat'),
		payload: {}
	};
}

// ─────────────────────────────────────────────
// LLM 모드 (서버 프록시 경유)
// ─────────────────────────────────────────────
// 서버가 키 없음(9501)이라고 응답하면 이후 LLM 시도 차단.
let llmDisabledForSession = false;

async function parseIntentLLM(text, ctx) {
	const res = await postAiChat({ text, ctx });
	if (res.status !== 0) {
		const err = new Error(`[${res.status}] ${res.message}`);
		err.status = res.status;
		throw err;
	}
	return res.map;
}

export async function parseIntent(text, ctx) {
	if (!llmDisabledForSession) {
		try {
			return await parseIntentLLM(text, ctx);
		} catch (e) {
			if (e.status === -500) {
				// 서버에 키가 없음 → 이번 세션 내내 룰로만 동작
				llmDisabledForSession = true;
				console.info('[chat] 서버 LLM 키 미설정, 룰 모드로 고정');
			} else {
				console.warn('[chat] LLM 호출 실패, 룰 폴백:', e.message);
			}
		}
	}
	return parseIntentRule(text, ctx);
}

/**
 * 서버의 /api/ai/status 를 읽어서 LLM 배지 표시 여부 결정.
 * 서버가 꺼져 있거나 키 미설정이면 false.
 */
export async function checkLlmStatus() {
	try {
		const res = await getAiStatus();
		const enabled = res?.map?.enabled === true;
		if (!enabled) llmDisabledForSession = true;
		return enabled;
	} catch {
		llmDisabledForSession = true;
		return false;
	}
}
