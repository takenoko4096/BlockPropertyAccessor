package com.gmail.takenokoii78.blockpropertyaccessor;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class BlockPropertyAccessor extends JavaPlugin {
    public static final String BLOCK_PROPERTY_ACCESSOR = "BlockPropertyAccessor";

    private static @Nullable BlockPropertyAccessor plugin;

    private final DatapackGenerator datapackGenerator = new DatapackGenerator(this);

    @Override
    public void onLoad() {
        plugin = this;
        getComponentLogger().info("BlockPropertyAccessor をロードしています");
    }

    @Override
    public void onEnable() {
        getComponentLogger().info("BlockPropertyAccessor が起動しました");

        getDataFolder().mkdirs();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands registrar = event.registrar();
            BlockPropertyAccessorCommand.BLOCK_PROPERTY_ACCESSOR_COMMAND.register(registrar);
        });
    }

    @Override
    public void onDisable() {
        getComponentLogger().info(Component.text("BlockPropertyAccessor が停止しました"));
    }

    public DatapackGenerator getDatapackGenerator() {
        return datapackGenerator;
    }

    public String getDatapackFileName() {
        return BLOCK_PROPERTY_ACCESSOR + '-' + getPluginMeta().getVersion() + '-' + Bukkit.getMinecraftVersion() + ".zip";
    }

    public static BlockPropertyAccessor getBlockPropertyAccessor() {
        if (plugin == null) throw new RuntimeException();
        return plugin;
    }
}
