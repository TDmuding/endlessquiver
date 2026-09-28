package com.example.endlessquiver.item;

import com.example.endlessquiver.inventory.QuiverMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Optional;

/**
 * 箭袋物品本体。
 * <p>
 * 记录的那一种箭矢写在物品自己的 NBT 上（键 {@link #TAG_ARROW}），所以箭袋被丢在地上、
 * 放进箱子或者在玩家之间交易时，记录都跟着走。记录的只是「哪一种箭」，箭矢本身不会被收走。
 * <p>
 * 实现 {@link ICurioItem} 即可让物品自动获得 Curios 的 {@code ICurio} capability；
 * 但“能不能放进背饰栏”是由物品标签 {@code data/curios/tags/items/back.json} 决定的。
 */
public class QuiverItem extends Item implements ICurioItem {

    /** NBT 键：记录的那一种箭矢（一个 ItemStack 的复合标签，数量恒为 1） */
    public static final String TAG_ARROW = "StoredArrow";

    public QuiverItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // ------------------------------------------------------------------
    // NBT 读写
    // ------------------------------------------------------------------

    /** 读出箭袋记录的那一种箭；没有记录就是空栈。 */
    public static ItemStack getStoredArrow(ItemStack quiver) {
        if (quiver.isEmpty()) {
            return ItemStack.EMPTY;
        }
        CompoundTag tag = quiver.getTag();
        if (tag == null || !tag.contains(TAG_ARROW, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        CompoundTag arrowTag = tag.getCompound(TAG_ARROW);
        if (arrowTag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack arrow = ItemStack.of(arrowTag);
        return arrow == null ? ItemStack.EMPTY : arrow;
    }

    /** 记录一种箭矢；传空栈表示清除记录。 */
    public static void setStoredArrow(ItemStack quiver, ItemStack arrow) {
        if (quiver.isEmpty()) {
            return;
        }
        if (arrow.isEmpty()) {
            CompoundTag tag = quiver.getTag();
            if (tag != null) {
                tag.remove(TAG_ARROW);
                if (tag.isEmpty()) {
                    quiver.setTag(null);
                }
            }
            return;
        }
        CompoundTag saved = new CompoundTag();
        arrow.copy().save(saved);
        quiver.getOrCreateTag().put(TAG_ARROW, saved);
    }

    // ------------------------------------------------------------------
    // 查询：玩家身上装备着的箭袋
    // ------------------------------------------------------------------

    /** 找到玩家（或任意生物）装备在饰品栏里的第一个箭袋。 */
    public static ItemStack findEquippedQuiver(LivingEntity entity) {
        Optional<ICuriosItemHandler> handler = CuriosApi.getCuriosInventory(entity).resolve();
        if (handler.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return handler.get()
                .findFirstCurio(stack -> stack.getItem() instanceof QuiverItem)
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }

    /** 取装备着的箭袋记录的箭矢；没有箭袋或箭袋没记录都返回空栈。 */
    public static ItemStack getEquippedArrow(LivingEntity entity) {
        return getStoredArrow(findEquippedQuiver(entity));
    }

    // ------------------------------------------------------------------
    // 打开储物界面
    // ------------------------------------------------------------------

    /** 在服务端为玩家打开箭袋的 1 格储物界面。 */
    public static boolean openQuiver(ServerPlayer player, ItemStack quiver) {
        if (quiver.isEmpty() || !(quiver.getItem() instanceof QuiverItem)) {
            return false;
        }
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider(
                        (containerId, inventory, p) -> new QuiverMenu(containerId, inventory, quiver),
                        Component.translatable("container.endlessquiver.quiver")),
                buffer -> buffer.writeItem(quiver));
        return true;
    }

    // ------------------------------------------------------------------
    // 提示文字
    // ------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ItemStack arrow = getStoredArrow(stack);
        if (arrow.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.endlessquiver.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.endlessquiver.recorded",
                    arrow.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.endlessquiver.hint").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.endlessquiver.open").withStyle(ChatFormatting.DARK_GRAY));
    }
}
