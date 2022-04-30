package net.fabricmc.example.inventories;

import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class LabyrinthTableScreen extends CottonInventoryScreen<LabyrinthTableGui> {

    public LabyrinthTableScreen(LabyrinthTableGui description, PlayerInventory inventory, Text title) {
        super(description, inventory, title);
    }
    
}
