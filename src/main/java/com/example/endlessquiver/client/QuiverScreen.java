package com.example.endlessquiver.client;

import com.example.endlessquiver.inventory.QuiverMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 箭袋界面：一张 256x256 的贴图（内容在左上 176x166）+ 一格箭位。
 */
public class QuiverScreen extends AbstractContainerScreen<QuiverMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("endlessquiver", "textures/gui/quiver.png");

    /** 贴图文件本身的尺寸；必须显式告诉 blit，否则它按 256x256 解释 UV（见下）。 */
    private static final int TEXTURE_W = 256;
    private static final int TEXTURE_H = 256;

    public QuiverScreen(QuiverMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // 用带「贴图尺寸」的重载。若改成 blit(贴图, x, y, 0, 0, 176, 166) 这个 7 参版本，
        // 它会固定按 256x256 解释 UV，贴图就会被拉伸放大、整个界面错位。
        guiGraphics.blit(TEXTURE, x, y, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, TEXTURE_W, TEXTURE_H);
    }
}
