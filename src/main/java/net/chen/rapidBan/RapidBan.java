package net.chen.rapidBan;

import lombok.Getter;
import lombok.Setter;
import net.chen.rapidBan.commands.BanCommand;
import net.chen.rapidBan.commands.HistoryCommand;
import net.chen.rapidBan.commands.UnbanCommand;
import net.chen.rapidBan.commands.UndoCommand;
import net.chen.rapidBan.database.*;
import net.chen.rapidBan.ip.IPManager;
import net.chen.rapidBan.listeners.PlayerConnectionListener;
import net.chen.rapidBan.punishment.PunishmentManager;
import net.chen.rapidBan.sync.SyncManager;
import net.chen.rapidBan.ui.KickScreenManager;
import net.chen.rapidBan.web.api.WebAPIServer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

@Getter
@Setter
public final class RapidBan extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PunishmentRepository punishmentRepository;
    private PlayerRepository playerRepository;
    private IPRepository ipRepository;
    private SyncRepository syncRepository;

    private PunishmentManager punishmentManager;
    private IPManager ipManager;
    private SyncManager syncManager;
    private KickScreenManager kickScreenManager;
    private WebAPIServer webAPIServer;

    private String serverId;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        serverId = getConfig().getString("server-id", "server-" + UUID.randomUUID().toString().substring(0, 8));

        getLogger().info("Starting RapidBan v" + getDescription().getVersion());
        getLogger().info("Server ID: " + serverId);

        databaseManager = new DatabaseManager(this);

        databaseManager.initialize().thenAccept(success -> {
            if (!success) {
                getLogger().severe("Failed to initialize database! Plugin will be disabled.");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }

            initializeDatabase();

            punishmentRepository = new PunishmentRepository(this, databaseManager);
            playerRepository = new PlayerRepository(this, databaseManager);
            ipRepository = new IPRepository(this, databaseManager);
            syncRepository = new SyncRepository(this, databaseManager);

            punishmentManager = new PunishmentManager(this, punishmentRepository);
            ipManager = new IPManager(this, ipRepository, punishmentRepository);
            syncManager = new SyncManager(this, syncRepository);
            kickScreenManager = new KickScreenManager(this);

            registerCommands();
            registerListeners();

            syncManager.startSync();

            int checkInterval = getConfig().getInt("tasks.check-expired-interval", 5);
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                punishmentManager.checkExpiredPunishments();
            }, 20L * 60 * checkInterval, 20L * 60 * checkInterval);

            if (getConfig().getBoolean("web.enabled", true)) {
                webAPIServer = new WebAPIServer(this);
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    webAPIServer.start();
                });
            }

            getLogger().info("RapidBan has been enabled successfully!");
        }).exceptionally(ex -> {
            getLogger().log(Level.SEVERE, "Failed to start plugin", ex);
            Bukkit.getPluginManager().disablePlugin(this);
            return null;
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down RapidBan...");

        if (syncManager != null) {
            syncManager.stopSync();
        }

        if (webAPIServer != null) {
            webAPIServer.stop();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        getLogger().info("RapidBan has been disabled.");
    }

    private void initializeDatabase() {
        String schemaFile = databaseManager.getDatabaseType() == DatabaseManager.DatabaseType.SQLITE
            ? "sql/schema-sqlite.sql"
            : "sql/schema.sql";

        try (InputStream is = getResource(schemaFile);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is));
             Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line).append("\n");
            }

            for (String statement : sql.toString().split(";")) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement);
                }
            }

            getLogger().info("Database schema initialized successfully (" + databaseManager.getDatabaseType() + ")");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize database schema", e);
        }
    }

    private void registerCommands() {
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("history").setExecutor(new HistoryCommand(this));
        getCommand("punishundo").setExecutor(new UndoCommand(this));

        getCommand("ban").setTabCompleter(new BanCommand(this));
        getCommand("unban").setTabCompleter(new UnbanCommand(this));
        getCommand("history").setTabCompleter(new HistoryCommand(this));
        getCommand("punishundo").setTabCompleter(new UndoCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
    }

    public IPManager getIPManager() {
        return ipManager;
    }

    public IPRepository getIPRepository() {
        return ipRepository;
    }
}
