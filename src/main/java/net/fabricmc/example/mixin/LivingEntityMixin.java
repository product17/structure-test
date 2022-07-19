package net.fabricmc.example.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.example.state.ZoneManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends LivingEntity {
  protected LivingEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
    super(entityType, world);
    //TODO Auto-generated constructor stub
  }

  @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
  public void getXpToDrop(CallbackInfo ci) {
    UUID mobId = this.getUuid();
    ZoneManager.removeMobFromZones(mobId);
  }
}
