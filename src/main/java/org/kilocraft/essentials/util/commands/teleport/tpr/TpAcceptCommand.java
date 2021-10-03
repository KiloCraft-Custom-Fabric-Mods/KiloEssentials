package org.kilocraft.essentials.util.commands.teleport.tpr;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.command.IEssentialCommand;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.api.util.schedule.TwoPlayerScheduler;
import org.kilocraft.essentials.config.KiloConfig;
import org.kilocraft.essentials.util.LocationUtil;
import org.kilocraft.essentials.util.player.UserUtils;

public class TpAcceptCommand extends EssentialCommand {
    public TpAcceptCommand() {
        super("tpaccept", TpaCommand.PERMISSION);
    }

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        RequiredArgumentBuilder<ServerCommandSource, String> selectorArgument = this.getOnlineUserArgument("victim")
                .executes(this::accept);

        this.commandNode.addChild(selectorArgument.build());
    }

    private int accept(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        OnlineUser victim = this.getOnlineUser(ctx);
        OnlineUser sender = this.getOnlineUser(ctx, "victim");

        if (!UserUtils.TpaRequests.hasRequest(sender, victim)) {
            victim.sendLangError("command.tpa.no_requests", sender.getFormattedDisplayName());
            return FAILED;
        }

        boolean toSender = UserUtils.TpaRequests.useRequestAndGetType(sender);

        if (LocationUtil.isDestinationToClose(toSender ? victim : sender, (toSender ? sender : victim).getLocation())) {
            return IEssentialCommand.FAILED;
        }

        sender.sendLangMessage("command.tpa.accepted.announce", victim.getFormattedDisplayName());
        victim.sendLangMessage("command.tpa.accepted", sender.getFormattedDisplayName());
        new TwoPlayerScheduler(toSender ? victim : sender, toSender ? sender : victim, 5, KiloConfig.main().server().cooldown);


        return SUCCESS;
    }
}
