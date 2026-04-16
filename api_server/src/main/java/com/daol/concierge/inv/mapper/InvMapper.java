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
}
