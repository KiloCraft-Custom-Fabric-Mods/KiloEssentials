package org.kilocraft.essentials.craft.threaded;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kilocraft.essentials.craft.provider.LocateStructureProvider;

public class ThreadedStructureLocator implements KiloThread, Runnable {
    private Logger logger;
    private ServerCommandSource source;
    private String name;
    public ThreadedStructureLocator(ServerCommandSource commandSource, String structureName) {
        source = commandSource;
        name = structureName;
    }

    @Override
    public String getName() {
        return "StructureLocator";
    }

    @Override
    public void run() {
        logger = LogManager.getFormatterLogger(getName());
        getLogger().info("Started thread StructureLocator by %s for structure \"%s\"", source.getName(), name);

        try {
            LocateStructureProvider.execute(source, name); // TODO WHY ARE WE RUNNING BIOME SOURCE LOGIC OFF MAIN
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

}
