package com.daol.concierge.ccs.rental;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.service.CcsRentalService;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "CCS Rental", description = "대여 카탈로그 + 주문 (Phase D)")
@Controller
@RequestMapping("/api/ccs/rental")
public class CcsRentalController extends BaseController {

	@Autowired private CcsRentalService service;

	@Operation(summary = "대여 물품 카탈로그 조회")
	@ResponseBody
	@RequestMapping(value = "/items", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse items() {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		List<Map<String, Object>> list = service.listItems(me.propCd(), me.cmpxCd());
		return Responses.MapResponse.of(Map.of("list", list));
	}

	@Operation(summary = "대여 물품 등록/수정")
	@ResponseBody
	@RequestMapping(value = "/items", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse upsertItem(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> body = new HashMap<>(requestParams.getParams());
		body.putIfAbsent("propCd", me.propCd());
		body.putIfAbsent("cmpxCd", me.cmpxCd());
		Map<String, Object> saved = service.upsertItem(body);
		return Responses.MapResponse.of(saved);
	}

	@Operation(summary = "대여 물품 수정")
	@ResponseBody
	@RequestMapping(value = "/items/{itemId}", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse updateItem(@PathVariable String itemId, RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> body = new HashMap<>(requestParams.getParams());
		body.put("itemId", itemId);
		body.putIfAbsent("propCd", me.propCd());
		body.putIfAbsent("cmpxCd", me.cmpxCd());
		Map<String, Object> saved = service.upsertItem(body);
		return Responses.MapResponse.of(saved);
	}

	@Operation(summary = "대여 주문 생성 (재고 감소)")
	@ResponseBody
	@RequestMapping(value = "/orders", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse createOrder(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> body = new HashMap<>(requestParams.getParams());
		body.putIfAbsent("propCd", me.propCd());
		body.putIfAbsent("cmpxCd", me.cmpxCd());
		Map<String, Object> saved = service.createOrder(body);
		return Responses.MapResponse.of(saved);
	}

	@Operation(summary = "대여 주문 조회")
	@ResponseBody
	@RequestMapping(value = "/orders", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse listOrders(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> filter = new HashMap<>();
		filter.put("propCd", me.propCd());
		filter.put("cmpxCd", me.cmpxCd());
		filter.put("statusCd", requestParams.getString("statusCd"));
		filter.put("rsvNo", requestParams.getString("rsvNo"));
		List<Map<String, Object>> list = service.listOrders(filter);
		return Responses.MapResponse.of(Map.of("list", list));
	}

	@Operation(summary = "대여 출고")
	@ResponseBody
	@RequestMapping(value = "/orders/{orderId}/loan", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse loan(@PathVariable String orderId) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> saved = service.loan(orderId, me.staffId());
		return Responses.MapResponse.of(saved);
	}

	@Operation(summary = "대여 반납 (재고 복구)")
	@ResponseBody
	@RequestMapping(value = "/orders/{orderId}/return", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse returnItem(@PathVariable String orderId) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		Map<String, Object> saved = service.returnItem(orderId, me.staffId());
		return Responses.MapResponse.of(saved);
	}
}
