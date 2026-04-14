package com.daol.concierge.auth;

import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.Responses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * 게스트 인증 컨트롤러
 *
 * POST /api/auth/guest-token: {rsvNo, chkInDt, birthDt} → {token, rsvNo, roomNo, perNm, chkOutDt, perUseLang}
 * 발급된 토큰은 이후 모든 /api/gr/**, /api/ai/chat 호출에 Authorization: Bearer <token> 로 첨부.
 */
@Controller
@ResponseBody
@RequestMapping("/api/auth")
public class AuthController {

	private static final String APPLICATION_JSON = "application/json;charset=UTF-8";

	@Autowired
	private AuthService authService;

	@PostMapping(value = "/guest-token", produces = APPLICATION_JSON)
	public ApiResponse issueGuestToken(@RequestBody Map<String, Object> params) {
		return Responses.MapResponse.of(authService.issueGuestToken(params));
	}
}
