package net.chen.rapidBan.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.chen.rapidBan.RapidBan;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class DatabaseManager {
    private final RapidBan plugin;
    private HikariDataSource dataSource;
    private final ExecutorService executor;
    private DatabaseType databaseType;

    public enum DatabaseType {
        MYSQL, SQLITE
    }

    public DatabaseManager(RapidBan plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread thread = new Thread(r, "RapidBan-Database-Thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String dbTypeStr = plugin.getConfig().getString("database.type", "MYSQL").toUpperCase();
                databaseType = DatabaseType.valueOf(dbTypeStr);

                HikariConfig config = new HikariConfig();

                if (databaseType == DatabaseType.SQLITE) {
                    initializeSQLite(config);
                } else {
                    initializeMySQL(config);
                }

                dataSource = new HikariDataSource(config);

                plugin.getLogger().info("Database connection pool initialized successfully (" + databaseType + ")");
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize database connection", e);
                return false;
            }
        }, executor);
    }

    private void initializeMySQL(HikariConfig config) {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "rapidban");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");

        config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.mariadb.jdbc.Driver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
    }

    private void initializeSQLite(HikariConfig config) {
        String fileName = plugin.getConfig().getString("database.sqlite.file", "rapidban.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);

        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");

        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(30000);

        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("cache_size", "10000");
        config.addDataSourceProperty("temp_store", "MEMORY");
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection pool is not initialized");
        }
        return dataSource.getConnection();
    }

    public <T> CompletableFuture<T> executeAsync(DatabaseTask<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                return task.execute(connection);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> executeAsyncVoid(DatabaseTaskVoid task) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                task.execute(connection);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed");
        }
        executor.shutdown();
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    @FunctionalInterface
    public interface DatabaseTask<T> {
        T execute(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface DatabaseTaskVoid {
        void execute(Connection connection) throws SQLException;
    }
}
