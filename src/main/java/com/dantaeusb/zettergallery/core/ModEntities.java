package com.dantaeusb.zettergallery.core;

import com.dantaeusb.zettergallery.ZetterGallery;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities
{
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onEntityTypeRegistration(final RegistryEvent.Register<EntityType<?>> event) {

    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    @OnlyIn(Dist.CLIENT)
    public static void onModelRegistryEvent(ModelRegistryEvent event) {

    }
}