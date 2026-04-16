package com.daol.concierge.auth;

import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.pms.mapper.PmsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	@Autowired private PmsMapper pmsMapper;
	@Autowired private JwtService jwtService;

	@Value("${concierge.tenant.prop-cd:0000000001}") private String tenantPropCd;
	@Value("${concierge.tenant.cmpx-cd:00001}") private String tenantCmpxCd;

	public Map<String, Object> issueGuestToken(Map<String, Object> params) {
		String rsvNo = trim(params.get("rsvNo"));
		String chkInDt = trim(params.get("chkInDt"));
		String birthDt = trim(params.get("birthDt"));

		if (rsvNo == null || chkInDt == null) {
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "필수값 누락 (rsvNo, chkInDt)");
		}

		log.info("[AUTH] tenant={}/{} rsvNo={} chkInDt={}", tenantPropCd, tenantCmpxCd, rsvNo, chkInDt);
		Map<String, Object> rsv = pmsMapper.selectReservation(tenantPropCd, tenantCmpxCd, rsvNo);
		log.info("[AUTH] rsv={}", rsv);
		if (rsv == null) {
			throw new ApiException(ApiStatus.INVALID_PASSWORD, "본인 확인 실패");
		}

		String arrDt = str(rsv.get("arrDt"));
		if (arrDt != null) arrDt = arrDt.replace("-", "");
		String inputDt = chkInDt.replace("-", "");
		log.info("[AUTH] arrDt={} inputDt={} match={}", arrDt, inputDt, inputDt.equals(arrDt));
		if (!inputDt.equals(arrDt)) {
			throw new ApiException(ApiStatus.INVALID_PASSWORD, "본인 확인 실패");
		}

		String depDt = str(rsv.get("depDt"));
		String token = jwtService.issue(rsvNo, tenantPropCd, tenantCmpxCd, depDt != null ? depDt : "99991231");

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("token", token);
		out.put("rsvNo", rsvNo);
		out.put("roomNo", str(rsv.get("rmNo")));
		out.put("perNm", str(rsv.get("perNm")));
		out.put("chkOutDt", depDt);
		out.put("perUseLang", str(rsv.get("perUseLang")));
		return out;
	}

	/**
	 * 방 번호 기반 토큰 발급 — 태블릿/QR 시나리오
	 * 해당 방에 현재 체크인 중인(resvStat='I') 투숙객이 있으면 토큰 발급.
	 */
	public Map<String, Object> issueRoomToken(Map<String, Object> params) {
		String rmNo = trim(params.get("rmNo"));
		if (rmNo == null) {
			throw new ApiException(ApiStatus.BAD_REQUEST, "객실번호(rmNo) 필수");
		}

		log.info("[AUTH-ROOM] tenant={}/{} rmNo={}", tenantPropCd, tenantCmpxCd, rmNo);
		Map<String, Object> rsv = pmsMapper.selectCheckedInByRoom(tenantPropCd, tenantCmpxCd, rmNo);
		if (rsv == null) {
			throw new ApiException(ApiStatus.NOT_FOUND, "현재 해당 객실에 체크인된 투숙객이 없습니다");
		}

		String rsvNo = str(rsv.get("resvNo"));
		String depDt = str(rsv.get("depDt"));
		String token = jwtService.issue(rsvNo, tenantPropCd, tenantCmpxCd, depDt != null ? depDt : "99991231");

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("token", token);
		out.put("rsvNo", rsvNo);
		out.put("roomNo", str(rsv.get("rmNo")));
		out.put("perNm", str(rsv.get("perNm")));
		out.put("chkOutDt", depDt);
		out.put("perUseLang", str(rsv.get("perUseLang")));
		return out;
	}

	private static String trim(Object v) {
		if (!(v instanceof String)) return null;
		String s = ((String) v).trim();
		return s.isEmpty() ? null : s;
	}

	private static String str(Object o) {
		return o == null ? null : String.valueOf(o);
	}
}
