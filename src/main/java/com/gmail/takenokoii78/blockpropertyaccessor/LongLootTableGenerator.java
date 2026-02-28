package com.gmail.takenokoii78.blockpropertyaccessor;

import com.gmail.takenokoii78.json.JSONFile;
import com.gmail.takenokoii78.json.values.JSONArray;
import com.gmail.takenokoii78.json.values.JSONObject;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NullMarked
public class LongLootTableGenerator {
    private final List<BlockType> list;

    public LongLootTableGenerator(Registry<BlockType> registry) {
        this.list = new ArrayList<>(registry.stream().toList());
    }

    public void writeOut() {
        final JSONFile jsonFile = new JSONFile(BlockBinarySearchLayerizer.NAMESPACE_DIRECTORY.resolve("loot_table/.json"));
        jsonFile.getPath().getParent().toFile().mkdirs();
        if (!jsonFile.exists()) jsonFile.create();
        jsonFile.write(jsonObject());
    }

    private JSONObject jsonObject() {
        final JSONObject root = new JSONObject();
        final JSONArray pools = new JSONArray();
        root.set("pools", pools);

        for (final BlockType blockType : list) {
            pools.add(pool(blockType));
        }

        return root;
    }

    private JSONObject pool(BlockType blockType) {
        final JSONObject pool = new JSONObject();
        pool.set("rolls", 1);
        final JSONArray entries = new JSONArray();
        pool.set("entries", entries);
        final JSONObject entry = new JSONObject();
        entries.add(entry);
        entry.set("type", "item");
        entry.set("name", "knowledge_book");

        final JSONArray functions = new JSONArray();
        final JSONObject function = new JSONObject();
        functions.add(function);
        function.set("function", "set_custom_data");
        function.set("tag", JSONObject.valueOf(Map.of(
            BlockBinarySearchLayerizer.NAMESPACE, JSONObject.valueOf(Map.of(
                "id", blockType.getKey().toString()
            )))
        ));

        final JSONArray conditions = new JSONArray();
        final JSONObject condition = new JSONObject();
        conditions.add(condition);
        condition.set("condition", "location_check");
        condition.set("predicate", JSONObject.valueOf(Map.of(
            "block", JSONObject.valueOf(Map.of(
                "blocks", JSONArray.valueOf(List.of(blockType.getKey().toString()))
            ))
        )));

        entry.set("functions", functions);
        entry.set("conditions", conditions);

        return pool;
    }
}
