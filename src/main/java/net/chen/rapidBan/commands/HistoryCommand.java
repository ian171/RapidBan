package net.chen.rapidBan.commands;

import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.models.Punishment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jspecify.annotations.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryCommand implements CommandExecutor, TabCompleter {
    private final RapidBan plugin;

    public HistoryCommand(RapidBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rapidban.history")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§c用法: /history <玩家>");
            return true;
        }

        String targetName = args[0];

        plugin.getPlayerRepository().getPlayerByName(targetName).thenAccept(optPlayer -> {
            if (optPlayer.isEmpty()) {
                sender.sendMessage("§c玩家 " + targetName + " 未找到");
                return;
            }

            String targetUuid = optPlayer.get().getUuid();

            plugin.getPunishmentManager().getPunishmentHistory(targetUuid).thenAccept(punishments -> {
                if (punishments.isEmpty()) {
                    sender.sendMessage("§e玩家 " + targetName + " 没有处罚记录");
                    return;
                }

                sender.sendMessage("§6========== §e" + targetName + " 的处罚历史 §6==========");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                for (Punishment p : punishments) {
                    String status = p.isRevoked() ? "§c已撤销" : (p.isActive() ? "§a活跃" : "§7已过期");
                    String date = sdf.format(new Date(p.getCreatedAt()));

                    sender.sendMessage("§7[§f#" + p.getId() + "§7] " + status + " §f" + p.getType().getDisplayName());
                    sender.sendMessage("  §7原因: §f" + p.getReason());
                    sender.sendMessage("  §7执行者: §f" + p.getOperatorName() + " §7| §f" + date);

                    if (p.isRevoked()) {
                        sender.sendMessage("  §c撤销者: §f" + p.getRevokedBy() + " §7| §f" + p.getRevokeReason());
                    }

                    sender.sendMessage("");
                }
            });
        }).exceptionally(ex -> {
            sender.sendMessage("§c执行命令时出错: " + ex.getMessage());
            return null;
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String @NonNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            plugin.getServer().getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        }
        return completions;
    }
}
