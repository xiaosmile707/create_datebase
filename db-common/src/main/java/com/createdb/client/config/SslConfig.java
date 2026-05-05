package com.createdb.client.config;

public class SslConfig {

    private final String caPath;
    private final String certPath;
    private final String keyPath;

    public SslConfig(String caPath, String certPath, String keyPath) {
        this.caPath = caPath;
        this.certPath = certPath;
        this.keyPath = keyPath;
    }

    public String getCaPath() {
        return caPath;
    }

    public String getCertPath() {
        return certPath;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public boolean hasAny() {
        return caPath != null || certPath != null || keyPath != null;
    }
}
