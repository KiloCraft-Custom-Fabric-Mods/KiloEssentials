package org.kilocraft.essentials.listeners;

import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.event.EventHandler;
import org.kilocraft.essentials.api.event.commands.OnCommandExecutionEvent;
import org.kilocraft.essentials.chat.ServerChat;
import org.kilocraft.essentials.commands.CmdUtils;
import org.kilocraft.essentials.config.KiloConfig;

public class OnCommand implements EventHandler<OnCommandExecutionEvent> {
    @Override
    public void handle(OnCommandExecutionEvent event) {
        if (CmdUtils.isPlayer(event.getExecutor())) {
            String command = event.getCommand().startsWith("/") ? event.getCommand().substring(1) : event.getCommand();

            for (String messageCommand : new String[]{"msg", "tell", "whisper", "r", "reply", "staffmsg", "buildermsg"}) {
                if (command.replace("/", "").startsWith(messageCommand))
                    return;
            }

            ServerChat.sendCommandSpy(event.getExecutor(), command);

            if (KiloConfig.main().server().logCommands)
                KiloServer.getLogger().info("[" + event.getExecutor().getName() + "]: " + command);
        }
    }
}
