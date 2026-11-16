package com.dcems.db;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * JDBC helper for MySQL. Creates the database + tables automatically on first run
 * and offers tiny query/update helpers used by all DAOs.
 */
public final class Database {
    public interface Mapper<T> { T map(ResultSet rs) throws SQLException; }

    private static final Properties P = new Properties();
    private static final String PARAMS =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";

    static {
        try (InputStream in = Database.class.getResourceAsStream("/db.properties")) {
            P.load(in);
        } catch (Exception e) { throw new RuntimeException("db.properties not found", e); }
    }

    private Database() {}

    private static String base() { return "jdbc:mysql://" + P.getProperty("db.host") + ":" + P.getProperty("db.port") + "/"; }
    public static String dbName() { return P.getProperty("db.name"); }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(base() + dbName() + PARAMS, P.getProperty("db.user"), P.getProperty("db.password"));
    }

    /** Creates the schema (if missing) and seeds demo data on first run. */
    public static void init() throws SQLException {
        try (Connection c = DriverManager.getConnection(base() + PARAMS, P.getProperty("db.user"), P.getProperty("db.password"));
             Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE IF NOT EXISTS " + dbName() + " CHARACTER SET utf8mb4");
        }
        try (Connection c = get(); Statement s = c.createStatement()) {
            for (String ddl : Schema.TABLES) s.execute(ddl);
        }
        SeedData.seedIfEmpty();
    }

    // ------------ helpers ------------
    private static void bind(PreparedStatement ps, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            if (a instanceof LocalDate d) ps.setDate(i + 1, Date.valueOf(d));
            else if (a instanceof LocalDateTime t) ps.setTimestamp(i + 1, Timestamp.valueOf(t));
            else ps.setObject(i + 1, a);
        }
    }

    public static <T> List<T> query(String sql, Mapper<T> m, Object... args) {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(m.map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DbException(e); }
    }

    public static int update(String sql, Object... args) {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            return ps.executeUpdate();
        } catch (SQLException e) { throw new DbException(e); }
    }

    public static int insert(String sql, Object... args) {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, args);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        } catch (SQLException e) { throw new DbException(e); }
    }

    public static long scalar(String sql, Object... args) {
        List<Long> r = query(sql, rs -> rs.getLong(1), args);
        return r.isEmpty() ? 0 : r.get(0);
    }

    public static class DbException extends RuntimeException {
        public DbException(SQLException e) { super(e.getMessage(), e); }
    }
}
