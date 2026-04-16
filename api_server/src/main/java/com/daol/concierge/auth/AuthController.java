package com.daol.concierge.auth;

import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 게스트 인증 컨트롤러
 *
 * POST /api/auth/guest-token: {rsvNo, chkInDt, birthDt} → {token, rsvNo, roomNo, perNm, chkOutDt, perUseLang}
 * 발급된 토큰은 이후 모든 /api/gr/**, /api/ai/chat 호출에 Authorization: Bearer <token> 로 첨부.
 */
@Controller
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

	@Autowired
	private AuthService authService;

	@ResponseBody
	@RequestMapping(value = "/guest-token", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse issueGuestToken(RequestParams requestParams) {
		return Responses.MapResponse.of(authService.issueGuestToken(requestParams.getParams()));
	}
}
