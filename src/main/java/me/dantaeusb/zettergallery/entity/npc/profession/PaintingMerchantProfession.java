package me.dantaeusb.zettergallery.entity.npc.profession;

import com.google.common.collect.ImmutableSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class PaintingMerchantProfession extends VillagerProfession {
    private final List<Supplier<SoundEvent>> soundEventSuppliers;

    @SafeVarargs
    public PaintingMerchantProfession(String name, PoiType pointOfInterest, ImmutableSet<Item> specificItems, ImmutableSet<Block> relatedWorldBlocksIn, Supplier<SoundEvent>... soundEventSuppliers) {
        super(name, pointOfInterest, specificItems, relatedWorldBlocksIn, null);
        this.soundEventSuppliers = Arrays.asList(soundEventSuppliers);
    }

    @Nullable
    @Override
    public SoundEvent getWorkSound() {
        int n = ThreadLocalRandom.current().nextInt(soundEventSuppliers.size());
        return soundEventSuppliers.get(n).get();
    }
}
