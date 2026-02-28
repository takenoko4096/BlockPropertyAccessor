package com.gmail.takenokoii78.blockpropertyaccessor;

import com.gmail.subnokoii78.gpcore.files.ResourceAccess;
import com.gmail.takenokoii78.json.JSONFile;
import com.gmail.takenokoii78.json.JSONPath;
import com.gmail.takenokoii78.json.values.JSONArray;
import com.gmail.takenokoii78.json.values.JSONObject;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class DatapackGenerator {
    public static final Path ROOT_DIRECTORY = Path.of(BlockPropertyAccessor.BLOCK_PROPERTY_ACCESSOR + '-' + Bukkit.getMinecraftVersion());

    public static final Path FINAL_OUTPUT = Path.of(ROOT_DIRECTORY + ".zip");

    private final BlockPropertyAccessor plugin;

    private boolean isProcessing;

    public DatapackGenerator(BlockPropertyAccessor plugin) {
        this.plugin = plugin;
    }

    private ComponentLogger getComponentLogger() {
        return plugin.getComponentLogger();
    }

    public boolean startGenerating(boolean requireTypeTraits, boolean requireDataTraits) {
        if (isProcessing) {
            return false;
        }

        isProcessing = true;

        final Instant start = Instant.now();

        if (Files.exists(FINAL_OUTPUT)) try {
            getComponentLogger().info("重複した出力先パスを消去しています");

            Files.delete(FINAL_OUTPUT);

            getComponentLogger().info("重複した出力先パスを消去しました");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        final ResourceAccess resourceAccess = new ResourceAccess(BlockPropertyAccessor.BLOCK_PROPERTY_ACCESSOR);

        getComponentLogger().info("リソースからデータパックの雛形をコピーしています");

        resourceAccess.copy(ROOT_DIRECTORY, events -> {
            events.register(ResourceAccess.RESOURCE_COPY_BEFORE, e -> {
                if (e.getTo().endsWith(".gitkeep")) {
                    e.ignore();
                    getComponentLogger().info("{} に .gitkeep を検出; 無視しました", e.getTo());
                }
            });

            events.register(ResourceAccess.RESOURCE_COPY_AFTER, e -> {
                getComponentLogger().info("{} をコピーしました", e.getTo());

                if (e.getTo().endsWith("pack.mcmeta")) {
                    getComponentLogger().info("pack.mcmeta を編集しています");

                    final JSONFile packMcmeta = new JSONFile(e.getTo());
                    final JSONObject jsonObject = packMcmeta.readAsObject();

                    final PackFormat packFormat = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
                    final JSONArray version = JSONArray.valueOf(List.of(packFormat.major(), packFormat.minor()));

                    jsonObject.set(JSONPath.of("pack.min_format"), version);
                    jsonObject.set(JSONPath.of("pack.max_format"), version);

                    packMcmeta.write(jsonObject);

                    getComponentLogger().info("pack.mcmeta をロードしました: バージョン {}", version.asList());
                }
            });
        });

        final BlockBinarySearchLayerizer layerizer = new BlockBinarySearchLayerizer(
            plugin,
            RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK),
            requireTypeTraits,
            requireDataTraits
        );

        layerizer.layerize();

        getComponentLogger().info("データパックを .zip に圧縮しています");

        final ZipCompressor compressor = new ZipCompressor(plugin, ROOT_DIRECTORY);
        compressor.compress(FINAL_OUTPUT);

        getComponentLogger().info(".zip に圧縮したコピーを生成しました");

        getComponentLogger().info("不要なコピー元ファイルを除去します");

        try (final Stream<Path> stream = Files.walk(ROOT_DIRECTORY).sorted(Comparator.reverseOrder())) {
            final List<Path> paths = stream.toList();

            int lastProgress = 0;

            for (int i = 0; i < paths.size(); i++) {
                final Path path = paths.get(i);

                try {
                    Files.delete(path);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }

                final int progress = (i + 1) * 100 / paths.size();
                if (progress % 10 == 0 && lastProgress != progress) {
                    lastProgress = progress;
                    getComponentLogger().info("進捗率: {} %", progress);
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        getComponentLogger().info("不要なファイルの除去が完了しました");

        getComponentLogger().info(
            Component.text("データパック {} が生成されました").color(NamedTextColor.GREEN),
            FINAL_OUTPUT
        );

        isProcessing = false;

        final double millis = Duration.between(start, Instant.now()).toMillis() / 1000d;

        getComponentLogger().info("経過時間: {} 秒", millis);

        return true;
    }
}
