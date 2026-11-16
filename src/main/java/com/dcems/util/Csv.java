package com.dcems.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class Csv {
    private Csv() {}

    public static void write(File f, String[] headers, List<String[]> rows) throws IOException {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append(line(headers));
        for (String[] r : rows) sb.append(line(r));
        Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String line(String[] cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            String c = cells[i] == null ? "" : cells[i].replace("\"", "\"\"");
            sb.append('"').append(c).append('"');
        }
        return sb.append("\r\n").toString();
    }
}
