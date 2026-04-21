import { ref } from 'vue';
import axios from 'axios';
import { API_BASE } from '../api/client.js';
import { getLang } from '../i18n/ui.js';

/**
 * 기능 플래그 스토어
 *
 * 앱 부팅 시 한 번 /api/concierge/features 를 호출해 현재 게스트의 프로퍼티에 활성화된 탭만 가져온다.
 * 결과는 featureCd → { useYn, sortOrd } 맵으로 보관.
 */

export const features = ref(new Map());
export const featuresLoaded = ref(false);

export const FEATURE_META = {
	AMENITY: { to: '/amenity',       icon: '🛎️', labels: { ko: '어메니티',       en: 'Amenity',       ja: 'アメニティ',           zh: '客房用品' } },
	HK:      { to: '/housekeeping',  icon: '🧹', labels: { ko: '객실 정비',      en: 'Housekeeping',  ja: 'ハウスキーピング',     zh: '客房服务' } },
	LATE_CO: { to: '/late-checkout', icon: '⏰', labels: { ko: '레이트 체크아웃', en: 'Late Checkout', ja: 'レイトチェックアウト', zh: '延迟退房' } },
	CHAT:    { to: '/chat',          icon: '🤖', labels: { ko: 'AI 컨시어지',    en: 'AI Concierge',  ja: 'AIコンシェルジュ',     zh: 'AI礼宾' } },
	NEARBY:  { to: '/nearby',        icon: '📍', labels: { ko: '주변 안내',      en: 'Nearby',        ja: '周辺案内',             zh: '周边信息' } },
	PARKING:   { to: '/parking',       icon: '🚗', labels: { ko: '주차',           en: 'Parking',       ja: '駐車',                 zh: '停车' } },
	LOSTFOUND: { to: '/lostfound',     icon: '🔍', labels: { ko: '분실물',         en: 'Lost & Found',  ja: '忘れ物',               zh: '失物招领' } },
	VOC:       { to: '/voc',           icon: '💬', labels: { ko: '불편/의견',      en: 'Feedback',      ja: 'ご意見',               zh: '意见反馈' } },
	RENTAL:    { to: '/rental',        icon: '🏷️', labels: { ko: '대여',           en: 'Rental',        ja: 'レンタル',             zh: '租借' } },
	HISTORY:   { to: '/history',       icon: '📋', labels: { ko: '요청 내역',      en: 'My Requests',   ja: 'リクエスト履歴',       zh: '请求记录' } }
};

function authHeader() {
	try {
		const token = sessionStorage.getItem('concierge.jwt');
		return token ? { Authorization: `Bearer ${token}` } : {};
	} catch {
		return {};
	}
}

export async function loadFeatures() {
	try {
		const res = await axios.get(`${API_BASE}/concierge/features`, {
			headers: authHeader(),
			timeout: 8000
		});
		const list = res.data?.list || [];
		const next = new Map();
		for (const row of list) {
			// 게스트용 API는 활성화된 것만 반환 — useYn 없으면 'Y'로 간주
			next.set(row.featureCd, { useYn: row.useYn || 'Y', sortOrd: row.sortOrd });
		}
		features.value = next;
		featuresLoaded.value = true;
	} catch (e) {
		console.warn('[features] load failed', e?.message || e);
		// 폴백: 기존 4탭만 노출해 앱이 빈 화면이 되지 않게
		const fb = new Map();
		fb.set('AMENITY', { useYn: 'Y', sortOrd: 10 });
		fb.set('HK',      { useYn: 'Y', sortOrd: 20 });
		fb.set('LATE_CO', { useYn: 'Y', sortOrd: 30 });
		fb.set('CHAT',    { useYn: 'Y', sortOrd: 40 });
		features.value = fb;
		featuresLoaded.value = true;
	}
}

export function isFeatureEnabled(featureCd) {
	const f = features.value.get(featureCd);
	return !!f && f.useYn === 'Y';
}

export function enabledSortedFeatures() {
	const out = [];
	const lang = getLang();
	for (const [cd, f] of features.value.entries()) {
		if (f.useYn !== 'Y') continue;
		const meta = FEATURE_META[cd];
		if (!meta) continue;
		const label = meta.labels[lang] || meta.labels.ko;
		out.push({ featureCd: cd, sortOrd: f.sortOrd, to: meta.to, icon: meta.icon, label });
	}
	out.sort((a, b) => a.sortOrd - b.sortOrd);
	// Always append HISTORY as a permanent nav item
	const historyMeta = FEATURE_META['HISTORY'];
	const historyLabel = historyMeta.labels[lang] || historyMeta.labels.ko;
	out.push({ featureCd: 'HISTORY', sortOrd: 99, to: historyMeta.to, icon: historyMeta.icon, label: historyLabel });
	return out;
}

export function firstEnabledPath() {
	const list = enabledSortedFeatures();
	return list.length ? list[0].to : '/amenity';
}
