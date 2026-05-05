package com.createdb.client.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private static final String DEFAULT_CONFIG_PATH = "db-secret.properties";

    private final Properties props;
    private final String prefix;

    public ConfigLoader(String type) {
        this(DEFAULT_CONFIG_PATH, type);
    }

    public ConfigLoader(String configPath, String type) {
        this.prefix = type + ".";
        props = new Properties();
        try (InputStream input = new FileInputStream(configPath)) {
            props.load(input);
        } catch (IOException e) {
            System.err.println("Failed to load config: " + configPath);
            System.exit(1);
        }
    }

    public String getHost() {
        return props.getProperty(prefix + "host", "localhost");
    }

    public int getPort() {
        return Integer.parseInt(props.getProperty(prefix + "port", "0"));
    }

    public String getVersion() {
        return props.getProperty(prefix + "version", "");
    }

    public String getUser() {
        return props.getProperty(prefix + "user", "");
    }

    public String getPassword() {
        return props.getProperty(prefix + "password", "");
    }

    public SslConfig getSslConfig() {
        String ca = props.getProperty(prefix + "ssl.ca");
        String cert = props.getProperty(prefix + "ssl.cert");
        String key = props.getProperty(prefix + "ssl.key");
        if (ca == null && cert == null && key == null) {
            return null;
        }
        return new SslConfig(ca, cert, key);
    }
}
