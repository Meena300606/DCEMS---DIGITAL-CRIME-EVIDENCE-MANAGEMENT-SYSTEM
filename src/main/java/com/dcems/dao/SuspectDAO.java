package com.dcems.dao;

import com.dcems.db.Database;
import com.dcems.model.Suspect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class SuspectDAO {
    private SuspectDAO() {}

    private static final String BASE =
            "SELECT s.*, c.case_no FROM suspects s LEFT JOIN crime_cases c ON c.id=s.case_id ";

    private static Suspect map(ResultSet r) throws SQLException {
        Suspect s = new Suspect();
        s.id = r.getInt("id"); s.fullName = r.getString("full_name"); s.alias = r.getString("alias");
        s.age = r.getInt("age"); s.phone = r.getString("phone"); s.address = r.getString("address");
        s.notes = r.getString("notes"); s.caseId = r.getInt("case_id"); s.caseNo = r.getString("case_no");
        return s;
    }

    public static List<Suspect> search(String q) {
        String like = "%" + (q == null ? "" : q.trim()) + "%";
        return Database.query(BASE + "WHERE s.full_name LIKE ? OR s.alias LIKE ? OR c.case_no LIKE ? ORDER BY s.id DESC",
                SuspectDAO::map, like, like, like);
    }

    public static List<Suspect> all() { return search(""); }

    public static List<Suspect> byCase(int caseId) {
        return Database.query(BASE + "WHERE s.case_id=?", SuspectDAO::map, caseId);
    }

    public static void insert(Suspect s) {
        Database.insert("INSERT INTO suspects(full_name,alias,age,phone,address,notes,case_id) VALUES (?,?,?,?,?,?,?)",
                s.fullName, s.alias, s.age == 0 ? null : s.age, s.phone, s.address, s.notes, s.caseId == 0 ? null : s.caseId);
    }

    public static void update(Suspect s) {
        Database.update("UPDATE suspects SET full_name=?,alias=?,age=?,phone=?,address=?,notes=?,case_id=? WHERE id=?",
                s.fullName, s.alias, s.age == 0 ? null : s.age, s.phone, s.address, s.notes, s.caseId == 0 ? null : s.caseId, s.id);
    }

    public static void delete(int id) { Database.update("DELETE FROM suspects WHERE id=?", id); }
}
