package com.daol.concierge.nearby;

import com.daol.concierge.auth.GuestPrincipal;
import com.daol.concierge.auth.SecurityContextUtil;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import com.daol.concierge.feature.FeatureService;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping(value = "/api/concierge/nearby")
public class NearbyController extends BaseController {

	private static final Set<String> ALLOWED = Set.of("food", "cafe", "conv", "tour", "pharmacy");

	@Autowired private NearbyProvider provider;
	@Autowired private InvMapper invMapper;
	@Autowired private FeatureService featureService;

	@Value("${nearby.default-radius-m:1000}")
	private int defaultRadiusM;

	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse search(RequestParams requestParams) {
		String category = requestParams.getString("category");
		if (!ALLOWED.contains(category)) throw new ApiException(ApiStatus.SYSTEM_ERROR, "category 값이 올바르지 않습니다");

		GuestPrincipal p = SecurityContextUtil.requirePrincipal();
		Map<String, Object> ext = invMapper.selectPropertyExt(p.propCd(), p.cmpxCd());
		if (ext == null || ext.get("lat") == null || ext.get("lng") == null)
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "프로퍼티 좌표 미설정");

		double lat = ((Number) ext.get("lat")).doubleValue();
		double lng = ((Number) ext.get("lng")).doubleValue();
		// 우선순위: 어드민 기능관리의 NEARBY.configJson.radiusM > PROPERTY_EXT.nearbyRadius > yml 디폴트
		Map<String, Object> cfg = featureService.getConfig(p.propCd(), p.cmpxCd(), "NEARBY");
		int radius;
		if (cfg.get("radiusM") instanceof Number n) {
			radius = n.intValue();
		} else if (ext.get("nearbyRadius") != null) {
			radius = ((Number) ext.get("nearbyRadius")).intValue();
		} else {
			radius = defaultRadiusM;
		}

		List<NearbyPlace> places = provider.search(lat, lng, category, radius);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("places", places);
		data.put("category", category);
		data.put("radiusM", radius);
		return Responses.MapResponse.of(data);
	}
}
