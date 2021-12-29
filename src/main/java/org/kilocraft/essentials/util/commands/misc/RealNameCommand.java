package org.kilocraft.essentials.util.commands.misc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.command.ArgumentSuggestions;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.text.ComponentText;
import org.kilocraft.essentials.api.user.CommandSourceUser;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.user.CommandSourceServerUser;
import org.kilocraft.essentials.util.CommandPermission;
import org.kilocraft.essentials.util.commands.KiloCommands;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public class RealNameCommand extends EssentialCommand {
    public RealNameCommand() {
        super("realname", CommandPermission.REALNAME);
        this.withUsage("command.realname.usage", "nickname");
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, String> nickArgument = this.argument("nickname", StringArgumentType.greedyString())
                .suggests(ArgumentSuggestions::allPlayerNicks)
                .executes(this::execute);

        this.commandNode.addChild(nickArgument.build());
    }

    private int execute(CommandContext<CommandSourceStack> ctx) {
        String input = getString(ctx, "nickname");
        CommandSourceUser source = CommandSourceServerUser.of(ctx);
        for (OnlineUser user : KiloEssentials.getUserManager().getOnlineUsersAsList(KiloCommands.hasPermission(ctx.getSource(), CommandPermission.VANISH))) {
            String nick = ComponentText.clearFormatting(user.getDisplayName());
            if (input.equalsIgnoreCase(nick) || input.equals(user.getUsername())) {
                source.sendLangMessage("command.realname.success", user.getFormattedDisplayName(), user.getUsername());
                return SUCCESS;
            }
        }

        source.sendLangMessage("command.realname.error");
        return FAILED;
    }

}
