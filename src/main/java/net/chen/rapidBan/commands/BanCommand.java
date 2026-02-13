package net.chen.rapidBan.commands;

import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.core.events.PlayerBannedByConsoleEvent;
import net.chen.rapidBan.core.events.PlayerBannedEvent;
import net.chen.rapidBan.enums.PunishmentType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BanCommand implements CommandExecutor, TabCompleter {
    private final RapidBan plugin;

    public BanCommand(RapidBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rapidban.ban")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /ban <玩家> <原因> [-t <时长>] [-s]");
            sender.sendMessage("§c示例: /ban Player123 作弊 -t 7d -s");
            return true;
        }

        String targetName = args[0];
        StringBuilder reasonBuilder = new StringBuilder();
        Long durationTemp = null;
        boolean silentTemp = false;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-t") && i + 1 < args.length) {
                durationTemp = parseDuration(args[i + 1]);
                i++;
            } else if (args[i].equalsIgnoreCase("-s")) {
                silentTemp = true;
            } else {
                if (!reasonBuilder.isEmpty()) reasonBuilder.append(" ");
                reasonBuilder.append(args[i]);
            }
        }

        final Long duration = durationTemp;
        final boolean silent = silentTemp;
        String reason = reasonBuilder.toString();
        if (reason.isEmpty()) {
            sender.sendMessage("§c请提供封禁原因");
            return true;
        }

        plugin.getPlayerRepository().getPlayerByName(targetName).thenAccept(optPlayer -> {
            if (optPlayer.isEmpty()) {
                sender.sendMessage("§c玩家 " + targetName + " 未找到");
                return;
            }

            String targetUuid = optPlayer.get().getUuid();
            String operatorUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
            String operatorName = sender.getName();

            PunishmentType type = duration != null ? PunishmentType.TEMPBAN : PunishmentType.BAN;

            plugin.getPunishmentManager().punishPlayer(targetUuid, type, reason, operatorUuid, operatorName, duration, silent)
                .thenAccept(punishment -> {
                    if (!silent) {
                        plugin.getServer().broadcast(
                            Component.text("§c" + targetName + " §7已被 §c" + operatorName + " §7封禁")
                                .append(Component.text("\n§7原因: §f" + reason)),
                            "rapidban.notify"
                        );
                    }
                    if(operatorName.equals("CONSOLE")){
                        PlayerBannedByConsoleEvent event = new PlayerBannedByConsoleEvent((net.chen.rapidBan.models.Player) Bukkit.getPlayer(targetUuid));
                        event.callEvent();
                    }else {
                        PlayerBannedEvent event = new PlayerBannedEvent((net.chen.rapidBan.models.Player) Bukkit.getPlayer(targetUuid));
                        event.callEvent();
                    }
                    sender.sendMessage("§a成功封禁玩家 " + targetName + " (ID: #" + punishment.getId() + ")");

                    Player target = plugin.getServer().getPlayer(targetName);
                    if (target != null && target.isOnline()) {
                        plugin.getPunishmentManager().kickPlayer(target, punishment);
                    }
                });
        }).exceptionally(ex -> {
            sender.sendMessage("§c执行命令时出错: ");
            if(RapidBan.instance.getSimpleConfig().isDebug){
                sender.sendMessage(ex.getMessage());
            }
            RapidBan.instance.logger.warning("Ban command error: " + ex.getMessage());
            return null;
        });

        return true;
    }

    private Long parseDuration(String input) {
        try {
            char unit = input.charAt(input.length() - 1);
            long value = Long.parseLong(input.substring(0, input.length() - 1));

            return switch (unit) {
                case 's' -> TimeUnit.SECONDS.toMillis(value);
                case 'm' -> TimeUnit.MINUTES.toMillis(value);
                case 'h' -> TimeUnit.HOURS.toMillis(value);
                case 'd' -> TimeUnit.DAYS.toMillis(value);
                case 'w' -> TimeUnit.DAYS.toMillis(value * 7);
                case 'M' -> TimeUnit.DAYS.toMillis(value * 30);
                case 'y' -> TimeUnit.DAYS.toMillis(value * 365);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            plugin.getServer().getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        } else if (args.length > 2) {
            completions.addAll(Arrays.asList("-t", "-s"));
        }

        return completions;
    }
}
