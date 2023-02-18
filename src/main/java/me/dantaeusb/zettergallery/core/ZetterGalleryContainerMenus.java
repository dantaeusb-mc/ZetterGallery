package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ZetterGalleryContainerMenus {
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, Zetter.MOD_ID);

    public static RegistryObject<ContainerType<PaintingMerchantMenu>> PAINTING_MERCHANT = CONTAINERS.register("painting_merchant_container", () -> IForgeContainerType.create(PaintingMerchantMenu::createMenuClientSide));

    public static void init(IEventBus bus) {
        CONTAINERS.register(bus);
    }
}
