package net.chen.rapidBan.listeners;

import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.models.IPRecord;
import net.chen.rapidBan.models.Player;
import net.chen.rapidBan.models.Punishment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerConnectionListener implements Listener {
    private final RapidBan plugin;

    public PlayerConnectionListener(RapidBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String uuid = event.getUniqueId().toString();
        String username = event.getName();
        String ipAddress = event.getAddress().getHostAddress();

        plugin.getPunishmentManager().getActiveBan(uuid).thenAccept(optBan -> {
            if (optBan.isPresent()) {
                Punishment ban = optBan.get();
                String kickMessage = plugin.getKickScreenManager().generateKickScreen(ban, username);
                event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(kickMessage)
                );
            }
        }).join();

        Player player = new Player(uuid, username, System.currentTimeMillis(), ipAddress);
        plugin.getPlayerRepository().createOrUpdatePlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String username = player.getName();
        String ipAddress = player.getAddress().getAddress().getHostAddress();

        plugin.getIPManager().recordLogin(uuid, username, ipAddress);

        if (player.hasPermission("rapidban.notify.alt")) {
            plugin.getIPManager().checkAltAccounts(player).thenAccept(altAccounts -> {
                if (!altAccounts.isEmpty()) {
                    plugin.getIPManager().notifyStaffAboutAlts(player, altAccounts);
                }
            });
        }

        boolean autobanAlts = plugin.getConfig().getBoolean("ip-check.auto-ban-alts", false);
        if (autobanAlts) {
            plugin.getIPManager().hasBannedAlts(ipAddress, uuid).thenAccept(hasBannedAlts -> {
                if (hasBannedAlts) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getPunishmentManager().punishPlayer(
                            uuid,
                            net.chen.rapidBan.enums.PunishmentType.BAN,
                            "关联账号被封禁",
                            "SYSTEM",
                            "系统",
                            null,
                            false
                        ).thenAccept(punishment -> {
                            plugin.getPunishmentManager().kickPlayer(player, punishment);
                        });
                    });
                }
            });
        }
    }
}
