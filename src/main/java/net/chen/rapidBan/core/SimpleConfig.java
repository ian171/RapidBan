package net.chen.rapidBan.core;

import lombok.Getter;
import lombok.ToString;
import net.chen.rapidBan.RapidBan;

@Getter
@ToString
public class SimpleConfig {
    private final RapidBan plugin;
    public boolean isDebug;
    public SimpleConfig(RapidBan javaPlugin){
        this.plugin = javaPlugin;
        this.isDebug = RapidBan.instance.getConfig().getBoolean("dev.debug");
    }
}
