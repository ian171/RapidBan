package net.chen.rapidBan.ip;

import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.database.IPRepository;
import net.chen.rapidBan.database.PunishmentRepository;
import net.chen.rapidBan.models.IPRecord;
import net.chen.rapidBan.models.Punishment;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IPManager {
    private final RapidBan plugin;
    private final IPRepository ipRepository;
    private final PunishmentRepository punishmentRepository;

    public IPManager(RapidBan plugin, IPRepository ipRepository, PunishmentRepository punishmentRepository) {
        this.plugin = plugin;
        this.ipRepository = ipRepository;
        this.punishmentRepository = punishmentRepository;
    }

    public CompletableFuture<Void> recordLogin(String uuid, String username, String ipAddress) {
        long now = System.currentTimeMillis();
        IPRecord record = new IPRecord(uuid, username, ipAddress, now);
        return ipRepository.recordIPLogin(record);
    }

    public CompletableFuture<List<String>> checkAltAccounts(Player player) {
        String uuid = player.getUniqueId().toString();
        String ipAddress = player.getAddress().getAddress().getHostAddress();

        return ipRepository.getUUIDsByIP(ipAddress).thenCompose(records -> {
            List<String> altAccounts = new ArrayList<>();
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (IPRecord record : records) {
                if (!record.getUuid().equals(uuid)) {
                    CompletableFuture<Void> future = punishmentRepository.getActiveBan(record.getUuid())
                        .thenAccept(optBan -> {
                            if (optBan.isPresent()) {
                                altAccounts.add(record.getUsername() + " (" + record.getUuid() + ") - BANNED");
                            } else {
                                altAccounts.add(record.getUsername() + " (" + record.getUuid() + ")");
                            }
                        });
                    futures.add(future);
                }
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> altAccounts);
        });
    }

    public CompletableFuture<Boolean> hasBannedAlts(String ipAddress, String excludeUuid) {
        return ipRepository.getUUIDsByIP(ipAddress).thenCompose(records -> {
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();

            for (IPRecord record : records) {
                if (!record.getUuid().equals(excludeUuid)) {
                    CompletableFuture<Boolean> future = punishmentRepository.getActiveBan(record.getUuid())
                        .thenApply(opt -> opt.isPresent());
                    futures.add(future);
                }
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().anyMatch(f -> f.join()));
        });
    }

    public void notifyStaffAboutAlts(Player player, List<String> altAccounts) {
        if (altAccounts.isEmpty()) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("rapidban.notify.alt"))
                .forEach(staff -> {
                    staff.sendMessage("§c[RapidBan] §e" + player.getName() + " §7has alt accounts:");
                    altAccounts.forEach(alt -> staff.sendMessage("§7  - §f" + alt));
                });
        });
    }
}
