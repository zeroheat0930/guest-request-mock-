package com.daol.concierge.ai;

import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.Responses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
@ResponseBody
@RequestMapping(value = "/api/ai")
public class AiChatController {

	private static final String APPLICATION_JSON = "application/json;charset=UTF-8";

	@Autowired
	private AiChatService aiChatService;

	/**
	 * 자연어 의도 파싱
	 * Request body: { "text": "...", "ctx": { rsvNo, roomNo, chkOutTm, perUseLang } }
	 * Response map: { intent, reply, payload }
	 */
	@PostMapping(value = "/chat", produces = APPLICATION_JSON)
	public ApiResponse chat(@RequestBody Map<String, Object> params) {
		return Responses.MapResponse.of(aiChatService.parseIntent(params));
	}

	/**
	 * LLM 사용 가능 여부 조회 (서버에 키가 설정돼 있는지)
	 * 프론트 챗봇의 'LLM/Rule' 배지 표시용
	 */
	@GetMapping(value = "/status", produces = APPLICATION_JSON)
	public ApiResponse status() {
		Map<String, Object> m = new HashMap<>();
		m.put("enabled", aiChatService.isConfigured());
		m.put("model", aiChatService.getModel());
		return Responses.MapResponse.of(m);
	}
}
