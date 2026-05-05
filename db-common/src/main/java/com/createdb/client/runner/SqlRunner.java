package com.createdb.client.runner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Collectors;

public class SqlRunner {

    public void runScript(Connection conn, String scriptPath) {
        System.out.println("Executing: " + scriptPath);
        try {
            String sql = readResource(scriptPath);
            String[] statements = sql.split(";");
            for (String stmt : statements) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                try (Statement s = conn.createStatement()) {
                    boolean hasResultSet = s.execute(trimmed);
                    if (hasResultSet) {
                        try (ResultSet rs = s.getResultSet()) {
                            int cols = rs.getMetaData().getColumnCount();
                            while (rs.next()) {
                                StringBuilder row = new StringBuilder();
                                for (int i = 1; i <= cols; i++) {
                                    row.append(rs.getString(i));
                                    if (i < cols) row.append(" | ");
                                }
                                System.out.println("  " + row);
                            }
                        }
                    } else {
                        int count = s.getUpdateCount();
                        if (count >= 0) {
                            System.out.println("  " + count + " row(s) affected");
                        }
                    }
                }
            }
            System.out.println("  OK");
        } catch (Exception e) {
            System.err.println("  FAILED: " + e.getMessage());
        }
    }

    public void runQuery(Connection conn, String sql, String label) {
        System.out.println(label);
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                StringBuilder row = new StringBuilder("  ");
                for (int i = 1; i <= cols; i++) {
                    row.append(rs.getString(i));
                    if (i < cols) row.append(" | ");
                }
                System.out.println(row);
            }
        } catch (Exception e) {
            System.err.println("  FAILED: " + e.getMessage());
        }
    }

    private String readResource(String path) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new RuntimeException("Resource not found: " + path);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
