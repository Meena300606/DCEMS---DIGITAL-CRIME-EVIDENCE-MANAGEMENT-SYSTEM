package com.dcems.dao;

import com.dcems.db.Database;
import com.dcems.model.User;
import com.dcems.util.Hash;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class UserDAO {
    private UserDAO() {}

    private static User map(ResultSet r) throws SQLException {
        User u = new User();
        u.id = r.getInt("id"); u.username = r.getString("username");
        u.fullName = r.getString("full_name"); u.role = r.getString("role");
        return u;
    }

    public static User login(String username, String password) {
        List<User> l = Database.query("SELECT * FROM users WHERE username=? AND password_hash=?",
                UserDAO::map, username, Hash.sha256(password));
        return l.isEmpty() ? null : l.get(0);
    }

    public static List<User> all() { return Database.query("SELECT * FROM users ORDER BY full_name", UserDAO::map); }

    public static void add(String username, String fullName, String password, String role) {
        Database.insert("INSERT INTO users(username,password_hash,full_name,role) VALUES (?,?,?,?)",
                username, Hash.sha256(password), fullName, role);
    }

    public static void delete(int id) { Database.update("DELETE FROM users WHERE id=?", id); }
}
