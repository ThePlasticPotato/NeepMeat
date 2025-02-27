package com.neep.neepmeat.client.screen;

import com.neep.neepmeat.NeepMeat;
import com.neep.neepmeat.client.screen.util.GUIUtil;
import com.neep.neepmeat.screen_handler.WorkstationScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(value = EnvType.CLIENT)
public class WorkstationScreen extends HandledScreen<WorkstationScreenHandler>
{
    private static final Identifier TEXTURE = new Identifier(NeepMeat.NAMESPACE, "textures/gui/workstation.png");

    public WorkstationScreen(WorkstationScreenHandler handler, PlayerInventory inventory, Text title)
    {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 178;
    }

    @Override
    protected void init()
    {
        super.init();
//        this.narrow = this.width < 379;
//        this.recipeBook.initialize(this.width, this.height, this.client, this.narrow, (AbstractRecipeScreenHandler)this.handler);
//        this.x = this.recipeBook.findLeftEdge(this.width, this.backgroundWidth);
//        this.addDrawableChild(new TexturedButtonWidget(this.x + 5, this.height / 2 - 49, 20, 18, 0, 0, 19, RECIPE_BUTTON_TEXTURE, button -> {
//            this.recipeBook.toggleOpen();
//            this.x = this.recipeBook.findLeftEdge(this.width, this.backgroundWidth);
//            ((TexturedButtonWidget)button).setPos(this.x + 5, this.height / 2 - 49);
//        }));
//        this.addSelectableChild(this.recipeBook);
//        this.setInitialFocus(this.recipeBook);
        this.titleX = 29;
    }

    @Override
    public void render(DrawContext matrices, int mouseX, int mouseY, float delta)
    {
        this.renderBackground(matrices);
//        if (this.recipeBook.isOpen() && this.narrow)
//        {
//            this.drawBackground(matrices, delta, mouseX, mouseY);
//            this.recipeBook.render(matrices, mouseX, mouseY, delta);
//        }
//        else
//        {
//            this.recipeBook.render(matrices, mouseX, mouseY, delta);
            super.render(matrices, mouseX, mouseY, delta);
//            this.recipeBook.drawGhostSlots(matrices, this.x, this.y, true, delta);
//        }
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
//        this.recipeBook.drawTooltip(matrices, this.x, this.y, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext matrices, int mouseX, int mouseY)
    {
        GUIUtil.drawText(matrices, textRenderer, this.title, (float)this.playerInventoryTitleX, (float)this.titleY, 0x404040, false);
    }

    @Override
    protected void drawBackground(DrawContext matrices, float delta, int mouseX, int mouseY)
    {
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
//        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = this.x;
        int j = (this.height - this.backgroundHeight) / 2;
        matrices.drawTexture(TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }
}
