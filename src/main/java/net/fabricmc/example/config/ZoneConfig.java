package net.fabricmc.example.config;

import java.util.ArrayList;
import java.util.List;

public class ZoneConfig {
    public Boolean elytraAllowed = false;
    public List<String> breakableBlocks = new ArrayList<>();
    public List<LootTier> chestLootTables = new ArrayList<>();
    public List<LootTier> defaultLootTables = new ArrayList<>();
    public String lootPools;
    public MobConfig mobs;
    public String name;
    public RoomPoolConfig roomPools;
    public Integer worldHeight = 64;
    public String dimentionType;

    public static String getLootTableAtLevel(int level, List<LootTier> lootTableList) {
        for (LootTier lootTier : lootTableList) {
            if (lootTier.levelRange == null) {
                return lootTier.lootTable;
            }

            if (
                (lootTier.levelRange.max == null ||
                lootTier.levelRange.max >= level) &&
                (lootTier.levelRange.min == null ||
                lootTier.levelRange.min <= level)
            ) {
                return lootTier.lootTable;
            }
        }

        return null;
    }
}
