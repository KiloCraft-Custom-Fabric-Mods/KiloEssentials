package org.kilocraft.essentials.mixin.patch.technical;

import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.server.MinecraftServer;
import org.kilocraft.essentials.api.Brandable;
import org.kilocraft.essentials.provided.BrandedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements Brandable {

    @Override
    public String getServerModName() {
        return BrandedServer.getFinalBrandName();
    }

}
