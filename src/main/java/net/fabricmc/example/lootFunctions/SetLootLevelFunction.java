package net.fabricmc.example.lootFunctions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.fabricmc.example.state.Zone;
import net.fabricmc.example.state.ZoneManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.util.JsonHelper;

public class SetLootLevelFunction extends ConditionalLootFunction {
    public LootNumberProvider multiplier;

    public SetLootLevelFunction(LootCondition[] conditions, LootNumberProvider lootNumberProvider) {
        super(conditions);
        this.multiplier = lootNumberProvider;
    }

    @Override
    public LootFunctionType getType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected ItemStack process(ItemStack itemStack, LootContext context) {
        // Get the player entity that is opening the loot
        Entity player = context.get(LootContextParameters.THIS_ENTITY);
        if (player == null) {
            return itemStack;
        }

        Zone zone = ZoneManager.getZoneByPlayerId(player.getUuid());
        if (zone == null) {
            return itemStack;
        }

        System.out.println("Stack Size: " + this.multiplier.nextFloat(context));
        itemStack.setCount(itemStack.getCount() + 2);

        return itemStack;
    }
    
    public static ConditionalLootFunction.Builder<?> builder(LootNumberProvider countRange) {
        return SetLootLevelFunction.builder((LootCondition[] conditions) -> new SetLootLevelFunction((LootCondition[])conditions, countRange));
    }

    public static class Serializer
    extends ConditionalLootFunction.Serializer<SetLootLevelFunction> {
        @Override
        public void toJson(JsonObject jsonObject, SetLootLevelFunction setCountLootFunction, JsonSerializationContext jsonSerializationContext) {
            super.toJson(jsonObject, setCountLootFunction, jsonSerializationContext);
            jsonObject.add("multiplier", jsonSerializationContext.serialize(setCountLootFunction.multiplier));
        }

        @Override
        public SetLootLevelFunction fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootCondition[] lootConditions) {
            LootNumberProvider lootNumberProvider = JsonHelper.deserialize(jsonObject, "multiplier", jsonDeserializationContext, LootNumberProvider.class);
            return new SetLootLevelFunction(lootConditions, lootNumberProvider);
        }
    }
}
