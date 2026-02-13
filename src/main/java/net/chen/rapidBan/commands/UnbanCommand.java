package net.chen.rapidBan.commands;

import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.core.events.PlayerUnBannedEvent;
import net.chen.rapidBan.models.Player;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class UnbanCommand implements CommandExecutor, TabCompleter {
    private final RapidBan plugin;

    public UnbanCommand(RapidBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rapidban.unban")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§c用法: /unban <玩家>");
            return true;
        }

        String targetName = args[0];

        plugin.getPlayerRepository().getPlayerByName(targetName).thenAccept(optPlayer -> {
            if (optPlayer.isEmpty()) {
                sender.sendMessage("§c玩家 " + targetName + " 未找到");
                return;
            }

            String targetUuid = optPlayer.get().getUuid();

            plugin.getPunishmentManager().unbanPlayer(targetUuid, sender.getName()).thenAccept(success -> {
                if (success) {
                    PlayerUnBannedEvent playerUnBannedEvent = new PlayerUnBannedEvent((Player) Bukkit.getPlayer(targetUuid));
                    playerUnBannedEvent.callEvent();
                    sender.sendMessage("§a成功解封玩家 " + targetName);
                    plugin.getServer().broadcast(
                        net.kyori.adventure.text.Component.text("§a" + targetName + " §7已被 §a" + sender.getName() + " §7解封"),
                        "rapidban.notify"
                    );
                } else {
                    sender.sendMessage("§c玩家 " + targetName + " 没有活跃的封禁记录");
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
        return new ArrayList<>();
    }
}
