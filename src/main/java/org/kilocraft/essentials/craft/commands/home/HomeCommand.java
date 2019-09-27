package org.kilocraft.essentials.craft.commands.home;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class HomeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> homeLiteral = CommandManager.literal("home");
        LiteralArgumentBuilder<ServerCommandSource> sethomeLiteral = CommandManager.literal("sethome");
        LiteralArgumentBuilder<ServerCommandSource> delhomeLiteral = CommandManager.literal("delhome");
        LiteralArgumentBuilder<ServerCommandSource> homeAlias = CommandManager.literal("h");
        LiteralArgumentBuilder<ServerCommandSource> argumentList, argumentRemove, argumentAdd, argumentSet, argumentTeleport;
        RequiredArgumentBuilder<ServerCommandSource, String> secondArgumentAdd, secondArgumentRemove, secondArgumentSet, secondArgumentTeleport;

        argumentList = CommandManager.literal("list");
        argumentAdd = CommandManager.literal("add");
        argumentSet = CommandManager.literal("set");
        argumentRemove = CommandManager.literal("remove");
        argumentTeleport = CommandManager.literal("tp");

        secondArgumentTeleport = CommandManager.argument("home", StringArgumentType.string());
        secondArgumentAdd = CommandManager.argument("name...", StringArgumentType.string());
        secondArgumentRemove = CommandManager.argument("home", StringArgumentType.string());
        secondArgumentSet = CommandManager.argument("home", StringArgumentType.string());

        secondArgumentSet.suggests(provideSuggestion);
        secondArgumentRemove.suggests(provideSuggestion);
        secondArgumentTeleport.suggests(provideSuggestion);

        argumentTeleport.then(secondArgumentTeleport);
        argumentAdd.then(secondArgumentAdd);
        argumentRemove.then(secondArgumentRemove);
        argumentSet.then(secondArgumentSet);

        homeLiteral.then(argumentRemove);
        homeLiteral.then(argumentAdd);
        homeLiteral.then(argumentSet);
        homeLiteral.then(argumentList);
        homeLiteral.then(argumentTeleport);
        homeAlias.then(secondArgumentTeleport);
        sethomeLiteral.then(secondArgumentSet);
        delhomeLiteral.then(secondArgumentRemove);

        dispatcher.register(homeLiteral);
        dispatcher.register(homeAlias);
    }


    private static SuggestionProvider<ServerCommandSource> provideSuggestion = (context, builder) -> {

        return builder.buildFuture();
    };

}