package net.fabricmc.example.items;

import net.fabricmc.example.state.ZoneManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ZoneOut extends Item {
    public static String name = "zone_out";

    public ZoneOut(Settings settings) {
        super(settings);
        //TODO Auto-generated constructor stub
    }
    
    @Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand user) {
        ItemStack tmpStack = player.getStackInHand(user);

        if (world.isClient) {
            return TypedActionResult.success(tmpStack, world.isClient());
        }

        ZoneManager.leaveZone(player);

        return TypedActionResult.success(tmpStack, world.isClient());
    }
}
