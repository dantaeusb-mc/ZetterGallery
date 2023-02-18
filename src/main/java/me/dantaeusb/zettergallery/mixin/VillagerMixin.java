package me.dantaeusb.zettergallery.mixin;

import me.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import me.dantaeusb.zettergallery.core.ZetterGalleryVillagers;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.packet.SMerchantInfoPacket;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @todo: [LOW] this mixin worth Forge PR for a bus event
 */
@Mixin(net.minecraft.entity.merchant.villager.VillagerEntity.class)
public abstract class VillagerMixin extends AbstractVillagerEntity {
    protected VillagerMixin(EntityType<? extends AbstractVillagerEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow public abstract VillagerData getVillagerData();

    @Shadow private void updateSpecialPrices(PlayerEntity player) {};

    //(Lnet/minecraft/entity/player/PlayerEntity;)V
    @Inject(method = "startTrading", at = @At("HEAD"), cancellable = true)
    private void startTrading(PlayerEntity player, CallbackInfo ci) {
        if (this.getVillagerData().getProfession().equals(ZetterGalleryVillagers.PAINTING_MERCHANT.get())) {
            ci.cancel();

            this.updateSpecialPrices(player);
            this.setTradingPlayer(player);
            player.openMenu(new SimpleNamedContainerProvider(
                (windowID, playerInv, usingPlayer) -> {
                    PaintingMerchantMenu menu = PaintingMerchantMenu.createMenuServerSide(windowID, playerInv, this);
                    menu.setMerchantId(this.getUUID());
                    menu.setMerchantLevel(this.getVillagerData().getLevel());

                    // @todo: use Forge hooks somehow? ; container not initialized when message received
                    SMerchantInfoPacket infoPacket = new SMerchantInfoPacket(this.getUUID(), this.getVillagerData().getLevel());
                    ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), infoPacket);

                    return menu;
                },
                this.getDisplayName())
            );
        }
    }
}
