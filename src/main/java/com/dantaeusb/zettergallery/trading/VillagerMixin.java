package com.dantaeusb.zettergallery.trading;

import com.dantaeusb.zettergallery.core.ZetterGalleryVillagers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.npc.Villager.class)
public abstract class VillagerMixin extends AbstractVillager {
    protected VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow public abstract VillagerData getVillagerData();

    @Inject(method = "startTrading(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), cancellable = true)
    private void startTrading(Player player, CallbackInfo ci) {
        if (this.getVillagerData().getProfession().equals(ZetterGalleryVillagers.PAINTING_MERCHANT)) {
            ci.cancel();

            this.openTradingScreen(player, this.getDisplayName(), this.getVillagerData().getLevel());
        }
    }
}
