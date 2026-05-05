package com.createdb.client.runner;

import java.sql.Connection;

public class DataSeeder {

    private final SqlRunner sqlRunner;

    public DataSeeder() {
        this.sqlRunner = new SqlRunner();
    }

    public void seed(Connection conn, String scriptPath) {
        sqlRunner.runScript(conn, scriptPath);
    }
}
