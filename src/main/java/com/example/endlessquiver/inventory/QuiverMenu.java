package com.example.endlessquiver.inventory;

import com.example.endlessquiver.EndlessQuiverMod;
import com.example.endlessquiver.item.QuiverItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;

/**
 * 箭袋的 1 格界面：上面一格是「记录格」，下面 36 格是玩家背包。
 * <p>
 * 记录格不存放箭矢：把箭矢放上去只是「给箭袋看一眼」，箭袋记下这一种箭之后就能无限供应，
 * 放上去的箭矢会原样退回背包，玩家不会因为装填而损失箭矢。
 */
public class QuiverMenu extends AbstractContainerMenu {

    /** 上面那格记录格在菜单里的索引 */
    public static final int ARROW_SLOT = 0;
    /** 玩家背包（含快捷栏）起始索引 */
    public static final int INVENTORY_START = 1;

    private final QuiverContainer container;

    /** 网络构造器：由 {@code IForgeMenuType.create(QuiverMenu::new)} 使用。 */
    public QuiverMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, data.readItem());
    }

    /** 服务端构造器。 */
    public QuiverMenu(int containerId, Inventory playerInventory, ItemStack quiver) {
        super(EndlessQuiverMod.QUIVER_MENU.get(), containerId);
        this.container = new QuiverContainer(quiver);

        // 记录格（只有箭头类的物品能被记录）
        this.addSlot(new ArrowSlot(this.container, ARROW_SLOT, 80, 35));

        // 玩家背包 3x9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏 1x9
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.setChanged();
    }

    /**
     * 记录格的点击一律自己处理，不交给原版的物品搬运逻辑：
     * <ul>
     *   <li>手上拿着箭矢 → 只把这一种箭记进箭袋，箭矢原样退回背包；</li>
     *   <li>其他情况（空手、拿着别的物品、想取出记录）→ 什么都不做。</li>
     * </ul>
     * 记录格里显示的那一支只是预览，取不出来，因此不存在刷箭矢的可能。
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId != ARROW_SLOT) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        ItemStack carried = this.getCarried();
        if (!player.level().isClientSide && !carried.isEmpty() && carried.getItem() instanceof ArrowItem) {
            this.record(carried);
            ItemStack back = carried.copy();
            this.setCarried(ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(back);
            this.broadcastChanges();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == ARROW_SLOT) {
            // 记录格里的预览箭取不出来
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.slots.get(index).getItem();
        if (!stack.isEmpty() && stack.getItem() instanceof ArrowItem) {
            // shift + 点击背包里的箭矢 = 直接记录，物品照样留在背包里
            this.record(stack);
        }
        return ItemStack.EMPTY;
    }

    /** 把某一种箭记进箭袋（只记种类，数量永远是 1）。 */
    private void record(ItemStack arrow) {
        this.container.setItem(ARROW_SLOT, arrow);
    }

    /** 记录格：只认箭矢类物品，而且里面的预览箭不能被拿走。 */
    private static class ArrowSlot extends Slot {

        ArrowSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // 普通箭、药水箭（TippedArrow）以及绝大多数模组箭矢都继承自 ArrowItem
            return stack.getItem() instanceof ArrowItem;
        }

        /** 记录是「预览」而不是存货，取走就等于凭空刷出一支箭。 */
        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
