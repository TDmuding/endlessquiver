package com.example.endlessquiver.client;

import com.example.endlessquiver.EndlessQuiverMod;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** 只在客户端执行：把箭袋菜单绑定到自己的界面。 */
@Mod.EventBusSubscriber(modid = EndlessQuiverMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(EndlessQuiverMod.QUIVER_MENU.get(), QuiverScreen::new));
    }
}
