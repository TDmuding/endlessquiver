package com.example.endlessquiver.inventory;

import com.example.endlessquiver.item.QuiverItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 箭袋的「记录格」容器。
 * <p>
 * 它保存的不是玩家的箭矢本身，而是<b>一种箭矢的记录</b>（数量恒为 1）：把箭矢拿给箭袋看一眼，
 * 箭袋就记住这一种箭并无限供应，玩家手上的箭矢不会被收走。
 * <p>
 * 容器不维护自己的物品数组，而是直接读写箭袋物品上的 NBT（{@link QuiverItem#TAG_ARROW}）：
 * {@link #setChanged()} 会把记录写回物品，所以界面里的改动会立刻持久化。
 * 也正因为如此，同一个 {@link ItemStack} 实例必须传进来（而不是它的副本），否则改动写不回去。
 */
public class QuiverContainer implements Container {

    public static final int SIZE = 1;

    private final ItemStack quiver;
    /** 当前记录的那一种箭（永远只有 1 支，只用于记录与预览） */
    private ItemStack record;

    public QuiverContainer(ItemStack quiver) {
        this.quiver = quiver;
        this.record = QuiverItem.getStoredArrow(quiver);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    /** 记录只记一种箭，所以这一格永远是 1 支。 */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.record.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.record;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        // 记录格里的预览箭不允许被取走（取走就等于凭空刷出一支箭），这里只是清掉记录。
        ItemStack result = this.record.copy();
        this.record = ItemStack.EMPTY;
        this.setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = this.record;
        this.record = ItemStack.EMPTY;
        return result;
    }

    /** 记录某一种箭：数量一律按 1 存。 */
    @Override
    public void setItem(int slot, ItemStack stack) {
        this.record = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        this.setChanged();
    }

    /** 把当前记录写回箭袋物品的 NBT。 */
    @Override
    public void setChanged() {
        QuiverItem.setStoredArrow(this.quiver, this.record);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.record = ItemStack.EMPTY;
        this.setChanged();
    }
}
