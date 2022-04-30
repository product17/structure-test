package net.fabricmc.example.items;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import net.fabricmc.example.config.LoadConfig;
import net.fabricmc.example.config.StructurePoolConfig;
import net.fabricmc.example.processors.JigsawProcessor;
import net.fabricmc.example.structure.StructureBuildQueue;
import net.minecraft.block.BlockState;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.Structure;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class BuildStructure extends Item {
  public static final String name = "build_structure";

  public BuildStructure(Settings settings) {
    super(settings);
    //TODO Auto-generated constructor stub
  }

  public StructureBlockInfo getConnectionJigsawBlock(List<StructureBlockInfo> structBlocks) {
    if (structBlocks.size() > 0) {
      for (StructureBlockInfo structBlock : structBlocks) {
        // TODO: pull the target from the config
        if (structBlock.nbt.getString(JigsawBlockEntity.NAME_KEY).equals("labyrinth:main_path")) {
          // This is our target block, need to shift this structure to line this up with our jigsaw block
          return structBlock;
        }
      }
    }

    return null;
  }

  public BlockRotation getRotationAmount(Direction jigsawDirection, Direction targetDirection) {
    if (jigsawDirection.getOpposite().equals(targetDirection)) {
      // if it's already the opposite no change
      return BlockRotation.NONE;
    }

    // If it's the same, just flip it
    if (jigsawDirection.equals(targetDirection)) {
      return BlockRotation.CLOCKWISE_180;
    }

    // Rotate 90 and run the check again
    BlockRotation adjusted = this.getRotationAmount(jigsawDirection, targetDirection.rotateClockwise(Axis.Y));
    // If it's the same, just rotate 90
    if (adjusted.equals(BlockRotation.NONE)) {
      return BlockRotation.CLOCKWISE_90;
    }

    // If it's not the same, rotate countClockwise
    return BlockRotation.COUNTERCLOCKWISE_90;
  }

  @Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand user) {
		ItemStack tmpStack = player.getStackInHand(user);

    if (world.isClient) {
      return TypedActionResult.success(tmpStack, world.isClient());
    }

    BlockPos blockPos = player.getCameraBlockPos();
    ServerWorld serverWorld = (ServerWorld)world;
    // player.getWorld();
    // RegistryKey<DimensionType> dim = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, new Identifier("labyrinth"));
    // world.getServer().getWorld(dim);
    for (RegistryKey<World> server : world.getServer().getWorldRegistryKeys()) {
      // TODO: map the key to the config of the map
      if (server.getValue().equals(new Identifier("labyrinth:labyrinth"))) {
        System.out.println(server.getValue());
        serverWorld = world.getServer().getWorld(server);
      }
    }
    StructureManager structureManager = serverWorld.getStructureManager();
    Optional<Structure> structure = structureManager.getStructure(new Identifier("test"));
    if (structure.isPresent()) {
      BlockPos offset = new BlockPos(0, -1, 0);
      Structure struct = structure.get();
      StructurePlacementData placementData = new StructurePlacementData().setMirror(BlockMirror.NONE);

      // Config stuff
      // Will need to while over the structureConfig like a getNext() setup
      // run through all path items
      // then we can run through the decor items
      // String id = UUID.randomUUID().toString();
      // StructureController.add(id, new StructureConfig(5, 1));
      StructureBuildQueue structConfig = new StructureBuildQueue("");
      structConfig.setMaxDepth(5);

      // Add the config processor
      placementData.addProcessor(
        new JigsawProcessor(structConfig)
      );

      // TODO: blockPos should come from the config
      // Such as how high the start position should be
      // maybe a chunk offset if a second instance needs to be created (6 chunks or something, probably bigger...)
      struct.place(serverWorld, blockPos.add(offset), null, placementData, new Random(serverWorld.getSeed()), 0);

      int depth = 0; // preventing an infinite loop...
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
          JigsawBlock pathJigsawBlock = (JigsawBlock)blockState.getBlock();

          // Get the target pool info
          String targetPool = structureBlockInfo.nbt.getString(JigsawBlockEntity.POOL_KEY);
          if (depth == structConfig.maxDepth) {
            // if it's the last room, load the boss room
            // targetPool = LoadConfig.config.roomPools.bossRoom;
          }

          StructurePoolConfig structurePoolConfig = LoadConfig.structurePools.get(targetPool);

          // If the pool has elements, continue
          if (structurePoolConfig != null && structurePoolConfig.elements.length > 0) {
            // TODO: grab weighted random element
            // Also add a level element? or some kind of tiering system
            int len = structurePoolConfig.elements.length;
            int rand = new Random().nextInt(len);
            Optional<Structure> optPathStructure = structureManager.getStructure(new Identifier(structurePoolConfig.elements[rand].element.location));
            if (optPathStructure.isPresent()) {

              // Initialize the Structure to place for this jigsaw block
              Structure pathStructure = optPathStructure.get();

              // Make this so we can add the processors to continue this process.. (it will add more items for this while to process, until done)
              // NOTE: this can cause an infinite loop if there is no max size
              StructurePlacementData pathPlacementData = new StructurePlacementData().setMirror(BlockMirror.NONE);
              pathPlacementData.addProcessor(
                new JigsawProcessor(structConfig)
              );

              // Get the target Jigsaw blocks in the chosen structure
              List<StructureBlockInfo> structBlocks = pathStructure.getInfosForBlock(structureBlockInfo.pos, pathPlacementData, pathJigsawBlock);
              StructureBlockInfo mainPath = this.getConnectionJigsawBlock(structBlocks);

              // If item has a main path
              // If not... maybe we should log an error? with the name of the structure that doesn't have a main_path
              if (mainPath != null) {
                // Align the rotation
                Direction mainPathDirection = JigsawBlock.getFacing(mainPath.state);
                mainPathRotationAlignment = this.getRotationAmount(jigsawDirection, mainPathDirection);
                pathPlacementData.setRotation(mainPathRotationAlignment);

                // Get the mainPath again with corrected direction
                structBlocks = pathStructure.getInfosForBlock(structureBlockInfo.pos, pathPlacementData, pathJigsawBlock);
                mainPath = this.getConnectionJigsawBlock(structBlocks);
                
                // Shift the pos for the maths
                BlockPos structBlockPos = structureBlockInfo.pos.offset(jigsawDirection);
                BlockPos mainPathBlockPos = mainPath.pos.offset(jigsawDirection);
                
                // Adjust position of structure to align jigsaw blocks
                int xDiff = structBlockPos.getX() - mainPathBlockPos.getX();
                int yDiff = structBlockPos.getY() - mainPathBlockPos.getY();
                int zDiff = structBlockPos.getZ() - mainPathBlockPos.getZ();
                BlockPos shift = new BlockPos(xDiff, yDiff, zDiff);
                BlockPos updatedPos = structBlockPos.add(shift);
                pathStructure.place(serverWorld, updatedPos, null, pathPlacementData, new Random(serverWorld.getSeed()), 0);
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
      // }
    }
    return TypedActionResult.success(tmpStack, world.isClient());
	}
}
