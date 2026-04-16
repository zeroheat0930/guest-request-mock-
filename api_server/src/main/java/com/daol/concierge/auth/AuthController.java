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

	/**
	 * 방 번호 기반 토큰 발급 — 객실 태블릿 / QR 스캔 시나리오
	 * POST /api/auth/room-token { rmNo: "00304" }
	 * → 해당 방에 체크인 중인 투숙객 자동 조회 → JWT 발급
	 */
	@ResponseBody
	@RequestMapping(value = "/room-token", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse issueRoomToken(RequestParams requestParams) {
		return Responses.MapResponse.of(authService.issueRoomToken(requestParams.getParams()));
	}
}
