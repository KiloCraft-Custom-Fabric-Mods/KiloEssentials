package org.kilocraft.essentials.api.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;
import org.kilocraft.essentials.KiloCommands;
import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.chat.TextFormat;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.commands.LiteralCommandModified;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class TabCompletions {

    private static PlayerManager playerManager = KiloServer.getServer().getPlayerManager();

    public static CompletableFuture<Suggestions> noSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return new CompletableFuture<>();
    }

    public static CompletableFuture<Suggestions> allPlayers(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(playerManager.getPlayerList().stream()
                .map(PlayerEntity::getEntityName), builder);
    }

    public static CompletableFuture<Suggestions> allPlayersExceptSource(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(playerManager.getPlayerList().stream().filter((p) -> {
            try {
                return !p.equals(context.getSource().getPlayer());
            } catch (CommandSyntaxException ignored) {}
            return false;
        }).map(PlayerEntity::getEntityName), builder);
    }

    public static CompletableFuture<Suggestions> allPlayerNicks(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        List<String> nicks = new ArrayList<>();
        for (OnlineUser user : KiloServer.getServer().getUserManager().getOnlineUsersAsList()) {
            nicks.add(TextFormat.removeAlternateColorCodes('&', user.getDisplayName()));
            nicks.add(user.getUsername());
        }

        return CommandSource.suggestMatching(nicks, builder);
    }

    public static CompletableFuture<Suggestions> dimensions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        List<String> dims = new ArrayList<>();
        Registry.DIMENSION_TYPE.forEach(dimType -> dims.add(Objects.requireNonNull(DimensionType.getId(dimType)).getPath()));
        return CommandSource.suggestMatching(dims.stream(), builder);
    }

    public static CompletableFuture<Suggestions> usableCommands(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return KiloCommands.toastSuggestions(context, builder);
    }

    public static CompletableFuture<Suggestions> commands(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(
                KiloCommands.getDispatcher().getRoot().getChildren().stream().filter((child) -> !child.getName().startsWith(LiteralCommandModified.getNMSCommandPrefix())).map(CommandNode::getName),
                builder
        );
    }

    public static CompletableFuture<Suggestions> textformatChars(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return suggestAtCursor(Arrays.stream(TextFormat.getList()).filter((it) -> context.getInput().charAt(getPendingCursor(context)) == '&'), context);
    }


    public static CompletableFuture<Suggestions> allNonOperators(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(playerManager.getPlayerList().stream().filter((p) -> !playerManager.isOperator(p.getGameProfile())).map(PlayerEntity::getEntityName), builder);
    }

    public static CompletableFuture<Suggestions> allOperators(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(playerManager.getOpNames(), builder);
    }

    public static SuggestionProvider<ServerCommandSource> getDateArguments = ((context, builder) ->
            CommandSource.suggestMatching(new String[]{"year", "month", "day", "minute", "second"}, builder)
    );

    public static CompletableFuture<Suggestions> timeSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        String[] values = {"s", "m", "h", "d"};
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);
        Integer in = Integer.parseInt(builder.getInput());

        if (builder.getInput().equals(String.valueOf(in))) {
            return CommandSource.suggestMatching(values, builder);
        }

        return null;
    }

    public static CompletableFuture<Suggestions> boolStyle(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(new String[]{"on", "off"}, builder);
    }

    public static CompletableFuture<Suggestions> stateSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(new String[]{"on", "off", "toggle"}, builder);
    }

    public static CompletableFuture<Suggestions> suggestAtArg(int arg, String[] strings, CommandContext<ServerCommandSource> context) {
        return suggestAt(getCursorAtArg(arg, context), strings, context);
    }

    public static CompletableFuture<Suggestions> suggestAtCursor(Stream<String> stream, CommandContext<ServerCommandSource> context) {
        return suggestAt(context.getInput().length(), stream, context);
    }

    public static CompletableFuture<Suggestions> suggestAtCursor(String string, CommandContext<ServerCommandSource> context) {
        return suggestAt(context.getInput().length(), new String[]{string}, context);
    }

    public static CompletableFuture<Suggestions> suggestAtCursor(String[] strings, CommandContext<ServerCommandSource> context) {
        return suggestAt(context.getInput().length(), strings, context);
    }

    public static CompletableFuture<Suggestions> suggestAtCursor(Iterable<String> iterable, CommandContext<ServerCommandSource> context) {
        return suggestAt(context.getInput().length(), iterable, context);
    }

    public static CompletableFuture<Suggestions> suggestAt(int position, Stream<String> stream, CommandContext<ServerCommandSource> context) {
        return CommandSource.suggestMatching(stream, new SuggestionsBuilder(context.getInput(), position));
    }

    public static CompletableFuture<Suggestions> suggestAt(int position, String[] strings, CommandContext<ServerCommandSource> context) {
        return CommandSource.suggestMatching(strings, new SuggestionsBuilder(context.getInput(), position));
    }

    public static CompletableFuture<Suggestions> suggestAt(int position, Iterable<String> iterable, CommandContext<ServerCommandSource> context) {
        return CommandSource.suggestMatching(iterable, new SuggestionsBuilder(context.getInput(), position));
    }

    private static String getInput(CommandContext<ServerCommandSource> context) {
        return context.getInput().replace("/" + context.getNodes().get(0) + " ", "");
    }

    public static int getPendingCursor(CommandContext<ServerCommandSource> context) {
        return (context.getInput().length() - 1);
    }

    private static int getCursor(CommandContext<ServerCommandSource> context) {
        return context.getInput().length();
    }

    private static int getCursorAtArg(int pos, CommandContext<ServerCommandSource> context) {
        return getInput(context).split(" ").length;
    }

}
