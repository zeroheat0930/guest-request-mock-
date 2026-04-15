import java.sql.*;

public class PmsProbe {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mariadb://211.34.228.191:3336/PMS?connectTimeout=5000&socketTimeout=10000";
        String user = args[0];
        String pw = args[1];
        System.out.println("[*] connecting as " + user + " ...");
        try (Connection c = DriverManager.getConnection(url, user, pw)) {
            System.out.println("[OK] connected. product=" + c.getMetaData().getDatabaseProductVersion());
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT CURRENT_USER(), DATABASE(), VERSION()")) {
                if (rs.next()) {
                    System.out.println("    user=" + rs.getString(1));
                    System.out.println("    db  =" + rs.getString(2));
                    System.out.println("    ver =" + rs.getString(3));
                }
            }
            System.out.println("[*] counting PMS_RESERVATION ...");
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM PMS_RESERVATION WHERE USE_YN='Y' AND RESV_STAT IN ('R','A','I')")) {
                if (rs.next()) System.out.println("    active rows = " + rs.getLong(1));
            }
            System.out.println("[*] sample PROP_CD/CMPX_CD distribution ...");
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT PROP_CD, CMPX_CD, COUNT(*) AS n FROM PMS_RESERVATION WHERE USE_YN='Y' AND RESV_STAT IN ('R','A','I') GROUP BY PROP_CD, CMPX_CD ORDER BY n DESC LIMIT 10")) {
                while (rs.next()) {
                    System.out.printf("    %s / %s  = %d%n", rs.getString(1), rs.getString(2), rs.getLong(3));
                }
            }
        } catch (SQLException e) {
            System.out.println("[FAIL] " + e.getErrorCode() + " / " + e.getSQLState() + " / " + e.getMessage());
            System.exit(2);
        }
    }
}
