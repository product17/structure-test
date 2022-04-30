package net.fabricmc.example.state;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class PreviousPos {
    public BlockPos lastPos;
    public RegistryKey<World> worldKey;

    public PreviousPos(BlockPos lastPos, RegistryKey<World> worldKey) {
        this.lastPos = lastPos;
        this.worldKey = worldKey;
    }
}
