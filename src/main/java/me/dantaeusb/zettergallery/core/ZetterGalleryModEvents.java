package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zetter.event.CanvasRegisterEvent;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ZetterGalleryModEvents {
    @SubscribeEvent
    public static void onPlayerDisconnected(CanvasRegisterEvent event) {
        event.getCanvasCode();

        if (Minecraft.getInstance().player.containerMenu == null) {
            return;
        }

        if (!(Minecraft.getInstance().player.containerMenu instanceof PaintingMerchantMenu)) {
            return;
        }

        PaintingMerchantMenu menu = (PaintingMerchantMenu) Minecraft.getInstance().player.containerMenu;

        if (menu.getCurrentOffer() == null) {
            return;
        }

        if (menu.getCurrentOffer().getCanvasCode().equals(event.getCanvasCode())) {
            menu.getContainer().setChanged();
        }
    }
}
