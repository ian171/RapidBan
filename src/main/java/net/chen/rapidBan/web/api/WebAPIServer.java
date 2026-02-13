package net.chen.rapidBan.web.api;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import net.chen.rapidBan.models.Player;
import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.enums.PunishmentType;
import net.chen.rapidBan.web.JWTManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WebAPIServer {
    private final RapidBan plugin;
    private final JWTManager jwtManager;
    private final Gson gson;
    private Javalin app;

    public WebAPIServer(RapidBan plugin) {
        this.plugin = plugin;
        this.jwtManager = new JWTManager(plugin.getConfig().getString("web.jwt-secret", "change-this-secret"));
        this.gson = new Gson();
    }

    public void start() {
        int port = plugin.getConfig().getInt("web.port", 8080);
        String host = plugin.getConfig().getString("web.host", "0.0.0.0");

        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.staticFiles.add("/web", io.javalin.http.staticfiles.Location.CLASSPATH);
        }).start(host, port);

        setupRoutes();

        if (plugin.getSimpleConfig().isDebug) {
            plugin.logger.info("Web API started on " + host + ":" + port);
            plugin.logger.info("Web UI available at http://" + host + ":" + port + "/");
        }
    }

    public void stop() {
        if (app != null) {
            app.stop();
            plugin.getLogger().info("Web API stopped");
        }
    }

    private void setupRoutes() {
        // Redirect root to index.html
        app.get("/", ctx -> ctx.redirect("/index.html"));

        app.before("/api/*", this::authMiddleware);

        app.post("/auth/login", this::handleLogin);

        app.get("/api/punishments/history/{player}", this::getPunishmentHistory);
        app.post("/api/punishments/ban", this::createBan);
        app.post("/api/punishments/unban", this::unbanPlayer);
        app.post("/api/punishments/revoke", this::revokePunishments);

        app.get("/api/players/search/{query}", this::searchPlayer);
        app.get("/api/players/{uuid}/alts", this::getAltAccounts);

        app.get("/api/stats/recent", this::getRecentPunishments);

        app.exception(Exception.class, (e, ctx) -> {
            plugin.getLogger().warning("Web API error: " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Internal server error"));
        });
    }

    private void authMiddleware(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }

        String token = authHeader.substring(7);
        String username = jwtManager.extractUsername(token);

        if (username == null) {
            ctx.status(401).json(Map.of("error", "Invalid token"));
            return;
        }

        ctx.attribute("username", username);
        ctx.attribute("role", jwtManager.extractRole(token));
    }

    private void handleLogin(Context ctx) {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String username = body.get("username");
        String password = body.get("password");

        String configUsername = plugin.getConfig().getString("web.admin-username", "admin");
        String configPassword = plugin.getConfig().getString("web.admin-password", "admin");

        if (username.equals(configUsername) && password.equals(configPassword)) {
            String token = jwtManager.generateToken(username, "ADMIN");
            ctx.json(Map.of("token", token, "username", username, "role", "ADMIN"));
        } else {
            ctx.status(401).json(Map.of("error", "Invalid credentials"));
        }
    }

    private void getPunishmentHistory(Context ctx) {
        String playerName = ctx.pathParam("player");

        try {
            var optPlayer = plugin.getPlayerRepository().getPlayerByName(playerName).join();
            if (optPlayer.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Player not found"));
                return;
            }

            String uuid = optPlayer.get().getUuid();
            var punishments = plugin.getPunishmentManager().getPunishmentHistory(uuid).join();

            // 构建返回数据，包含玩家名称
            Map<String, Object> response = new HashMap<>();
            response.put("punishments", punishments);
            response.put("playerName", optPlayer.get().getUsername());

            ctx.json(response);
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting punishment history: " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Failed to get punishment history"));
        }
    }

    private void createBan(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String playerName = (String) body.get("player");
            String reason = (String) body.get("reason");
            Long duration = body.containsKey("duration") ? ((Number) body.get("duration")).longValue() : null;
            boolean silent = body.containsKey("silent") && (boolean) body.get("silent");

            String operator = ctx.attribute("username");

            var optPlayer = plugin.getPlayerRepository().getPlayerByName(playerName).join();
            if (optPlayer.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Player not found"));
                return;
            }

            String uuid = optPlayer.get().getUuid();
            PunishmentType type = duration != null ? PunishmentType.TEMPBAN : PunishmentType.BAN;

            var punishment = plugin.getPunishmentManager().punishPlayer(uuid, type, reason, "WEB", operator, duration, silent).join();
            ctx.json(Map.of("success", true, "punishment", punishment));
        } catch (Exception e) {
            plugin.getLogger().warning("Error creating ban: " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Failed to create ban"));
        }
    }

    private void unbanPlayer(Context ctx) {
        try {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String playerName = body.get("player");
            String operator = ctx.attribute("username");

            var optPlayer = plugin.getPlayerRepository().getPlayerByName(playerName).join();
            if (optPlayer.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Player not found"));
                return;
            }

            String uuid = optPlayer.get().getUuid();
            boolean success = plugin.getPunishmentManager().unbanPlayer(uuid, operator).join();
            ctx.json(Map.of("success", success));
        } catch (Exception e) {
            plugin.getLogger().warning("Error unbanning player: " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Failed to unban player"));
        }
    }

    private void revokePunishments(Context ctx) {
        try {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String playerName = body.get("player");
            String reason = body.getOrDefault("reason", "Web撤销");
            String operator = ctx.attribute("username");

            var optPlayer = plugin.getPlayerRepository().getPlayerByName(playerName).join();
            if (optPlayer.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Player not found"));
                return;
            }

            String uuid = optPlayer.get().getUuid();
            boolean success = plugin.getPunishmentManager().revokeAllPunishments(uuid, operator, reason).join();
            ctx.json(Map.of("success", success));
        } catch (Exception e) {
            plugin.getLogger().warning("Error revoking punishments: " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Failed to revoke punishments"));
        }
    }

    private void searchPlayer(Context ctx) {
        try {
            String query = ctx.pathParam("query");

            var optPlayer = plugin.getPlayerRepository().getPlayerByName(query).join();
            if (optPlayer.isPresent()) {
                ctx.json(Map.of("player", optPlayer.get()));
            } else {
                ctx.status(404).json(Map.of("error", "Player not found"));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error searching player: " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Failed to search player"));
        }
    }

    private void getAltAccounts(Context ctx) {
        try {
            String uuid = ctx.pathParam("uuid");

            var ipRecords = plugin.getIPRepository().getIPsByUUID(uuid).join();
            ctx.json(Map.of("alts", ipRecords));
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting alt accounts: " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Failed to get alt accounts"));
        }
    }

    private void getRecentPunishments(Context ctx) {
        try {
            // 同步获取所有数据
            int total = plugin.getPunishmentRepository().getTotalPunishmentsCount().join();
            int active = plugin.getPunishmentRepository().getActiveBansCount().join();
            int today = plugin.getPunishmentRepository().getTodayPunishmentsCount().join();
            int players = plugin.getPlayerRepository().getTotalPlayersCount().join();
            List<net.chen.rapidBan.models.Punishment> punishments = plugin.getPunishmentRepository().getRecentPunishments(10).join();

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("active", active);
            stats.put("today", today);
            stats.put("players", players);

            List<Map<String, Object>> recentList = new ArrayList<>();

            for (net.chen.rapidBan.models.Punishment p : punishments) {
                Map<String, Object> punishmentMap = new HashMap<>();
                punishmentMap.put("id", p.getId());
                punishmentMap.put("playerUuid", p.getUuid());
                punishmentMap.put("type", p.getType().name());
                punishmentMap.put("reason", p.getReason());
                punishmentMap.put("issuerName", p.getOperatorName());
                punishmentMap.put("issuerUuid", p.getOperator());
                punishmentMap.put("createdAt", p.getCreatedAt());
                punishmentMap.put("expiresAt", p.getExpiresAt());
                punishmentMap.put("duration", p.getExpiresAt() != null ? p.getExpiresAt() - p.getCreatedAt() : null);

                String status = "active";
                if (p.isRevoked()) {
                    status = "revoked";
                } else if (p.isExpired()) {
                    status = "expired";
                } else if (!p.isActive()) {
                    status = "expired";
                }
                punishmentMap.put("status", status);

                // 同步获取玩家名称
                plugin.getPlayerRepository().getPlayerByUUID(p.getUuid()).thenAccept(optPlayer -> {
                    optPlayer.ifPresent(player -> punishmentMap.put("playerName", player.getUsername()));
                }).join();

                recentList.add(punishmentMap);
            }

            stats.put("recent", recentList);
            ctx.json(stats);
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting recent punishments: " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }
}
