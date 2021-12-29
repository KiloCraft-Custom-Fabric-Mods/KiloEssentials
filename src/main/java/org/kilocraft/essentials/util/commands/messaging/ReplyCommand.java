package org.kilocraft.essentials.util.commands.messaging;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.user.NeverJoinedUser;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.api.util.EntityIdentifiable;
import org.kilocraft.essentials.chat.ServerChat;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public class ReplyCommand extends EssentialCommand {
    public ReplyCommand() {
        super("reply", new String[]{"r", "respond"});
        this.withUsage("command.message.reply.usage", "message");
    }

    @Override
    public void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        final RequiredArgumentBuilder<CommandSourceStack, String> messageArgument = this.argument("message", greedyString())
                .executes(this::execute);

        this.commandNode.addChild(messageArgument.build());
    }

    private int execute(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        OnlineUser user = KiloEssentials.getUserManager().getOnline(ctx.getSource());
        String message = getString(ctx, "message");
        EntityIdentifiable lastReceptionist = user.getLastMessageReceptionist();

        if (lastReceptionist == null || lastReceptionist.getId() == null) {
            user.sendLangMessage("chat.private_chat.no_message");
            return FAILED;
        }

        OnlineUser target = KiloEssentials.getUserManager().getOnline(lastReceptionist.getId());

        if (target == null || target instanceof NeverJoinedUser) {
            throw EntityArgument.NO_PLAYERS_FOUND.create();
        }

        return ServerChat.sendDirectMessage(ctx.getSource(), target, message);
    }
}
