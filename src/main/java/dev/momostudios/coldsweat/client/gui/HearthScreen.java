package dev.momostudios.coldsweat.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.common.container.HearthContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;

public class HearthScreen extends AbstractContainerScreen<HearthContainer>
{
    private static final ResourceLocation HEARTH_GUI = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/screen/hearth_gui.png");

    public HearthScreen(HearthContainer screenContainer, Inventory inv, Component titleIn)
    {
        super(screenContainer, inv, new TranslatableComponent("container." + ColdSweat.MOD_ID + ".hearth"));
        this.leftPos = 0;
        this.topPos = 0;
        this.imageWidth = 176;
        this.imageHeight = 188;
    }

    boolean hideParticles = this.menu.te.getTileData().getBoolean("hideParticles");

    @Override
    public void init()
    {
        super.init();
        this.addRenderableWidget(new ImageButton(leftPos + 82, topPos + 68, 12, 12, 176 + (!hideParticles ? 0 : 12), 36, 12, HEARTH_GUI, (button) ->
        {
            hideParticles = !hideParticles;
            this.menu.te.getTileData().putBoolean("hideParticles", hideParticles);
            Field imageX = ObfuscationReflectionHelper.findField(ImageButton.class, "f_94224_");
            imageX.setAccessible(true);
            try
            {
                imageX.set(button, 176 + (!hideParticles ? 0 : 12));
            }
            catch (Exception ignored) {}
        })
        {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button)
            {
                if (this.active && this.visible && this.isValidClickButton(button) && this.clicked(mouseX, mouseY))
                {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, !hideParticles ? 1.5f : 1.9f, 0.75f));
                    this.onClick(mouseX, mouseY);
                    return true;
                }
                return false;
            }

            @Override
            public void renderToolTip(PoseStack poseStack, int mouseX, int mouseY)
            {
                HearthScreen.this.renderComponentTooltip(poseStack, List.of(new TranslatableComponent("cold_sweat.screen.hearth.show_particles")), mouseX, mouseY, font);
            }
        });
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTicks, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, HEARTH_GUI);
        int x = (this.width - this.getXSize()) / 2;
        int y = (this.height - this.getYSize()) / 2;
        this.blit(poseStack, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int hotFuel =  (int) (this.menu.getHotFuel()  / 27.7);
        int coldFuel = (int) (this.menu.getColdFuel() / 27.7);

        // Render hot/cold fuel gauges
        blit(poseStack, leftPos + 61,  topPos + 66 - hotFuel,  176, 36 - hotFuel,  12, hotFuel, 256, 256);
        blit(poseStack, leftPos + 103, topPos + 66 - coldFuel, 188, 36 - coldFuel, 12, coldFuel, 256, 256);
    }
}
