package net.chen.rapidBan.core.events;

import lombok.Getter;
import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.models.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerBannedEvent extends Event implements Cancellable {
    private static final HandlerList handlerList = new HandlerList();
    protected Player player;

    public PlayerBannedEvent(Player player){
        this.player = player;
    }
    @Override
    public boolean isCancelled() {
        return true;
    }

    @Override
    public void setCancelled(boolean cancel) {
        RapidBan.instance.getPunishmentManager().unbanPlayer(player.getUuid(),"CONSOLE");
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
