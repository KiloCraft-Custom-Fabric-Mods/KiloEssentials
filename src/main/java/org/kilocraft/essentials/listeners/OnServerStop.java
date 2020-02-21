package org.kilocraft.essentials.listeners;

import org.kilocraft.essentials.KiloEssentialsImpl;
import org.kilocraft.essentials.api.event.EventHandler;
import org.kilocraft.essentials.api.event.server.lifecycle.ServerStopEvent;
import org.kilocraft.essentials.provided.LocateBiomeProvided;

public class OnServerStop implements EventHandler<ServerStopEvent> {
    @Override
    public void handle(ServerStopEvent event) {
        KiloEssentialsImpl.getInstance().onServerStop();
        LocateBiomeProvided.stopAll();

    }
}
