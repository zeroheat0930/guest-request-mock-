package com.daol.concierge.nearby;

import com.daol.concierge.auth.GuestPrincipal;
import com.daol.concierge.auth.SecurityContextUtil;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.BizException;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.inv.mapper.InvMapper;
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

@Controller
@ResponseBody
@RequestMapping(value = "/api/concierge/nearby")
public class NearbyController {

	private static final Set<String> ALLOWED = Set.of("food", "cafe", "conv", "tour", "pharmacy");

	@Autowired private NearbyProvider provider;
	@Autowired private InvMapper invMapper;

	@Value("${nearby.default-radius-m:1000}")
	private int defaultRadiusM;

	@RequestMapping(method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public ApiResponse search(@RequestParam String category) {
		if (!ALLOWED.contains(category)) throw new BizException("9001", "category 값이 올바르지 않습니다");

		GuestPrincipal p = SecurityContextUtil.requirePrincipal();
		Map<String, Object> ext = invMapper.selectPropertyExt(p.propCd(), p.cmpxCd());
		if (ext == null || ext.get("lat") == null || ext.get("lng") == null)
			throw new BizException("9010", "프로퍼티 좌표 미설정");

		double lat = ((Number) ext.get("lat")).doubleValue();
		double lng = ((Number) ext.get("lng")).doubleValue();
		int radius = ext.get("nearbyRadius") != null ? ((Number) ext.get("nearbyRadius")).intValue() : defaultRadiusM;

		List<NearbyPlace> places = provider.search(lat, lng, category, radius);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("places", places);
		data.put("category", category);
		data.put("radiusM", radius);
		return Responses.MapResponse.of(data);
	}
}
