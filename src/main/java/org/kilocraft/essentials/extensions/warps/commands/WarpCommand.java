package org.kilocraft.essentials.extensions.warps.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.registry.Registry;
import org.kilocraft.essentials.CommandPermission;
import org.kilocraft.essentials.KiloCommands;
import org.kilocraft.essentials.chat.ChatMessage;
import org.kilocraft.essentials.chat.KiloChat;
import org.kilocraft.essentials.commands.teleport.BackCommand;
import org.kilocraft.essentials.config.KiloConfig;
import org.kilocraft.essentials.extensions.warps.Warp;
import org.kilocraft.essentials.extensions.warps.WarpManager;

import java.text.DecimalFormat;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WarpCommand {
    private static final SimpleCommandExceptionType WARP_NOT_FOUND_EXCEPTION = new SimpleCommandExceptionType(new LiteralText("Can not find the warp specified!"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = literal("warp")
                .requires(src -> KiloCommands.hasPermission(src, CommandPermission.WARP));
        RequiredArgumentBuilder<ServerCommandSource, String> warpArg = argument("warp", string());
        LiteralArgumentBuilder<ServerCommandSource> listLiteral = literal("warps");

        LiteralArgumentBuilder<ServerCommandSource> spawnLiteral = literal("spawn")
                .requires(src -> KiloCommands.hasPermission(src, CommandPermission.WARP));

        LiteralArgumentBuilder<ServerCommandSource> setSpawnLiteral = literal("setspawn")
                .requires(src -> KiloCommands.hasPermission(src, CommandPermission.SETWARP));

        warpArg.executes(c -> executeTeleport(c.getSource(), getString(c, "warp")));
        listLiteral.executes(c -> executeList(c.getSource()));
        spawnLiteral.executes(c -> executeTeleport(c.getSource(), "spawn"));
        setSpawnLiteral.executes(c -> executeAdd(c.getSource(), "spawn"));

        warpArg.suggests(WarpManager::suggestions);

        builder.then(warpArg);
        registerAdmin(builder, dispatcher);
        dispatcher.register(spawnLiteral);
        dispatcher.register(setSpawnLiteral);
        dispatcher.register(listLiteral);
        dispatcher.register(builder);
    }

    private static void registerAdmin(LiteralArgumentBuilder<ServerCommandSource> builder, CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> aliasAdd = literal("setwarp");
        LiteralArgumentBuilder<ServerCommandSource> aliasRemove = literal("delwarp");
        RequiredArgumentBuilder<ServerCommandSource, String> removeArg = argument("warp", string());
        RequiredArgumentBuilder<ServerCommandSource, String> addArg = argument("name", string());

        aliasAdd.requires(s -> KiloCommands.hasPermission(s, CommandPermission.SETWARP));
        aliasRemove.requires(s -> KiloCommands.hasPermission(s, CommandPermission.DELWARP));

        removeArg.executes(c -> executeRemove(c.getSource(), getString(c, "warp")));
        addArg.executes(c -> executeAdd(c.getSource(), getString(c, "name")));

        removeArg.suggests(WarpManager::suggestions);

        aliasAdd.then(addArg);
        aliasRemove.then(removeArg);

        dispatcher.register(aliasAdd);
        dispatcher.register(aliasRemove);
    }

    private static int executeTeleport(ServerCommandSource source, String name) throws CommandSyntaxException {
        if (WarpManager.getWarpsByName().contains(name)) {
            Warp warp = WarpManager.getWarp(name);

            ServerWorld world = source.getMinecraftServer().getWorld(Registry.DIMENSION.get(warp.getDimId()));

            KiloChat.sendMessageTo(source, new ChatMessage(
                    KiloConfig.getProvider().getMessages().get(true, "commands.serverWideWarps.teleportTo")
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
        if (WarpManager.getWarps().isEmpty()) {
            KiloChat.sendMessageTo(source, new ChatMessage("&cNo warps!", true));
            return 1;
        }

        StringBuilder warps = new StringBuilder();
        warps.append("&6Warps&8:");

        for (Warp warp : WarpManager.getWarps()) {
            warps.append("&7,&f ").append(warp.getName());
        }

        KiloChat.sendMessageTo(source, new ChatMessage(
                warps.toString().replaceFirst("&7,", ""), true));

        return 1;
    }

    private static int executeAdd(ServerCommandSource source, String name) throws CommandSyntaxException {
        DecimalFormat df = new DecimalFormat("#.##");
        WarpManager.addWarp(
                new Warp(
                        name,
                        Double.parseDouble(df.format(source.getPlayer().getPos().x)),
                        Double.parseDouble(df.format(source.getPlayer().getPos().y)),
                        Double.parseDouble(df.format(source.getPlayer().getPos().z)),
                        Float.parseFloat(df.format(source.getPlayer().yaw)),
                        Float.parseFloat(df.format(source.getPlayer().pitch)),
                        Registry.DIMENSION.getId(source.getWorld().getDimension().getType())));

        KiloChat.sendLangMessageTo(source, "command.warp.set", name);

        return 1;
    }

    private static int executeRemove(ServerCommandSource source, String warp) throws CommandSyntaxException {
        if (WarpManager.getWarpsByName().contains(warp)) {
            WarpManager.removeWarp(warp);
            KiloChat.sendLangMessageTo(source, "command.warp.remove", warp);
        }
        else
            throw WARP_NOT_FOUND_EXCEPTION.create();
        return 1;
    }

}
