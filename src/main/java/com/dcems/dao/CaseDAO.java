package com.dcems.dao;

import com.dcems.db.Database;
import com.dcems.model.CaseFile;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public final class CaseDAO {
    private CaseDAO() {}

    private static final String BASE =
            "SELECT c.*, u.full_name AS assignee, " +
            "(SELECT COUNT(*) FROM evidence e WHERE e.case_id=c.id) AS ev_count " +
            "FROM crime_cases c LEFT JOIN users u ON u.id=c.assigned_to ";

    private static CaseFile map(ResultSet r) throws SQLException {
        CaseFile c = new CaseFile();
        c.id = r.getInt("id"); c.caseNo = r.getString("case_no"); c.title = r.getString("title");
        c.description = r.getString("description"); c.category = r.getString("category");
        c.status = r.getString("status"); c.priority = r.getString("priority");
        c.assignedTo = r.getInt("assigned_to"); c.assignee = r.getString("assignee");
        c.openedOn = r.getDate("opened_on").toLocalDate(); c.evidenceCount = r.getInt("ev_count");
        return c;
    }

    public static List<CaseFile> search(String q) {
        String like = "%" + (q == null ? "" : q.trim()) + "%";
        return Database.query(BASE + "WHERE c.case_no LIKE ? OR c.title LIKE ? OR c.category LIKE ? OR c.status LIKE ? ORDER BY c.id DESC",
                CaseDAO::map, like, like, like, like);
    }

    public static List<CaseFile> all() { return search(""); }

    public static String nextCaseNo() {
        long n = Database.scalar("SELECT COALESCE(MAX(id),0)+1 FROM crime_cases");
        return String.format("CASE-%d-%03d", LocalDate.now().getYear(), n);
    }

    public static void insert(CaseFile c) {
        Database.insert("INSERT INTO crime_cases(case_no,title,description,category,status,priority,assigned_to,opened_on) VALUES (?,?,?,?,?,?,?,?)",
                c.caseNo, c.title, c.description, c.category, c.status, c.priority, c.assignedTo == 0 ? null : c.assignedTo, c.openedOn);
    }

    public static void update(CaseFile c) {
        Database.update("UPDATE crime_cases SET title=?,description=?,category=?,status=?,priority=?,assigned_to=?,opened_on=? WHERE id=?",
                c.title, c.description, c.category, c.status, c.priority, c.assignedTo == 0 ? null : c.assignedTo, c.openedOn, c.id);
    }

    public static void delete(int id) { Database.update("DELETE FROM crime_cases WHERE id=?", id); }
}
