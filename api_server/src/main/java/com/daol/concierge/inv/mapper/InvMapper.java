package com.daol.concierge.inv.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface InvMapper {

	// ── CONCIERGE_PROPERTY_EXT ──
	Map<String, Object> selectPropertyExt(@Param("propCd") String propCd, @Param("cmpxCd") String cmpxCd);
	int insertPropertyExt(Map<String, Object> param);
	int updatePropertyExt(Map<String, Object> param);

	// ── CONCIERGE_FEATURE ──
	List<Map<String, Object>> selectFeatures(@Param("propCd") String propCd, @Param("cmpxCd") String cmpxCd);
	List<Map<String, Object>> selectEnabledFeatures(@Param("propCd") String propCd, @Param("cmpxCd") String cmpxCd);
	int insertFeature(Map<String, Object> param);
	int updateFeature(Map<String, Object> param);

	// ── CONCIERGE_AMENITY_ITEM ──
	List<Map<String, Object>> selectAmenityItems(@Param("propCd") String propCd, @Param("cmpxCd") String cmpxCd);
	int insertAmenityItem(Map<String, Object> param);

	// ── CONCIERGE_AMENITY_REQ ──
	List<Map<String, Object>> selectAmenityReqList(@Param("propCd") String propCd,
	                                                @Param("cmpxCd") String cmpxCd,
	                                                @Param("resvNo") String resvNo);
	int insertAmenityReq(Map<String, Object> param);
	int updateAmenityReqStatus(Map<String, Object> param);

	// ── CONCIERGE_PARKING ──
	List<Map<String, Object>> selectParkingList(@Param("propCd") String propCd,
	                                             @Param("cmpxCd") String cmpxCd,
	                                             @Param("resvNo") String resvNo);
	int insertParking(Map<String, Object> param);
	int updateParkingSyncYn(Map<String, Object> param);

	// ── CONCIERGE_LATE_CO ──
	List<Map<String, Object>> selectLateCo(@Param("propCd") String propCd,
	                                        @Param("cmpxCd") String cmpxCd,
	                                        @Param("resvNo") String resvNo);
	int insertLateCo(Map<String, Object> param);
	int updateLateCoStatus(Map<String, Object> param);

	// ── CCS_DEPARTMENT ──
	int insertDepartment(Map<String, Object> param);
	int updateDepartment(Map<String, Object> param);
	int deleteDepartment(@Param("propCd") String propCd, @Param("cmpxCd") String cmpxCd, @Param("deptCd") String deptCd);

	// ── HK (CCS_TASK 기반) ──
	Map<String, Object> selectLatestHkTask(@Param("propCd") String propCd,
	                                        @Param("cmpxCd") String cmpxCd,
	                                        @Param("rmNo") String rmNo);
	int selectTaskCountByStatus(@Param("assigneeId") String assigneeId,
	                             @Param("statusCd") String statusCd);

	// ── CCS_TASK ──
	List<Map<String, Object>> selectTasksByDept(@Param("propCd") String propCd,
	                                             @Param("cmpxCd") String cmpxCd,
	                                             @Param("deptCd") String deptCd,
	                                             @Param("statusCd") String statusCd);
	Map<String, Object> selectTask(@Param("taskId") String taskId);
	int insertTask(Map<String, Object> param);
	int updateTaskStatus(Map<String, Object> param);
	int updateTaskAssignee(Map<String, Object> param);
	List<Map<String, Object>> selectTaskCountByAssignee(@Param("propCd") String propCd,
	                                                     @Param("cmpxCd") String cmpxCd,
	                                                     @Param("deptCd") String deptCd);
	Map<String, Object> selectTodayStats(@Param("propCd") String propCd,
	                                      @Param("cmpxCd") String cmpxCd,
	                                      @Param("deptCd") String deptCd);

	// ── CCS_LOSTFOUND (Phase B) ──
	int insertLostFound(Map<String, Object> param);
	List<Map<String, Object>> selectLostFoundList(Map<String, Object> param);
	Map<String, Object> selectLostFound(@Param("lfId") String lfId);
	int updateLostFoundStatus(Map<String, Object> param);
	int updateLostFoundMatch(Map<String, Object> param);

	// ── CCS_VOC (Phase B) ──
	int insertVoc(Map<String, Object> param);
	List<Map<String, Object>> selectVocList(Map<String, Object> param);
	Map<String, Object> selectVoc(@Param("vocId") String vocId);
	int updateVocStatus(Map<String, Object> param);
	int updateVocResolution(Map<String, Object> param);
	int updateVocSatisfaction(Map<String, Object> param);

	// ── CCS_RENTAL_ITEM (Phase D) ──
	List<Map<String, Object>> selectRentalItems(@Param("propCd") String propCd, @Param("cmpxCd") String cmpxCd);
	Map<String, Object> selectRentalItem(@Param("itemId") String itemId);
	int insertRentalItem(Map<String, Object> param);
	int updateRentalItem(Map<String, Object> param);
	int updateRentalItemStock(Map<String, Object> param);

	// ── CCS_RENTAL_ORDER (Phase D) ──
	int insertRentalOrder(Map<String, Object> param);
	List<Map<String, Object>> selectRentalOrderList(Map<String, Object> param);
	Map<String, Object> selectRentalOrder(@Param("orderId") String orderId);
	int updateRentalOrderStatus(Map<String, Object> param);

	// ── CCS_DUTY_LOG (Phase D) ──
	int insertDutyLog(Map<String, Object> param);
	List<Map<String, Object>> selectDutyLogList(Map<String, Object> param);
	Map<String, Object> selectDutyLog(@Param("logId") String logId);
	Map<String, Object> selectDutyLogToday(@Param("propCd") String propCd,
	                                        @Param("cmpxCd") String cmpxCd,
	                                        @Param("shift") String shift);
	int updateDutyLogHandover(Map<String, Object> param);
	int updateDutyLogClose(Map<String, Object> param);
	Map<String, Object> selectPmsNightAuditStatus(@Param("propCd") String propCd, @Param("dateYmd") String dateYmd);

	// ── CCS_AUDIT_LOG (Phase E) ──
	int insertAuditLog(Map<String, Object> param);
	List<Map<String, Object>> selectAuditLogList(Map<String, Object> param);

	// ── Reports (Phase E) ──
	List<Map<String, Object>> selectDailyReport(Map<String, Object> param);
	List<Map<String, Object>> selectSlaReport(Map<String, Object> param);
	List<Map<String, Object>> selectHeatmapReport(Map<String, Object> param);

	// ── CCS_ROLE_GRANT (Phase G — 메뉴별 권한) ──
	List<Map<String, Object>> selectRoleGrantsByUser(@Param("userId") String userId);
	List<String> selectGrantedMenuCdsByUser(@Param("userId") String userId);
	int upsertRoleGrant(Map<String, Object> param);
}
