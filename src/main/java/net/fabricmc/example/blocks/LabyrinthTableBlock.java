package net.fabricmc.example.blocks;

import java.util.Random;
import java.util.UUID;

import net.fabricmc.example.blockEntities.LabyrinthTableBlockEntity;
import net.fabricmc.example.items.ItemLoader;
import net.fabricmc.example.state.Zone;
import net.fabricmc.example.state.ZoneManager;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LabyrinthTableBlock extends BlockWithEntity {

    public LabyrinthTableBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LabyrinthTableBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		// You need a Block.createScreenHandlerFactory implementation that delegates to the block entity,
		// such as the one from BlockWithEntity
        // System.out.println("AMOUNT: " + 200 * (1/256f));
		player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
		return ActionResult.SUCCESS;
	}

    @Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ItemLoader.LABYRNITH_BLOCK_ENTITY, (world1, pos, state1, be) -> LabyrinthTableBlockEntity.tick(world, pos, state, be));
    }
}
