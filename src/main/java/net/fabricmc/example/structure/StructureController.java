package net.fabricmc.example.structure;

import java.util.Map;

import com.google.common.collect.Maps;

public class StructureController {
    public static Map<String, StructureBuildQueue> structureConfigs = Maps.newHashMap();

    public static void add(String id, StructureBuildQueue config) {
        StructureController.structureConfigs.putIfAbsent(id, config);
    }

    public static StructureBuildQueue get(String structureName) {
        return StructureController.structureConfigs.get(structureName);
    }
}
