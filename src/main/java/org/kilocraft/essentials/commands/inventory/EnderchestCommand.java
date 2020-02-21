package org.kilocraft.essentials.commands.inventory;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.command.EntitySelector;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.kilocraft.essentials.CommandPermission;
import org.kilocraft.essentials.KiloCommands;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.command.TabCompletions;
import org.kilocraft.essentials.chat.KiloChat;
import org.kilocraft.essentials.commands.CmdUtils;
import org.kilocraft.essentials.inventory.ServerUserInventory;

import static net.minecraft.command.arguments.EntityArgumentType.getPlayer;
import static net.minecraft.command.arguments.EntityArgumentType.player;

public class EnderchestCommand extends EssentialCommand {
    public EnderchestCommand() {
        super("enderchest", CommandPermission.ENDERCHEST_SELF, new String[]{"ec", "ender"});
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        RequiredArgumentBuilder<ServerCommandSource, EntitySelector> selectorArgument = argument("target", player())
                .requires(src -> KiloCommands.hasPermission(src, CommandPermission.ENDERCHEST_OTHERS))
                .suggests(TabCompletions::allPlayers)
                .executes(ctx -> execute(ctx.getSource().getPlayer(), getPlayer(ctx, "target")));

        argumentBuilder.executes(ctx -> execute(ctx.getSource().getPlayer(), ctx.getSource().getPlayer()));
        commandNode.addChild(selectorArgument.build());
    }

//    private int execute(CommandContext<ServerCommandSource> ctx, String inputName) throws CommandSyntaxException {
//        OnlineUser sender = getOnlineUser(ctx.getSource().getPlayer());
//        ServerPlayerEntity target = KiloServer.getServer().getPlayer(inputName);
//        if (target != null) {
//            ServerUserInventory.openEnderchest(sender.getPlayer(), target);
//        } else {
//            essentials.getUserThenAcceptAsync(sender, inputName, (user) -> {
//                PlayerDataModifier dataModifier = new PlayerDataModifier(user.getUuid());
//                if (!dataModifier.load())
//                    return;
//                ServerUserInventory.openEnderchest(sender.getPlayer(), NBTUtils.tagToEnderchest(NBTUtils.getPlayerTag(user.getUuid())));
//            });
//        }
//
//        if (CommandHelper.areTheSame(sender.getPlayer(), target))
//            sender.sendLangMessage("general.open_container", "Ender Chest");
//        else
//            KiloChat.sendLangMessageTo(target, "general.seek_container", target.getEntityName(), "Ender Chest");
//
//        return SINGLE_SUCCESS;
//    }

    private int execute(ServerPlayerEntity sender, ServerPlayerEntity target) {
        ServerUserInventory.openEnderchest(sender, target);
        if (CmdUtils.areTheSame(sender, target))
            KiloChat.sendLangMessageTo(sender, "general.open_container", "Ender chest");
        else
            KiloChat.sendLangMessageTo(sender, "general.seek_container", target.getEntityName(), "Ender chest");

        return SINGLE_SUCCESS;
    }

}
