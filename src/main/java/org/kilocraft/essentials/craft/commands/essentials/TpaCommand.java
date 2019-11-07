package org.kilocraft.essentials.craft.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.kilocraft.essentials.api.util.CommandSuggestions;
import org.kilocraft.essentials.craft.commands.essentials.staffcommands.BackCommand;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.command.arguments.EntityArgumentType.getPlayer;
import static net.minecraft.command.arguments.EntityArgumentType.player;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * @author Indigo Amann
 */
public class TpaCommand {
    private static Map<ServerPlayerEntity, Pair<Pair<ServerPlayerEntity, Boolean>, Long>> tpMap = new HashMap<>();
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        {
            LiteralArgumentBuilder<ServerCommandSource> tpa = literal("tpa");
            tpa.requires(source -> source.hasPermissionLevel(2));
            tpa.requires(source -> {
                try {
                    return source.getPlayer() != null;
                } catch (CommandSyntaxException e) {
                    return false;
                }
            });
            LiteralArgumentBuilder<ServerCommandSource> tpahere = literal("tpahere");
            tpahere.requires(source -> source.hasPermissionLevel(2));
            tpahere.requires(source -> {
                try {
                    return source.getPlayer() != null;
                } catch (CommandSyntaxException e) {
                    return false;
                }
            });
            RequiredArgumentBuilder<ServerCommandSource, EntitySelector> playerA = argument("player", player());
            RequiredArgumentBuilder<ServerCommandSource, EntitySelector> playerB = argument("player", player());

            playerA.suggests((context, builder) -> CommandSuggestions.allPlayers.getSuggestions(context, builder));
            playerA.suggests((context, builder) -> CommandSuggestions.allPlayers.getSuggestions(context, builder));

            playerA.executes(context -> executeRequest(context, false));
            playerB.executes(context -> executeRequest(context, true));
            tpa.then(playerA);
            tpahere.then(playerB);
            dispatcher.register(tpa);
            dispatcher.register(tpahere);
        }
        {
            LiteralArgumentBuilder accept = literal("tpaccept");
            ArgumentBuilder player = argument("player", player());
            player.executes(context -> executeResponse(context, true));
            accept.then(player);
            dispatcher.register(accept);
        }
        {
            LiteralArgumentBuilder deny = literal("tpdeny");
            ArgumentBuilder player = argument("player", player());
            player.executes(context -> executeResponse(context, false));
            deny.then(player);
            dispatcher.register(deny);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> cancel = literal("tpcancel");
            cancel.requires(source -> source.hasPermissionLevel(1));
            cancel.executes(TpaCommand::cancelRequest);
            dispatcher.register(cancel);
        }
    }
    private static int executeRequest(CommandContext<ServerCommandSource> context, boolean here) throws CommandSyntaxException {
        ServerPlayerEntity target = getPlayer(context, "player");
        ServerPlayerEntity sender = context.getSource().getPlayer();
        tpMap.put(sender, new Pair<>(new Pair<>(target, here), new Date().getTime()));
        // TODO Magic value -- START
        target.sendMessage(new LiteralText("").append(sender.getDisplayName()).append(new LiteralText(" has requested " + (here ? "that you teleport to them" : "to teleport to you") + ". ").formatted(Formatting.GOLD)).append(
                new LiteralText("[ACCEPT] ").setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + sender.getGameProfile().getName()))
                .setColor(Formatting.GREEN))).append(
                new LiteralText("[DENY] ").setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + sender.getGameProfile().getName()))
                        .setColor(Formatting.RED))));
        sender.sendMessage(new LiteralText("Your request was sent. ").formatted(Formatting.GOLD).append(new LiteralText("[CANCEL]").setStyle(new Style()
        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpcancel"))
        .setColor(Formatting.RED))));
        // TODO Magic value -- END
        return 0;
    }
    private static int executeResponse(CommandContext<ServerCommandSource> context, boolean accepted) throws CommandSyntaxException {
        ServerPlayerEntity sender = getPlayer(context, "player");
        ServerPlayerEntity target = context.getSource().getPlayer();
        if (hasTPRequest(sender, target)) {
            if (accepted) {
                // TODO Magic value
                sender.sendMessage(new LiteralText("").append(new LiteralText("Your teleportation request to ").formatted(Formatting.GOLD)).append(target.getDisplayName()).append(new LiteralText(" was ").formatted(Formatting.GOLD).append(accepted ? new LiteralText("ACCEPTED").formatted(Formatting.GREEN) : new LiteralText("DENIED").formatted(Formatting.RED))));
                // TODO Magic value
                boolean toSender = useTPRequest(sender);
                ServerPlayerEntity tpTo = (toSender ? sender : target);
				BackCommand.setLocation(target, new Vector3f((float) target.getPos().x, (float) target.getPos().y, (float) target.getPos().z), target.dimension);
                (toSender ? target : sender).teleport(tpTo.getServerWorld(), tpTo.getPos().x, tpTo.getPos().y, tpTo.getPos().z, tpTo.yaw, tpTo.pitch);
            } else {
                sender.sendMessage(new LiteralText("Your teleportation requrest was denied.").formatted(Formatting.RED));  // TODO Magic value
                target.sendMessage(new LiteralText("The request was denied.").formatted(Formatting.GREEN));  // TODO Magic value
                tpMap.remove(sender);
            }
        } else {
            target.sendMessage(new LiteralText("").append(sender.getDisplayName()).append(new LiteralText(" is not requesting a telepoert.").formatted(Formatting.RED)));  // TODO Magic value
        }
        return 0;
    }
    private static int cancelRequest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity sender = context.getSource().getPlayer();
        if (hasAnyTPRequest(sender)) {
            tpMap.get(sender).getLeft().getLeft().sendMessage(new LiteralText("").append(sender.getDisplayName()).append(new LiteralText(" cancelled their teleportation request.").formatted(Formatting.GOLD)));  // TODO Magic value
            tpMap.remove(sender);
            sender.sendMessage(new LiteralText("Your teleportation request was cancelled.").formatted(Formatting.GOLD));  // TODO Magic value
        } else {
            sender.sendMessage(new LiteralText("You don't have an active teleportation request.").formatted(Formatting.RED));  // TODO Magic value
        }
        return 0;
    }
    private static boolean hasAnyTPRequest(ServerPlayerEntity source) {
        if (tpMap.containsKey(source)) {
            if (new Date().getTime() - tpMap.get(source).getRight() > 60000) {
                tpMap.remove(source);
            }
            else return true;
        }
        return false;
    }
    private static boolean hasTPRequest(ServerPlayerEntity source, ServerPlayerEntity target) {
        if (tpMap.containsKey(source)) {
            if (tpMap.get(source).getLeft().getLeft().equals(target)) {
                if (new Date().getTime() - tpMap.get(source).getRight() > 60000) {
                    tpMap.remove(source);
                }
                else return true;
            }
        }
        return false;
    }
    private static boolean useTPRequest(ServerPlayerEntity source) {
        boolean toSender = tpMap.get(source).getLeft().getRight();
        tpMap.remove(source);
        return toSender;
    }
}
