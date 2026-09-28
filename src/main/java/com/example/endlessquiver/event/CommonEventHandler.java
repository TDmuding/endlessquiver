package com.example.endlessquiver.event;

import com.example.endlessquiver.EndlessQuiverMod;
import com.example.endlessquiver.inventory.QuiverMenu;
import com.example.endlessquiver.item.QuiverItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 模组的三个核心逻辑：
 * <ol>
 *     <li>{@link #onLivingGetProjectile} —— 把装备着的箭袋里的箭矢“喂”给任何弓/弩；</li>
 *     <li>{@link #onArrowJoinLevel} —— 由箭袋供应的箭矢落地后不可捡回；</li>
 *     <li>{@link #onRightClickItem} / {@link #onRightClickBlock} —— 手持箭袋右键打开记录界面（不需要潜行）。</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = EndlessQuiverMod.MODID)
public class CommonEventHandler {

    // ------------------------------------------------------------------
    // 1. 无限箭矢供应
    // ------------------------------------------------------------------

    /**
     * 弓与弩在射击时都会调用 {@code Player#getProjectile(ItemStack)}，Forge 在那里开了一个
     * {@code LivingGetProjectileEvent}。因为这是所有武器取弹药的唯一汇聚点，所以本方法对
     * 原版弓、原版弩以及任何走标准流程的模组弓都生效。
     * <p>
     * 关键点：返回的是一个<b>副本</b>。弓拿到弹药后会执行 {@code ItemStack#shrink(1)}，被消耗的是这个副本。
     */
    @SubscribeEvent
    public static void onLivingGetProjectile(LivingGetProjectileEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack arrow = QuiverItem.getEquippedArrow(player);
        if (arrow.isEmpty()) {
            return;
        }
        // 确认这把武器真的能用这种弹药（例如原版弩的 ARROW_OR_FIREWORK）
        ItemStack weapon = event.getProjectileWeaponItemStack();
        if (weapon.getItem() instanceof ProjectileWeaponItem weaponItem
                && !weaponItem.getAllSupportedProjectiles().test(arrow)) {
            return;
        }
        // 决策：只要箭袋里装了箭，就优先用箭袋的箭
        event.setProjectileItemStack(arrow.copy());
    }

    // ------------------------------------------------------------------
    // 2. 箭袋供应的箭矢不可捡回
    // ------------------------------------------------------------------

    /**
     * 由箭袋“供应”出去的箭矢落地后不能被捡回来，否则就变成无限刷箭了。
     * <p>
     * 判定只有一条：<b>这一箭的主人是玩家，且该玩家背着装了箭的箭袋</b>。
     * 理由是 {@link #onLivingGetProjectile} 采取“箭袋优先”——只要箭袋里装了箭，
     * 无论玩家背包里有没有别的箭，弓/弩拿到的都是箭袋里的箭的<b>副本</b>，
     * 所以这名玩家射出的每一支箭都是箭袋给的，全部都不该能捡回来。
     * <p>
     * 曾经这里还多写了一层“背包里还有别的箭就跳过”的判断，结果正好在最常见的场景里漏判：
     * 身上带着几支普通箭 + 背着装了箭的箭袋时，射出去的其实是箭袋的副本，却被当成自带箭放行，
     * 捡回来就白赚一支 → 无限刷箭。两处判定现在保持一致。
     * <p>
     * 用 {@link EntityJoinLevelEvent} 而不是在物品上打标记，是因为弓和弩把箭实体放进世界时都会走这里，
     * 原版弓、原版弩、模组弓一律适用，不需要 Mixin；而且它一定发生在
     * {@code AbstractArrow#setOwner} 之后（那个方法会把 pickup 重写成 ALLOWED / CREATIVE_ONLY，
     * 所以赋值必须晚于它，否则会被覆盖掉）。
     */
    @SubscribeEvent
    public static void onArrowJoinLevel(EntityJoinLevelEvent event) {
        // 只在服务端处理“新生成”的实体；区块加载时被带进来的实体全部跳过（不然太费）
        if (event.loadedFromDisk() || event.getLevel().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof Player player)) {
            return;
        }
        // 没带装了箭的箭袋 → 与模组无关（此时这一箭是玩家自己的，保持原版的可捡回行为）
        if (QuiverItem.getEquippedArrow(player).isEmpty()) {
            return;
        }
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    // ------------------------------------------------------------------
    // 3. 手持右键打开界面
    // ------------------------------------------------------------------

    /** 手持物品右键（对着空气 / 对着方块但事件被合并时）——双端都会触发，服务端处理。 */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (tryOpen(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** 手持物品右键方块 —— 服务端触发，顺手阻止方块被使用（否则会去开箱子之类）。 */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (tryOpen(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
        }
    }

    /** 判断是否该为这次右键打开箭袋界面。手持即可，不需要潜行。只在服务端真正打开。 */
    private static boolean tryOpen(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof QuiverItem)) {
            return false;
        }
        // 防止同一次操作被两个事件各打开一次
        if (serverPlayer.containerMenu instanceof QuiverMenu) {
            return false;
        }
        player.swing(hand, true);
        return QuiverItem.openQuiver(serverPlayer, held);
    }
}
