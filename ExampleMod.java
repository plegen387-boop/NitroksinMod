package com.example.examplemod;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(ExampleMod.MODID)
public class ExampleMod {

    public static final String MODID = "examplemod";

    private static final Logger LOGGER = LogUtils.getLogger();

    /*
     * Minecraft item kayıt sistemi
     */
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    /*
     * Nitroksin item'i
     */
    public static final RegistryObject<Item> NITROKSIN =
            ITEMS.register(
                    "nitroksin",
                    () -> new NitroksinItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public ExampleMod() {

        IEventBus modEventBus =
                FMLJavaModLoadingContext.get().getModEventBus();

        // Item'ları Forge'a kaydet
        ITEMS.register(modEventBus);

        LOGGER.info("Nitroksin modu başlatıldı!");
    }
}