package com.dcems.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Fmt {
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private Fmt() {}

    public static String dt(LocalDateTime t) { return t == null ? "" : t.format(DT); }
    public static String d(LocalDate t) { return t == null ? "" : t.format(D); }
    public static String nz(String s) { return s == null ? "" : s; }

    public static String size(long bytes) {
        if (bytes <= 0) return "—";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / 1048576.0);
        return String.format("%.2f GB", bytes / 1073741824.0);
    }
}
