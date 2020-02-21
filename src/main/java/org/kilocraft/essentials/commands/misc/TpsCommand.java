package org.kilocraft.essentials.commands.misc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.chat.ChatMessage;
import org.kilocraft.essentials.chat.KiloChat;

import static org.kilocraft.essentials.util.TPSTracker.*;

public class TpsCommand extends EssentialCommand {
    public TpsCommand() {
        super("tps", new String[]{"tickspersecond"});
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        argumentBuilder.executes(this::run);
    }

    private int run(CommandContext<ServerCommandSource> ctx) {
        KiloChat.sendMessageToSource(ctx.getSource(), new ChatMessage(String.format(
                        "&6TPS&8:&%s %s&7 &8(&75m&8/&715m&8/&730m&8/&71h&8)&%s %s&8,&%s %s&8,&%s %s&8,&%s %s&r",
                        tpstoColorCode(tps1.getAverage()), tps1.getShortAverage(),
                        tpstoColorCode(tps5.getAverage()), tps5.getShortAverage(),
                        tpstoColorCode(tps15.getAverage()), tps15.getShortAverage(),
                        tpstoColorCode(tps30.getAverage()), tps30.getShortAverage(),
                        tpstoColorCode(tps60.getAverage()), tps60.getShortAverage()), true));

        return (int) Math.floor(tps1.getAverage());
    }

    private char tpstoColorCode(double tps){
        if (tps > 15)
            return 'a';
        if (tps > 10)
            return 'e';

        return 'c';
    }

}
