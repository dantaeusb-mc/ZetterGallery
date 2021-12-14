package com.dantaeusb.zettergallery.core;

import com.dantaeusb.zetter.core.ZetterBlocks;
import com.dantaeusb.zettergallery.ZetterGallery;
import com.google.common.collect.ImmutableSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;

public class ZetterGalleryVillagers {
    public static PoiType PAINTING_MERCHANT_POI;
    public static VillagerProfession PAINTING_MERCHANT;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onVillagerPoiTypeRegister(final RegistryEvent.Register<PoiType> event) {
        PAINTING_MERCHANT_POI = new PoiType("painting_merchant", getAllStates(ZetterBlocks.ARTIST_TABLE), 1, 1);
        event.getRegistry().register(PAINTING_MERCHANT_POI);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onVillagerProfessionRegister(final RegistryEvent.Register<VillagerProfession> event) {
        PAINTING_MERCHANT = new VillagerProfession(ZetterGallery.MOD_ID + ":painting_merchant", PAINTING_MERCHANT_POI, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_AMBIENT)
        event.getRegistry().register(PAINTING_MERCHANT);
    }

    private static Set<BlockState> getAllStates(Block block) {
        return ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates());
    }
}
