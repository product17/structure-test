package net.fabricmc.example.inventories;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.networking.NetworkSide;
import io.github.cottonmc.cotton.gui.networking.ScreenNetworking;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WItemSlot;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.fabricmc.example.ExampleMod;
import net.fabricmc.example.Util;
import net.fabricmc.example.gui.Selection;
import net.fabricmc.example.inventories.data.CurrentZoneData;
import net.fabricmc.example.state.Zone;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class LabyrinthTableGui extends SyncedGuiDescription {
    public static String clientDataEvent = "inv_data";
    public Identifier selectEventId;
    public Identifier playerEnterEventId;
    public Boolean selected = false;
    public List<Selection> selections = new ArrayList<>();
    public Boolean activeZone = false;
    public WGridPanel root;
    public String zoneId;
    public BlockPos blockPos;
    public Zone zone;

    public LabyrinthTableGui(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, new SimpleInventory(1));
        Gson gson = new Gson();
        String data = buf.readString();
        CurrentZoneData zoneData = gson.fromJson(data, CurrentZoneData.class);
        this.activeZone = zoneData.active;
        this.zoneId = zoneData.zoneName;
        this.blockPos = zoneData.blockPos;

        System.out.println("UI: " + data);
        if (this.activeZone) {
            this.addJoinButton();
        }
    }

    public LabyrinthTableGui(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ExampleMod.LABYRINTH_SCREEN_HANDLER_TYPE, syncId, playerInventory);
        this.blockInventory = inventory;
        this.selectEventId = Util.id("button_selected" + syncId);
        this.playerEnterEventId = Util.id("teleport_triggered" + syncId);

        // Button clicked listener
        if (!this.world.isClient() && this.world instanceof ServerWorld) {
            // ScreenNetworking.of(this, NetworkSide.SERVER).receive(this.teleportEventId, buf -> {
            //     System.out.println("Loading the zone");

            //     // Load the zone...
            //     ZoneManager.joinZone(this.zone.getId(), this.playerInventory.player);
            // });

            // return; // return on the server...
        } else {
            ScreenNetworking.of(this, NetworkSide.CLIENT).receive(Util.id(LabyrinthTableGui.clientDataEvent + syncId), buf -> {
                System.out.println("Got a message!!!" + buf.readString());
            });
        }

        this.root = new WGridPanel(1);
        setRootPanel(root);
        root.setSize(178, 166);
        root.setInsets(Insets.ROOT_PANEL);

        if (!this.activeZone) {
            WItemSlot itemSlot = this.buildInputSlot(root);
            root.add(itemSlot, 13, 36);
        }

        // Adds the player inventory to the screen
        // So, aligned left and down a bit from the last cell rendered... I think
        root.add(this.createPlayerInventoryPanel(), 0, 70);

        // This syncs the Client
        root.validate(this);
    }

    public void addJoinButton() {
        System.out.println("AddJoinButton Rendered");
        this.selections.forEach((selection) -> {
            this.root.remove(selection);
        });
        WButton button = new WButton(new LiteralText("Enter"));
        button.setOnClick(() -> {
            ScreenNetworking.of(this, NetworkSide.CLIENT).send(this.playerEnterEventId,
                buf -> buf.writeString(this.zoneId));
        });
        this.root.add(button, 72, 31, 80, 20);
    }

    public void bonusSelectedHandler(WGridPanel root, String value) {
        // propertyDelegate.set(0, 1);
        System.out.println("Clicked on: " + value);
        this.addJoinButton();

        ScreenNetworking.of(this, NetworkSide.CLIENT).send(this.selectEventId,
                buf -> buf.writeString(this.zoneId));

    }

    public WItemSlot buildInputSlot(WGridPanel root) {
        WItemSlot inputItemSlot = WItemSlot.of(this.blockInventory, 0);

        // Only maps
        // TODO: make this configable
        inputItemSlot.setFilter(stack -> stack.getItem() == Items.MAP);
        inputItemSlot.addChangeListener((slot, inventory, index, stack) -> {
            if (index == 0 && !stack.isEmpty() && !this.selected && !this.activeZone) {
                // reset the selections
                this.selections = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    Selection optionOne = new Selection(new LiteralText("+" + (5 * i) + "% More Items"));
                    selections.add(optionOne);
                    optionOne.setAlignment(HorizontalAlignment.LEFT);
                    optionOne.setSubText(new LiteralText(Integer.toString(((i + 1) * 2))));
                    String test = "" + 5 * i;
                    optionOne.setOnClick(() -> {
                        this.bonusSelectedHandler(root, test);
                    });
                    root.add(optionOne, 62, 12 + (i * 19), 100, 19);
                }
            } else if (index == 0 && this.selections.size() > 0) {
                selections.forEach((selection) -> {
                    root.remove(selection);
                });
                this.selections = new ArrayList<>(); 
            }
        });

        return inputItemSlot;
    }
}
