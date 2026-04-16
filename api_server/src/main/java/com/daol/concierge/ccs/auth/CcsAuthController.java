package com.daol.concierge.ccs.auth;

import com.daol.concierge.ccs.util.CcsResponse;
import com.daol.concierge.core.api.BizException;
import com.daol.concierge.pms.mapper.PmsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ccs/auth")
public class CcsAuthController {

	private static final String DEFAULT_PROP_CD = "0000000001";
	private static final String DEFAULT_CMPX_CD = "00001";

	@Autowired private PmsMapper pmsMapper;
	@Autowired private CcsJwtService jwtService;

	@PostMapping("/login")
	public Map<String, Object> login(@RequestBody Map<String, Object> body) {
		String userId = str(body.get("loginId"));
		String password = str(body.get("password"));

		if (userId == null || userId.isBlank() || password == null || password.isBlank())
			throw new BizException("9400", "loginId/password 필수");

		Map<String, Object> user = pmsMapper.selectUser(userId);
		if (user == null) throw new BizException("9404", "계정 없음");
		if (!"Y".equals(str(user.get("useYn")))) throw new BizException("9102", "비활성 계정");

		String storedPw = str(user.get("userPw"));
		if (!password.equals(storedPw)) throw new BizException("9102", "비밀번호 오류");

		String propCd = str(user.get("propCd"));
		String cmpxCd = str(user.get("cmpxCd"));
		String deptCd = str(user.get("deptCd"));
		if (propCd == null) propCd = DEFAULT_PROP_CD;
		if (cmpxCd == null) cmpxCd = DEFAULT_CMPX_CD;

		String token = jwtService.issue(userId, str(user.get("userNm")),
				propCd, cmpxCd, deptCd != null ? deptCd : "HK");

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("token", token);
		map.put("staffId", userId);
		map.put("staffNm", str(user.get("userNm")));
		map.put("deptCd", deptCd);
		return CcsResponse.ok(map);
	}

	private static String str(Object o) {
		return o == null ? null : String.valueOf(o);
	}
}
