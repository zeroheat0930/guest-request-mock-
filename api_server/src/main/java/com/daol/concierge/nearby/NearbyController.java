package com.daol.concierge.nearby;

import com.daol.concierge.auth.GuestPrincipal;
import com.daol.concierge.auth.SecurityContextUtil;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.BizException;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.feature.ConciergeProperty;
import com.daol.concierge.feature.ConciergePropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 주변 안내(NEARBY) 컨트롤러.
 *
 * 게스트 JWT 에 담긴 propCd 로 ConciergeProperty 좌표를 조회해
 * 선택된 NearbyProvider(mock | kakao)로 위임한다.
 */
@Controller
@ResponseBody
@RequestMapping(value = "/api/concierge/nearby")
public class NearbyController {

	private static final String APPLICATION_JSON = "application/json;charset=UTF-8";
	private static final Set<String> ALLOWED = Set.of("food", "cafe", "conv", "tour", "pharmacy");

	@Autowired private NearbyProvider provider;
	@Autowired private ConciergePropertyRepository propertyRepo;

	@Value("${nearby.default-radius-m:1000}")
	private int defaultRadiusM;

	@RequestMapping(method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse search(@RequestParam String category) {
		if (!ALLOWED.contains(category)) {
			throw new BizException("9001", "category 값이 올바르지 않습니다");
		}
		GuestPrincipal p = SecurityContextUtil.requirePrincipal();
		ConciergeProperty prop = propertyRepo.findById(p.propCd())
				.orElseThrow(() -> new BizException("9010", "프로퍼티 좌표 미설정"));
		if (prop.getLat() == null || prop.getLng() == null) {
			throw new BizException("9010", "프로퍼티 좌표 미설정");
		}

		List<NearbyPlace> places = provider.search(prop.getLat(), prop.getLng(), category, defaultRadiusM);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("places", places);
		data.put("category", category);
		data.put("radiusM", defaultRadiusM);
		return Responses.MapResponse.of(data);
	}
}
