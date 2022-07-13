package net.fabricmc.example.blockEntities;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

import io.github.cottonmc.cotton.gui.networking.NetworkSide;
import io.github.cottonmc.cotton.gui.networking.ScreenNetworking;
import net.fabricmc.example.Util;
import net.fabricmc.example.config.LoadConfig;
import net.fabricmc.example.inventories.ConfigLabyrinthTableGui;
import net.fabricmc.example.inventories.LabyrinthTableGui;
import net.fabricmc.example.inventories.LabyrinthTableInventory;
import net.fabricmc.example.inventories.data.ConfigLabyrinthTableData;
import net.fabricmc.example.inventories.data.CurrentZoneData;
import net.fabricmc.example.items.ItemLoader;
import net.fabricmc.example.state.Zone;
import net.fabricmc.example.state.ZoneManager;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LabyrinthTableBlockEntity extends BlockEntity implements LabyrinthTableInventory, ExtendedScreenHandlerFactory {
    public static final String CONFING_UPDATE_EVENT = "button_selected";
    private ArrayList<String> targetZoneList = new ArrayList<>(); // list of zones that can be selected
    private UUID zoneInstanceId;
    private final String TARGET_ZONE_LIST = "target_zone_list";
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(4, ItemStack.EMPTY);

    public LabyrinthTableBlockEntity(BlockPos pos, BlockState state) {
        super(ItemLoader.LABYRNITH_BLOCK_ENTITY, pos, state);
    }

    public UUID getZoneUuid() {
        return this.zoneInstanceId;
    }

    public Boolean isConfigMenu(PlayerEntity player) {
        return player.isSneaking() && player.isCreative();
    }

    public static void tick(World world, BlockPos pos, BlockState state, LabyrinthTableBlockEntity be) {
        if (be.zoneInstanceId != null) {
            Zone zone = ZoneManager.getZone(be.zoneInstanceId);

            if (zone == null) {
                // make sure to set zoneInstanceId to null to prevent this from looping
                be.zoneInstanceId = null;
                return;
            }

            if (zone.getPlayerCount() == 0) {
                // Once the zone hits the max ticks it will remove itself
                zone.incrementEmptyTicks();
            }
        }
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    @Override
	public boolean canPlayerUse(PlayerEntity player) {
		return pos.isWithinDistance(player.getBlockPos(), 4.5);
	}

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        String target = nbt.getString(TARGET_ZONE_LIST);
        this.targetZoneList = new ArrayList<>();
        if (target != null) {
            String[] list = target.split(","); // maybe change this to json... w/e
            for (String item : list) {
                if (!item.isEmpty()) {
                    this.targetZoneList.add(item);
                }
            }
        }
    }
 
    @Override
    public void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, items);
        if (this.targetZoneList.size() > 0) {
            nbt.putString(TARGET_ZONE_LIST, StringUtils.join(this.targetZoneList, ","));
        } else {
            nbt.remove(TARGET_ZONE_LIST);
        }

        super.writeNbt(nbt);
    }

    @Override
	public Text getDisplayName() {
		// Using the block name as the screen title
		// return new TranslatableText(getCachedState().getBlock().getTranslationKey());
        return new LiteralText("Map Atlas");
	}

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        // This method only fires on the server

        if (this.isConfigMenu(player)) {
            // Admin... setting up the configs for the block
            ConfigLabyrinthTableGui configWindow = new ConfigLabyrinthTableGui(syncId, inventory);

            ScreenNetworking.of(configWindow, NetworkSide.SERVER).receive(Util.id(CONFING_UPDATE_EVENT + syncId), buf -> {
                Gson gson = new Gson();
                String jsonData = buf.readString();
                ConfigLabyrinthTableData updatedConfig = gson.fromJson(jsonData, ConfigLabyrinthTableData.class);
                System.out.println(updatedConfig.selectedZoneList);
                this.targetZoneList = updatedConfig.selectedZoneList;
            });

            return configWindow;
        }

        LabyrinthTableGui uiWindow = new LabyrinthTableGui(syncId, inventory, this);

        ScreenNetworking screenNetworking = ScreenNetworking.of(uiWindow, NetworkSide.SERVER);

        screenNetworking.receive(uiWindow.selectEventId, buf -> {
            String buttonPress = buf.readString();
            System.out.println("Button was pressed: " + buttonPress + " : " + pos.toShortString() + " : " + pos.toString());
            
            Optional<Zone> zoneOpt = ZoneManager.generateZone(world, inventory.player, pos, buttonPress);
            if (zoneOpt.isPresent()) {
                // TODO: emit zone selection to other player in inventory
                this.zoneInstanceId = zoneOpt.get().getId();
                System.out.println("Zone exists");
            }
        });

        screenNetworking.receive(uiWindow.playerEnterEventId, buf -> {
            if (this.zoneInstanceId != null && ZoneManager.getZone(this.zoneInstanceId) != null) {
                // TODO: Check that zone exists too...
                ZoneManager.joinZone(this.zoneInstanceId, inventory.player);
            }
        });

		return uiWindow;
	}

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        Gson gson = new Gson();

        if (this.isConfigMenu(player)) {
            ConfigLabyrinthTableData configData = new ConfigLabyrinthTableData();
            configData.zoneList = new ArrayList<String>(LoadConfig.zones.keySet());
            configData.selectedZoneList = this.targetZoneList;
            String json = gson.toJson(configData);

            buf.writeString(json);
            return;
        }

        Zone existingZone = ZoneManager.getZoneAtLocation(player.getWorld().getDimension(), pos);
        CurrentZoneData zoneData = new CurrentZoneData();
        zoneData.zoneName = "test_dungeon:base_lab";
        zoneData.blockPos = pos;
        zoneData.active = existingZone != null;

        System.out.println(gson.toJson(zoneData));

        // TODO: pull the string from config
        buf.writeString(gson.toJson(zoneData));
    }
}
