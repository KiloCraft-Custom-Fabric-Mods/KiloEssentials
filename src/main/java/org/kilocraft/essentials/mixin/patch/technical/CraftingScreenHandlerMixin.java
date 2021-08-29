package org.kilocraft.essentials.mixin.patch.technical;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.CraftingScreenHandler;
import org.kilocraft.essentials.util.CommandPermission;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerMixin {

    @Inject(
            method = "canUse",
            at = @At("HEAD"),
            cancellable = true
    )
    public void allowUsage(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (Permissions.check(player, CommandPermission.WORKBENCH.getNode())) cir.setReturnValue(true);
    }

}
