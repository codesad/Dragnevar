package sh.stefan.dragnevar.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.stefan.dragnevar.feature.ItemHighlighter;
import sh.stefan.dragnevar.feature.LootReplacementFeature;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen extends Screen {
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    protected MixinAbstractContainerScreen(Component title) {
        super(title);
    }

    @Inject(method = "extractSlots", at = @At("TAIL"))
    private void dragnevar$extractSlotConnections(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        LootReplacementFeature.renderContainerBackground(
                this.menu,
                graphics,
                this.hoveredSlot,
                mouseX - this.leftPos,
                mouseY - this.topPos
        );
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void dragnevar$extractSlot(
            GuiGraphicsExtractor graphics,
            Slot slot,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        ItemHighlighter.renderSlot(this.menu, graphics, slot);
    }
}
