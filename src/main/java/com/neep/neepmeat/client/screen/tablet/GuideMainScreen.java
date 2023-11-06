package com.neep.neepmeat.client.screen.tablet;

import com.mojang.blaze3d.systems.RenderSystem;
import com.neep.neepmeat.NeepMeat;
import com.neep.neepmeat.guide.GuideNode;
import com.neep.neepmeat.guide.GuideReloadListener;
import com.neep.neepmeat.guide.article.Article;
import com.neep.neepmeat.screen_handler.GuideScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Environment(value= EnvType.CLIENT)
public class GuideMainScreen extends HandledScreen<GuideScreenHandler> implements GuideScreen
{
    public static final Identifier LOGO_TEXTURE = new Identifier(NeepMeat.NAMESPACE, "textures/gui/tablet/neep.png");

    protected int contentWidth = 340;
    protected int contentHeight = 280;

    protected int animationTicks;
    protected boolean start;

    protected ContentPane leftPane;
    protected ContentPane rightPane;

    // Current location within the entry tree
    protected final Deque<GuideNode> path = new LinkedList<>();

    public GuideMainScreen(GuideScreenHandler handler, PlayerInventory inventory, Text title)
    {
        super(handler, inventory.player.getInventory(), Text.translatable(""));
        this.leftPane = new GuideListPane(this);
        this.rightPane = new GuideArticlePane(this, Article.EMPTY);
        this.start = true;

        GuideNode root = GuideReloadListener.getInstance().getRootNode();
        if (root != null)
        {
            push(root);
        }
        else throw new IllegalStateException("Error loading the guide contents.");
    }

    @Override
    public void setLeftPane(ContentPane element)
    {
        remove(leftPane);
        this.leftPane = element;
        init();
    }

    @Override
    public void setRightPane(ContentPane element)
    {
        remove(rightPane);
        this.rightPane = element;
        init();
    }

    @Override
    public void push(GuideNode node)
    {
        path.push(node);
        leftPane.init();
    }

    @Override
    public GuideNode pop()
    {
        GuideNode ret = null;
        if (path.size() > 1) ret = path.pop();
        leftPane.init();
        return ret;
    }

    @Override
    public Deque<GuideNode> getPath()
    {
        return path;
    }

    @Override
    public int getAnimationTicks()
    {
        return animationTicks;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        this.renderBackground(matrices);
        this.drawForeground(matrices, mouseX, mouseY);

        // Initial animation
        matrices.push();
        float scale = MathHelper.clampedLerp(0.01f, 1f, (animationTicks + delta) / 10f);
        matrices.translate(0, height / 2f * (1 - scale), 0);
        matrices.scale(1, scale, 1);
        if (animationTicks > 10)
        {
            super.render(matrices, mouseX, mouseY, delta);
            this.drawMouseoverTooltip(matrices, mouseX, mouseY);
            this.drawLogo(matrices, delta);
        }

        int borderCol = 0xFF008800;
        int offset = 3;

        if (animationTicks != 0 && animationTicks != 2)
            GUIUtil.renderBorder(matrices, x, y, contentWidth, contentHeight, borderCol, offset);
        matrices.pop();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount)
    {
        return this.hoveredElement(mouseX, mouseY).filter(element ->
                element.mouseScrolled(mouseX, mouseY, amount)).isPresent();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (leftPane.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.shouldCloseOnEsc())
        {
            this.close();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers)
    {
        return leftPane != null && leftPane.charTyped(chr, modifiers);
    }

    @Override
    protected void init()
    {
        super.init();
        addDrawableChild(leftPane);
        addDrawableChild(rightPane);
        contentWidth = (int) (1920 * 0.2);
        contentHeight = (int) (1080 * 0.2);
        this.x = (this.width - contentWidth) / 2;
        this.y = (this.height - contentHeight) / 2;
        this.backgroundWidth = contentWidth;
        this.backgroundHeight = contentHeight;

        float ratio = 0.4f;
        int leftWidth = (int) (ratio * contentWidth);
        int rightWidth = (int) ((1 - ratio) * contentWidth);
        int rightStart = this.x + leftWidth;

        if (leftPane != null)
        {
            leftPane.setDimensions(x, y, leftWidth, contentHeight - 26);
            leftPane.init(client);
        }
        if (rightPane != null)
        {
            rightPane.setDimensions(rightStart, y, rightWidth, contentHeight);
            rightPane.init(client);
        }
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY)
    {
    }

    protected void drawLogo(MatrixStack matrices, float delta)
    {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, LOGO_TEXTURE);
        int logoHeight = 24;
        int logoWidth = 60;
        drawTexture(matrices, x, y + contentHeight - logoHeight + 1, 0, 0, logoWidth, logoHeight, logoWidth, 26);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY)
    {
//        super.drawForeground(matrices, mouseX, mouseY);

    }

    @Override
    protected void renderTooltip(MatrixStack matrices, ItemStack stack, int x, int y)
    {
        this.renderTooltip(matrices, this.getTooltipFromItem(stack), stack.getTooltipData(), x, y);
    }

    @Override
    public void renderTooltip(MatrixStack matrices, List<Text> lines, Optional<TooltipData> data2, int x, int y)
    {
        List<TooltipComponent> list = lines.stream().map(Text::asOrderedText).map(TooltipComponent::of).collect(Collectors.toList());
        data2.ifPresent(data -> list.add(1, TooltipComponent.of(data)));
        this.renderTooltipFromComponents(matrices, list, x, y);
    }

    @Override
    protected void handledScreenTick()
    {
        ++animationTicks;
        super.handledScreenTick();
        leftPane.tick();
        rightPane.tick();
    }

    private void renderTooltipFromComponents(MatrixStack matrices, List<TooltipComponent> components, int x, int y)
    {
        TooltipComponent tooltipComponent2;
        int s;
        int k;
        if (components.isEmpty()) {
            return;
        }
        int i = 0;
        int j = components.size() == 1 ? -2 : 0;
        for (TooltipComponent tooltipComponent : components) {
            k = tooltipComponent.getWidth(this.textRenderer);
            if (k > i) {
                i = k;
            }
            j += tooltipComponent.getHeight();
        }
        int l = x + 12;
        int startY = y - 12;
        k = i;
        int m = j;
        if (l + i > this.width) {
            l -= 28 + i;
        }
        if (startY + m + 6 > this.height) {
            startY = this.height - m - 6;
        }
        matrices.push();
        int n = -267386864;
        int o = 0x505000FF;
        int p = 1344798847;
        int q = 400;
        float f = this.itemRenderer.zOffset;
        this.itemRenderer.zOffset = 400.0f;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        int borderCol = -0x50FF50FF;
        // Border top
//        Screen.fillGradient(matrix4f, bufferBuilder, l - 3, startY - 4, l + k + 3, startY - 3, 400, -267386864, -267386864);
        // Border bottom
//        Screen.fillGradient(matrix4f, bufferBuilder, l - 3, startY + m + 3, l + k + 3, startY + m + 4, 400, -267386864, -267386864);
        // Background
        Screen.fillGradient(matrix4f, bufferBuilder, l - 3, startY - 3, l + k + 3, startY + m + 3, 400, -267486464, -267386864);
        // Border left
//        Screen.fillGradient(matrix4f, bufferBuilder, l - 4, startY - 3, l - 3, startY + m + 3, 400, -267386864, -267386864);
        // Border right
//        Screen.fillGradient(matrix4f, bufferBuilder, l + k + 3, startY - 3, l + k + 4, startY + m + 3, 400, -267386864, -267386864);
        // Gradient left
        Screen.fillGradient(matrix4f, bufferBuilder, l - 3, startY - 3 + 1, l - 3 + 1, startY + m + 3 - 1, 400, borderCol, borderCol);
        // Gradient right
        Screen.fillGradient(matrix4f, bufferBuilder, l + k + 2, startY - 3 + 1, l + k + 3, startY + m + 3 - 1, 400, borderCol, borderCol);
        // Gradient top
        Screen.fillGradient(matrix4f, bufferBuilder, l - 3, startY - 3, l + k + 3, startY - 3 + 1, 400, borderCol, borderCol);
        // Gradient bottom
        Screen.fillGradient(matrix4f, bufferBuilder, l - 3, startY + m + 2, l + k + 3, startY + m + 3, 400, borderCol, borderCol);

        RenderSystem.enableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferRenderer.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        matrices.translate(0.0, 0.0, 400.0);
        int r = startY;
        for (s = 0; s < components.size(); ++s) {
            tooltipComponent2 = components.get(s);
            tooltipComponent2.drawText(this.textRenderer, l, r, matrix4f, immediate);
            r += tooltipComponent2.getHeight() + (s == 0 ? 2 : 0);
        }
        immediate.draw();
        matrices.pop();
        r = startY;
        for (s = 0; s < components.size(); ++s) {
            tooltipComponent2 = components.get(s);
            tooltipComponent2.drawItems(this.textRenderer, l, r, matrices, this.itemRenderer, 400);
            r += tooltipComponent2.getHeight() + (s == 0 ? 2 : 0);
        }
        this.itemRenderer.zOffset = f;
    }
}
