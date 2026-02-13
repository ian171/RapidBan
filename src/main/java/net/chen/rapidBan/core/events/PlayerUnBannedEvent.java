package net.chen.rapidBan.core.events;

import lombok.Getter;
import net.chen.rapidBan.models.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerUnBannedEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    protected Player player;
    public PlayerUnBannedEvent(Player player){
        this.player = player;
    }
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
