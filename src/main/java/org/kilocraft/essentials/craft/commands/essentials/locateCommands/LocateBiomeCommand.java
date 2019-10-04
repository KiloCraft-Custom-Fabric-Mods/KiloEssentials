package org.kilocraft.essentials.craft.commands.essentials.locateCommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import org.kilocraft.essentials.craft.ThreadManager;
import org.kilocraft.essentials.craft.provider.LocateBiomeProvider;
import org.kilocraft.essentials.craft.provider.ThreadedBiomeLocator;

public class LocateBiomeCommand {
    public static void registerAsChild(LiteralArgumentBuilder<ServerCommandSource> builder) {
        LiteralArgumentBuilder<ServerCommandSource> literalBiome = CommandManager.literal("biome")
                .requires(s -> Thimble.hasPermissionChildOrOp(s, "kiloessentials.command.locate.biome", 2));

        Registry.BIOME.stream().forEach((biome) -> {
            literalBiome.then(CommandManager.literal(LocateBiomeProvider.getBiomeId(biome))
                .executes(c -> execute(c.getSource(), biome)));
        });

        builder.then(literalBiome);
    }

    private static int execute(ServerCommandSource source, Biome biome) {
        ThreadManager thread = new ThreadManager(new ThreadedBiomeLocator(source, biome));
        thread.start();

        return 0;
    }

}
