package net.fabricmc.example.structure;

import java.util.LinkedList;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.example.ExampleMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.util.math.BlockPos;

public class StructureBuildQueue {
    public Integer level; // config for difficulty
    public Integer maxDepth = 3; // How many chains down the main path in the jigsaw
    public Integer currentDepth = 0; // used while building to track depth
    public static LinkedList<StructureBlockInfo> mainPathQueue = new LinkedList<>();
    public static LinkedList<JigsawBlockEntity> jigsawQueue = new LinkedList<>();
    public LinkedList<BlockPos> spawnPositions = new LinkedList<>();
    public Block playerSpawnBlockType;

    public StructureBuildQueue(String spawnBlockTypeString) {
        // reset them on create
        mainPathQueue = new LinkedList<>();
        jigsawQueue = new LinkedList<>();
        StringReader blockString = new StringReader(spawnBlockTypeString);
        try {
            BlockArgumentParser blockArgumentParser = new BlockArgumentParser(blockString, false).parse(true);
            BlockState blockState = blockArgumentParser.getBlockState();
            this.playerSpawnBlockType = blockState.getBlock();
        } catch(CommandSyntaxException exception) {
            ExampleMod.LOGGER.info(exception.getMessage());
            this.playerSpawnBlockType = Blocks.TARGET; // Default to target block
        }
    }

    public void addJigsawBlockEntity(JigsawBlockEntity entity) {
        StructureBuildQueue.jigsawQueue.add(entity);
    }

    public void addMainPathEntity(StructureBlockInfo mainPathEntity) {
        StructureBuildQueue.mainPathQueue.add(mainPathEntity);
    }

    public StructureBlockInfo next() {
        if (StructureBuildQueue.mainPathQueue.size() > 0) {
            return StructureBuildQueue.mainPathQueue.removeFirst();
        }

        return null;
    }

    public JigsawBlockEntity nextJigsaw() {
        if (StructureBuildQueue.jigsawQueue.size() > 0) {
            return StructureBuildQueue.jigsawQueue.removeFirst();
        }

        return null;
    }

    public void setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }
}
