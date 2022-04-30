package net.fabricmc.example.structure;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class JigsawPlacement {
    // Position the entry JigsawBlock should be placed
    public BlockPos blockPos;
    // Direction the entry JigsawBlock should be facing
    public Direction direction;
    // Target type of structure
    public String target;

    public JigsawPlacement(BlockPos blockPos, Direction direction, String target) {
        this.blockPos = blockPos;
        this.direction = direction;
        this.target = target;
    }
}
