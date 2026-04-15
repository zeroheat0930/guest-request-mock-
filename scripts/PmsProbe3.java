import java.sql.*;

public class PmsProbe3 {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mariadb://211.34.228.191:3336/PMS?connectTimeout=5000&socketTimeout=10000&useUnicode=true&characterEncoding=UTF-8";
        try (Connection c = DriverManager.getConnection(url, args[0], args[1])) {
            String prop = "0000000001", cmpx = "00001";

            System.out.println("[1] RESV_STAT distribution for " + prop + "/" + cmpx);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT RESV_STAT, COUNT(*) n, SUM(CASE WHEN RM_NO IS NOT NULL THEN 1 ELSE 0 END) with_room " +
                    "FROM PMS_RESERVATION WHERE PROP_CD=? AND CMPX_CD=? AND USE_YN='Y' GROUP BY RESV_STAT ORDER BY n DESC")) {
                ps.setString(1, prop); ps.setString(2, cmpx);
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.println("    STAT  count  with_room");
                    while (rs.next())
                        System.out.printf("    %-5s %5d  %d%n", rs.getString(1), rs.getLong(2), rs.getLong(3));
                }
            }

            System.out.println("[2] currently in-house (ARR<=today<=DEP, RM_NO set)");
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM PMS_RESERVATION WHERE PROP_CD=? AND CMPX_CD=? AND USE_YN='Y' " +
                    "AND RM_NO IS NOT NULL AND ARR_DT <= CURDATE() AND DEP_DT >= CURDATE()")) {
                ps.setString(1, prop); ps.setString(2, cmpx);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); System.out.println("    in-house now = " + rs.getLong(1)); }
            }

            System.out.println("[3] sample in-house rows (any stat)");
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT RESV_NO, RM_NO, PER_NM, ARR_DT, DEP_DT, RESV_STAT FROM PMS_RESERVATION " +
                    "WHERE PROP_CD=? AND CMPX_CD=? AND USE_YN='Y' AND RM_NO IS NOT NULL " +
                    "AND ARR_DT <= CURDATE() AND DEP_DT >= CURDATE() LIMIT 10")) {
                ps.setString(1, prop); ps.setString(2, cmpx);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        System.out.printf("    RESV=%s RM=%s PER=%s ARR=%s DEP=%s ST=%s%n",
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6));
                }
            }

            System.out.println("[4] most recent RM_NO-assigned rows regardless of date");
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT RESV_NO, RM_NO, PER_NM, ARR_DT, DEP_DT, RESV_STAT FROM PMS_RESERVATION " +
                    "WHERE PROP_CD=? AND CMPX_CD=? AND USE_YN='Y' AND RM_NO IS NOT NULL " +
                    "ORDER BY ARR_DT DESC LIMIT 10")) {
                ps.setString(1, prop); ps.setString(2, cmpx);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        System.out.printf("    RESV=%s RM=%s PER=%s ARR=%s DEP=%s ST=%s%n",
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6));
                }
            }
        }
    }
}
