package net.chen.rapidBan.web.api;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.enums.PunishmentType;
import net.chen.rapidBan.web.JWTManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        plugin.getPlayerRepository().getPlayerByName(playerName).thenAccept(optPlayer -> {
            if (optPlayer.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Player not found"));
                return;
            }

            String uuid = optPlayer.get().getUuid();
            plugin.getPunishmentManager().getPunishmentHistory(uuid).thenAccept(punishments -> {
                ctx.json(punishments);
            });
        });
    }

    private void createBan(Context ctx) {
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String playerName = (String) body.get("player");
        String reason = (String) body.get("reason");
        Long duration = body.containsKey("duration") ? ((Number) body.get("duration")).longValue() : null;
        boolean silent = body.containsKey("silent") && (boolean) body.get("silent");

        String operator = ctx.attribute("username");

        plugin.getPlayerRepository().getPlayerByName(playerName).thenAccept(optPlayer -> {
            if (optPlayer.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Player not found"));
                return;
            }

            String uuid = optPlayer.get().getUuid();
            PunishmentType type = duration != null ? PunishmentType.TEMPBAN : PunishmentType.BAN;

            plugin.getPunishmentManager().punishPlayer(uuid, type, reason, "WEB", operator, duration, silent)
                .thenAccept(punishment -> {
                    ctx.json(Map.of("success", true, "punishment", punishment));
                });
        });
    }

    private void unbanPlayer(Context ctx) {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String playerName = body.get("player");
        String operator = ctx.attribute("username");

        plugin.getPlayerRepository().getPlayerByName(playerName).thenAccept(optPlayer -> {
            if (optPlayer.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Player not found"));
                return;
            }

            String uuid = optPlayer.get().getUuid();
            plugin.getPunishmentManager().unbanPlayer(uuid, operator).thenAccept(success -> {
                ctx.json(Map.of("success", success));
            });
        });
    }

    private void revokePunishments(Context ctx) {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String playerName = body.get("player");
        String reason = body.getOrDefault("reason", "Web撤销");
        String operator = ctx.attribute("username");

        plugin.getPlayerRepository().getPlayerByName(playerName).thenAccept(optPlayer -> {
            if (optPlayer.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Player not found"));
                return;
            }

            String uuid = optPlayer.get().getUuid();
            plugin.getPunishmentManager().revokeAllPunishments(uuid, operator, reason).thenAccept(success -> {
                ctx.json(Map.of("success", success));
            });
        });
    }

    private void searchPlayer(Context ctx) {
        String query = ctx.pathParam("query");

        plugin.getPlayerRepository().getPlayerByName(query).thenAccept(optPlayer -> {
            if (optPlayer.isPresent()) {
                ctx.json(Map.of("player", optPlayer.get()));
            } else {
                ctx.status(404).json(Map.of("error", "Player not found"));
            }
        });
    }

    private void getAltAccounts(Context ctx) {
        String uuid = ctx.pathParam("uuid");

        plugin.getIPRepository().getIPsByUUID(uuid).thenAccept(ipRecords -> {
            ctx.json(Map.of("alts", ipRecords));
        });
    }

    private void getRecentPunishments(Context ctx) {
        // 简化实现，返回模拟数据
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("active", 0);
        stats.put("today", 0);
        stats.put("players", 0);
        stats.put("recent", List.of());
        ctx.json(stats);
    }
}
