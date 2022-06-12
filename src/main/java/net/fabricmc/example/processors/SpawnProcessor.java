package net.fabricmc.example.processors;

import net.minecraft.structure.Structure.StructureBlockInfo;

import java.util.Random;

import com.mojang.serialization.Codec;

import net.fabricmc.example.structure.StructureBuildQueue;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.MobSpawnerEntry;
import net.minecraft.world.MobSpawnerLogic;
import net.minecraft.world.World;
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
            NbtCompound nbt = structureBlockInfo.nbt;
            // TODO: Pull the lootTable from configs
            nbt.putString("LootTable", "test_dungeon:chests/test_loot");
        }

        // Spawner shit
        if (state.isOf(Blocks.SPAWNER)) {
            NbtCompound nbt = structureBlockInfo.nbt;
            // SpawnerBlock spawnBlock = (SpawnerBlock)state.getBlock();
            // MobSpawnerBlockEntity mobSpawnerBlockEntity = (MobSpawnerBlockEntity)spawnBlock.createBlockEntity(structureBlockInfo.pos, state);
            // MobSpawnerLogic logic = mobSpawnerBlockEntity.getLogic();

            // NbtCompound mobData = new NbtCompound();
            // mobData.putString("id", "zombie");

            // MobSpawnerEntry mobEntry = new MobSpawnerEntry(mobData, null);
            // // mobEntry.
            // logic.setSpawnEntry((World) world, blockPos, mobEntry);
            // logic.setEntityId(EntityType.ZOMBIE);

            // NbtList list = nbt.getList("SpawnPotentials", 10);
            // String e1 = list.getCompound(0).getCompound("Entity").getString("id");
            // String e2 = nbt.getCompound("SpawnData").getString("id");
            
            // if(!(e1.equals(e2))) {
                // list.getCompound(0).getCompound("Entity").putString("id", "zombie");
                // nbt.put("SpawnPotentials", list);
            // }

            // String test = nbt.getCompound("SpawnData").getString("id");
            nbt.putShort("Delay", (short) 20);
            nbt.putShort("MinSpawnDelay", (short) 20);
            nbt.putShort("MaxSpawnDelay", (short) 80);
            nbt.putString("EntityId", "zombie");
            
            NbtCompound spawnData = new NbtCompound();
            NbtCompound entityData = new NbtCompound();
            entityData.putString("id", "zombie");
            spawnData.put("entity", entityData);

            nbt.put("SpawnData", spawnData);

            System.out.println("testing the Spawner");
        }

        return structureBlockInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.RULE;
    }
    
}
