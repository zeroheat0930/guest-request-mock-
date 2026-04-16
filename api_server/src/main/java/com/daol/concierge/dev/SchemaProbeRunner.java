package com.daol.concierge.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 임시 스키마 프로브 — PMS/INV 실제 테이블 컬럼 덤프.
 * 엔티티 재매핑 전에 한 번만 쓰고 삭제 예정.
 */
@Component
@Profile("dev")
public class SchemaProbeRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(SchemaProbeRunner.class);

	private final DataSource ds;

	public SchemaProbeRunner(DataSource ds) {
		this.ds = ds;
	}

	@Override
	public void run(String... args) throws Exception {
		String[][] targets = {
			{"PMS", "PMS_RESERVATION"},
			{"PMS", "PMS_USER_MTR"},
			{"PMS", "PMS_PROPERTY"},
			{"PMS", "PMS_COMPLEX"},
			{"PMS", "PMS_CUST_MGMT"},
			{"INV", "CONCIERGE_FEATURE"},
			{"INV", "CONCIERGE_PROPERTY_EXT"},
			{"INV", "CONCIERGE_AMENITY_ITEM"},
			{"INV", "CONCIERGE_AMENITY_REQ"},
			{"INV", "CONCIERGE_LATE_CO"},
			{"INV", "CONCIERGE_PARKING"},
			{"INV", "CCS_TASK"}
		};
		try (Connection c = ds.getConnection()) {
			log.info("[SCHEMA-PROBE] connected product={} version={}",
				c.getMetaData().getDatabaseProductName(), c.getMetaData().getDatabaseProductVersion());
			for (String[] t : targets) {
				dumpColumns(c, t[0], t[1]);
			}
		}
	}

	private void dumpColumns(Connection c, String schema, String table) {
		String sql = "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY, COLUMN_DEFAULT, COLUMN_COMMENT "
				+ "FROM INFORMATION_SCHEMA.COLUMNS "
				+ "WHERE TABLE_SCHEMA = '" + schema + "' AND TABLE_NAME = '" + table + "' "
				+ "ORDER BY ORDINAL_POSITION";
		try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
			StringBuilder sb = new StringBuilder();
			sb.append("\n===== ").append(schema).append(".").append(table).append(" =====\n");
			int n = 0;
			while (rs.next()) {
				sb.append(String.format("  %-25s %-20s %-4s %-4s%n",
					rs.getString(1),
					rs.getString(2),
					rs.getString(3),
					rs.getString(4) == null ? "" : rs.getString(4)));
				n++;
			}
			if (n == 0) {
				sb.append("  (table not found)\n");
			} else {
				sb.append("  -- ").append(n).append(" columns --\n");
			}
			log.info(sb.toString());
		} catch (Exception e) {
			log.warn("[SCHEMA-PROBE] {} {} failed: {}", schema, table, e.getMessage());
		}
	}
}
