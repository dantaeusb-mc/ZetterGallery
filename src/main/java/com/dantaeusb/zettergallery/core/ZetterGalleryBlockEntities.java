package com.dantaeusb.zettergallery.core;

import com.dantaeusb.zettergallery.ZetterGallery;
import com.dantaeusb.zettergallery.tileentity.PaintingMerchantTileEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZetterGalleryBlockEntities
{
    public static BlockEntityType<PaintingMerchantTileEntity> TRADER_TILE_ENTITY;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onTileEntityTypeRegistration(final RegistryEvent.Register<BlockEntityType<?>> event) {
        // @todo: change block
        TRADER_TILE_ENTITY =
                BlockEntityType.Builder.of(PaintingMerchantTileEntity::new, ModBlocks.TEST).build(null);
        TRADER_TILE_ENTITY.setRegistryName(ZetterGallery.MOD_ID, "trader_tile_entity");
        event.getRegistry().register(TRADER_TILE_ENTITY);
    }
}