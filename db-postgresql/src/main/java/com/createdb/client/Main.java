package com.createdb.client;

import com.createdb.client.config.ConfigLoader;
import com.createdb.client.db.DatabaseClient;
import com.createdb.client.db.PostgresqlClient;
import com.createdb.client.runner.DataSeeder;
import com.createdb.client.runner.SqlRunner;

import java.sql.Connection;

public class Main {

    private static final DatabaseClient CLIENT = new PostgresqlClient();
    private static final String DB_TYPE = "postgresql";

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : "db-secret.properties";

        ConfigLoader config = new ConfigLoader(configPath, DB_TYPE);
        String version = config.getVersion();
        String host = config.getHost();
        int port = config.getPort();
        String user = config.getUser();
        String password = config.getPassword();

        System.out.println("===== db-postgresql =====");
        System.out.println("Type: " + DB_TYPE + " | Version: " + version);
        System.out.println("Host: " + host + ":" + port);

        SqlRunner sqlRunner = new SqlRunner();
        DataSeeder seeder = new DataSeeder();

        try (Connection conn = CLIENT.getConnection(host, port, version, user, password, config.getSslConfig())) {
            System.out.println("Connected.");

            sqlRunner.runQuery(conn, "SELECT 1", "Connection test:");

            String basePath = "sql/" + version + "/";

            sqlRunner.runScript(conn, basePath + "schema.sql");
            sqlRunner.runScript(conn, basePath + "test.sql");

            seeder.seed(conn, basePath + "seed.sql");

            System.out.println("Done.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
