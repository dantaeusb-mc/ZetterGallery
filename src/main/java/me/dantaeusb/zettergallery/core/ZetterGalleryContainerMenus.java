package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZetterGalleryContainerMenus {
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, Zetter.MOD_ID);

    public static RegistryObject<MenuType<PaintingMerchantMenu>> PAINTING_MERCHANT = CONTAINERS.register("painting_merchant_container", () -> IForgeMenuType.create(PaintingMerchantMenu::createMenuClientSide));

    public static void init(IEventBus bus) {
        CONTAINERS.register(bus);
    }
}
