package com.dcems.dao;

import com.dcems.db.Database;
import com.dcems.model.Activity;
import com.dcems.util.Session;

import java.time.LocalDateTime;
import java.util.List;

public final class ActivityDAO {
    private ActivityDAO() {}

    public static void log(String action, String detail) {
        Database.insert("INSERT INTO activity_log(username,action,detail,created_at) VALUES (?,?,?,?)",
                Session.name(), action, detail, LocalDateTime.now().withNano(0));
    }

    public static List<Activity> recent(int limit) {
        return Database.query("SELECT * FROM activity_log ORDER BY id DESC LIMIT " + limit, r -> {
            Activity a = new Activity();
            a.username = r.getString("username"); a.action = r.getString("action");
            a.detail = r.getString("detail"); a.createdAt = r.getTimestamp("created_at").toLocalDateTime();
            return a;
        });
    }
}
