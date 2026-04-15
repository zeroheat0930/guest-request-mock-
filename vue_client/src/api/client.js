import axios from 'axios';

/**
 * API base URL 결정 규칙
 *
 * - 개발(localhost): http://localhost:8080/api
 * - LAN 접속(휴대폰 등 동일 WiFi): http://<PC-LAN-IP>:8080/api
 *     → window.location.hostname 에서 추출 (Vite가 192.168.x.x 로 서빙 중이면 그대로)
 * - HTTPS 터널(ngrok 등): https://<ngrok-host>/api
 *     → 터널이 5173/8080을 동시에 프록시하지 않는 경우 VITE_API_BASE 환경변수로 override 가능
 *
 * 번들 타임이 아니라 런타임에 결정해야 "같은 빌드 산출물"이 어디서 서빙되든 동작.
 */
function resolveApiBase() {
	const envBase = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE) || '';
	if (envBase) return envBase.replace(/\/$/, '');

	if (typeof window === 'undefined') {
		return 'http://localhost:8080/api';
	}
	const { protocol, hostname, port } = window.location;

	// ngrok 등 HTTPS 터널: 같은 오리진 + /api 가정 (리버스 프록시로 8080 연결해둔 경우)
	if (protocol === 'https:') {
		return `${protocol}//${hostname}${port ? ':' + port : ''}/api`;
	}

	// 로컬/LAN: 호스트는 같고 포트만 8080으로 스위치
	return `${protocol}//${hostname}:8080/api`;
}

const API_BASE = resolveApiBase();
export { API_BASE };

// ─────────────────────────────────────────────
// 공통 인터셉터
// ─────────────────────────────────────────────
// sessionStorage 의 JWT 를 Authorization 헤더로 자동 첨부. authBootstrap.js 가 세팅한다.
// 401 이 오면 토큰 만료로 간주 — 호출자에게 에러 전파 (ChatFab 이 재인증 플로우 담당).
function attachAuthHeader(config) {
	try {
		const token = typeof sessionStorage !== 'undefined' && sessionStorage.getItem('concierge.jwt');
		if (token) {
			config.headers = config.headers || {};
			config.headers.Authorization = `Bearer ${token}`;
		}
	} catch {}
	return config;
}

function unwrapOk(r) {
	return r.data;
}

function unwrapErr(e) {
	const data = e.response?.data;
	if (e.response?.status === 401) {
		return Promise.reject({ resCd: '9102', resMsg: '인증 필요', map: {} });
	}
	return Promise.reject(data || { resCd: '9999', resMsg: e.message, map: {} });
}

// ─────────────────────────────────────────────
// 클라이언트 인스턴스
// ─────────────────────────────────────────────
const client = axios.create({
	baseURL: `${API_BASE}/gr`,
	timeout: 5000
});
client.interceptors.request.use(attachAuthHeader);
client.interceptors.response.use(unwrapOk, unwrapErr);

// AI(LLM 프록시)는 Anthropic 왕복을 기다려야 해서 타임아웃을 여유있게
const aiClient = axios.create({
	baseURL: `${API_BASE}/ai`,
	timeout: 20000
});
aiClient.interceptors.request.use(attachAuthHeader);
aiClient.interceptors.response.use(unwrapOk, unwrapErr);

// 컨시어지 부가 기능(NEARBY 등) — /api/concierge/** 하위
const conciergeClient = axios.create({
	baseURL: `${API_BASE}/concierge`,
	timeout: 8000
});
conciergeClient.interceptors.request.use(attachAuthHeader);
conciergeClient.interceptors.response.use(unwrapOk, unwrapErr);

// 예약
export const fetchReservation     = (rsvNo) => client.get('/reservation', { params: { rsvNo } });
export const fetchReservationList = ()      => client.get('/reservation/list');

// 어메니티
export const fetchAmenityItems = ()       => client.get('/amenity/items');
export const fetchAmenityList  = (rsvNo)  => client.get('/amenity/list', { params: { rsvNo } });
export const requestAmenity    = (body)   => client.post('/amenity', body);

// 하우스키핑
export const fetchHousekeeping  = (rsvNo) => client.get('/housekeeping', { params: { rsvNo } });
export const updateHousekeeping = (body)  => client.post('/housekeeping', body);

// 레이트 체크아웃
export const checkLateCheckout   = (rsvNo, reqOutTm) => client.get('/late-checkout', { params: { rsvNo, reqOutTm } });
export const requestLateCheckout = (body)            => client.post('/late-checkout', body);

// 주차
export const fetchParkingList = (rsvNo) => client.get('/parking/list', { params: { rsvNo } });
export const requestParking   = (body)  => client.post('/parking', body);

// AI (LLM 프록시 — Spring Boot가 Anthropic 키를 보관/호출)
export const postAiChat  = (body) => aiClient.post('/chat', body);
export const getAiStatus = ()     => aiClient.get('/status');

// 주변 안내 (NEARBY) — category: food|cafe|conv|tour|pharmacy
export const fetchNearby = (category) => conciergeClient.get('/nearby', { params: { category } });

// ─────────────────────────────────────────────
// CCS (스태프 콘솔) — /api/ccs/**
// ─────────────────────────────────────────────
// 게스트 토큰(concierge.jwt)과 완전히 분리된 세션키(ccs.token)를 사용한다.
function attachCcsAuthHeader(config) {
	try {
		const token = typeof sessionStorage !== 'undefined' && sessionStorage.getItem('ccs.token');
		if (token) {
			config.headers = config.headers || {};
			config.headers.Authorization = `Bearer ${token}`;
		}
	} catch {}
	return config;
}

const ccsClient = axios.create({
	baseURL: `${API_BASE}/ccs`,
	timeout: 8000
});
ccsClient.interceptors.request.use(attachCcsAuthHeader);
ccsClient.interceptors.response.use(unwrapOk, unwrapErr);

export const postCcsLogin = ({ loginId, password, propCd, cmpxCd }) =>
	ccsClient.post('/auth/login', { loginId, password, propCd, cmpxCd });

export const fetchCcsTasks = (statusCd) =>
	ccsClient.get('/tasks', { params: statusCd ? { statusCd } : {} });

export const assignCcsTask = (taskId, assigneeId) =>
	ccsClient.put(`/tasks/${encodeURIComponent(taskId)}/assign`, { assigneeId });

export const transitionCcsTask = (taskId, statusCd) =>
	ccsClient.put(`/tasks/${encodeURIComponent(taskId)}/status`, { statusCd });

export const createCcsTask = (body) => ccsClient.post('/tasks', body);

export const fetchCcsDeptLoad = (deptCd) => ccsClient.get(`/dept/${encodeURIComponent(deptCd)}/load`);

export const fetchCcsStatsToday = (deptCd) => ccsClient.get('/stats/today', { params: { deptCd } });
