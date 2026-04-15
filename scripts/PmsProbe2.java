import java.sql.*;

public class PmsProbe2 {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mariadb://211.34.228.191:3336/PMS?connectTimeout=5000&socketTimeout=10000&useUnicode=true&characterEncoding=UTF-8";
        String user = args[0];
        String pw = args[1];
        String prop = "0000000001";
        String cmpx = "00001";
        try (Connection c = DriverManager.getConnection(url, user, pw)) {
            System.out.println("[*] count for " + prop + "/" + cmpx);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM PMS_RESERVATION WHERE PROP_CD=? AND CMPX_CD=? AND USE_YN='Y' AND RESV_STAT IN ('R','A','I')")) {
                ps.setString(1, prop); ps.setString(2, cmpx);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); System.out.println("    active = " + rs.getLong(1)); }
            }
            System.out.println("[*] sample 5 rows");
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT RESV_NO, PROP_CD, CMPX_CD, RM_NO, PER_NM, ARR_DT, DEP_DT, DEP_HOUR, RM_TP_CD, RESV_STAT " +
                    "FROM PMS_RESERVATION WHERE PROP_CD=? AND CMPX_CD=? AND USE_YN='Y' AND RESV_STAT IN ('R','A','I') " +
                    "ORDER BY ARR_DT DESC LIMIT 5")) {
                ps.setString(1, prop); ps.setString(2, cmpx);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        System.out.printf("  RESV=%s RM=%s PER=%s ARR=%s DEP=%s HR=%s TP=%s ST=%s%n",
                            rs.getString("RESV_NO"), rs.getString("RM_NO"), rs.getString("PER_NM"),
                            rs.getString("ARR_DT"), rs.getString("DEP_DT"), rs.getString("DEP_HOUR"),
                            rs.getString("RM_TP_CD"), rs.getString("RESV_STAT"));
                    }
                }
            }
            System.out.println("[*] PMS_CUST_MGMT existence check (for perUseLang)");
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SHOW TABLES LIKE 'PMS_CUST_MGMT'")) {
                System.out.println("    PMS_CUST_MGMT exists? " + rs.next());
            }
        }
    }
}
