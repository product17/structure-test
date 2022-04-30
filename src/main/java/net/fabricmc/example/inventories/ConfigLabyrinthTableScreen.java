package net.fabricmc.example.inventories;

import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class ConfigLabyrinthTableScreen extends CottonInventoryScreen<ConfigLabyrinthTableGui> {

    public ConfigLabyrinthTableScreen(ConfigLabyrinthTableGui description, PlayerInventory inventory, Text title) {
        super(description, inventory, title);
    }
}
