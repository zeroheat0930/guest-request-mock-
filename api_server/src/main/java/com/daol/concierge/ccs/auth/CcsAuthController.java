package com.daol.concierge.ccs.auth;

import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import com.daol.concierge.pms.mapper.PmsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/ccs/auth")
public class CcsAuthController extends BaseController {

	private static final String DEFAULT_PROP_CD = "0000000001";
	private static final String DEFAULT_CMPX_CD = "00001";

	@Autowired private PmsMapper pmsMapper;
	@Autowired private CcsJwtService jwtService;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(11);

	@ResponseBody
	@RequestMapping(value = "/login", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse login(RequestParams requestParams) {
		String userId = requestParams.getString("loginId");
		String password = requestParams.getString("password");

		if (userId == null || userId.isBlank() || password == null || password.isBlank())
			throw new ApiException(ApiStatus.BAD_REQUEST, "loginId/password 필수");

		Map<String, Object> user = pmsMapper.selectUser(userId);
		if (user == null) throw new ApiException(ApiStatus.NOT_FOUND, "계정 없음");
		if (!"Y".equals(str(user.get("useYn")))) throw new ApiException(ApiStatus.ACCESS_DENIED, "비활성 계정");

		String storedPw = str(user.get("userPw"));
		if (storedPw == null || !passwordEncoder.matches(password, storedPw))
			throw new ApiException(ApiStatus.INVALID_PASSWORD, "비밀번호 오류");

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
		return Responses.MapResponse.of(map);
	}

	private static String str(Object o) {
		return o == null ? null : String.valueOf(o);
	}
}
