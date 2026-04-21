package com.daol.concierge.ccs.service;

import com.daol.concierge.ccs.sync.PmsSyncAdapter;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 대여(Rental) 서비스 — 카탈로그 + 주문/반납 재고 관리.
 * 주문 생성: STOCK_AVAILABLE 감소 → 상태 REQUESTED. 반납: 증가 → RETURNED.
 */
@Service
public class CcsRentalService {

	@Autowired private InvMapper invMapper;
	@Autowired private PmsSyncAdapter pmsSyncAdapter;
	@Autowired(required = false) SimpMessagingTemplate messagingTemplate;

	public List<Map<String, Object>> listItems(String propCd, String cmpxCd) {
		return invMapper.selectRentalItems(propCd, cmpxCd);
	}

	public Map<String, Object> upsertItem(Map<String, Object> body) {
		require(body, "propCd", "cmpxCd", "name", "category");
		String itemId = str(body.get("itemId"));
		if (itemId == null || itemId.isBlank()) {
			itemId = "RI" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
			body.put("itemId", itemId);
			body.putIfAbsent("stockTotal", 0);
			body.putIfAbsent("stockAvailable", 0);
			body.putIfAbsent("useYn", "Y");
			invMapper.insertRentalItem(body);
		} else {
			invMapper.updateRentalItem(body);
		}
		return invMapper.selectRentalItem(itemId);
	}

	public Map<String, Object> createOrder(Map<String, Object> body) {
		require(body, "propCd", "cmpxCd", "itemId");
		Map<String, Object> item = invMapper.selectRentalItem(str(body.get("itemId")));
		if (item == null) throw new ApiException(ApiStatus.NOT_FOUND, "대여 품목 없음");

		int qty = 1;
		Object q = body.get("qty");
		if (q != null) {
			try { qty = Math.max(1, Integer.parseInt(String.valueOf(q))); } catch (Exception ignore) {}
		}
		int avail = toInt(item.get("stockAvailable"));
		if (avail < qty) throw new ApiException(ApiStatus.SYSTEM_ERROR, "재고 부족 (avail=" + avail + ")");

		String orderId = "RO" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
		Map<String, Object> p = new HashMap<>(body);
		p.put("orderId", orderId);
		p.put("qty", qty);
		p.putIfAbsent("statusCd", "REQUESTED");
		invMapper.insertRentalOrder(p);

		// 재고 차감
		Map<String, Object> stockUpdate = new HashMap<>();
		stockUpdate.put("itemId", str(body.get("itemId")));
		stockUpdate.put("delta", -qty);
		invMapper.updateRentalItemStock(stockUpdate);

		Map<String, Object> saved = invMapper.selectRentalOrder(orderId);
		try { pmsSyncAdapter.syncRental(saved); } catch (Exception ignore) {}
		publish(saved);
		return saved;
	}

	public List<Map<String, Object>> listOrders(Map<String, Object> filter) {
		return invMapper.selectRentalOrderList(filter);
	}

	public Map<String, Object> loan(String orderId, String handlerId) {
		Map<String, Object> o = invMapper.selectRentalOrder(orderId);
		if (o == null) throw new ApiException(ApiStatus.NOT_FOUND, "주문 없음");
		if (!"REQUESTED".equals(str(o.get("statusCd")))) throw new ApiException(ApiStatus.SYSTEM_ERROR, "상태 전이 불가");
		Map<String, Object> p = new HashMap<>();
		p.put("orderId", orderId);
		p.put("statusCd", "LOANED");
		p.put("handlerId", handlerId);
		invMapper.updateRentalOrderStatus(p);
		Map<String, Object> saved = invMapper.selectRentalOrder(orderId);
		try { pmsSyncAdapter.syncRental(saved); } catch (Exception ignore) {}
		publish(saved);
		return saved;
	}

	public Map<String, Object> returnItem(String orderId, String handlerId) {
		Map<String, Object> o = invMapper.selectRentalOrder(orderId);
		if (o == null) throw new ApiException(ApiStatus.NOT_FOUND, "주문 없음");
		String cur = str(o.get("statusCd"));
		if (!"LOANED".equals(cur) && !"REQUESTED".equals(cur))
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "상태 전이 불가");

		Map<String, Object> p = new HashMap<>();
		p.put("orderId", orderId);
		p.put("statusCd", "RETURNED");
		p.put("handlerId", handlerId);
		invMapper.updateRentalOrderStatus(p);

		// 재고 복구
		Map<String, Object> stockUpdate = new HashMap<>();
		stockUpdate.put("itemId", str(o.get("itemId")));
		stockUpdate.put("delta", toInt(o.get("qty")));
		invMapper.updateRentalItemStock(stockUpdate);

		Map<String, Object> saved = invMapper.selectRentalOrder(orderId);
		try { pmsSyncAdapter.syncRental(saved); } catch (Exception ignore) {}
		publish(saved);
		return saved;
	}

	private static void require(Map<String, Object> p, String... keys) {
		for (String k : keys) {
			Object v = p.get(k);
			if (v == null || String.valueOf(v).isBlank())
				throw new ApiException(ApiStatus.BAD_REQUEST, "필수값 누락: " + k);
		}
	}

	void publish(Map<String, Object> order) {
		if (messagingTemplate == null || order == null) return;
		String propCd = str(order.get("propCd"));
		String cmpxCd = str(order.get("cmpxCd"));
		String handlerId = str(order.get("handlerId"));
		messagingTemplate.convertAndSend("/topic/ccs/dept/FR", order);
		if (propCd != null && cmpxCd != null)
			messagingTemplate.convertAndSend("/topic/ccs/cmpx/" + propCd + "/" + cmpxCd, order);
		if (propCd != null)
			messagingTemplate.convertAndSend("/topic/ccs/prop/" + propCd, order);
		if (handlerId != null && !handlerId.isEmpty())
			messagingTemplate.convertAndSend("/topic/ccs/staff/" + handlerId, order);
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
	private static int toInt(Object o) {
		if (o == null) return 0;
		if (o instanceof Number) return ((Number) o).intValue();
		try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
	}
}
