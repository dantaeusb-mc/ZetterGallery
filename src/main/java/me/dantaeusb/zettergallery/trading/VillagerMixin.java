package me.dantaeusb.zettergallery.trading;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.ZetterGalleryVillagers;
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

/**
 * @todo: this mixin worth Forge PR for a bus event
 */
@Mixin(net.minecraft.world.entity.npc.Villager.class)
public abstract class VillagerMixin extends AbstractVillager {
    protected VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow public abstract VillagerData getVillagerData();

    //(Lnet/minecraft/world/entity/player/Player;)V
    @Inject(method = "startTrading", at = @At("HEAD"), cancellable = true)
    private void startTrading(Player player, CallbackInfo ci) {
        ZetterGallery.LOG.error("Oh hi!");

        if (this.getVillagerData().getProfession().equals(ZetterGalleryVillagers.PAINTING_MERCHANT)) {
            ci.cancel();

            //this.openTradingScreen(player, this.getDisplayName(), this.getVillagerData().getLevel());
        }
    }
}
