package net.fabricmc.example.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import net.fabricmc.example.config.LoadConfig;
import net.fabricmc.example.config.StructurePoolConfig;
import net.fabricmc.example.config.ZoneConfig;
import net.fabricmc.example.processors.JigsawProcessor;
import net.fabricmc.example.processors.SpawnProcessor;
import net.fabricmc.example.structure.StructureBuildQueue;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.text.LiteralText;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class ZoneManager {
    // Active Labyrinths
    private static Map<UUID, Zone> activeZones = new HashMap<>();
    private static Map<UUID, UUID> playerToZoneMap = new HashMap<>();
    
    public static void addLabyrinth(UUID key, Zone lab) {
        ZoneManager.activeZones.put(key, lab);
    }

    public static int getActiveZonesInWorld(String worldName) {
        int activeZoneCount = 0;
        for (Zone zoneConf : ZoneManager.activeZones.values()) {
            if (zoneConf.getDimentionType().equals(worldName)) {
                activeZoneCount++;
            }
        }

        return activeZoneCount;
    }

    public static Zone getZone(UUID key) {
        return ZoneManager.activeZones.get(key);
    }

    public static Optional<Zone> generateZone(World world, PlayerEntity player, String zoneName) {
        if (world.isClient) {
            return Optional.ofNullable(null);
        }

        // Create new Zone
        ZoneConfig zoneConfig = LoadConfig.getZoneConfig(zoneName);
        zoneConfig.worldType = zoneConfig.worldType != null ? zoneConfig.worldType : "labyrinth:labyrinth";
        int activeZones = ZoneManager.getActiveZonesInWorld(zoneConfig.worldType);
        
        // openZones * offset
        BlockPos startLocation = new BlockPos(activeZones * 16, zoneConfig.worldHeight, 0);
        
        
        // TODO: load the block from config
        StructureBuildQueue structConfig = new StructureBuildQueue(Blocks.TARGET.toString());
        // TODO: load maxdepth from config
        structConfig.setMaxDepth(5);


        Zone zone = new Zone(zoneConfig.worldType, 1);
        zone.addBuildConfig(structConfig);
        ZoneManager.activeZones.put(zone.getId(), zone);
        // set start pos by checking existing in the biome

        ServerWorld serverWorld = (ServerWorld) world;
        for (RegistryKey<World> server : world.getServer().getWorldRegistryKeys()) {
            if (server.getValue().equals(new Identifier(zoneConfig.worldType))) {
                serverWorld = world.getServer().getWorld(server);
                zone.setWorld(serverWorld);
            }
        }

        StructureManager structureManager = serverWorld.getStructureManager();
        StructurePoolConfig startPool = LoadConfig.structurePools.get(zoneConfig.roomPools.start);
        Optional<Structure> structure = structureManager.getStructure(new Identifier(startPool.elements[0].element.location));
        if (structure.isPresent()) {
            Structure startStructure = structure.get();
            StructurePlacementData placementData = new StructurePlacementData().setMirror(BlockMirror.NONE);

            // Add the config processor
            placementData.addProcessor(new JigsawProcessor(structConfig));
            placementData.addProcessor(new SpawnProcessor(structConfig));

            // Place the Start Structure
            startStructure.place(serverWorld, startLocation, null, placementData, new Random(serverWorld.getSeed()), 0);

            int depth = 0; // preventing an infinite loop... starting at 0
            BlockRotation mainPathRotationAlignment = null;
            while (StructureBuildQueue.mainPathQueue.peek() != null) {
                StructureBlockInfo structureBlockInfo = structConfig.next();
                depth++;

                if (depth > structConfig.maxDepth) {
                    break;
                }

                if (structureBlockInfo != null) {

                    // Block info of the jigsaw block we are building from
                    BlockState blockState = structureBlockInfo.state;
                    // Use last rotation to align this block
                    if (mainPathRotationAlignment != null) {
                        blockState = blockState.rotate(mainPathRotationAlignment);
                    }

                    Direction jigsawDirection = JigsawBlock.getFacing(blockState);
                    JigsawBlock pathJigsawBlock = (JigsawBlock) blockState.getBlock();

                    // Get the target pool info
                    String targetPool = structureBlockInfo.nbt.getString(JigsawBlockEntity.POOL_KEY);
                    if (depth == structConfig.maxDepth) {
                        // if it's the last room, load the boss room
                        targetPool = zoneConfig.roomPools.bossRoom;
                    }

                    StructurePoolConfig structurePoolConfig = LoadConfig.structurePools.get(targetPool);

                    // If the pool has elements, continue
                    if (structurePoolConfig != null && structurePoolConfig.elements.length > 0) {
                        // TODO: grab weighted random element
                        // Also add a level element? or some kind of tiering system
                        int len = structurePoolConfig.elements.length;
                        int rand = new Random().nextInt(len);
                        Optional<Structure> optPathStructure = structureManager
                                .getStructure(new Identifier(structurePoolConfig.elements[rand].element.location));
                        if (optPathStructure.isPresent()) {

                            // Initialize the Structure to place for this jigsaw block
                            Structure pathStructure = optPathStructure.get();

                            // Make this so we can add the processors to continue this process.. (it will
                            // add more items for this while to process, until done)
                            // NOTE: this can cause an infinite loop if there is no max size
                            StructurePlacementData pathPlacementData = new StructurePlacementData()
                                    .setMirror(BlockMirror.NONE);
                            pathPlacementData.addProcessor(
                                    new JigsawProcessor(structConfig));

                            // Get the target Jigsaw blocks in the chosen structure
                            List<StructureBlockInfo> structBlocks = pathStructure
                                    .getInfosForBlock(structureBlockInfo.pos, pathPlacementData, pathJigsawBlock);
                            StructureBlockInfo mainPath = ZoneManager.getConnectionJigsawBlock(structBlocks);

                            // If item has a main path
                            // If not... maybe we should log an error? with the name of the structure that
                            // doesn't have a main_path
                            if (mainPath != null) {
                                // Align the rotation
                                Direction mainPathDirection = JigsawBlock.getFacing(mainPath.state);
                                mainPathRotationAlignment = ZoneManager.getRotationAmount(jigsawDirection, mainPathDirection);
                                pathPlacementData.setRotation(mainPathRotationAlignment);

                                // Get the mainPath again with corrected direction
                                structBlocks = pathStructure.getInfosForBlock(structureBlockInfo.pos, pathPlacementData,
                                        pathJigsawBlock);
                                mainPath = ZoneManager.getConnectionJigsawBlock(structBlocks);

                                // Shift the pos for the maths
                                BlockPos structBlockPos = structureBlockInfo.pos.offset(jigsawDirection);
                                BlockPos mainPathBlockPos = mainPath.pos.offset(jigsawDirection);

                                // Adjust position of structure to align jigsaw blocks
                                int xDiff = structBlockPos.getX() - mainPathBlockPos.getX();
                                int yDiff = structBlockPos.getY() - mainPathBlockPos.getY();
                                int zDiff = structBlockPos.getZ() - mainPathBlockPos.getZ();
                                BlockPos shift = new BlockPos(xDiff, yDiff, zDiff);
                                BlockPos updatedPos = structBlockPos.add(shift);
                                pathStructure.place(serverWorld, updatedPos, null, pathPlacementData,
                                        new Random(serverWorld.getSeed()), 0);

                                // Remove the Jigsaw Blocks
                                // world.setBlockState(structBlockPos, Registry.BLOCK.get(new Identifier("air")).getDefaultState());
                                // world.setBlockState(structureBlockInfo.pos, Registry.BLOCK.get(new Identifier("air")).getDefaultState());
                            }
                        }
                    }
                }
            }

            while (StructureBuildQueue.jigsawQueue.peek() != null) {
                JigsawBlockEntity jigsawBlockEntity = structConfig.nextJigsaw();

                if (jigsawBlockEntity != null) {
                    jigsawBlockEntity.generate(serverWorld, 3, false);
                }
            }
        }
        return Optional.of(zone);
    }

    public static StructureBlockInfo getConnectionJigsawBlock(List<StructureBlockInfo> structBlocks) {
        if (structBlocks.size() > 0) {
            for (StructureBlockInfo structBlock : structBlocks) {
                // TODO: pull the target from the config
                if (structBlock.nbt.getString(JigsawBlockEntity.NAME_KEY).equals("labyrinth:main_path")) {
                    // This is our target block, need to shift this structure to line this up with
                    // our jigsaw block
                    return structBlock;
                }
            }
        }

        return null;
    }

    public static BlockRotation getRotationAmount(Direction jigsawDirection, Direction targetDirection) {
        if (jigsawDirection.getOpposite().equals(targetDirection)) {
            // if it's already the opposite no change
            return BlockRotation.NONE;
        }

        // If it's the same, just flip it
        if (jigsawDirection.equals(targetDirection)) {
            return BlockRotation.CLOCKWISE_180;
        }

        // Rotate 90 and run the check again
        BlockRotation adjusted = ZoneManager.getRotationAmount(jigsawDirection, targetDirection.rotateClockwise(Axis.Y));
        // If it's the same, just rotate 90
        if (adjusted.equals(BlockRotation.NONE)) {
            return BlockRotation.CLOCKWISE_90;
        }

        // If it's not the same, rotate countClockwise
        return BlockRotation.COUNTERCLOCKWISE_90;
    }

    public static Zone getZoneByPlayerId(UUID playerId) {
        UUID zoneId = ZoneManager.playerToZoneMap.get(playerId);
        if (zoneId == null) {
            return null;
        }

        return ZoneManager.activeZones.get(zoneId);
    }

    public static void joinZone(UUID zoneName, PlayerEntity player) {
        // add the player to the specific lab when the teleport there
        Zone activeZone = ZoneManager.activeZones.get(zoneName);
        if (activeZone != null) {
            Boolean successfullyAdded = activeZone.addPlayer(player);
            if (successfullyAdded) {
                ZoneManager.playerToZoneMap.put(player.getUuid(), zoneName);
            }
        } else {
            // Send message: Cannot join right now
            player.sendMessage(new LiteralText("Unable to join zone"), true);
        }
    }

    public static void leaveZone(PlayerEntity player) {
        UUID zoneName = ZoneManager.playerToZoneMap.get(player.getUuid());

        if (zoneName != null) {
            Zone zone = ZoneManager.activeZones.get(zoneName);
            if (zone != null) {
                zone.removePlayer(player);
            }

            ZoneManager.playerToZoneMap.remove(player.getUuid());
        }
    }
}
