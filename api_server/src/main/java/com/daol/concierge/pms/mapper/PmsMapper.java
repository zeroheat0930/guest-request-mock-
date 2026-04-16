package com.daol.concierge.pms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PmsMapper {

	Map<String, Object> selectReservation(@Param("propCd") String propCd,
	                                       @Param("cmpxCd") String cmpxCd,
	                                       @Param("resvNo") String resvNo);

	List<Map<String, Object>> selectCheckedInReservations(@Param("propCd") String propCd,
	                                                       @Param("cmpxCd") String cmpxCd);

	List<Map<String, Object>> selectReservationList(@Param("propCd") String propCd,
	                                                 @Param("cmpxCd") String cmpxCd);

	Map<String, Object> selectCheckedInByRoom(@Param("propCd") String propCd,
	                                           @Param("cmpxCd") String cmpxCd,
	                                           @Param("rmNo") String rmNo);

	Map<String, Object> selectComplexConf(@Param("propCd") String propCd,
	                                      @Param("cmpxCd") String cmpxCd);

	Map<String, Object> selectComplex(@Param("propCd") String propCd,
	                                   @Param("cmpxCd") String cmpxCd);

	Map<String, Object> selectUser(@Param("userId") String userId);

	List<Map<String, Object>> selectUsersByDept(@Param("propCd") String propCd,
	                                             @Param("cmpxCd") String cmpxCd,
	                                             @Param("deptCd") String deptCd);
}
