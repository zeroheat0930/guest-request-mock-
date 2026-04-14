package com.daol.concierge.auth;

import com.daol.concierge.core.api.BizException;
import com.daol.concierge.gr.domain.Reservation;
import com.daol.concierge.gr.repo.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 게스트 토큰 발급 서비스
 *
 * 본인 확인 = 예약번호 + 체크인일자 + 생년월일 일치. 프로덕션에선 추가로 OTP 나 예약 전화번호 검증 등을 붙이는 게 바람직.
 */
@Service
public class AuthService {

	@Autowired
	private ReservationRepository reservationRepo;

	@Autowired
	private JwtService jwtService;

	@Transactional(readOnly = true)
	public Map<String, Object> issueGuestToken(Map<String, Object> params) {
		String rsvNo = trim(params.get("rsvNo"));
		String chkInDt = trim(params.get("chkInDt"));
		String birthDt = trim(params.get("birthDt"));

		if (rsvNo == null || chkInDt == null || birthDt == null) {
			throw new BizException("9001", "필수값 누락 (rsvNo, chkInDt, birthDt)");
		}

		Reservation r = reservationRepo.findById(rsvNo)
				.orElseThrow(() -> new BizException("9101", "본인 확인 실패"));
		if (!chkInDt.equals(r.getChkInDt()) || !birthDt.equals(r.getBirthDt())) {
			// 구체적 사유는 노출 금지 (enumeration 방지)
			throw new BizException("9101", "본인 확인 실패");
		}

		String token = jwtService.issue(rsvNo, r.getPropCd(), r.getChkOutDt());
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("token", token);
		out.put("rsvNo", rsvNo);
		out.put("roomNo", r.getRoomNo());
		out.put("perNm", r.getPerNm());
		out.put("chkOutDt", r.getChkOutDt());
		out.put("perUseLang", r.getPerUseLang());
		return out;
	}

	private static String trim(Object v) {
		if (!(v instanceof String)) return null;
		String s = ((String) v).trim();
		return s.isEmpty() ? null : s;
	}
}
