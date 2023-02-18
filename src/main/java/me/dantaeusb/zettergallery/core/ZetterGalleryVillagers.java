package me.dantaeusb.zettergallery.core;

import com.google.common.collect.ImmutableSet;
import me.dantaeusb.zetter.core.ZetterBlocks;
import me.dantaeusb.zettergallery.ZetterGallery;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.util.SoundEvents;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.Set;

public class ZetterGalleryVillagers {
    private static final DeferredRegister<PointOfInterestType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, ZetterGallery.MOD_ID);
    private static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(ForgeRegistries.PROFESSIONS, ZetterGallery.MOD_ID);

    public static final RegistryObject<PointOfInterestType> PAINTING_MERCHANT_POI = POI_TYPES.register("artist_table", () -> new PointOfInterestType("artist_table", getAllStates(ZetterBlocks.ARTIST_TABLE.get()), 1, 1));
    public static final RegistryObject<VillagerProfession> PAINTING_MERCHANT = VILLAGER_PROFESSIONS.register("painting_merchant", () -> {
        return new VillagerProfession(
            "painting_merchant",
            PAINTING_MERCHANT_POI.get(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_AMBIENT
        );
    });

    public static void init(IEventBus bus) {
        POI_TYPES.register(bus);
        VILLAGER_PROFESSIONS.register(bus);
    }

    private static Set<BlockState> getAllStates(Block block) {
        return ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates());
    }
}
