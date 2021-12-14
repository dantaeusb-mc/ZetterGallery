package com.dantaeusb.zettergallery.core;

import com.dantaeusb.zettergallery.ZetterGallery;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems
{
    private static final List<Item> ITEMS = new ArrayList<>();

    public static final BlockItem TEST = registerBlockItem("test", ModBlocks.TEST);

    private static BlockItem registerBlockItem(String name, Block block)
    {
        Item.Properties itemProps = new Item.Properties().tab(CreativeModeTab.TAB_TOOLS);
        BlockItem blockItem = new BlockItem(block, itemProps);

        return (BlockItem) register(name, blockItem);
    }

    private static Item register(String name, Item item)
    {
        item.setRegistryName(ZetterGallery.MOD_ID, name);
        ITEMS.add(item);

        return item;
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void registerItems(final RegistryEvent.Register<Item> event)
    {
        ITEMS.forEach(item -> event.getRegistry().register(item));
        ITEMS.clear();
    }
}