package me.dantaeusb.zettergallery.mixin;

import me.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import me.dantaeusb.zettergallery.core.ZetterGalleryVillagers;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.packet.SGalleryAuthorizationRequestPacket;
import me.dantaeusb.zettergallery.network.packet.SGalleryMerchantInfoPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
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

    @Shadow private void updateSpecialPrices(Player player) {};

    //(Lnet/minecraft/world/entity/player/Player;)V
    @Inject(method = "startTrading", at = @At("HEAD"), cancellable = true)
    private void startTrading(Player player, CallbackInfo ci) {
        if (this.getVillagerData().getProfession().equals(ZetterGalleryVillagers.PAINTING_MERCHANT.get())) {
            ci.cancel();

            this.updateSpecialPrices(player);
            this.setTradingPlayer(player);
            player.openMenu(new SimpleMenuProvider(
                (windowID, playerInv, usingPlayer) -> {
                    PaintingMerchantMenu menu = PaintingMerchantMenu.createMenuServerSide(windowID, playerInv, this);
                    menu.setMerchantId(this.getUUID());
                    menu.setMerchantLevel(this.getVillagerData().getLevel());

                    // @todo: use Forge hooks somehow? ; container not initialized when message received
                    SGalleryMerchantInfoPacket infoPacket = new SGalleryMerchantInfoPacket(this.getUUID(), this.getVillagerData().getLevel());
                    ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), infoPacket);

                    return menu;
                },
                this.getDisplayName())
            );
        }
    }
}
