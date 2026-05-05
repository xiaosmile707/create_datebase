package com.createdb.client.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * 配置加载器，从 {@code db-connection.json} 读取所有数据库连接参数。
 */
public class ConfigLoader {

    private static final String CONNECTION_JSON = "db-connection.json";

    private final String host;
    private final int port;
    private final String version;
    private final String user;
    private final String password;

    /**
     * 使用默认 JSON 路径的构造器。
     */
    public ConfigLoader() {
        this(CONNECTION_JSON);
    }

    /**
     * 指定 JSON 文件路径的构造器。
     *
     * @param connectionJsonPath db-connection.json 路径
     */
    public ConfigLoader(String connectionJsonPath) {
        String h = null;
        int p = -1;
        String ver = null;
        String u = null;
        String pw = null;

        String json = readFile(connectionJsonPath);
        if (json != null) {
            h = extractJsonString(json, "host");
            String portStr = extractJsonString(json, "port");
            if (portStr != null) {
                try { p = Integer.parseInt(portStr); } catch (NumberFormatException ignored) {}
            }
            ver = extractJsonString(json, "version");
            u = extractJsonString(json, "user");
            pw = extractJsonString(json, "password");
        }

        this.host = (h != null && !h.isEmpty()) ? h : "localhost";
        this.port = Math.max(p, 0);
        this.version = ver != null ? ver : "";
        this.user = u != null ? u : "";
        this.password = pw != null ? pw : "";
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getVersion() { return version; }
    public String getUser() { return user; }
    public String getPassword() { return password; }

    /** SSL 暂未在 JSON 中配置，始终返回 null。 */
    public SslConfig getSslConfig() {
        return null;
    }

    private static String readFile(String path) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(path))))) {
            return reader.lines().collect(Collectors.joining());
        } catch (IOException e) {
            System.err.println("Warning: Failed to load connection config: " + path);
            return null;
        }
    }

    private static String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(":", keyIdx + pattern.length());
        if (colonIdx < 0) return null;
        int startQuote = json.indexOf("\"", colonIdx + 1);
        if (startQuote < 0) {
            return json.substring(colonIdx + 1).trim().split("[,}]", 2)[0].trim();
        }
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote < 0) return null;
        return json.substring(startQuote + 1, endQuote);
    }

}
