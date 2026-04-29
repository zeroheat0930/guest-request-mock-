package com.daol.concierge.ccs.service;

import com.daol.concierge.ccs.sync.PmsSyncAdapter;
import com.daol.concierge.inv.mapper.InvMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class CcsLostFoundServiceTest {

	private CcsLostFoundService service;
	private AtomicInteger insertCalls;
	private AtomicInteger publishCalls;
	private AtomicInteger syncCalls;

	@BeforeEach
	void setUp() throws Exception {
		service = new CcsLostFoundService();
		insertCalls = new AtomicInteger();
		publishCalls = new AtomicInteger();
		syncCalls = new AtomicInteger();

		InvMapper mapperStub = new InvMapperStub(insertCalls);
		PmsSyncAdapter adapterStub = new PmsSyncAdapter() {
			@Override public void syncLostFound(Map<String, Object> payload) { syncCalls.incrementAndGet(); }
		};
		SimpMessagingTemplate msgStub = new SimpMessagingTemplate((message, timeout) -> true) {
			@Override
			public void convertAndSend(String destination, Object payload) {
				publishCalls.incrementAndGet();
			}
		};

		// 리플렉션으로 필드 주입 — @Autowired 대체
		inject(service, "invMapper", mapperStub);
		inject(service, "pmsSyncAdapter", adapterStub);
		inject(service, "messagingTemplate", msgStub);
	}

	@Test
	void createReport_persistsAndPublishes() {
		Map<String, Object> body = new HashMap<>();
		body.put("propCd", "0000000001");
		body.put("cmpxCd", "00001");
		body.put("reporterType", "GUEST");
		body.put("reporterRef", "RSV-1");
		body.put("category", "WALLET");
		body.put("itemName", "검은색 지갑");
		body.put("description", "주머니에서 빠진 것 같습니다");
		body.put("rmNo", "702");

		Map<String, Object> saved = service.createReport(body);

		assertNotNull(saved);
		assertEquals(1, insertCalls.get());
		assertEquals(1, syncCalls.get());
		// 5개 토픽 (dept/cmpx/prop + 담당자 없음 → 3개) — handler null 이라 4개 채널 중 3개만
		assertTrue(publishCalls.get() >= 3);
	}

	@Test
	void createReport_failsWithoutRequired() {
		Map<String, Object> body = new HashMap<>();
		body.put("propCd", "P");
		// 필수값 누락 — cmpxCd/category/itemName
		assertThrows(Exception.class, () -> service.createReport(body));
	}

	private static void inject(Object target, String field, Object value) throws Exception {
		java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
		f.setAccessible(true);
		f.set(target, value);
	}

	/** 최소한의 no-op 매퍼 stub — 실제 DB 접근 없이 메서드 호출만 카운트. */
	public static class InvMapperStub implements InvMapper {
		private final AtomicInteger insertCalls;
		public InvMapperStub(AtomicInteger insertCalls) { this.insertCalls = insertCalls; }

		@Override public int insertLostFound(Map<String, Object> param) { insertCalls.incrementAndGet(); return 1; }
		@Override public Map<String, Object> selectLostFound(String lfId) {
			Map<String, Object> m = new HashMap<>();
			m.put("lfId", lfId);
			m.put("propCd", "0000000001");
			m.put("cmpxCd", "00001");
			m.put("statusCd", "REPORTED");
			return m;
		}

		// ── 미사용 메서드는 전부 throw / 기본값 ──
		@Override public Map<String, Object> selectPropertyExt(String a, String b) { return null; }
		@Override public int insertPropertyExt(Map<String, Object> p) { return 0; }
		@Override public int updatePropertyExt(Map<String, Object> p) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectFeatures(String a, String b) { return null; }
		@Override public java.util.List<Map<String, Object>> selectEnabledFeatures(String a, String b) { return null; }
		@Override public int insertFeature(Map<String, Object> p) { return 0; }
		@Override public int updateFeature(Map<String, Object> p) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectAmenityItems(String a, String b) { return null; }
		@Override public int insertAmenityItem(Map<String, Object> p) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectAmenityReqList(String a, String b, String c) { return null; }
		@Override public int insertAmenityReq(Map<String, Object> p) { return 0; }
		@Override public int updateAmenityReqStatus(Map<String, Object> p) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectParkingList(String a, String b, String c) { return null; }
		@Override public int insertParking(Map<String, Object> p) { return 0; }
		@Override public int updateParkingSyncYn(Map<String, Object> p) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectLateCo(String a, String b, String c) { return null; }
		@Override public int insertLateCo(Map<String, Object> p) { return 0; }
		@Override public int updateLateCoStatus(Map<String, Object> p) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectDepartmentList(String a, String b) { return null; }
		@Override public Map<String, Object> selectDepartment(String a, String b, String c) { return null; }
		@Override public int insertDepartment(Map<String, Object> p) { return 0; }
		@Override public int updateDepartment(Map<String, Object> p) { return 0; }
		@Override public int deleteDepartment(String a, String b, String c) { return 0; }
		@Override public Map<String, Object> selectLatestHkTask(String a, String b, String c) { return null; }
		@Override public int selectTaskCountByStatus(String a, String b) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectTasksByDept(String a, String b, String c, String d) { return null; }
		@Override public Map<String, Object> selectTask(String a) { return null; }
		@Override public int insertTask(Map<String, Object> p) { return 0; }
		@Override public int updateTaskStatus(Map<String, Object> p) { return 0; }
		@Override public int updateTaskAssignee(Map<String, Object> p) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectTaskCountByAssignee(String a, String b, String c) { return null; }
		@Override public Map<String, Object> selectTodayStats(String a, String b, String c) { return null; }
		@Override public java.util.List<Map<String, Object>> selectLostFoundList(Map<String, Object> p) { return null; }
		@Override public int updateLostFoundStatus(Map<String, Object> p) { return 0; }
		@Override public int updateLostFoundMatch(Map<String, Object> p) { return 0; }
		@Override public int insertVoc(Map<String, Object> p) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectVocList(Map<String, Object> p) { return null; }
		@Override public Map<String, Object> selectVoc(String a) { return null; }
		@Override public int updateVocStatus(Map<String, Object> p) { return 0; }
		@Override public int updateVocResolution(Map<String, Object> p) { return 0; }
		@Override public int updateVocSatisfaction(Map<String, Object> p) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectRentalItems(String a, String b) { return null; }
		@Override public Map<String, Object> selectRentalItem(String a) { return null; }
		@Override public int insertRentalItem(Map<String, Object> p) { return 0; }
		@Override public int updateRentalItem(Map<String, Object> p) { return 0; }
		@Override public int updateRentalItemStock(Map<String, Object> p) { return 0; }
		@Override public int insertRentalOrder(Map<String, Object> p) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectRentalOrderList(Map<String, Object> p) { return null; }
		@Override public Map<String, Object> selectRentalOrder(String a) { return null; }
		@Override public int updateRentalOrderStatus(Map<String, Object> p) { return 0; }
		@Override public int insertDutyLog(Map<String, Object> p) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectDutyLogList(Map<String, Object> p) { return null; }
		@Override public Map<String, Object> selectDutyLog(String a) { return null; }
		@Override public Map<String, Object> selectDutyLogToday(String a, String b, String c) { return null; }
		@Override public int updateDutyLogHandover(Map<String, Object> p) { return 0; }
		@Override public int updateDutyLogClose(Map<String, Object> p) { return 0; }
		@Override public int deleteDutyLog(String logId) { return 0; }
		@Override public Map<String, Object> selectPmsNightAuditStatus(String a, String b) { return null; }
		@Override public int insertAuditLog(Map<String, Object> p) { return 0; }
		@Override public java.util.List<Map<String, Object>> selectAuditLogList(Map<String, Object> p) { return null; }
		@Override public java.util.List<Map<String, Object>> selectDailyReport(Map<String, Object> p) { return null; }
		@Override public java.util.List<Map<String, Object>> selectSlaReport(Map<String, Object> p) { return null; }
		@Override public java.util.List<Map<String, Object>> selectHeatmapReport(Map<String, Object> p) { return null; }
		@Override public java.util.List<Map<String, Object>> selectRoleGrantsByUser(String userId) { return java.util.Collections.emptyList(); }
		@Override public java.util.List<String> selectGrantedMenuCdsByUser(String userId) { return java.util.Collections.emptyList(); }
		@Override public int upsertRoleGrant(Map<String, Object> p) { return 1; }
	}
}
