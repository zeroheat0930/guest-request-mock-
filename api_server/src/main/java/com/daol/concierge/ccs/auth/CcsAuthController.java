package com.daol.concierge.ccs.auth;

import com.daol.concierge.ccs.domain.CcsStaff;
import com.daol.concierge.ccs.repo.CcsStaffRepository;
import com.daol.concierge.ccs.util.CcsResponse;
import com.daol.concierge.core.api.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CCS 스태프 로그인.
 *
 * PMS 스타일 응답 봉투 ({resCd, resMsg, map}) 사용.
 * 데모 환경에서는 propCd/cmpxCd 가 생략되면 기본 프로퍼티(0000000010/00001) 로 fallback.
 */
@RestController
@RequestMapping("/api/ccs/auth")
public class CcsAuthController {

	private static final String DEFAULT_PROP_CD = "0000000010";
	private static final String DEFAULT_CMPX_CD = "00001";

	@Autowired
	private CcsStaffRepository staffRepo;

	@Autowired
	private CcsJwtService jwtService;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@PostMapping("/login")
	public Map<String, Object> login(@RequestBody Map<String, Object> body) {
		String loginId = str(body.get("loginId"));
		String password = str(body.get("password"));
		String propCd = strOr(body.get("propCd"), DEFAULT_PROP_CD);
		String cmpxCd = strOr(body.get("cmpxCd"), DEFAULT_CMPX_CD);

		if (loginId == null || loginId.isBlank() || password == null || password.isBlank()) {
			throw new BizException("9400", "loginId/password 필수");
		}

		CcsStaff staff = staffRepo.findByLoginIdAndPropCdAndCmpxCd(loginId, propCd, cmpxCd)
				.orElseThrow(() -> new BizException("9404", "계정 없음"));

		if (!"Y".equals(staff.getUseYn())) {
			throw new BizException("9102", "비활성 계정");
		}

		if (!passwordEncoder.matches(password, staff.getPasswordHash())) {
			throw new BizException("9102", "비밀번호 오류");
		}

		String token = jwtService.issue(staff);

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("token", token);
		map.put("staffId", staff.getStaffId());
		map.put("staffNm", staff.getStaffNm());
		map.put("deptCd", staff.getDeptCd());

		return CcsResponse.ok(map);
	}

	private static String str(Object o) {
		return o == null ? null : String.valueOf(o);
	}

	private static String strOr(Object o, String def) {
		String s = str(o);
		return (s == null || s.isBlank()) ? def : s;
	}
}
