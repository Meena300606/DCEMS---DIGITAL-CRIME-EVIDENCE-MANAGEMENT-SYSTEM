package com.dcems.dao;

import com.dcems.db.Database;
import com.dcems.model.Evidence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class EvidenceDAO {
    private EvidenceDAO() {}

    private static final String BASE =
            "SELECT e.*, c.case_no, u.full_name AS collector FROM evidence e " +
            "JOIN crime_cases c ON c.id=e.case_id LEFT JOIN users u ON u.id=e.collected_by ";

    private static Evidence map(ResultSet r) throws SQLException {
        Evidence e = new Evidence();
        e.id = r.getInt("id"); e.evidenceNo = r.getString("evidence_no"); e.caseId = r.getInt("case_id");
        e.caseNo = r.getString("case_no"); e.type = r.getString("type"); e.description = r.getString("description");
        e.sourceDevice = r.getString("source_device"); e.location = r.getString("location_found");
        e.fileName = r.getString("file_name"); e.filePath = r.getString("file_path");
        e.sha256 = r.getString("sha256"); e.fileSize = r.getLong("file_size");
        e.collectedBy = r.getInt("collected_by"); e.collector = r.getString("collector");
        Timestamp t = r.getTimestamp("collected_at");
        e.collectedAt = t == null ? null : t.toLocalDateTime();
        e.status = r.getString("status");
        return e;
    }

    /** Filtered search: free text + type + status (null / "All" = no filter). */
    public static List<Evidence> search(String q, String type, String status) {
        StringBuilder sql = new StringBuilder(BASE).append("WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            sql.append("AND (e.evidence_no LIKE ? OR e.description LIKE ? OR c.case_no LIKE ? OR e.file_name LIKE ? OR e.source_device LIKE ?) ");
            String like = "%" + q.trim() + "%";
            for (int i = 0; i < 5; i++) args.add(like);
        }
        if (type != null && !type.equals("All")) { sql.append("AND e.type=? "); args.add(type); }
        if (status != null && !status.equals("All")) { sql.append("AND e.status=? "); args.add(status); }
        sql.append("ORDER BY e.collected_at DESC");
        return Database.query(sql.toString(), EvidenceDAO::map, args.toArray());
    }

    public static List<Evidence> all() { return search(null, null, null); }

    public static List<Evidence> byCase(int caseId) {
        return Database.query(BASE + "WHERE e.case_id=? ORDER BY e.collected_at", EvidenceDAO::map, caseId);
    }

    public static List<Evidence> recent(int limit) {
        return Database.query(BASE + "ORDER BY e.collected_at DESC LIMIT " + limit, EvidenceDAO::map);
    }

    public static String nextEvidenceNo() {
        long n = Database.scalar("SELECT COALESCE(MAX(id),0)+1 FROM evidence");
        return String.format("EV-%d-%04d", LocalDate.now().getYear(), n);
    }

    public static int insert(Evidence e) {
        return Database.insert("INSERT INTO evidence(evidence_no,case_id,type,description,source_device,location_found,file_name,file_path,sha256,file_size,collected_by,collected_at,status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                e.evidenceNo, e.caseId, e.type, e.description, e.sourceDevice, e.location, e.fileName, e.filePath,
                e.sha256, e.fileSize, e.collectedBy == 0 ? null : e.collectedBy, e.collectedAt, e.status);
    }

    public static void updateStatus(int id, String status) {
        Database.update("UPDATE evidence SET status=? WHERE id=?", status, id);
    }

    public static void delete(int id) { Database.update("DELETE FROM evidence WHERE id=?", id); }
}
