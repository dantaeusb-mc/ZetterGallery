package me.dantaeusb.zettergallery.core;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zettergallery.ZetterGallery;
import net.minecraft.entity.merchant.villager.VillagerTrades;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterGalleryVillagerTrades {
    public static final int BUY_OFFER_ID = 0;
    public static final int SELL_OFFER_ID = 1;

    public enum WhichTrades {
        NONE,
        PCB_BLUEPRINT,
        ALL;

        boolean shouldAddBlueprint() {
            return this != NONE;
        }
    }

    @SubscribeEvent
    public static void registerTrades(VillagerTradesEvent event) {
        Int2ObjectMap<List<VillagerTrades.ITrade>> trades = event.getTrades();

        if (event.getType() == ZetterGalleryVillagers.PAINTING_MERCHANT.get()) {
            trades.get(1).add(
                (entity, random) -> new MerchantOffer(
                        new ItemStack(Items.EMERALD, 4),
                        ItemStack.EMPTY,
                        new ItemStack(ZetterItems.PAINTING.get(), 1),
                        4,
                        2,
                        0.05F
                )
            );
            trades.get(1).add(
                (entity, random) -> new MerchantOffer(
                        new ItemStack(ZetterItems.PAINTING.get(), 1),
                        ItemStack.EMPTY,
                        new ItemStack(Items.EMERALD, 4),
                        16,
                        2,
                        0.05F
                )
            );
        }
    }
}
