package net.fabricmc.example.inventories;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.networking.NetworkSide;
import io.github.cottonmc.cotton.gui.networking.ScreenNetworking;
import io.github.cottonmc.cotton.gui.widget.WBox;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WScrollPanel;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.fabricmc.example.ExampleMod;
import net.fabricmc.example.Util;
import net.fabricmc.example.blockEntities.LabyrinthTableBlockEntity;
import net.fabricmc.example.gui.Selection;
import net.fabricmc.example.inventories.data.ConfigLabyrinthTableData;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;

public class ConfigLabyrinthTableGui extends SyncedGuiDescription {
    public ConfigLabyrinthTableData configData;

    // Selected zone list
    public WBox selectedZoneList;
    public HashMap<String, WButton> selectedZoneListMap = new HashMap<>();
    public WLabel selectedZoneListNoItemsText;

    // Full zone list
    public WBox zoneList;
    public HashMap<String, WButton> zoneListMap = new HashMap<>();
    public WLabel zoneListNoItemsText;

    public ConfigLabyrinthTableGui(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory);
        String jsonString = buf.readString();

        if (jsonString != null && !jsonString.isEmpty()) {
            Gson gson = new Gson();
            this.configData = gson.fromJson(jsonString, ConfigLabyrinthTableData.class); // might need to handle the json error for future updates
            this.buildZoneListItems();
            this.buildSelectedZoneListItems();
        }
    }

    public ConfigLabyrinthTableGui(int syncId, PlayerInventory playerInventory) {
        super(ExampleMod.CONFIG_LABYRINTH_SCREEN_HANDLER_TYPE, syncId, playerInventory);

        WGridPanel root = new WGridPanel();
        setRootPanel(root);
        root.setInsets(Insets.ROOT_PANEL);

        // Add the zoneList on the left side
        this.zoneList = this.buildZoneListItems();
		root.add(new WScrollPanel(this.zoneList), 0, 2, 8, 5);

        // Add the selected zones to the list on the right side
        this.selectedZoneList = this.buildSelectedZoneListItems();
		root.add(new WScrollPanel(this.selectedZoneList), 9, 2, 8, 5);

        // This syncs the Client
        root.validate(this);
    }

    public WBox buildZoneListItems() {
        if (this.zoneList == null) {
            this.zoneList = new WBox(Axis.VERTICAL);
        }

        if (this.configData != null && this.configData.zoneList.size() > 0) {
            // Enforce the removal
            if (this.zoneListNoItemsText != null) {
                this.zoneList.remove(this.zoneListNoItemsText);
                this.zoneListNoItemsText = null;
            }

            for (String zoneName : this.configData.zoneList) {
                if (
                    !this.zoneListMap.containsKey(zoneName) &&
                    !this.configData.selectedZoneList.contains(zoneName)
                ) {
                    WButton button = new WButton(new LiteralText(zoneName));
                    button.setOnClick(() -> {
                        this.zoneListMap.remove(zoneName);
                        this.zoneList.remove(button);
                        this.configData.selectedZoneList.add(zoneName);
                        this.buildSelectedZoneListItems();

                        if (this.zoneListMap.size() == 0) {
                            this.zoneListNoItemsText = new WLabel("No Zones...");
                            this.zoneList.add(this.zoneListNoItemsText);
                        }

                        this.emitConfigUpdateEvent();
                    });

                    this.zoneListMap.put(zoneName, button);
                    this.zoneList.add(button, 134, 18);
                } else if (this.configData.selectedZoneList.contains(zoneName)) {
                    // If it has been added to the selected list remove it from zoneList
                    this.zoneList.remove(this.zoneListMap.get(zoneName));
                    this.zoneListMap.remove(zoneName);
                }
            }
        } else {
            this.zoneListNoItemsText = new WLabel("No Zones...");
            this.zoneList.add(this.zoneListNoItemsText);
        }

        this.zoneList.setSize(134, 80);

        return this.zoneList;
    }

    public WBox buildSelectedZoneListItems() {
        if (this.selectedZoneList == null) {
            this.selectedZoneList = new WBox(Axis.VERTICAL);
        }

        if (this.configData != null && this.configData.selectedZoneList.size() > 0) {
            // Enforce the removal
            if (this.selectedZoneListNoItemsText != null) {
                this.selectedZoneList.remove(this.selectedZoneListNoItemsText);
                this.selectedZoneListNoItemsText = null;
            }

            for (String zoneName : this.configData.selectedZoneList) {
                if (!this.selectedZoneListMap.containsKey(zoneName)) {
                    WButton button = new WButton(new LiteralText(zoneName));
                    button.setSize(134, 20);
                    button.setOnClick(() -> {
                        this.selectedZoneListMap.remove(zoneName);
                        this.selectedZoneList.remove(button);
                        this.configData.selectedZoneList.remove(zoneName);
                        this.buildZoneListItems();

                        if (this.selectedZoneListMap.size() == 0 && this.selectedZoneListNoItemsText == null) {
                            this.selectedZoneListNoItemsText = new WLabel("No Zones selected...");
                            this.selectedZoneList.add(this.selectedZoneListNoItemsText);
                        }

                        this.emitConfigUpdateEvent();
                    });
                    this.selectedZoneListMap.put(zoneName, button);
                    this.selectedZoneList.add(button, 134, 18);
                }
            }
        } else if (this.selectedZoneListNoItemsText == null) {
            this.selectedZoneListNoItemsText = new WLabel("No Zones selected...");
            this.selectedZoneList.add(this.selectedZoneListNoItemsText);
        }
        this.selectedZoneList.setSize(134, 80);

        return this.selectedZoneList;
    }

    public void emitConfigUpdateEvent() {
        Gson gson = new Gson();
        ScreenNetworking.of(this, NetworkSide.CLIENT).send(
            Util.id(LabyrinthTableBlockEntity.CONFING_UPDATE_EVENT + this.syncId),
            buf -> buf.writeString(gson.toJson(this.configData))
        );
    }
}
