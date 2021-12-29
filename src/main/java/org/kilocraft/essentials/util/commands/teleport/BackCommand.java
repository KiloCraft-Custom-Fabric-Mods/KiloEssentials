package org.kilocraft.essentials.util.commands.teleport;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerPlayer;
import org.kilocraft.essentials.api.command.ArgumentSuggestions;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.api.world.location.Location;
import org.kilocraft.essentials.util.CommandPermission;
import org.kilocraft.essentials.util.commands.CommandUtils;

import static net.minecraft.commands.arguments.EntityArgument.getPlayer;
import static net.minecraft.commands.arguments.EntityArgument.player;

public class BackCommand extends EssentialCommand {
    public BackCommand() {
        super("back", CommandPermission.BACK_SELF, new String[]{"goback"});
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> selectorArgument = this.argument("target", player())
                .requires(src -> this.hasPermission(src, CommandPermission.BACK_OTHERS))
                .suggests(ArgumentSuggestions::allPlayers)
                .executes(this::executeOthers);

        this.commandNode.addChild(selectorArgument.build());
        this.argumentBuilder.executes(this::executeSelf);
    }

    private int executeSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return this.sendBack(ctx, ctx.getSource().getPlayerOrException());
    }

    private int executeOthers(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return this.sendBack(ctx, getPlayer(ctx, "target"));
    }

    private int sendBack(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        OnlineUser user = this.getOnlineUser(target);

        if (user.getLastSavedLocation() == null) {
            user.sendLangMessage("command.back.no_loc");
            return FAILED;
        }

        Location loc = user.getLastSavedLocation();
        user.saveLocation();
        user.teleport(loc, true);

        if (CommandUtils.areTheSame(ctx.getSource(), target))
            user.sendLangMessage("command.back.self", loc.asFormattedString());
        else
            user.sendLangMessage("command.back.others", user.getUsername(), loc.asFormattedString());

        return SUCCESS;
    }
}
