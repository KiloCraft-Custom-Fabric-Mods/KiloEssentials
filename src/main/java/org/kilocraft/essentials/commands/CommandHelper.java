package org.kilocraft.essentials.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.user.OnlineUser;

import java.util.concurrent.atomic.AtomicBoolean;

public class CommandHelper {
    public static boolean isConsole(ServerCommandSource source) {
        try {
            source.getEntityOrThrow();
            return false;
        } catch (CommandSyntaxException e) {
            return true;
        }
    }

    public static boolean isPlayer(ServerCommandSource source) {
        try {
            source.getPlayer();
            return true;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    public static void failIfConsole(ServerCommandSource source) throws CommandSyntaxException {
        source.getEntityOrThrow();
    }

    public static boolean isOnline(ServerPlayerEntity playerEntity) {
        AtomicBoolean bool = new AtomicBoolean(false);
        try {
            KiloServer.getServer().getPlayerManager().getPlayerList().forEach((player) -> {
                if (player == playerEntity) bool.set(true);
            });
        } catch (Exception e) {
            bool.set(false);
        }

        return bool.get();
    }

    public static boolean areTheSame(ServerPlayerEntity playerEntity1, ServerPlayerEntity playerEntity2) {
        return playerEntity1.getUuid().equals(playerEntity2.getUuid());
    }

    public static boolean areTheSame(ServerCommandSource source, ServerPlayerEntity playerEntity) {
        return source.getName().equals(playerEntity.getName().asString());
    }

    public static boolean areTheSame(ServerCommandSource source, OnlineUser user) {
        return source.getName().equals(user.getUsername());
    }

    public static boolean areTheSame(OnlineUser user1, OnlineUser user2) {
        return user1.getUsername().equals(user2.getUsername());
    }

    public static String getDisplayName(ServerCommandSource source) throws CommandSyntaxException {
        return isConsole(source) ? source.getName() : source.getPlayer().getDisplayName().asString();
    }


}
