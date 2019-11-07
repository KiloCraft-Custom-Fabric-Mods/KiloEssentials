package org.kilocraft.essentials.craft.threaded;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kilocraft.essentials.craft.provider.LocateBiomeProvider;

public class ThreadedBiomeLocator implements KiloThread, Runnable {
    private Logger logger;
    private ServerCommandSource source;
    private Biome biome;

    public ThreadedBiomeLocator(ServerCommandSource commandSource, Biome biomeToFind) {
        source = commandSource;
        biome = biomeToFind;
    }

    @Override
    public String getName() {
        return "BiomeLocator";
    }

    @Override
    public void run() {
        logger = LogManager.getFormatterLogger(getName());
        getLogger().info("Started thread BiomeLocator by %s for biome \"%s\"", source.getName(), LocateBiomeProvider.getBiomeName(biome));

        LocateBiomeProvider.execute(source, biome); // TODO WHY ARE WE RUNNING BIOME SOURCE LOGIC OFF MAIN
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

}
