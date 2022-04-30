package net.fabricmc.example.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.fabricmc.example.structure.StructureBuildQueue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class Zone {
    private List<PlayerEntity> players = new ArrayList<>();
    private HashMap<UUID, PreviousPos> previousPlayerPositions = new HashMap<>();
    private int difficulty = 0;
    private String dimentionType;
    private UUID id = UUID.randomUUID();
    private ServerWorld world;
    private StructureBuildQueue buildConfig;
    
    public Zone(String dimentionType, int difficulty) {
        // Loot tables?
        this.dimentionType = dimentionType;
        this.difficulty = difficulty;
    }

    public void addBuildConfig(StructureBuildQueue buildConfig) {
        this.buildConfig = buildConfig;
    }

    public StructureBuildQueue getBuildConfig() {
        return this.buildConfig;
    }

    public Boolean addPlayer(PlayerEntity player) {
        if (this.world.isClient) {
            return false;
        }

        this.players.add(player);
        if (this.world != null) {
            ServerPlayerEntity servPlayer = (ServerPlayerEntity)player;
            // servPlayer.set
            // player.nbt
            this.previousPlayerPositions.put(servPlayer.getUuid(), new PreviousPos(
                servPlayer.getBlockPos(),
                servPlayer.getWorld().getRegistryKey()
            ));
            int index = new Random().nextInt(this.buildConfig.spawnPositions.size());
            BlockPos spawnLoc = this.buildConfig.spawnPositions.get(index);
            servPlayer.teleport(this.world, spawnLoc.getX() + 0.5, spawnLoc.getY() + 1, spawnLoc.getZ() + 0.5, player.getYaw(), player.getPitch());
            return true;
        }

        return false;
    }

    public int getDifficulty() {
        return this.difficulty;
    }

    public String getDimentionType() {
        return this.dimentionType;
    }

    public UUID getId() {
        return this.id;
    }

    public ServerWorld getWorld() {
        return this.world;
    }

    public void removePlayer(PlayerEntity player) {
        ServerPlayerEntity servPlayer = (ServerPlayerEntity)player;
        this.players.remove(player);
        PreviousPos previousPos = this.previousPlayerPositions.get(player.getUuid());
        servPlayer.teleport(
            this.world.getServer().getWorld(previousPos.worldKey),
            previousPos.lastPos.getX(),
            previousPos.lastPos.getY(),
            previousPos.lastPos.getZ(),
            player.getYaw(),
            player.getPitch()
        );
    }

    public void setWorld(ServerWorld world) {
        this.world = world;
    }
}
