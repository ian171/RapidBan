package net.chen.rapidBan.commands;

import net.chen.rapidBan.RapidBan;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class UndoCommand implements CommandExecutor, TabCompleter {
    private final RapidBan plugin;

    public UndoCommand(RapidBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rapidban.undo")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§c用法: /punishundo <玩家> [原因]");
            return true;
        }

        String targetName = args[0];
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "管理员撤销";

        plugin.getPlayerRepository().getPlayerByName(targetName).thenAccept(optPlayer -> {
            if (optPlayer.isEmpty()) {
                sender.sendMessage("§c玩家 " + targetName + " 未找到");
                return;
            }

            String targetUuid = optPlayer.get().getUuid();

            plugin.getPunishmentManager().revokeAllPunishments(targetUuid, sender.getName(), reason).thenAccept(success -> {
                if (success) {
                    sender.sendMessage("§a成功撤销玩家 " + targetName + " 的所有处罚");
                    plugin.getServer().broadcast(
                        net.kyori.adventure.text.Component.text("§e" + targetName + " §7的所有处罚已被 §e" + sender.getName() + " §7撤销"),
                        "rapidban.notify"
                    );
                } else {
                    sender.sendMessage("§c撤销处罚时出错");
                }
            });
        }).exceptionally(ex -> {
            sender.sendMessage("§c执行命令时出错: " + ex.getMessage());
            return null;
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            plugin.getServer().getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        }
        return completions;
    }
}
