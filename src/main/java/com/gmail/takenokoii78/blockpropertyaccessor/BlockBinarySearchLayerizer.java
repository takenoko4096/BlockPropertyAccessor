package com.gmail.takenokoii78.blockpropertyaccessor;

import com.gmail.takenokoii78.json.JSONFile;
import com.gmail.takenokoii78.json.values.JSONArray;
import com.gmail.takenokoii78.json.values.JSONObject;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.world.level.block.state.properties.Property;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.SoundGroup;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

@NullMarked
public class BlockBinarySearchLayerizer {
    public static final String IDENTIFIER = "id";

    public static final String PROPERTIES = "properties";

    public static final String TRAITS = "traits";

    public static final String REQUIRE_TRAITS = "require_traits";

    public static final String NAMESPACE = "block_property_accessor";

    public static final Path DATA_DIRECTORY = DatapackGenerator.ROOT_DIRECTORY.resolve("data");

    public static final Path NAMESPACE_DIRECTORY = DATA_DIRECTORY.resolve(NAMESPACE);

    public static final Path FUNCTION_DIRECTORY = NAMESPACE_DIRECTORY.resolve("function");

    public static final Path TAGS_BLOCK_DIRECTORY = NAMESPACE_DIRECTORY.resolve("tags/block");

    public static final Path TAGS_FUNCTION_DIRECTORY = NAMESPACE_DIRECTORY.resolve("tags/function");

    private static final char ZERO = '0';

    private static final char ONE = '1';

    private final Plugin plugin;

    private final List<BlockType> list;

    private int blockTypeCount;

    private int lastProgress = 0;

    public BlockBinarySearchLayerizer(Plugin plugin, Registry<BlockType> registry) {
        this.plugin = plugin;
        this.list = new ArrayList<>(registry.stream().toList());
    }

    private ComponentLogger getComponentLogger() {
        return plugin.getComponentLogger();
    }

    public void layerize() {
        getComponentLogger().info("二分探索を階層構造化しています");

        layerize(list, FUNCTION_DIRECTORY);

        getComponentLogger().info("階層構造の生成が完了しました");

        getComponentLogger().info("エントリポイントのコマンドを調整しています");

        final Path entrypoint = FUNCTION_DIRECTORY.resolve(".mcfunction");
        try {
            final List<String> lines = new ArrayList<>(Files.readAllLines(entrypoint));
            lines.addFirst(String.format(
                "data remove storage %s: %s",
                NAMESPACE,
                TRAITS
            ));
            lines.addFirst(String.format(
                "data remove storage %s: %s",
                NAMESPACE,
                PROPERTIES
            ));
            lines.addFirst(String.format(
                "data remove storage %s: %s",
                NAMESPACE,
                IDENTIFIER
            ));
            Files.write(entrypoint, lines, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        final Path withTraits = FUNCTION_DIRECTORY.resolve("with_traits.mcfunction");
        final List<String> lines = List.of(
            String.format(
                "data modify storage %s: %s set value true",
                NAMESPACE,
                REQUIRE_TRAITS
            ),
            String.format(
                "function %s:",
                NAMESPACE
            ),
            String.format(
                "data remove storage %s: %s",
                NAMESPACE,
                REQUIRE_TRAITS
            )
        );
        try {
            Files.createFile(withTraits);
            Files.write(withTraits, lines, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        getComponentLogger().info("エントリポイントの編集が完了しました");

        getComponentLogger().info("処理されたブロックタイプ数: {}", blockTypeCount);

        getComponentLogger().info("関数タグを作成します");

        final Path tagJsonPath = TAGS_FUNCTION_DIRECTORY.resolve(".json");
        final JSONFile file = new JSONFile(tagJsonPath);
        final JSONObject object = file.readAsObject();
        object.set("values", JSONArray.valueOf(List.of(NAMESPACE + ':')));
        file.write(object);

        getComponentLogger().info("関数タグ '#{}' を作成しました", NAMESPACE + ':');

        final Path tagJsonPath2 = TAGS_FUNCTION_DIRECTORY.resolve("with_traits.json");
        final JSONFile file2 = new JSONFile(tagJsonPath2);
        final JSONObject object2 = file2.readAsObject();
        object2.set("values", JSONArray.valueOf(List.of(NAMESPACE + ':' + "with_traits")));
        file2.write(object2);

        getComponentLogger().info("関数タグ '#{}' を作成しました", NAMESPACE + ':' + "with_traits");
    }

    private @Nullable List<BlockType> layerize(List<BlockType> list, Path directory) {
        try {
            Files.createDirectories(directory);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (list.size() <= 2) {
            final List<BlockType> values = new ArrayList<>();
            if (!list.isEmpty()) values.add(list.removeFirst());
            if (!list.isEmpty()) values.add(list.removeFirst());

            finalBranchFunction(directory, values);

            for (final BlockType ignored : values) {
                blockTypeCount++;

                final int progress = 100 * blockTypeCount / this.list.size();
                if (progress % 10 == 0 && progress != lastProgress) {
                    lastProgress = progress;
                    getComponentLogger().info("進捗率: {} %", progress);
                }
            }

            return values;
        }
        else {
            final int s = list.size() / 2;
            final List<BlockType> a = list.subList(0, s);
            final List<BlockType> b = list.subList(s, list.size());
            final List<BlockType> values0 = layerize(new ArrayList<>(a), directory.resolve(String.valueOf(ZERO)));
            final List<BlockType> values1 = layerize(new ArrayList<>(b), directory.resolve(String.valueOf(ONE)));

            final Path relative = FUNCTION_DIRECTORY.relativize(directory);
            final Path tagDirectory = TAGS_BLOCK_DIRECTORY.resolve(relative);

            try {
                Files.createDirectories(tagDirectory);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (values0 == null) {
                tagBlockTags(tagDirectory.resolve(ZERO + ".json"), relative.resolve(String.valueOf(ZERO)));
            }
            else {
                tagBlocks(tagDirectory.resolve(ZERO + ".json"), values0);
            }

            if (values1 == null) {
                tagBlockTags(tagDirectory.resolve(ONE + ".json"), relative.resolve(String.valueOf(ONE)));
            }
            else {
                tagBlocks(tagDirectory.resolve(ONE + ".json"), values1);
            }

            final Path functionPath = directory.resolve(".mcfunction");
            try {
                Files.createFile(functionPath);
                Files.write(functionPath, List.of(
                    String.format(
                        "execute if block ~ ~ ~ #%s:%s run function %s:%s",
                        NAMESPACE,
                        relative.resolve(String.valueOf(ZERO)).toString().replaceAll("\\\\", "/"),
                        NAMESPACE,
                        relative.resolve(String.valueOf(ZERO)).toString().replaceAll("\\\\", "/") + '/'
                    ),
                    String.format(
                        "execute if block ~ ~ ~ #%s:%s run function %s:%s",
                        NAMESPACE,
                        relative.resolve(String.valueOf(ONE)).toString().replaceAll("\\\\", "/"),
                        NAMESPACE,
                        relative.resolve(String.valueOf(ONE)).toString().replaceAll("\\\\", "/") + '/'
                    )
                ));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            return null;
        }
    }

    private void finalBranchFunction(Path directory, List<BlockType> values) {
        final Path path = directory.resolve(".mcfunction");
        final List<String> lines = new ArrayList<>();

        for (final BlockType blockType : values) {
            final String functionName = FUNCTION_DIRECTORY
                .relativize(directory.resolve(blockType.getKey().value()))
                .toString()
                .replaceAll("\\\\", "/");

            lines.add(String.format(
                "execute if block ~ ~ ~ %s run function %s:%s",
                blockType.getKey(),
                NAMESPACE,
                functionName
            ));

            dataModifyFunction(directory, blockType);
        }

        try {
            Files.createFile(path);
            Files.write(path, lines, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void dataModifyFunction(Path directory, BlockType blockType) {
        final NamespacedKey key = blockType.getKey();
        final Path specificBlockFunctionPath = directory.resolve(key.value() + ".mcfunction");
        final List<String> lines = new ArrayList<>();
        lines.add(String.format(
            "data modify storage %s: %s set value \"%s\"",
            NAMESPACE,
            IDENTIFIER,
            key
        ));

        final Collection<Property<?>> properties = ((CraftBlockType<?>) blockType).getHandle()
            .defaultBlockState().getProperties();

        for (final Property<?> property : properties) {
            property.getAllValues().forEach(value -> lines.add(String.format(
                "execute if block ~ ~ ~ %s[%s=%s] run data modify storage %s: %s.%s set value %s",
                key,
                property.getName(),
                getPropertyValueName(value),
                NAMESPACE,
                PROPERTIES,
                property.getName(),
                getPropertyValueName(value)
            )));
        }

        final String traitsFunctionName = FUNCTION_DIRECTORY
            .relativize(directory.resolve(key.value() + "_traits"))
            .toString()
            .replaceAll("\\\\", "/");

        lines.add(String.format(
            "execute if data storage %s: {%s: true} run function %s:%s",
            NAMESPACE,
            REQUIRE_TRAITS,
            NAMESPACE,
            traitsFunctionName
        ));

        traitsFunction(directory, blockType);

        try {
            Files.createFile(specificBlockFunctionPath);
            Files.write(specificBlockFunctionPath, lines);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void traitsFunction(Path directory, BlockType blockType) {
        final Path path = directory.resolve(blockType.key().value() + "_traits.mcfunction");
        final BlockTraits blockTraits = new BlockTraits();

        blockTraits.addTrait("blast_resistance", blockType.getBlastResistance());
        blockTraits.addTrait("hardness", blockType.getHardness());
        blockTraits.addTrait("slipperiness", blockType.getSlipperiness());
        blockTraits.addTrait("is_flammable", blockType.isFlammable());
        blockTraits.addTrait("is_burnable", blockType.isBurnable());
        blockTraits.addTrait("is_occluding", blockType.isOccluding());
        blockTraits.addTrait("is_solid", blockType.isSolid());
        blockTraits.addTrait("has_collision", blockType.hasCollision());
        blockTraits.addTrait("has_gravity", blockType.hasGravity());
        blockTraits.addTrait("is_air", blockType.isAir());
        blockTraits.addTrait("translation_key", blockType.translationKey());
        if (blockType.hasItemType()) blockTraits.addTrait("item_type", blockType.getItemType().getKey().toString());

        final BlockData blockData = blockType.createBlockData();
        blockTraits.addTrait("light_emission", blockData.getLightEmission());

        final Color color = blockData.getMapColor();
        blockTraits.addTrait("map_color", Map.of(
            "red", color.getRed(),
            "green", color.getGreen(),
            "blue", color.getBlue()
        ));

        final SoundGroup soundGroup = blockData.getSoundGroup();
        blockTraits.addTrait("sound_group", Map.of(
            "on_break", Registry.SOUNDS.getKey(soundGroup.getBreakSound()).toString(),
            "on_fall", Registry.SOUNDS.getKey(soundGroup.getFallSound()).toString(),
            "on_hit", Registry.SOUNDS.getKey(soundGroup.getHitSound()).toString(),
            "on_place", Registry.SOUNDS.getKey(soundGroup.getPlaceSound()).toString(),
            "on_step", Registry.SOUNDS.getKey(soundGroup.getStepSound()).toString(),
            "volume", soundGroup.getVolume(),
            "pitch", soundGroup.getPitch()
        ));

        blockTraits.addTrait("piston_move_reaction", blockData.getPistonMoveReaction().name().toLowerCase());
        blockTraits.addTrait("is_replaceable", blockData.isReplaceable());
        blockTraits.addTrait("is_randomly_ticked", blockData.isRandomlyTicked());
        blockTraits.addTrait("requires_correct_tool_for_drops", blockData.requiresCorrectToolForDrops());

        try {
            Files.createFile(path);
            Files.write(path, blockTraits.lines());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends Comparable<T>> String getPropertyValueName(Property.Value<T> value) {
        return value.property().getName(value.value());
    }

    private void tagBlocks(Path path, List<BlockType> values) {
        final JSONObject object = new JSONObject();
        object.set("replace", false);
        final JSONArray array = new JSONArray();
        for (final BlockType value : values) {
            array.add(value.getKey().toString());
        }
        object.set("values", array);

        final JSONFile file = new JSONFile(path);
        if (!file.exists()) file.create();
        file.write(object);
    }

    private void tagBlockTags(Path path, Path directory) {
        final JSONObject object = new JSONObject();
        object.set("replace", false);
        final JSONArray array = new JSONArray();
        final String $0 = '#' + NAMESPACE + ':' + directory.resolve(String.valueOf(ZERO)).toString().replaceAll("\\\\", "/");
        final String $1 = '#' + NAMESPACE + ':' + directory.resolve(String.valueOf(ONE)).toString().replaceAll("\\\\", "/");
        array.add(JSONObject.valueOf(Map.of("id", $0, "required", false)));
        array.add(JSONObject.valueOf(Map.of("id", $1, "required", false)));
        object.set("values", array);

        final JSONFile file = new JSONFile(path);
        if (!file.exists()) file.create();
        file.write(object);
    }

    public static final class BlockTraits {
        private final List<String> lines = new ArrayList<>();

        public void addTrait(String name, Object value) {
            lines.add(String.format(
                "data modify storage %s: %s.%s set value %s",
                NAMESPACE,
                TRAITS,
                name,
                stringify(value)
            ));
        }

        private String stringify(Object value) {
            return switch (value) {
                case Boolean b -> String.valueOf(b);
                case Byte b -> b + "b";
                case Short s -> s + "s";
                case Long l -> l + "L";
                case Float f -> f + "f";
                case Double d -> d + "d";
                case String s -> "\"" + s + "\"";
                case Map<?, ?> m -> {
                    String c = "{";
                    final Set<? extends Map.Entry<?, ?>> entries = Set.copyOf(m.entrySet());
                    final String s = String.join(", ", entries.stream().map(entry -> {
                        return entry.getKey().toString() + ": " + stringify(entry.getValue());
                    }).toArray(String[]::new));
                    c += s;
                    c += "}";
                    yield c;
                }
                default -> value.toString();
            };
        }

        public List<String> lines() {
            return lines;
        }
    }
}
