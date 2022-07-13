package net.fabricmc.example.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.fabricmc.example.config.MobDefinition;
import net.fabricmc.example.config.ZoneConfig;
import net.fabricmc.example.structure.RoomData;
import net.fabricmc.example.structure.StructureBuildQueue;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class Zone {
    private BlockPos blockPos;
    private StructureBuildQueue buildConfig;
    private int difficulty = 0;
    private String dimentionType;
    private int emptyTicks = 0;
    private int emptyTicksMax = 100; // TODO: load this from a config for cleanup time
    private DimensionType generatedFromDimensionType;
    private UUID id = UUID.randomUUID();
    private int instanceKey;
    private Boolean hasSpawnedMobs = false;
    private List<PlayerEntity> players = new ArrayList<>();
    private HashMap<UUID, PreviousPos> previousPlayerPositions = new HashMap<>();
    private ServerWorld world;
    private ZoneConfig zoneConfig;
    
    public Zone(ZoneConfig zoneConfig, DimensionType generatedFromDimensionType, BlockPos blockPos, int instanceKey, int difficulty) {
        // Loot tables?
        this.blockPos = blockPos;
        this.dimentionType = zoneConfig.dimentionType;
        this.difficulty = difficulty;
        this.instanceKey = instanceKey;
        this.generatedFromDimensionType = generatedFromDimensionType;
        this.zoneConfig = zoneConfig;
    }

    public void addBuildConfig(StructureBuildQueue buildConfig) {
        this.buildConfig = buildConfig;
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
            this.previousPlayerPositions.put(
                servPlayer.getUuid(),
                new PreviousPos(
                    servPlayer.getBlockPos(),
                    servPlayer.getWorld().getRegistryKey()
                )
            );
                int index = new Random().nextInt(this.buildConfig.spawnPositions.size());
            BlockPos spawnLoc = this.buildConfig.spawnPositions.get(index);
            servPlayer.teleport(this.world, spawnLoc.getX() + 0.5, spawnLoc.getY() + 1, spawnLoc.getZ() + 0.5, player.getYaw(), player.getPitch());

            if (!this.hasSpawnedMobs) {
                for (RoomData room : this.buildConfig.rooms) {
                    for (BlockPos spawnPos : room.mobPositions) {
                        MobDefinition mobDefinition = this.zoneConfig.mobs.getRandomMob(this.difficulty);
                        EntityType<?> entity = Registry.ENTITY_TYPE.get(new Identifier(mobDefinition.mobType));
                        
                        MobEntity mob = (MobEntity)entity.create((World)world);
                        
                        // ZombieEntity zombie = new ZombieEntity((World)world);
                        mob.setCustomName(new LiteralText("Walker"));

                        String lootTable = ZoneConfig.getLootTableAtLevel(this.difficulty, zoneConfig.defaultLootTables);
                        if (mobDefinition.lootTables != null) {
                            String hasLootForDifficulty = ZoneConfig.getLootTableAtLevel(this.difficulty, mobDefinition.lootTables);
                            if (hasLootForDifficulty != null) {
                                lootTable = hasLootForDifficulty;
                            }
                        }

                        System.out.println("LootTable: " + lootTable);
                        NbtCompound nbt = new NbtCompound();
                        if (lootTable != null) {
                            nbt.putString("DeathLootTable", lootTable);
                            nbt.putLong("DeathLootTableSeed", (new Random()).nextLong());
                            mob.readNbt(nbt);
                        }

                        // Boost the damage
                        mob.getAttributeInstance(
                            EntityAttributes.GENERIC_ATTACK_DAMAGE
                        ).addPersistentModifier(
                            new EntityAttributeModifier(
                                "damageMultiplier",
                                mobDefinition.damageMultiplier,
                                EntityAttributeModifier.Operation.MULTIPLY_TOTAL
                            )
                        );

                        // Boost the health
                        mob.getAttributeInstance(
                            EntityAttributes.GENERIC_MAX_HEALTH
                        ).addPersistentModifier(
                            new EntityAttributeModifier(
                                "healthMultiplier",
                                mobDefinition.healthMultiplier,
                                EntityAttributeModifier.Operation.MULTIPLY_TOTAL
                            )
                        );

                        mob.setPosition(new Vec3d(spawnPos.getX(), spawnPos.getY() + 1, spawnPos.getZ()));
                        
                        System.out.println(mobDefinition.mobType + " : " + mob.getLootTable().toString());
                        world.spawnEntity(mob);
                    }
                }

                this.hasSpawnedMobs = true;
            }

            return true;
        }

        return false;
    }

    public List<BlockPos> getBlockList() {
        return this.buildConfig.blockList;
    }

    public StructureBuildQueue getBuildConfig() {
        return this.buildConfig;
    }
    
    public int getDifficulty() {
        return this.difficulty;
    }

    public String getDimentionType() {
        return this.dimentionType;
    }

    public int getEmptyTicks() {
        return this.emptyTicks;
    }

    public int getEmptyTicksMax() {
        return this.emptyTicksMax;
    }

    public UUID getId() {
        return this.id;
    }

    public int getInstanceKey() {
        return this.instanceKey;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public ServerWorld getWorld() {
        return this.world;
    }

    public Boolean matchEntryPoint(DimensionType dimType, BlockPos blockPos) {
        return this.generatedFromDimensionType.equals(dimType) && this.blockPos.equals(blockPos);
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

        // just always set this back to 0 when someone leaves
        // it will only start counting when no one is left though
        this.emptyTicks = 0;
    }

    public void incrementEmptyTicks() {
        this.emptyTicks++;
        if (this.emptyTicks >= this.emptyTicksMax) {
            // Run Cleanup
            // This can be batched over several ticks if it gets laggy
            List<BlockPos> blockList = getBlockList();
            for (BlockPos blockPos : blockList) {
                if (blockPos != null) {
                    BlockState existingBlock = this.world.getBlockState(blockPos);
                    if (existingBlock != null) {
                        BlockState blockState = Registry.BLOCK.get(new Identifier("air")).getDefaultState();
                        // System.out.println("Is this even hitting?");
                        this.world.setBlockState(blockPos, blockState);
                    }
                }
            }

            ZoneManager.cleanupZone(this.id);
        }
    }

    public void setWorld(ServerWorld world) {
        this.world = world;
    }
}
