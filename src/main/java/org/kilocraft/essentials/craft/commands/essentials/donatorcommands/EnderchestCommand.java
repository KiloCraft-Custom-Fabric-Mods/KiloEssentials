package org.kilocraft.essentials.craft.commands.essentials.donatorcommands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.client.network.ClientDummyContainerProvider;
import net.minecraft.command.arguments.GameProfileArgumentType;
import net.minecraft.container.GenericContainer;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.chat.TextFormat;
import org.kilocraft.essentials.api.util.CommandHelper;
import org.kilocraft.essentials.api.util.CommandSuggestions;
import org.kilocraft.essentials.craft.KiloCommands;

import java.util.Collection;
import java.util.Iterator;

import static net.minecraft.command.arguments.GameProfileArgumentType.gameProfile;
import static net.minecraft.command.arguments.GameProfileArgumentType.getProfileArgument;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class EnderchestCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        KiloCommands.getCommandPermission("enderchest");
        KiloCommands.getCommandPermission("enderchest.others");
        LiteralArgumentBuilder<ServerCommandSource> argumentBuilder = literal("enderchest").requires(EnderchestCommand::permission)
                .executes(c -> openEnderchest(c.getSource().getPlayer(), c.getSource().getPlayer()));
        LiteralArgumentBuilder<ServerCommandSource> aliasBuilder = literal("ec").requires(EnderchestCommand::permission)
                .executes(c -> openEnderchest(c.getSource().getPlayer(), c.getSource().getPlayer()));;

        RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> selectorArg = argument("gameProfile", gameProfile())
                .requires(s -> Thimble.hasPermissionOrOp(s, KiloCommands.getCommandPermission("enderchest.others"), 2))
                .suggests((context, builder) -> {
                    return CommandSuggestions.allPlayers.getSuggestions(context, builder);
                })
                .executes(context -> execute(context.getSource(), getProfileArgument(context, "gameProfile")));

        argumentBuilder.then(selectorArg);
        aliasBuilder.then(selectorArg);

        dispatcher.register(aliasBuilder);
        dispatcher.register(argumentBuilder);
    }

    private static boolean permission(ServerCommandSource source) {
        return Thimble.hasPermissionOrOp(source, "kiloessentials.command.enderchest", 2);
    }

    private static int execute(ServerCommandSource source, Collection<GameProfile> gameProfiles) throws CommandSyntaxException {
        Iterator v = gameProfiles.iterator();

        if (gameProfiles.size() > 1) source.sendError(new LiteralText("You can only select one player but the provided selector includes more!"));  // TODO Magic value
        else if (!CommandHelper.isConsole(source)) {
            GameProfile gameProfile = (GameProfile) v.next();
            ServerPlayerEntity ecSource = KiloServer.getServer().getPlayerManager().getPlayer(gameProfile.getId());

            TextFormat.sendToSource(source, false, "&eNow looking at &6%s's&e enderchest", gameProfile.getName());  // TODO Magic value

            openEnderchest(source.getPlayer(), ecSource);
        }
        else source.sendError(new LiteralText("Only players can use this command!"));  // TODO Magic value

        return 1;
    }

    public static int openEnderchest(ServerPlayerEntity sender, ServerPlayerEntity targetEnderChest) {
        EnderChestInventory enderChestInventory = targetEnderChest.getEnderChestInventory();
        sender.openContainer(new ClientDummyContainerProvider((i, pInv, pEntity) -> {
            return GenericContainer.createGeneric9x3(i, pInv, enderChestInventory);
        }, new TranslatableText("container.enderchest")));

        return 1;
    }

}
