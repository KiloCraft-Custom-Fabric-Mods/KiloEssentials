package org.kilocraft.essentials.craft.commands.essentials.staffcommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.registry.Registry;
import org.kilocraft.essentials.craft.KiloCommands;
import org.kilocraft.essentials.craft.chat.ChatMessage;
import org.kilocraft.essentials.craft.chat.KiloChat;
import org.kilocraft.essentials.craft.config.KiloConifg;
import org.kilocraft.essentials.craft.worldwarps.Warp;
import org.kilocraft.essentials.craft.worldwarps.WarpManager;

import java.text.DecimalFormat;

public class WarpCommand {
    private static final SimpleCommandExceptionType WARP_NOT_FOUND_EXCEPTION = new SimpleCommandExceptionType(new LiteralText("Can not find the warp specified!"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("warp");
        RequiredArgumentBuilder<ServerCommandSource, String> warpArg = CommandManager.argument("warp", StringArgumentType.string());
        LiteralArgumentBuilder<ServerCommandSource> listLiteral = CommandManager.literal("warps");

        warpArg.executes(c -> executeTeleport(c.getSource(), StringArgumentType.getString(c, "warp")));
        listLiteral.executes(c -> executeList(c.getSource()));

        warpArg.suggests((context, builder1) -> WarpManager.suggestWarps.getSuggestions(context, builder1));

        builder.then(warpArg);
        registerAdmin(builder, dispatcher);
        dispatcher.register(listLiteral);
        dispatcher.register(builder);
    }

    private static void registerAdmin(LiteralArgumentBuilder<ServerCommandSource> builder, CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> aliasAdd = CommandManager.literal("addwarp");
        LiteralArgumentBuilder<ServerCommandSource> aliasRemove = CommandManager.literal("delwarp");
        RequiredArgumentBuilder<ServerCommandSource, String> removeArg = CommandManager.argument("warp", StringArgumentType.string());
        RequiredArgumentBuilder<ServerCommandSource, String> addArg = CommandManager.argument("name", StringArgumentType.string());
        RequiredArgumentBuilder<ServerCommandSource, Boolean> argPermission = CommandManager.argument("requiresPermission", BoolArgumentType.bool());

        aliasAdd.requires(s -> Thimble.hasPermissionOrOp(s, KiloCommands.getCommandPermission("warps.manage"), 2));
        aliasRemove.requires(s -> Thimble.hasPermissionOrOp(s, KiloCommands.getCommandPermission("warps.manage"), 2));

        removeArg.executes(c -> executeRemove(c.getSource(), StringArgumentType.getString(c, "warp")));
        argPermission.executes(c -> executeAdd(c.getSource(), StringArgumentType.getString(c, "name"), BoolArgumentType.getBool(c, "requiresPermission")));

        removeArg.suggests((context, builder1) -> WarpManager.suggestWarps.getSuggestions(context, builder1));

        addArg.then(argPermission);
        aliasAdd.then(addArg);
        aliasRemove.then(removeArg);

        dispatcher.register(aliasAdd);
        dispatcher.register(aliasRemove);
    }

    private static int executeTeleport(ServerCommandSource source, String name) throws CommandSyntaxException {
        if (WarpManager.getWarpsByName().contains(name)) {
            Warp warp = WarpManager.getWarp(name);
            ServerWorld world = source.getMinecraftServer().getWorld(Registry.DIMENSION.get(warp.getDimension() + 1));

            KiloChat.sendMessageTo(source, new ChatMessage(
                    KiloConifg.getProvider().getMessages().get(true, "commands.serverWideWarps.teleportTo")
                            .replace("%WARPNAME%", name),
                    true
            ));

            BackCommand.setLocation(source.getPlayer(), new Vector3f(source.getPosition()), source.getPlayer().dimension);
            source.getPlayer().teleport(world, warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch());
        } else
            throw WARP_NOT_FOUND_EXCEPTION.create();
        return 1;
    }

    private static int executeList(ServerCommandSource source) throws CommandSyntaxException {
        StringBuilder warps = new StringBuilder();
        warps.append("&6Warps&8:");

        for (Warp warp : WarpManager.getWarps()) {
            warps.append("&7,&f ").append(warp.getName());
        }

        KiloChat.sendMessageTo(source, new ChatMessage(
                warps.toString().replaceFirst("&7,", ""), true
        ));

        return 1;
    }

    private static int executeAdd(ServerCommandSource source, String name, boolean requiresPermission) throws CommandSyntaxException {
        DecimalFormat df = new DecimalFormat("#.##");
        WarpManager.addWarp(
                new Warp(
                        name,
                        Double.parseDouble(df.format(source.getPlayer().getPos().x)),
                        Double.parseDouble(df.format(source.getPlayer().getPos().y)),
                        Double.parseDouble(df.format(source.getPlayer().getPos().z)),
                        Float.parseFloat(df.format(source.getPlayer().yaw)),
                        Float.parseFloat(df.format(source.getPlayer().pitch)),
                        source.getWorld().getDimension().getType().getRawId(),
                        requiresPermission
                )
        );

        KiloChat.sendMessageTo(source, new ChatMessage("&eYou have &aadded&e the &6" + name + "&e warp!", true));

        return 1;
    }

    private static int executeRemove(ServerCommandSource source, String warp) throws CommandSyntaxException {
        if (WarpManager.getWarpsByName().contains(warp)) {
            WarpManager.removeWarp(warp);
            KiloChat.sendMessageTo(source, new ChatMessage("&eYou have &cremoved&e the &6" + warp + "&e warp!", true));
        }
        else
            throw WARP_NOT_FOUND_EXCEPTION.create();
        return 1;
    }

}