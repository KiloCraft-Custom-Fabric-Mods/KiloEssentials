package org.kilocraft.essentials.util.commands.user;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.kilocraft.essentials.api.ModConstants;
import org.kilocraft.essentials.api.command.ArgumentSuggestions;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.text.TextInput;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.api.util.Cached;
import org.kilocraft.essentials.user.ServerUserManager;
import org.kilocraft.essentials.util.CacheManager;
import org.kilocraft.essentials.util.CommandPermission;
import org.kilocraft.essentials.util.NameLookup;
import org.kilocraft.essentials.util.TimeDifferenceUtil;
import org.kilocraft.essentials.util.text.ListedText;
import org.kilocraft.essentials.util.text.Texter;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;

public class WhoWasCommand extends EssentialCommand {
    private static final String LINE_FORMAT = ModConstants.translation("command.whowas.format");
    private static final String DATE_FORMAT = ModConstants.translation("command.whowas.format.time");
    private static final String INITIAL_FORMAT = ModConstants.translation("command.whowas.format.initial");
    private static final String CACHE_ID = "command.whowas";

    public WhoWasCommand() {
        super("whowas", CommandPermission.WHOWAS_SELF, new String[]{"namehistory"});
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, String> usernameArgument = this.getUserArgument("username")
                .suggests(ArgumentSuggestions::allPlayers)
                .executes(ctx -> this.executeOthers(ctx, 1));

        RequiredArgumentBuilder<CommandSourceStack, Integer> pageArgument = this.argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> this.executeOthers(ctx, IntegerArgumentType.getInteger(ctx, "page")));

        this.argumentBuilder.executes(this::executeSelf);
        usernameArgument.then(pageArgument);
        this.commandNode.addChild(usernameArgument.build());
    }

    private int executeSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        OnlineUser user = this.getOnlineUser(ctx);
        return this.send(user, user.getUsername(), 1);
    }

    private int executeOthers(CommandContext<CommandSourceStack> ctx, int page) throws CommandSyntaxException {
        OnlineUser src = this.getOnlineUser(ctx);
        String input = this.getUserArgumentInput(ctx, "username");

        if (!src.hasPermission(CommandPermission.WHOIS_OTHERS) && !src.getUsername().equals(input)) {
            src.sendLangError("command.exception.permission");
            return FAILED;
        }

        CompletableFuture.supplyAsync(() -> {
            ServerUserManager.LoadingText loadingText = new ServerUserManager.LoadingText(src.asPlayer(), "general.querying");

            try {
                loadingText.start();
                this.send(src, input, page);
                loadingText.stop();
            } catch (Exception e) {
                src.sendLangError("api.mojang.request_failed", e.getMessage());
                loadingText.stop();
            }

            return AWAIT;
        });

        return AWAIT;
    }

    private int send(OnlineUser user, String name, int page) throws CommandSyntaxException {
        String uuid;
        try {
            uuid = NameLookup.getPlayerUUID(name);
        } catch (IOException e) {
            user.sendLangError("command.whowas.failed", name);
            return FAILED;
        }
        if (uuid == null) {
            throw GameProfileArgument.ERROR_UNKNOWN_PLAYER.create();
        }

        if (CacheManager.isPresent(getCacheId(uuid))) {
            AtomicReference<NameLookup.PreviousPlayerNameEntry[]> reference = new AtomicReference<>();
            CacheManager.ifPresent(getCacheId(uuid), (cached) -> reference.set((NameLookup.PreviousPlayerNameEntry[]) cached));
            return this.send(user, name, page, reference.get());
        }

        try {
            user.sendLangMessage("api.mojang.wait");
            NameLookup.PreviousPlayerNameEntry[] entries = NameLookup.getPlayerPreviousNames(uuid);
            Cached<NameLookup.PreviousPlayerNameEntry[]> cached = new Cached<>(getCacheId(uuid), 3, TimeUnit.HOURS, entries);
            CacheManager.cache(cached);

            return this.send(user, name, page, entries);
        } catch (IOException e) {
            user.sendError("Can not get the data! " + e.getMessage());
            return FAILED;
        }
    }

    private int send(OnlineUser src, String name, int page, NameLookup.PreviousPlayerNameEntry[] nameEntries) {
        TextInput input = new TextInput();

        if (nameEntries == null) {
            src.sendLangError("api.mojang.request_failed", "Could not get the name history of that player");
            return FAILED;
        }

        int i = 0;
        for (NameLookup.PreviousPlayerNameEntry entry : nameEntries) {
            i++;
            Component dateText;
            if (entry.isPlayersInitialName()) {
                dateText = Texter.newText(INITIAL_FORMAT);
            } else {
                dateText = Texter.newText(String.format(DATE_FORMAT, TimeDifferenceUtil.formatDateDiff(entry.getChangeTime())))
                        .withStyle((style) ->
                                style.withHoverEvent(Texter.Events.onHover("&d" + ModConstants.DATE_FORMAT.format(new Date(entry.getChangeTime()))))
                        );
            }

            input.append(Texter.newText(String.format(LINE_FORMAT, i, entry.getPlayerName())).append(" ").append(dateText));
        }

        ListedText.Page paged = ListedText.getPageFromText(ListedText.Options.builder().setPageIndex(page - 1).build(), input.getTextLines());

        paged.send(src.getCommandSource(), "Name history of " + name + " (&e" + i + "&6)", "/whowas " + name + " %page%");
        return SUCCESS;
    }

    private static String getCacheId(String username) {
        return CACHE_ID + "." + username;
    }
}
