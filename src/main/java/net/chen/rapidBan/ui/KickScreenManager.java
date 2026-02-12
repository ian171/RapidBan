package net.chen.rapidBan.ui;

import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.models.Punishment;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class KickScreenManager {
    private final RapidBan plugin;
    private FileConfiguration kickScreenConfig;

    public KickScreenManager(RapidBan plugin) {
        this.plugin = plugin;
        loadKickScreens();
    }

    public void loadKickScreens() {
        File kickScreenFile = new File(plugin.getDataFolder(), "kickscreens.yml");
        if (!kickScreenFile.exists()) {
            plugin.saveResource("kickscreens.yml", false);
        }
        kickScreenConfig = YamlConfiguration.loadConfiguration(kickScreenFile);
    }

    public String generateKickScreen(Punishment punishment, String playerName) {
        String templateKey = "screens." + punishment.getType().name().toLowerCase();
        String template = kickScreenConfig.getString(templateKey, getDefaultTemplate(punishment.getType().name()));

        return replacePlaceholders(template, punishment, playerName);
    }

    private String replacePlaceholders(String template, Punishment punishment, String playerName) {
        String result = template;

        result = result.replace("{player}", playerName);
        result = result.replace("{reason}", punishment.getReason());
        result = result.replace("{id}", String.valueOf(punishment.getId()));
        result = result.replace("{operator}", punishment.getOperatorName());
        result = result.replace("{type}", punishment.getType().getDisplayName());

        if (punishment.getExpiresAt() != null) {
            long remaining = punishment.getRemainingTime();
            result = result.replace("{remaining}", formatDuration(remaining));
            result = result.replace("{expires}", formatDate(punishment.getExpiresAt()));
        } else {
            result = result.replace("{remaining}", "永久");
            result = result.replace("{expires}", "永不");
        }

        result = result.replace("{date}", formatDate(punishment.getCreatedAt()));

        return result;
    }

    private String formatDuration(long millis) {
        if (millis <= 0) return "已过期";

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天 ");
        if (hours > 0) sb.append(hours).append("小时 ");
        if (minutes > 0) sb.append(minutes).append("分钟 ");
        if (seconds > 0 && days == 0) sb.append(seconds).append("秒");

        return sb.toString().trim();
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

    private String getDefaultTemplate(String type) {
        return "<red><bold>你已被封禁</bold></red>\n\n" +
               "<gray>原因: <white>{reason}</white></gray>\n" +
               "<gray>执行者: <white>{operator}</white></gray>\n" +
               "<gray>剩余时间: <white>{remaining}</white></gray>\n\n" +
               "<gray>处罚ID: <white>#{id}</white></gray>";
    }
}
