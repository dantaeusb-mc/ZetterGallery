package me.dantaeusb.zettergallery.mixin;

import me.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import me.dantaeusb.zettergallery.core.ZetterGalleryVillagers;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.packet.SMerchantOffersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;

/**
 * @todo: [LOW] this mixin worth Forge PR for a bus event
 */
@Mixin(net.minecraft.world.entity.npc.Villager.class)
public abstract class VillagerMixin extends AbstractVillager {
    protected VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow public abstract VillagerData getVillagerData();

    @Shadow public abstract int getVillagerXp();

    @Shadow private void updateSpecialPrices(Player player) {};

    @Shadow public abstract boolean canRestock();

    //(Lnet/minecraft/world/entity/player/Player;)V
    @Inject(method = "startTrading", at = @At("HEAD"), cancellable = true)
    private void startTrading(Player player, CallbackInfo ci) {
        if (this.getVillagerData().getProfession().equals(ZetterGalleryVillagers.PAINTING_MERCHANT.get())) {
            ci.cancel();

            this.updateSpecialPrices(player);
            this.setTradingPlayer(player);
            OptionalInt windowId = player.openMenu(new SimpleMenuProvider(
                (windowID, playerInv, usingPlayer) -> {
                    PaintingMerchantMenu menu = PaintingMerchantMenu.createMenuServerSide(windowID, playerInv, this);
                    menu.setMerchantId(this.getUUID());
                    menu.setMerchantLevel(this.getVillagerData().getLevel());

                    return menu;
                },
                this.getDisplayName())
            );

            if (windowId.isPresent()) {
                MerchantOffers merchantOffers = this.getOffers();
                if (!merchantOffers.isEmpty()) {
                    SMerchantOffersPacket infoPacket = new SMerchantOffersPacket(
                        this.getUUID(),
                        windowId.getAsInt(),
                        merchantOffers,
                        this.getVillagerData().getLevel(),
                        this.getVillagerXp()
                    );
                    ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), infoPacket);
                }
            }
        }
    }
}
