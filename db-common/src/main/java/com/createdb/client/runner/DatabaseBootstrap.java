package com.createdb.client.runner;

import com.createdb.client.config.ConfigLoader;
import com.createdb.client.config.SslConfig;
import com.createdb.client.db.DatabaseClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库启动引导器，封装了连接、建表、测试查询、种子数据灌入的统一流程。
 * <p>
 * 各数据库模块的 Main 入口只需提供对应的 {@link DatabaseClient} 实现和模块标识，
 * 无需重复编写相同的执行逻辑。
 *
 * <pre>
 * DatabaseBootstrap.run(new MysqlClient(), "mysql");
 * </pre>
 */
public class DatabaseBootstrap {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBootstrap.class);

    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 2000;

    private DatabaseBootstrap() {
        // 工具类，禁止实例化
    }

    /**
     * 执行完整的数据库初始化流程：
     * <ol>
     *   <li>从多源加载配置（JSON + 环境变量）</li>
     *   <li>建立 JDBC 连接（含指数退避重试）</li>
     *   <li>在事务中执行：连接测试 → schema.sql → test.sql → seed.sql</li>
     *   <li>失败时回滚事务并执行 cleanup.sql 清理残留对象</li>
     * </ol>
     *
     * @param client 数据库客户端实现
     * @param dbType 数据库类型标识（如 "mysql", "postgresql"）
     */
    public static void run(DatabaseClient client, String dbType) {
        ConfigLoader config = new ConfigLoader();
        String version = config.getVersion();
        String host = config.getHost();
        int port = config.getPort();
        String database = config.getDatabase();
        String user = config.getUser();
        String password = config.getPassword();

        log.info("===== db-{} =====", dbType);
        log.info("Type: {} | Version: {}", dbType, version);
        log.info("Host: {}:{}", host, port);

        SqlRunner sqlRunner = new SqlRunner();
        String basePath = "sql/" + version + "/";

        try (Connection conn = connectWithRetry(client, host, port, database, version, user, password, config.getSslConfig())) {
            log.info("Connected.");

            sqlRunner.runQuery(conn, client.getPingQuery(), "Connection test:");

            // 开启事务：任一 SQL 步骤失败则整体回滚
            conn.setAutoCommit(false);
            try {
                sqlRunner.runScript(conn, basePath + "schema.sql");
                sqlRunner.runScript(conn, basePath + "test.sql");
                sqlRunner.runScript(conn, basePath + "seed.sql");
                conn.commit();
                log.info("All SQL scripts executed and committed successfully.");
            } catch (Exception e) {
                log.error("SQL execution failed, rolling back transaction: {}", e.getMessage());
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    log.warn("Rollback failed: {}", rollbackEx.getMessage());
                }
                // 执行清理脚本删除可能已残留的对象
                executeCleanup(conn, basePath);
                throw e;
            }

            log.info("Done.");
        } catch (Exception e) {
            log.error("Database bootstrap failed: {}", e.getMessage());
            System.exit(1);
        }
    }

    /**
     * 尝试执行 cleanup.sql 清理残留的数据库对象（使用独立的 lenient SqlRunner）。
     * <p>
     * 若 cleanup.sql 不存在或部分语句失败，仅记录警告，不影响主流程。
     * 清理模式下 stopOnError=false，确保所有 DROP 语句都被尝试执行。
     */
    private static void executeCleanup(Connection conn, String basePath) {
        SqlRunner lenientRunner = new SqlRunner(false);
        try {
            lenientRunner.runScript(conn, basePath + "cleanup.sql");
            log.info("Cleanup script executed.");
        } catch (Exception cleanupEx) {
            log.warn("Cleanup script execution failed or not found: {}", cleanupEx.getMessage());
        }
    }

    /**
     * 带指数退避重试的连接方法。
     * <p>
     * 首次连接失败后，每隔 2s、4s、8s、16s、30s 重试（最多 5 次）。
     */
    private static Connection connectWithRetry(DatabaseClient client, String host, int port,
                                               String database, String version, String user,
                                               String password, SslConfig ssl)
            throws Exception {
        long delay = INITIAL_RETRY_DELAY_MS;
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return client.getConnection(host, port, database, version, user, password, ssl);
            } catch (SQLException e) {
                lastException = e;
                if (attempt < MAX_RETRIES - 1) {
                    log.info("Connection attempt {} failed, retrying in {}s...",
                            (attempt + 1), (delay / 1000));
                    Thread.sleep(delay);
                    delay = Math.min(delay * 2, 30000);
                }
            }
        }
        throw lastException != null ? lastException : new SQLException("Failed to connect after " + MAX_RETRIES + " attempts");
    }
}
