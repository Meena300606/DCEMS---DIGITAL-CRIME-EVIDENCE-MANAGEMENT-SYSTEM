package com.dcems.dao;

import com.dcems.db.Database;
import com.dcems.model.CustodyEntry;
import com.dcems.util.Session;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public final class CustodyDAO {
    private CustodyDAO() {}

    private static final String BASE =
            "SELECT l.*, e.evidence_no FROM custody_log l JOIN evidence e ON e.id=l.evidence_id ";

    private static CustodyEntry map(ResultSet r) throws SQLException {
        CustodyEntry c = new CustodyEntry();
        c.id = r.getInt("id"); c.evidenceId = r.getInt("evidence_id"); c.evidenceNo = r.getString("evidence_no");
        c.action = r.getString("action"); c.fromPerson = r.getString("from_person"); c.toPerson = r.getString("to_person");
        c.remarks = r.getString("remarks"); c.loggedBy = r.getString("logged_by");
        c.loggedAt = r.getTimestamp("logged_at").toLocalDateTime();
        return c;
    }

    /** evidenceId = 0 returns the complete log. */
    public static List<CustodyEntry> list(int evidenceId) {
        if (evidenceId == 0) return Database.query(BASE + "ORDER BY l.logged_at DESC, l.id DESC", CustodyDAO::map);
        return Database.query(BASE + "WHERE l.evidence_id=? ORDER BY l.logged_at DESC, l.id DESC", CustodyDAO::map, evidenceId);
    }

    public static void add(int evidenceId, String action, String from, String to, String remarks) {
        Database.insert("INSERT INTO custody_log(evidence_id,action,from_person,to_person,remarks,logged_by,logged_at) VALUES (?,?,?,?,?,?,?)",
                evidenceId, action, from, to, remarks, Session.name(), LocalDateTime.now().withNano(0));
    }
}
