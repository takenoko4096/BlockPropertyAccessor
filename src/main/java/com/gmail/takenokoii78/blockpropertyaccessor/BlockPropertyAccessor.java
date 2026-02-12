package com.gmail.takenokoii78.blockpropertyaccessor;

import com.gmail.subnokoii78.gpcore.files.ResourceAccess;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@NullMarked
public final class BlockPropertyAccessor extends JavaPlugin {
    public static final String BLOCK_PROPERTY_ACCESSOR = "BlockPropertyAccessor";

    public static final String NAMESPACE = "block_property_accessor";

    public static final Path ROOT_DIRECTORY = Path.of(BLOCK_PROPERTY_ACCESSOR + '-' + Bukkit.getMinecraftVersion());

    public static final Path DATA_DIRECTORY = ROOT_DIRECTORY.resolve("data");

    public static final Path NAMESPACE_DIRECTORY = DATA_DIRECTORY.resolve(NAMESPACE);

    public static final Path FUNCTION_DIRECTORY = NAMESPACE_DIRECTORY.resolve("function");

    public static final Path TAGS_BLOCK_DIRECTORY = NAMESPACE_DIRECTORY.resolve("tags/block");

    @Override
    public void onLoad() {
        getComponentLogger().info("BlockPropertyAccessor をロードしています");
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getComponentLogger().info("BlockPropertyAccessor が起動しました");

        final ResourceAccess resourceAccess = new ResourceAccess(BLOCK_PROPERTY_ACCESSOR);
        resourceAccess.copy(ROOT_DIRECTORY);

        final List<BlockType> blockTypes = RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK).stream().toList();
        final BlockBinarySearchLayerizer layerizer = new BlockBinarySearchLayerizer(new ArrayList<>(blockTypes));
        layerizer.layerize();

        getComponentLogger().info("データパック {} が生成されました", ROOT_DIRECTORY);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getComponentLogger().info(Component.text("BlockPropertyAccessor が停止しました"));
    }
}
