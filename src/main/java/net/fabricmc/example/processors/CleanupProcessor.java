package net.fabricmc.example.processors;

import com.mojang.serialization.Codec;

import net.fabricmc.example.structure.StructureBuildQueue;
import net.minecraft.block.Blocks;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

public class CleanupProcessor extends StructureProcessor {
    public static final Codec<CleanupProcessor> CODEC = Codec.unit(new CleanupProcessor(new StructureBuildQueue(Blocks.TARGET.toString())));
    
    private StructureBuildQueue config;

    public CleanupProcessor(StructureBuildQueue config) {
        // Can pass the depth level here
        this.config = config;
    }

    @Override
    public StructureBlockInfo process(
        WorldView world,
        BlockPos pos,
        BlockPos var1,
        StructureBlockInfo struct1,
        StructureBlockInfo structureBlockInfo,
        StructurePlacementData var6
    ) {
        if (structureBlockInfo.pos == null) {
            System.out.println("this one is null");
        }
        this.config.blockList.add(structureBlockInfo.pos);

        return structureBlockInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.RULE;
    }
    
}
