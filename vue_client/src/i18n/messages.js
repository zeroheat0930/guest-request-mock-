/**
 * 챗봇 다국어 메시지 사전.
 * 예약의 perUseLang(ko_KR / en_US / ja_JP / zh_CN)에 맞춰 응답.
 */
export const LANGS = ['ko_KR', 'en_US', 'ja_JP', 'zh_CN'];

export const dict = {
	ko_KR: {
		chatTitle: 'AI 컨시어지',
		chatPlaceholder: '무엇을 도와드릴까요?',
		send: '보내기',
		welcome: '안녕하세요. 무엇을 도와드릴까요? (예: 수건 2개 가져다 주세요)',
		thinking: '확인 중...',
		amenitySuccess: '✅ 요청이 프론트 데스크로 전달되었습니다 (요청번호: {reqNo})',
		hkSuccess: '✅ {stat} 요청이 처리되었습니다 (요청번호: {reqNo})',
		lateSuccess: '✅ 레이트 체크아웃이 신청되었습니다 (요금 {amt}원, 요청번호: {reqNo})',
		lateInfo: '💡 {tm}까지 가능하며 요금은 {amt}원입니다. 진행할까요? (네/아니오)',
		lateNotAvail: '죄송합니다. 요청하신 시간으로는 연장이 불가합니다.',
		failPrefix: '❌ 처리 실패',
		intentChat: '문의 감사합니다. 더 필요하신 것 있으시면 말씀해 주세요.',
		hkMu: '객실정비',
		hkDnd: '방해금지',
		hkClr: '방해금지 해제'
	},
	en_US: {
		chatTitle: 'AI Concierge',
		chatPlaceholder: 'How can I help?',
		send: 'Send',
		welcome: 'Hello! How can I help you? (e.g., "Please bring 2 towels")',
		thinking: 'Checking...',
		amenitySuccess: '✅ Your request has been sent to the front desk (Req #: {reqNo})',
		hkSuccess: '✅ {stat} request processed (Req #: {reqNo})',
		lateSuccess: '✅ Late checkout confirmed (Fee {amt} KRW, Req #: {reqNo})',
		lateInfo: '💡 Available until {tm}, fee {amt} KRW. Proceed? (yes/no)',
		lateNotAvail: 'Sorry, the requested time is not available for extension.',
		failPrefix: '❌ Request failed',
		intentChat: 'Thank you. Let me know if you need anything else.',
		hkMu: 'Make Up Room',
		hkDnd: 'Do Not Disturb',
		hkClr: 'Clear DND'
	},
	ja_JP: {
		chatTitle: 'AIコンシェルジュ',
		chatPlaceholder: 'ご用件をどうぞ',
		send: '送信',
		welcome: 'こんにちは。ご用件をどうぞ。（例：タオルを2枚ください）',
		thinking: '確認中...',
		amenitySuccess: '✅ フロントへ依頼を送信しました（依頼番号：{reqNo}）',
		hkSuccess: '✅ {stat}の依頼を処理しました（依頼番号：{reqNo}）',
		lateSuccess: '✅ レイトチェックアウトを承りました（料金 {amt}KRW、依頼番号：{reqNo}）',
		lateInfo: '💡 {tm}まで可能、料金は {amt}KRWです。進めますか？（はい/いいえ）',
		lateNotAvail: '申し訳ございません。ご希望の時間での延長はできません。',
		failPrefix: '❌ 処理に失敗しました',
		intentChat: 'お問い合わせありがとうございます。他にご要望があればお申し付けください。',
		hkMu: '客室清掃',
		hkDnd: 'お休み中',
		hkClr: 'お休み中解除'
	},
	zh_CN: {
		chatTitle: 'AI礼宾',
		chatPlaceholder: '请问需要什么帮助？',
		send: '发送',
		welcome: '您好，请问需要什么帮助？（例如：请送两瓶水）',
		thinking: '正在确认...',
		amenitySuccess: '✅ 已转交前台（请求号：{reqNo}）',
		hkSuccess: '✅ {stat}请求已处理（请求号：{reqNo}）',
		lateSuccess: '✅ 延迟退房已确认（费用 {amt} 韩元，请求号：{reqNo}）',
		lateInfo: '💡 可延长至 {tm}，费用 {amt} 韩元。是否继续？（是/否）',
		lateNotAvail: '抱歉，所请求的时间无法延长。',
		failPrefix: '❌ 处理失败',
		intentChat: '感谢您的咨询。如有其他需要请告诉我。',
		hkMu: '清扫房间',
		hkDnd: '请勿打扰',
		hkClr: '取消请勿打扰'
	}
};

export function t(lang, key, vars = {}) {
	const safeLang = dict[lang] ? lang : 'en_US';
	const tpl = dict[safeLang][key] || dict.en_US[key] || key;
	return tpl.replace(/\{(\w+)\}/g, (_, k) => (vars[k] != null ? vars[k] : ''));
}
