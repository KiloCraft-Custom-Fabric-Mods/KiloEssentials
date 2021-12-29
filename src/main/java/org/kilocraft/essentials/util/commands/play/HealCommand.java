package org.kilocraft.essentials.util.commands.play;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerPlayer;
import org.kilocraft.essentials.api.command.ArgumentSuggestions;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.util.CommandPermission;
import org.kilocraft.essentials.util.commands.CommandUtils;
import org.kilocraft.essentials.util.commands.KiloCommands;

import static net.minecraft.commands.arguments.EntityArgument.getPlayer;
import static net.minecraft.commands.arguments.EntityArgument.player;

public class HealCommand extends EssentialCommand {
    public HealCommand() {
        super("heal", CommandPermission.HEAL_SELF);
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> target = this.argument("target", player())
                .requires(s -> KiloCommands.hasPermission(s, CommandPermission.HEAL_OTHERS))
                .suggests(ArgumentSuggestions::allPlayers)
                .executes(context -> this.execute(context, getPlayer(context, "target")));

        this.argumentBuilder.executes(context -> this.execute(context, context.getSource().getPlayerOrException()));
        this.commandNode.addChild(target.build());
    }

    private int execute(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        OnlineUser self = this.getCommandSource(context);
        OnlineUser target = this.getOnlineUser(player);
        boolean shouldHeal = player.getHealth() == player.getMaxHealth() && player.getFoodData().getFoodLevel() == 20;
        if (CommandUtils.areTheSame(self, target)) {
            if (shouldHeal) {
                target.sendLangMessage("command.heal.exception.self");
            } else {
                target.sendLangMessage("command.heal.self");
            }
        } else {
            if (shouldHeal) {
                self.sendLangMessage("command.heal.exception.others", target.getFormattedDisplayName());
            } else {
                target.sendLangMessage("command.heal.announce", self.getDisplayName());
                self.sendLangMessage("command.heal.other", target.getDisplayName());
            }
        }

        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);

        return SUCCESS;
    }
}
