package com.daol.concierge.core.dao;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CommonDAO {

	private final SqlSession sqlSession;

	protected CommonDAO(SqlSession sqlSession) {
		sqlSession.getConfiguration().setMapUnderscoreToCamelCase(true);
		this.sqlSession = sqlSession;
	}

	public List<Map<String, Object>> selectList(String sqlId) {
		return toCamelCaseMapList(sqlSession.selectList(sqlId));
	}

	public List<Map<String, Object>> selectList(String sqlId, Object parameter) {
		return toCamelCaseMapList(sqlSession.selectList(sqlId, parameter));
	}

	public Map<String, Object> selectOne(String sqlId) {
		return toCamelCaseMap(sqlSession.selectOne(sqlId));
	}

	public Map<String, Object> selectOne(String sqlId, Object parameter) {
		return toCamelCaseMap(sqlSession.selectOne(sqlId, parameter));
	}

	public int insert(String sqlId) {
		return sqlSession.insert(sqlId);
	}

	public int insert(String sqlId, Map<String, Object> parameter) {
		return sqlSession.insert(sqlId, parameter);
	}

	public int update(String sqlId) {
		return sqlSession.update(sqlId);
	}

	public int update(String sqlId, Map<String, Object> parameter) {
		return sqlSession.update(sqlId, parameter);
	}

	public int delete(String sqlId) {
		return sqlSession.delete(sqlId);
	}

	public int delete(String sqlId, Map<String, Object> parameter) {
		return sqlSession.delete(sqlId, parameter);
	}

	private Map<String, Object> toCamelCaseMap(Map<String, Object> orgMap) {
		if (orgMap == null) return null;
		Map<String, Object> rtnMap = new HashMap<>();
		orgMap.forEach((k, v) -> rtnMap.put(toCamelCase(k), v));
		return rtnMap;
	}

	private List<Map<String, Object>> toCamelCaseMapList(List<Map<String, Object>> orgList) {
		return orgList == null ? null : orgList.stream().map(this::toCamelCaseMap).collect(Collectors.toList());
	}

	private String toCamelCase(String target) {
		StringBuilder buffer = new StringBuilder();
		for (String token : target.toLowerCase().split("_")) {
			buffer.append(StringUtils.capitalize(token));
		}
		return StringUtils.uncapitalize(buffer.toString());
	}
}
