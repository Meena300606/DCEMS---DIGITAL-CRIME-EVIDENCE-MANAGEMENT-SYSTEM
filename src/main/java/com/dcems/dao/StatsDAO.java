package com.dcems.dao;

import com.dcems.db.Database;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregate queries (COUNT / GROUP BY / JOIN) used by the dashboard and reports. */
public final class StatsDAO {
    private StatsDAO() {}

    public static long totalCases()    { return Database.scalar("SELECT COUNT(*) FROM crime_cases"); }
    public static long openCases()     { return Database.scalar("SELECT COUNT(*) FROM crime_cases WHERE status<>'CLOSED'"); }
    public static long totalEvidence() { return Database.scalar("SELECT COUNT(*) FROM evidence"); }
    public static long totalSuspects() { return Database.scalar("SELECT COUNT(*) FROM suspects"); }
    public static long verifiedEvidence() { return Database.scalar("SELECT COUNT(*) FROM evidence WHERE status='VERIFIED'"); }

    private static Map<String, Long> group(String sql) {
        Map<String, Long> m = new LinkedHashMap<>();
        Database.query(sql, r -> { m.put(r.getString(1), r.getLong(2)); return 0; });
        return m;
    }

    public static Map<String, Long> evidenceByType() {
        return group("SELECT type, COUNT(*) FROM evidence GROUP BY type ORDER BY COUNT(*) DESC");
    }
    public static Map<String, Long> casesByStatus() {
        return group("SELECT status, COUNT(*) FROM crime_cases GROUP BY status");
    }
    public static Map<String, Long> evidenceByStatus() {
        return group("SELECT status, COUNT(*) FROM evidence GROUP BY status");
    }

    /** case no, title, status, evidence count, verified count, suspects count */
    public static List<String[]> caseSummary() {
        return Database.query(
                "SELECT c.case_no, c.title, c.status, COUNT(DISTINCT e.id) AS ev, " +
                "COUNT(DISTINCT CASE WHEN e.status='VERIFIED' THEN e.id END) AS ver, COUNT(DISTINCT s.id) AS sus " +
                "FROM crime_cases c LEFT JOIN evidence e ON e.case_id=c.id LEFT JOIN suspects s ON s.case_id=c.id " +
                "GROUP BY c.id, c.case_no, c.title, c.status ORDER BY c.id DESC",
                r -> new String[]{r.getString(1), r.getString(2), r.getString(3),
                        String.valueOf(r.getInt(4)), String.valueOf(r.getInt(5)), String.valueOf(r.getInt(6))});
    }
}
