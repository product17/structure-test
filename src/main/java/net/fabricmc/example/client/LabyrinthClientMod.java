package net.fabricmc.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.example.ExampleMod;
import net.fabricmc.example.inventories.ConfigLabyrinthTableGui;
import net.fabricmc.example.inventories.ConfigLabyrinthTableScreen;
import net.fabricmc.example.inventories.LabyrinthTableGui;
import net.fabricmc.example.inventories.LabyrinthTableScreen;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

public class LabyrinthClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Config Menu
        ScreenRegistry.<ConfigLabyrinthTableGui, ConfigLabyrinthTableScreen>register(
			ExampleMod.CONFIG_LABYRINTH_SCREEN_HANDLER_TYPE,
			(gui, inventory, title) -> new ConfigLabyrinthTableScreen(gui, inventory, title)
		);

        // Dungeon Start Menu
        ScreenRegistry.<LabyrinthTableGui, LabyrinthTableScreen>register(
			ExampleMod.LABYRINTH_SCREEN_HANDLER_TYPE,
			(gui, inventory, title) -> new LabyrinthTableScreen(gui, inventory, title)
		);
    }
    
}
