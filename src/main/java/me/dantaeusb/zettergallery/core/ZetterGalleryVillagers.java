package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterBlocks;
import me.dantaeusb.zettergallery.ZetterGallery;
import com.google.common.collect.ImmutableSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;
import java.util.Set;

public class ZetterGalleryVillagers {
    private static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, ZetterGallery.MOD_ID);
    private static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(ForgeRegistries.PROFESSIONS, ZetterGallery.MOD_ID);

    public static final RegistryObject<PoiType> PAINTING_MERCHANT_POI = POI_TYPES.register("artist_table", () -> new PoiType("artist_table", getAllStates(ZetterBlocks.ARTIST_TABLE.get()), 1, 1));
    public static final RegistryObject<VillagerProfession> PAINTING_MERCHANT = VILLAGER_PROFESSIONS.register("painting_merchant", () -> new VillagerProfession(
            "painting_merchant",
            PAINTING_MERCHANT_POI.get(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_AMBIENT
    ));

    public static void init(IEventBus bus) {
        POI_TYPES.register(bus);
        VILLAGER_PROFESSIONS.register(bus);
    }

    private static Set<BlockState> getAllStates(Block block) {
        return ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates());
    }
}
