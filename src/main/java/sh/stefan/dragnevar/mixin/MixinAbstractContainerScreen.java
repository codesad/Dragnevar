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

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen extends Screen {
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    protected MixinAbstractContainerScreen(Component title) {
        super(title);
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
