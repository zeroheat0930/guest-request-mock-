package com.daol.concierge.ai;

import com.daol.concierge.auth.SecurityContextUtil;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM 프록시 컨트롤러
 *
 * 프론트엔드가 Anthropic API를 직접 호출하면 브라우저 번들에 키가 노출되므로
 * 서버 경유로 전환. 서버는 환경변수 ANTHROPIC_API_KEY에서만 키를 읽음.
 */
@Controller
@RequestMapping(value = "/api/ai")
public class AiChatController extends BaseController {

	@Autowired
	private AiChatService aiChatService;

	@Autowired
	private AiRateLimiter rateLimiter;

	/**
	 * 자연어 의도 파싱
	 * Request body: { "text": "...", "ctx": { rsvNo, roomNo, chkOutTm, perUseLang } }
	 * Response map: { intent, reply, payload }
	 */
	@ResponseBody
	@RequestMapping(value = "/chat", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse chat(RequestParams requestParams) {
		String rsvNo = SecurityContextUtil.requirePrincipal().rsvNo();
		if (!rateLimiter.isAllowed(rsvNo)) {
			throw new ApiException(ApiStatus.BAD_REQUEST, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
		}
		return Responses.MapResponse.of(aiChatService.parseIntent(requestParams.getParams()));
	}

	/**
	 * LLM 사용 가능 여부 조회 (서버에 키가 설정돼 있는지)
	 * 프론트 챗봇의 'LLM/Rule' 배지 표시용
	 */
	@ResponseBody
	@RequestMapping(value = "/status", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse status() {
		Map<String, Object> m = new HashMap<>();
		m.put("enabled", aiChatService.isConfigured());
		m.put("model", aiChatService.getModel());
		return Responses.MapResponse.of(m);
	}
}
