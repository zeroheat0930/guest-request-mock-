import { ref } from 'vue';
import axios from 'axios';
import { API_BASE } from '../api/client.js';

/**
 * 기능 플래그 스토어
 *
 * 앱 부팅 시 한 번 /api/concierge/features 를 호출해 현재 게스트의 프로퍼티에 활성화된 탭만 가져온다.
 * 결과는 featureCd → { useYn, sortOrd } 맵으로 보관.
 */

export const features = ref(new Map());
export const featuresLoaded = ref(false);

export const FEATURE_META = {
	AMENITY: { to: '/amenity',       icon: '🛎️', label: '어메니티',       labelEn: 'Amenity' },
	HK:      { to: '/housekeeping',  icon: '🧹', label: '객실 정비',      labelEn: 'Housekeeping' },
	LATE_CO: { to: '/late-checkout', icon: '⏰', label: '레이트 체크아웃', labelEn: 'Late Checkout' },
	CHAT:    { to: '/chat',          icon: '🤖', label: 'AI 컨시어지',    labelEn: 'Multilingual' },
	NEARBY:  { to: '/nearby',        icon: '📍', label: '주변 안내',      labelEn: 'Nearby' },
	PARKING: { to: '/parking',       icon: '🚗', label: '주차',           labelEn: 'Parking' }
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
	for (const [cd, f] of features.value.entries()) {
		if (f.useYn !== 'Y') continue;
		const meta = FEATURE_META[cd];
		if (!meta) continue;
		out.push({ featureCd: cd, sortOrd: f.sortOrd, ...meta });
	}
	out.sort((a, b) => a.sortOrd - b.sortOrd);
	return out;
}

export function firstEnabledPath() {
	const list = enabledSortedFeatures();
	return list.length ? list[0].to : '/amenity';
}
