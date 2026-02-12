package net.chen.rapidBan.punishment;

import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.database.PunishmentRepository;
import net.chen.rapidBan.enums.PunishmentType;
import net.chen.rapidBan.enums.SyncEventType;
import net.chen.rapidBan.models.Punishment;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentManager {
    private final RapidBan plugin;
    private final PunishmentRepository repository;
    private final Map<String, Punishment> activeBanCache;

    public PunishmentManager(RapidBan plugin, PunishmentRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.activeBanCache = new ConcurrentHashMap<>();
    }

    public CompletableFuture<Punishment> punishPlayer(String uuid, PunishmentType type, String reason,String operator, String operatorName, Long duration, boolean silent) {
        return CompletableFuture.supplyAsync(() -> {
            long now = System.currentTimeMillis();
            Punishment punishment = new Punishment(uuid, type, reason, operator, operatorName, now);
            if (duration != null && duration > 0) {
                punishment.setExpiresAt(now + duration);
            }
            punishment.setSilent(silent);
            punishment.setServerId(plugin.getServerId());
            return punishment;
        }).thenCompose(punishment ->
            repository.createPunishment(punishment).thenApply(id -> {
                punishment.setId(id);
                if (punishment.getType().isBan()) {
                    activeBanCache.put(uuid, punishment);
                }
                plugin.getSyncManager().broadcastEvent(
                    punishment.getType().isTemporary() ? SyncEventType.TEMPBAN : SyncEventType.BAN,
                    uuid,
                    id
                );
                return punishment;
            })
        );
    }

    public CompletableFuture<Boolean> unbanPlayer(String uuid, String operator) {
        return repository.getActiveBan(uuid).thenCompose(optPunishment -> {
            if (optPunishment.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }

            Punishment punishment = optPunishment.get();
            return repository.deactivatePunishment(punishment.getId()).thenApply(v -> {
                activeBanCache.remove(uuid);
                plugin.getSyncManager().broadcastEvent(SyncEventType.UNBAN, uuid, punishment.getId());
                return true;
            });
        });
    }

    public CompletableFuture<Boolean> revokeAllPunishments(String uuid, String revokedBy, String reason) {
        return repository.revokeAllPunishments(uuid, revokedBy, reason).thenApply(v -> {
            activeBanCache.remove(uuid);
            plugin.getSyncManager().broadcastEvent(SyncEventType.REVOKE, uuid, null);
            return true;
        });
    }

    public CompletableFuture<Optional<Punishment>> getActiveBan(String uuid) {
        Punishment cached = activeBanCache.get(uuid);
        if (cached != null) {
            if (cached.isExpired()) {
                activeBanCache.remove(uuid);
                repository.deactivatePunishment(cached.getId());
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return repository.getActiveBan(uuid).thenApply(opt -> {
            opt.ifPresent(punishment -> {
                if (!punishment.isExpired()) {
                    activeBanCache.put(uuid, punishment);
                } else {
                    repository.deactivatePunishment(punishment.getId());
                    return;
                }
            });
            return opt.filter(p -> !p.isExpired());
        });
    }

    public CompletableFuture<List<Punishment>> getPunishmentHistory(String uuid) {
        return repository.getPunishmentHistory(uuid);
    }

    public void checkExpiredPunishments() {
        repository.checkExpiredPunishments().thenRun(() -> {
            activeBanCache.entrySet().removeIf(entry -> {
                if (entry.getValue().isExpired()) {
                    plugin.getLogger().info("Removed expired ban for " + entry.getKey());
                    return true;
                }
                return false;
            });
        });
    }

    public void kickPlayer(Player player, Punishment punishment) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String kickMessage = plugin.getKickScreenManager().generateKickScreen(punishment, player.getName());
            player.kick(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(kickMessage));
        });
    }

    public void invalidateCache(String uuid) {
        activeBanCache.remove(uuid);
    }

    public void reloadCache(String uuid) {
        activeBanCache.remove(uuid);
        getActiveBan(uuid);
    }
}
