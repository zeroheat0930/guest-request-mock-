package com.daol.concierge.ccs.service;

import com.daol.concierge.ccs.sync.PmsSyncAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CcsVocServiceTest {

	private CcsVocService service;
	private AtomicInteger insertCalls;
	private AtomicInteger publishCalls;
	private AtomicInteger escalateCalls;

	@BeforeEach
	void setUp() throws Exception {
		service = new CcsVocService();
		insertCalls = new AtomicInteger();
		publishCalls = new AtomicInteger();
		escalateCalls = new AtomicInteger();

		CcsLostFoundServiceTest.InvMapperStub mapperStub = new VocMapperStub(insertCalls);
		PmsSyncAdapter adapterStub = new PmsSyncAdapter() {
			@Override public void syncVoc(Map<String, Object> payload) {}
		};
		SimpMessagingTemplate msgStub = new SimpMessagingTemplate((m, t) -> true) {
			@Override
			public void convertAndSend(String destination, Object payload) {
				publishCalls.incrementAndGet();
				if (destination.startsWith("/topic/ccs/esc/")) escalateCalls.incrementAndGet();
			}
		};

		inject(service, "invMapper", mapperStub);
		inject(service, "pmsSyncAdapter", adapterStub);
		inject(service, "messagingTemplate", msgStub);
	}

	@Test
	void createReport_normal_noEscalation() {
		Map<String, Object> body = new HashMap<>();
		body.put("propCd", "0000000001");
		body.put("cmpxCd", "00001");
		body.put("category", "CLEAN");
		body.put("severity", "NORMAL");
		body.put("title", "욕실 수건이 부족합니다");
		body.put("content", "교체 부탁드립니다");

		Map<String, Object> saved = service.createReport(body);
		assertNotNull(saved);
		assertEquals(1, insertCalls.get());
		assertTrue(publishCalls.get() >= 3);
		assertEquals(0, escalateCalls.get(), "NORMAL severity 는 에스컬레이션 미발생");
	}

	@Test
	void createReport_urgent_triggersEscalation() {
		Map<String, Object> body = new HashMap<>();
		body.put("propCd", "0000000001");
		body.put("cmpxCd", "00001");
		body.put("category", "FACILITY");
		body.put("severity", "URGENT");
		body.put("title", "물이 새고 있습니다");
		body.put("content", "긴급 점검 요청");

		service.createReport(body);
		assertEquals(1, escalateCalls.get(), "URGENT severity 는 /topic/ccs/esc/ 푸시");
	}

	private static void inject(Object target, String field, Object value) throws Exception {
		java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
		f.setAccessible(true);
		f.set(target, value);
	}

	static class VocMapperStub extends CcsLostFoundServiceTest.InvMapperStub {
		private Map<String, Object> lastInserted;
		VocMapperStub(AtomicInteger insertCalls) { super(insertCalls); }

		@Override public int insertVoc(Map<String, Object> p) {
			this.lastInserted = new HashMap<>(p);
			return 1;
		}
		@Override public Map<String, Object> selectVoc(String vocId) {
			Map<String, Object> m = new HashMap<>();
			m.put("vocId", vocId);
			m.put("propCd", "0000000001");
			m.put("cmpxCd", "00001");
			m.put("statusCd", "OPEN");
			m.put("severity", lastInserted != null ? lastInserted.get("severity") : "NORMAL");
			return m;
		}
	}
}
