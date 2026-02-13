package net.chen.rapidBan.core.events;

import lombok.Getter;
import lombok.Setter;
import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.models.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

@Getter
@Setter
public class PlayerBannedByConsoleEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    protected Player player;

    public PlayerBannedByConsoleEvent(Player player){
        this.player = player;
    }
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @Override
    public boolean isCancelled() {
        return true;
    }

    @Override
    public void setCancelled(boolean cancel) {
        RapidBan.instance.getPunishmentManager().unbanPlayer(player.getUuid(),"CONSOLE");
    }
}
