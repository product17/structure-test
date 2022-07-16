package net.fabricmc.example;

import java.io.InputStream;
import java.util.UUID;

import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.example.config.LoadConfig;
import net.fabricmc.example.config.StructurePoolConfig;
import net.fabricmc.example.config.ZoneConfig;
import net.fabricmc.example.inventories.ConfigLabyrinthTableGui;
import net.fabricmc.example.inventories.LabyrinthTableGui;
import net.fabricmc.example.items.ItemLoader;
import net.fabricmc.example.lootFunctions.SetLootLevelFunction;
import net.fabricmc.example.processors.CleanupProcessor;
import net.fabricmc.example.processors.JigsawProcessor;
import net.fabricmc.example.processors.SpawnProcessor;
import net.fabricmc.example.state.Zone;
import net.fabricmc.example.state.ZoneManager;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class ExampleMod implements ModInitializer {
	public static final String modId = "labyrinth";
	public static ScreenHandlerType<ConfigLabyrinthTableGui> CONFIG_LABYRINTH_SCREEN_HANDLER_TYPE;
	public static ScreenHandlerType<LabyrinthTableGui> LABYRINTH_SCREEN_HANDLER_TYPE;
	public static StructureProcessorType<CleanupProcessor> CLEANUP_PROCESSOR = () -> CleanupProcessor.CODEC;
	public static StructureProcessorType<JigsawProcessor> JIGSAW_PROCESSOR = () -> JigsawProcessor.CODEC;
	public static StructureProcessorType<SpawnProcessor> SPAWN_PROCESSOR = () -> SpawnProcessor.CODEC;

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.4
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LogManager.getLogger(modId);

	
	@Override
	public void onInitialize() {
		CONFIG_LABYRINTH_SCREEN_HANDLER_TYPE = ScreenHandlerRegistry.registerExtended(
			Util.id("config_labyrinth_table_block"),
			ConfigLabyrinthTableGui::new
		);

		LABYRINTH_SCREEN_HANDLER_TYPE = ScreenHandlerRegistry.registerExtended(
			Util.id("labyrinth_table_block"),
			LabyrinthTableGui::new
		);

		// LootFunctionType.register();

		LootTableLoadingCallback.EVENT.register((resourceManager, lootManager, id, table, setter) -> {
			if (id.toString().startsWith("test_dungeon")) {
				System.out.println("Hit the loot table: " + id.toString());
				table.withFunction(SetLootLevelFunction.builder(ConstantLootNumberProvider.create(2.0f)).build());
				// LootTable lootTable = lootManager.getTable(new Identifier("test_dungeon:chests/test_loot"));
				// lootTable.
				// table.withPool(table.build());
			}
			// table.getPools();
			// table = FabricLootSupplierBuilder.of(lootManager.getTable(id));
		});

		Registry.register(Registry.STRUCTURE_PROCESSOR, new Identifier(modId, "jigsaw_processor"), JIGSAW_PROCESSOR);
		Registry.register(Registry.STRUCTURE_PROCESSOR, new Identifier(modId, "cleanup_processor"), CLEANUP_PROCESSOR);

		PlayerBlockBreakEvents.BEFORE.register((World world, PlayerEntity player, BlockPos blockPos, BlockState blockState, BlockEntity entity) -> {
			Zone zone = ZoneManager.getZoneByPlayerId(player.getUuid());

			// If the player is not in create and is in a Zone, check
			if (!player.isCreative() && zone != null) {
				return zone.canBreakBlock(player, blockState);
			}
			return true;
		});

		// This tails the respawn event and can probably be used to teleport the player
		ServerPlayerEvents.AFTER_RESPAWN.register((
			ServerPlayerEntity oldPlayer,
			ServerPlayerEntity newPlayer,
			boolean alive
		) -> {
			Zone zone = ZoneManager.getZoneByPlayerId(newPlayer.getUuid());
			if (zone != null) {
				if (zone.shouldKeepInventory(newPlayer.getUuid())) {
					newPlayer.getInventory().clone(oldPlayer.getInventory());
				}

				// respawn in the Zone
				zone.respawnPlayer(newPlayer);
			}
		});

		EntityElytraEvents.ALLOW.register((LivingEntity entity) -> {
			Zone zone = ZoneManager.getZoneByPlayerId(entity.getUuid());
			if (zone != null) {
				return zone.canUseElytra();
			}

			return true;
		});

		LoadConfig initConfig = new LoadConfig(modId);
		initConfig.readConfigFromFile();

		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return new Identifier(modId, "template_pools");
			}

			@Override
			public void reload(ResourceManager manager) {
				for(Identifier id : manager.findResources("worldgen/template_pool", path -> path.endsWith(".json"))) {
					try (InputStream stream = manager.getResource(id).getInputStream()) {
						byte[] bytes = new byte[stream.available()];
						Gson gson = new Gson();
						stream.read(bytes);
						String file = new String(bytes);
						StructurePoolConfig pool = gson.fromJson(file, StructurePoolConfig.class);
						LoadConfig.addPoolConfig(pool);
						stream.close();
					} catch (Exception e) {
						LOGGER.error("Error occurred while loading resource json " + id.toString(), e);
					}
				}

				for(Identifier id : manager.findResources("zone", path -> path.endsWith(".json"))) {
					try (InputStream stream = manager.getResource(id).getInputStream()) {
						byte[] bytes = new byte[stream.available()];
						Gson gson = new Gson();
						stream.read(bytes);
						String file = new String(bytes);
						ZoneConfig zoneConfig = gson.fromJson(file, ZoneConfig.class);
						LoadConfig.addZoneConfig(zoneConfig);
						stream.close();
					} catch (Exception e) {
						LOGGER.error("Error occurred while loading resource json " + id.toString(), e);
					}
				}
			}
		});
		
		ItemLoader.init();
		
		LOGGER.info("Loaded: " + modId);
	}
}
