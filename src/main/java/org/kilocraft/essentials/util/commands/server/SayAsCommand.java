package org.kilocraft.essentials.util.commands.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.command.ArgumentSuggestions;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.user.CommandSourceUser;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.chat.ServerChat;
import org.kilocraft.essentials.util.CommandPermission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

import static com.mojang.brigadier.arguments.StringArgumentType.*;

public class SayAsCommand extends EssentialCommand {
    public SayAsCommand() {
        super("sayas", CommandPermission.SAYAS_OTHERS, 4);
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ArgumentCommandNode<CommandSourceStack, String> selectorArg = this.argument("target", word())
                .suggests(this::playerSuggestions)
                .build();

        ArgumentCommandNode<CommandSourceStack, String> channelArg = this.argument("channel", word())
                .suggests(this::channelIdSuggestions)
                .build();

        ArgumentCommandNode<CommandSourceStack, String> messageArg = this.argument("message", greedyString())
                .suggests(ArgumentSuggestions::noSuggestions)
                .executes(this::execute)
                .build();

        channelArg.addChild(messageArg);
        selectorArg.addChild(channelArg);
        this.commandNode.addChild(selectorArg);
    }

    private int execute(CommandContext<CommandSourceStack> ctx) {
        String inputTarget = getString(ctx, "target");
        String message = StringArgumentType.getString(ctx, "message");
        ServerChat.Channel channel = ServerChat.Channel.getById(StringArgumentType.getString(ctx, "channel"));
        CommandSourceUser src = this.getCommandSource(ctx);
        OnlineUser target = this.getOnlineUser(inputTarget);

        if (channel == null) {
            src.sendLangError("channel.invalid");
            return FAILED;
        }
        ServerChat.sendChatMessage(target, message, channel);
        return SUCCESS;
    }

    private CompletableFuture<Suggestions> playerSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> strings = new ArrayList<>(Arrays.asList(KiloEssentials.getMinecraftServer().getPlayerList().getPlayerNamesArray()));
        return SharedSuggestionProvider.suggest(strings, builder);
    }

    private CompletableFuture<Suggestions> channelIdSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> strings = new ArrayList<>();
        for (ServerChat.Channel value : ServerChat.Channel.values()) {
            strings.add(value.getId());
        }
        return SharedSuggestionProvider.suggest(strings, builder);
    }
}
