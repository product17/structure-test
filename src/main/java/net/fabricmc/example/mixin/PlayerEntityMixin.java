package net.fabricmc.example.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;

import net.fabricmc.example.state.Zone;
import net.fabricmc.example.state.ZoneManager;
import net.minecraft.entity.player.PlayerEntity;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
  // @Shadow public UUID getUuid();
  @Shadow public GameProfile getGameProfile(){
    return null;
  };

  @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
  public void safeInv(CallbackInfo ci) {
    UUID playerId = PlayerEntity.getUuidFromProfile(this.getGameProfile());
    Zone zone = ZoneManager.getZoneByPlayerId(playerId);
    if (zone != null && zone.shouldKeepInventory(playerId)) {
      // cancel the dropInventory method
      ci.cancel();
      return;
    }
  }
}
