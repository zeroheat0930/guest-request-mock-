/**
 * 게스트 인증 부트스트랩
 *
 * 상용화 단계(US-004)에서 백엔드가 /api/gr/** 전 엔드포인트에 JWT를 강제함.
 * 이 파일은 데모/로컬 환경에서 자동으로 인증을 거치게 하기 위한 스캐폴딩.
 *
 * ⚠️ 프로덕션 전환 시 해야 할 것:
 *   1. DEMO_RESERVATIONS 상수 제거
 *   2. 실제 로그인 UI (예약번호 + 체크인일자 + 생년월일 입력 화면) 도입
 *   3. sessionStorage 키도 고정값이 아닌 세션ID 기반으로
 */
import axios from 'axios';
import { API_BASE } from '../api/client.js';

/** 데모 자격증명 (PMS DB 실 예약과 매칭) */
export const DEMO_RESERVATIONS = [
	{
		rsvNo: '0000014632',
		chkInDt: '20260520',
		birthDt: '19920930',
		label: '정동준'
	},
	{
		rsvNo: '0000014633',
		chkInDt: '20260422',
		birthDt: '19920930',
		label: '테스트 게스트'
	}
];

const TOKEN_KEY = 'concierge.jwt';
const RSV_KEY = 'concierge.rsvNo';

export function getStoredToken() {
	try {
		return sessionStorage.getItem(TOKEN_KEY);
	} catch {
		return null;
	}
}

export function getStoredRsvNo() {
	try {
		return sessionStorage.getItem(RSV_KEY);
	} catch {
		return null;
	}
}

export function clearToken() {
	try {
		sessionStorage.removeItem(TOKEN_KEY);
		sessionStorage.removeItem(RSV_KEY);
	} catch {}
}

/**
 * 지정한 rsvNo 로 서버 인증을 받아 토큰을 저장.
 * 성공 시 { token, rsvNo, roomNo, perNm, chkOutDt, perUseLang } 반환.
 */
export async function authenticateGuest(rsvNo) {
	const creds = DEMO_RESERVATIONS.find(r => r.rsvNo === rsvNo);
	if (!creds) {
		throw new Error(`알 수 없는 예약: ${rsvNo}`);
	}
	// 인증 호출 자체는 "bootstrap" 이라 기존 client 인터셉터(토큰 주입)를 거치지 않고 별도로 쏜다.
	const res = await axios.post(`${API_BASE}/auth/guest-token`, {
		rsvNo: creds.rsvNo,
		chkInDt: creds.chkInDt,
		birthDt: creds.birthDt
	}, { timeout: 10000 });
	const data = res.data;
	if (!data || data.status !== 0) {
		throw new Error(`인증 실패 [${data?.status}] ${data?.message}`);
	}
	const token = data.map?.token;
	if (!token) throw new Error('토큰 없음');
	try {
		sessionStorage.setItem(TOKEN_KEY, token);
		sessionStorage.setItem(RSV_KEY, creds.rsvNo);
	} catch {}
	return data.map;
}
