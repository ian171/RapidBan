package net.chen.rapidBan.sync;

import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.database.SyncRepository;
import net.chen.rapidBan.enums.SyncEventType;
import net.chen.rapidBan.models.SyncEvent;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;

public class SyncManager {
    private final RapidBan plugin;
    private final SyncRepository syncRepository;
    private final String serverId;
    private int taskId = -1;

    public SyncManager(RapidBan plugin, SyncRepository syncRepository) {
        this.plugin = plugin;
        this.syncRepository = syncRepository;
        this.serverId = plugin.getServerId();
    }

    public void startSync() {
        int intervalTicks = plugin.getConfig().getInt("sync.interval-seconds", 5) * 20;

        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            syncRepository.getUnprocessedEvents(serverId).thenAccept(events -> {
                for (SyncEvent event : events) {
                    processEvent(event);
                    syncRepository.markEventProcessed(event.getId());
                }
            }).exceptionally(ex -> {
                plugin.getLogger().warning("Failed to sync events: " + ex.getMessage());
                return null;
            });
        }, 20L, intervalTicks).getTaskId();

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
            syncRepository.cleanOldEvents(oneWeekAgo);
        }, 20L * 60 * 60, 20L * 60 * 60);

        plugin.getLogger().info("Sync system started with interval: " + (intervalTicks / 20) + " seconds");
    }

    public void stopSync() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            plugin.getLogger().info("Sync system stopped");
        }
    }

    public void broadcastEvent(SyncEventType eventType, String targetUuid, Long punishmentId) {
        SyncEvent event = new SyncEvent(eventType, targetUuid, serverId);
        event.setPunishmentId(punishmentId);

        syncRepository.createSyncEvent(event).thenAccept(id -> {
            // 日志已移除 - 避免控制台输出封禁信息
        }).exceptionally(ex -> {
            plugin.getLogger().warning("Failed to broadcast sync event: " + ex.getMessage());
            return null;
        });
    }

    private void processEvent(SyncEvent event) {
        // 日志已移除 - 避免控制台输出封禁信息

        switch (event.getEventType()) {
            case BAN:
            case TEMPBAN:
                plugin.getPunishmentManager().reloadCache(event.getTargetUuid());
                kickPlayerIfOnline(event.getTargetUuid());
                break;

            case UNBAN:
                plugin.getPunishmentManager().invalidateCache(event.getTargetUuid());
                break;

            case REVOKE:
                plugin.getPunishmentManager().invalidateCache(event.getTargetUuid());
                break;

            case UPDATE:
                plugin.getPunishmentManager().reloadCache(event.getTargetUuid());
                break;
        }
    }

    private void kickPlayerIfOnline(String uuid) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getUniqueId().toString().equals(uuid))
                .findFirst()
                .ifPresent(player -> {
                    plugin.getPunishmentManager().getActiveBan(uuid).thenAccept(optBan -> {
                        optBan.ifPresent(ban -> {
                            plugin.getPunishmentManager().kickPlayer(player, ban);
                        });
                    });
                });
        });
    }
}
