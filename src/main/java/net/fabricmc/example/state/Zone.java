package net.fabricmc.example.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.lwjgl.system.CallbackI.P;

import net.fabricmc.example.config.MobDefinition;
import net.fabricmc.example.config.ZoneConfig;
import net.fabricmc.example.structure.RoomData;
import net.fabricmc.example.structure.StructureBuildQueue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
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
    private List<Block> breakableBlocks;
    private StructureBuildQueue buildConfig;
    private int difficulty = 0;
    private String dimentionType;
    private int emptyTicks = 0;
    private int emptyTicksMax = 100; // TODO: load this from a config for cleanup time
    private DimensionType generatedFromDimensionType;
    private UUID id = UUID.randomUUID();
    private int instanceKey;
    private Boolean hasSpawnedMobsAndChests = false;
    private Map<UUID, Room> mobToRoomMap = new HashMap<>();
    private List<PlayerEntity> players = new ArrayList<>();
    private HashMap<UUID, PreviousPos> previousPlayerPositions = new HashMap<>();
    private Random random = new Random();
    private List<Room> rooms = new ArrayList<>();
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

        if (this.world != null) {
            this.players.add(player);
            ServerPlayerEntity servPlayer = (ServerPlayerEntity)player;

            this.previousPlayerPositions.put(
                servPlayer.getUuid(),
                new PreviousPos(
                    servPlayer.getBlockPos(),
                    servPlayer.getWorld().getRegistryKey()
                )
            );

            this.teleportToZoneEntrance(servPlayer);

            if (!this.hasSpawnedMobsAndChests) {
                for (RoomData roomData : this.buildConfig.rooms) {
                    Room room = new Room();
                    this.rooms.add(room);

                    for (BlockPos chestPos : roomData.chestPositions) {
                        Optional<ChestBlockEntity> chestOpt = world.getBlockEntity(chestPos, BlockEntityType.CHEST);
                        if (chestOpt.isPresent()) {
                            String chestLootTable = ZoneConfig.getLootTableAtLevel(this.difficulty, zoneConfig.defaultLootTables);
                            if (zoneConfig.chestLootTables != null) {
                                String hasChestLootTable = ZoneConfig.getLootTableAtLevel(this.difficulty, zoneConfig.chestLootTables);
                                if (hasChestLootTable != null) {
                                    chestLootTable = hasChestLootTable;
                                }
                            }

                            ChestBlockEntity chest = chestOpt.get();
                            chest.setLootTable(new Identifier(chestLootTable), this.random.nextInt());
                        }
                    }

                    // Place the boss and remove one spawn location
                    if (roomData.isBossRoom) {
                        this.placeMob(room, roomData.mobPositions.remove(0), true);
                    }

                    // Place all mobs for each room
                    for (BlockPos spawnPos : roomData.mobPositions) {
                        this.placeMob(room, spawnPos, false);
                    }
                }

                this.hasSpawnedMobsAndChests = true;
            }

            return true;
        }

        return false;
    }

    public Boolean canBreakBlock(PlayerEntity player, BlockState blockState) {
        if (this.zoneConfig.breakableBlocks.size() > 0) {
            if (this.breakableBlocks == null) {
                this.breakableBlocks = new ArrayList<>();
                for (String blockName : this.zoneConfig.breakableBlocks) {
                    this.breakableBlocks.add(Registry.BLOCK.get(new Identifier(blockName)));
                }
            }

            for (Block breakableBlock : this.breakableBlocks) {
                if (blockState.isOf(breakableBlock)) {
                    return true;
                }
            }

            String msg = String.join(", ", this.zoneConfig.breakableBlocks);
            LiteralText text = new LiteralText("Can only Break: " + msg);
            player.sendMessage(text, true);
            return false;
        }

        return false;
    }

    public Boolean canUseElytra() {
        return this.zoneConfig.elytraAllowed;
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

    public MobDetails getMobById(UUID mobId) {
        Room room = this.mobToRoomMap.get(mobId);
        if (room != null) {
            MobDetails mobDetails = room.mobs.get(mobId);
            if (mobDetails == null) {
                mobDetails = room.bosses.get(mobId);
            }

            return mobDetails;
        }

        return null;
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


    // Takes a room and list of pos, choses and removes pos
    private void placeMob(Room room, BlockPos spawnPos, Boolean isBoss) {
        MobDetails mobDetails = new MobDetails();
        MobDefinition mobDefinition = this.zoneConfig.mobs.getRandomMob(this.difficulty, isBoss);
        EntityType<?> entity = Registry.ENTITY_TYPE.get(new Identifier(mobDefinition.mobType));
        MobEntity mob = (MobEntity)entity.create((World)world);

        mobDetails.expMultiplier = mobDefinition.xpMultiplier != null ? mobDefinition.xpMultiplier : 0;
        mob.setCustomName(new LiteralText(isBoss ? "Da Boss" : "Walker"));

        String lootTable = ZoneConfig.getLootTableAtLevel(this.difficulty, zoneConfig.defaultLootTables);
        if (mobDefinition.lootTables != null) {
            String hasLootForDifficulty = ZoneConfig.getLootTableAtLevel(this.difficulty, mobDefinition.lootTables);
            if (hasLootForDifficulty != null) {
                lootTable = hasLootForDifficulty;
            }
        }

        // Add loottable if exists
        if (lootTable != null) {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("DeathLootTable", lootTable);
            nbt.putLong("DeathLootTableSeed", (new Random()).nextLong());
            mob.readNbt(nbt);
        }

        // Boost the damage
        if (mobDefinition.damageMultiplier != null) {
            mob.getAttributeInstance(
                EntityAttributes.GENERIC_ATTACK_DAMAGE
            ).addPersistentModifier(
                new EntityAttributeModifier(
                    "damageMultiplier",
                    this.difficulty * mobDefinition.damageMultiplier,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL
                )
            );
        }

        // Boost the health
        if (mobDefinition.healthMultiplier != null) {
            mob.getAttributeInstance(
                EntityAttributes.GENERIC_MAX_HEALTH
            ).addPersistentModifier(
                new EntityAttributeModifier(
                    "healthMultiplier",
                    this.difficulty * mobDefinition.healthMultiplier,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL
                )
            );

            // Mobs need to be healed if their life is increased
            mob.heal(mob.getMaxHealth());
        }

        mob.setPosition(new Vec3d(spawnPos.getX(), spawnPos.getY() + 1, spawnPos.getZ()));
        world.spawnEntity(mob);
        mobDetails.mob = mob;
        if (isBoss) {
            room.bosses.put(mob.getUuid(), mobDetails);
        } else {
            room.mobs.put(mob.getUuid(), mobDetails);
        }

        this.mobToRoomMap.put(mob.getUuid(), room);
        ZoneManager.mapMobToZone(mob.getUuid(), this);
    }

    public void removeMobById(UUID mobId) {
        Room room = this.mobToRoomMap.get(mobId);
        if (room != null) {
            room.mobs.remove(mobId);
            room.bosses.remove(mobId);
        }
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

    public void respawnPlayer(ServerPlayerEntity player) {
        this.teleportToZoneEntrance(player);
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

    public Boolean shouldKeepInventory(UUID playerId) {
        // TODO: do some kind of death count on the player
        // if they exceed that they will die and lose their stuff
        return this.zoneConfig.keepInventoryOnDeath;
    }

    public void teleportToZoneEntrance(ServerPlayerEntity player) {
        int randomPos = this.random.nextInt(this.buildConfig.spawnPositions.size());
        BlockPos spawnLoc = this.buildConfig.spawnPositions.get(randomPos);
        player.teleport(
            this.world,
            spawnLoc.getX() + 0.5,
            spawnLoc.getY() + 1,
            spawnLoc.getZ() + 0.5,
            player.getYaw(),
            player.getPitch()
        );
    }
}
