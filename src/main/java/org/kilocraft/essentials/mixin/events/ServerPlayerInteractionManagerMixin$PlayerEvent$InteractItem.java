package org.kilocraft.essentials.mixin.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.event.player.PlayerInteractItem;
import org.kilocraft.essentials.events.player.PlayerInteractItemEventImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin$PlayerEvent$InteractItem {

    @Shadow public ServerWorld world;

    @Inject(method = "interactItem", cancellable = true,
            at = @At(value = "HEAD", target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;interactItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"))
    private void onInteractItem(PlayerEntity playerEntity, World world, ItemStack itemStack, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PlayerInteractItem event = new PlayerInteractItemEventImpl(playerEntity, world, hand, itemStack);
        KiloServer.getServer().triggerEvent(event);

        if (event.getReturnValue() != null)
            cir.setReturnValue(event.getReturnValue());

        if (event.isCancelled())
            cir.cancel();
    }

}
