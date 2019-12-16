package org.kilocraft.essentials.mixin.accessor;

import net.minecraft.server.ServerConfigEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerConfigEntry.class)
public interface ServerConfigEntryAccessor<T> {

    @Accessor
    @Nullable T getKey();

}