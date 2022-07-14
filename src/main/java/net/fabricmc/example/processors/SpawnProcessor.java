package net.fabricmc.example.processors;

import java.util.Random;

import com.mojang.serialization.Codec;

import net.fabricmc.example.structure.StructureBuildQueue;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

public class SpawnProcessor extends StructureProcessor {
    public static Random random = new Random();
    public static final Codec<SpawnProcessor> CODEC = Codec.unit(new SpawnProcessor(new StructureBuildQueue("")));
    private StructureBuildQueue config;

    public SpawnProcessor(StructureBuildQueue config) {
        this.config = config;
    }

    @Override
    public StructureBlockInfo process(
        WorldView world,
        BlockPos blockPos,
        BlockPos var3,
        StructureBlockInfo struct1,
        StructureBlockInfo structureBlockInfo,
        StructurePlacementData var6
    ) {
        BlockState state = structureBlockInfo.state;
        if (state.isOf(config.playerSpawnBlockType)) {
            config.spawnPositions.add(structureBlockInfo.pos);
            return null; // Does this remove the block?
        }

        // Chest shit
        if (state.isOf(Blocks.CHEST)) {
            this.config.addChestToCurrentRoom(structureBlockInfo.pos);
            // NbtCompound nbt = structureBlockInfo.nbt;
            // // TODO: Pull the lootTable from configs
            // nbt.putString("LootTable", "test_dungeon:chests/test_loot");

            // just tracking the chest location to add lootTable later
        }

        // Spawner shit
        if (state.isOf(Blocks.SPAWNER)) {
            this.config.addMobToCurrentRoom(structureBlockInfo.pos);
            
            // return null to remove the spawner block
            return null;
        }

        return structureBlockInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.RULE;
    }
    
}
