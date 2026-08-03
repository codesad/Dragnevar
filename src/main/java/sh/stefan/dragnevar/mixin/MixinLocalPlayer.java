package sh.stefan.dragnevar.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sh.stefan.dragnevar.config.DragnevarConfig;
import sh.stefan.dragnevar.ravengard.RavengardDetector;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer {
    @Redirect(
            method = "drop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;removeFromSelected(Z)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack dragnevar$keepSelectedItem(Inventory inventory, boolean dropAll) {
        if (DragnevarConfig.INSTANCE.getValues().items.preventRavengardItemDrop
                && RavengardDetector.isOnRavengard()) {
            return inventory.getSelectedItem();
        }

        return inventory.removeFromSelected(dropAll);
    }
}
