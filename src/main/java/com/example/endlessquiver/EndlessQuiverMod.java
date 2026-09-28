package com.example.endlessquiver;

import com.example.endlessquiver.inventory.QuiverMenu;
import com.example.endlessquiver.item.QuiverItem;
import com.mojang.logging.LogUtils;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * 无限箭袋：Curios 饰品模组。
 * <p>
 * 物品 {@code endlessquiver:quiver} 有两个用途：
 * <ul>
 *     <li>装备到 Curios 的 back（背后）槽 —— 为弓/弩无限供应里面存放的那一种箭矢（不消耗）；</li>
 *     <li>手持右键 —— 打开它自己的 1 格储物界面（不需要潜行）。</li>
 * </ul>
 */
@Mod(EndlessQuiverMod.MODID)
public class EndlessQuiverMod {

    public static final String MODID = "endlessquiver";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    public static final RegistryObject<Item> QUIVER =
            ITEMS.register("quiver", () -> new QuiverItem(new Item.Properties()));

    public static final RegistryObject<MenuType<QuiverMenu>> QUIVER_MENU =
            MENUS.register("quiver", () -> IForgeMenuType.create(QuiverMenu::new));

    public EndlessQuiverMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        modEventBus.addListener(this::addCreative);
        LOGGER.info("[{}] 无限箭袋已加载（需要 Curios 作为前置）", MODID);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(QUIVER);
        }
    }
}
