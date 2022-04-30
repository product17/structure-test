package net.fabricmc.example.items;

import net.fabricmc.example.Util;
import net.fabricmc.example.blockEntities.LabyrinthTableBlockEntity;
import net.fabricmc.example.blocks.LabyrinthTableBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.registry.Registry;

public class ItemLoader {
  public static BuildStructure BUILD_STRUCTURE = new BuildStructure(
		new Item.Settings().group(ItemGroup.MISC).maxCount(1)
	);
  public static ZoneOut ZONE_OUT = new ZoneOut(
    new Item.Settings().group(ItemGroup.MISC).maxCount(1)
  );
  public static Block LABYRNITH_TABLE_BLOCK = new LabyrinthTableBlock(FabricBlockSettings.of(Material.METAL).strength(1.5f, 6.0f));
  public static BlockItem LABYRINTH_BOCK_ITEM = new BlockItem(
    LABYRNITH_TABLE_BLOCK,
    new FabricItemSettings().group(ItemGroup.DECORATIONS)
  );

  public static BlockEntityType<LabyrinthTableBlockEntity> LABYRNITH_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(
    LabyrinthTableBlockEntity::new,
    LABYRNITH_TABLE_BLOCK
  ).build(null);
  
  
  public static void init() {
    Registry.register(Registry.ITEM, Util.id(BuildStructure.name), BUILD_STRUCTURE);
    Registry.register(Registry.ITEM, Util.id(ZoneOut.name), ZONE_OUT);
    Registry.register(Registry.BLOCK, Util.id("labyrinth_table_block"), LABYRNITH_TABLE_BLOCK);
    Registry.register(Registry.ITEM, Util.id("labyrinth_table_block"), LABYRINTH_BOCK_ITEM);
    Registry.register(Registry.BLOCK_ENTITY_TYPE, Util.id("labyrinth_table_block"), LABYRNITH_BLOCK_ENTITY);
  }
}
