package com.daol.concierge.ccs.service;

import com.daol.concierge.ccs.sync.PmsSyncAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CcsRentalServiceTest {

	private CcsRentalService service;
	private AtomicInteger insertOrderCalls;
	private AtomicInteger stockDelta;

	@BeforeEach
	void setUp() throws Exception {
		service = new CcsRentalService();
		insertOrderCalls = new AtomicInteger();
		stockDelta = new AtomicInteger();

		RentalMapperStub mapperStub = new RentalMapperStub(insertOrderCalls, stockDelta);
		PmsSyncAdapter adapterStub = new PmsSyncAdapter() {
			@Override public void syncRental(Map<String, Object> payload) {}
		};
		SimpMessagingTemplate msgStub = new SimpMessagingTemplate((m, t) -> true) {
			@Override public void convertAndSend(String destination, Object payload) {}
		};

		inject(service, "invMapper", mapperStub);
		inject(service, "pmsSyncAdapter", adapterStub);
		inject(service, "messagingTemplate", msgStub);
	}

	@Test
	void createOrder_decrementsStock() {
		Map<String, Object> body = new HashMap<>();
		body.put("propCd", "0000000001");
		body.put("cmpxCd", "00001");
		body.put("itemId", "RI001");
		body.put("qty", 2);

		service.createOrder(body);
		assertEquals(1, insertOrderCalls.get());
		assertEquals(-2, stockDelta.get(), "재고 2 감소");
	}

	@Test
	void returnItem_restoresStock() {
		service.returnItem("RO_LOANED", "S001");
		assertEquals(3, stockDelta.get(), "반납 시 재고 +3 (stub qty=3)");
	}

	private static void inject(Object target, String field, Object value) throws Exception {
		java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
		f.setAccessible(true);
		f.set(target, value);
	}

	static class RentalMapperStub extends CcsLostFoundServiceTest.InvMapperStub {
		private final AtomicInteger insertOrderCalls;
		private final AtomicInteger stockDelta;

		RentalMapperStub(AtomicInteger insertOrderCalls, AtomicInteger stockDelta) {
			super(new AtomicInteger()); // unused parent counter
			this.insertOrderCalls = insertOrderCalls;
			this.stockDelta = stockDelta;
		}

		@Override public Map<String, Object> selectRentalItem(String itemId) {
			Map<String, Object> m = new HashMap<>();
			m.put("itemId", itemId);
			m.put("stockAvailable", 10);
			return m;
		}
		@Override public int insertRentalOrder(Map<String, Object> p) {
			insertOrderCalls.incrementAndGet();
			return 1;
		}
		@Override public int updateRentalItemStock(Map<String, Object> p) {
			int d = 0;
			try { d = Integer.parseInt(String.valueOf(p.get("delta"))); } catch (Exception ignore) {}
			stockDelta.addAndGet(d);
			return 1;
		}
		@Override public Map<String, Object> selectRentalOrder(String orderId) {
			Map<String, Object> m = new HashMap<>();
			m.put("orderId", orderId);
			m.put("propCd", "0000000001");
			m.put("cmpxCd", "00001");
			m.put("itemId", "RI001");
			m.put("qty", 3);
			m.put("statusCd", "LOANED");
			return m;
		}
		@Override public int updateRentalOrderStatus(Map<String, Object> p) { return 1; }
	}
}
