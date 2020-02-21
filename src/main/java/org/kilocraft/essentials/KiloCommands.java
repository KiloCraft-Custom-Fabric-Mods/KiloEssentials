package org.kilocraft.essentials;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.indicode.fabric.permissions.PermChangeBehavior;
import net.minecraft.SharedConstants;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.ModConstants;
import org.kilocraft.essentials.api.chat.LangText;
import org.kilocraft.essentials.api.chat.TextFormat;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.event.commands.OnCommandExecutionEvent;
import org.kilocraft.essentials.chat.ChatMessage;
import org.kilocraft.essentials.chat.KiloChat;
import org.kilocraft.essentials.commands.help.UsageCommand;
import org.kilocraft.essentials.commands.inventory.AnvilCommand;
import org.kilocraft.essentials.commands.inventory.EnderchestCommand;
import org.kilocraft.essentials.commands.inventory.WorkbenchCommand;
import org.kilocraft.essentials.commands.item.ItemCommand;
import org.kilocraft.essentials.commands.locate.LocateCommand;
import org.kilocraft.essentials.commands.messaging.*;
import org.kilocraft.essentials.commands.misc.*;
import org.kilocraft.essentials.commands.moderation.ClearchatCommand;
import org.kilocraft.essentials.commands.moderation.IpInfoCommand;
import org.kilocraft.essentials.commands.play.*;
import org.kilocraft.essentials.commands.server.*;
import org.kilocraft.essentials.commands.teleport.BackCommand;
import org.kilocraft.essentials.commands.teleport.RtpCommand;
import org.kilocraft.essentials.commands.teleport.TeleportCommands;
import org.kilocraft.essentials.commands.teleport.TpaCommand;
import org.kilocraft.essentials.commands.world.TimeCommand;
import org.kilocraft.essentials.config.KiloConfig;
import org.kilocraft.essentials.events.commands.OnCommandExecutionEventImpl;
import org.kilocraft.essentials.simplecommand.SimpleCommand;
import org.kilocraft.essentials.simplecommand.SimpleCommandManager;
import org.kilocraft.essentials.util.TextUtils;
import org.kilocraft.essentials.util.messages.MessageUtil;
import org.kilocraft.essentials.util.messages.nodes.ArgExceptionMessageNode;
import org.kilocraft.essentials.util.messages.nodes.CommandMessageNode;
import org.kilocraft.essentials.util.messages.nodes.ExceptionMessageNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.github.indicode.fabric.permissions.Thimble.hasPermissionOrOp;
import static io.github.indicode.fabric.permissions.Thimble.permissionWriters;
import static net.minecraft.server.command.CommandManager.literal;
import static org.kilocraft.essentials.api.KiloEssentials.getLogger;
import static org.kilocraft.essentials.api.KiloEssentials.getServer;
import static org.kilocraft.essentials.commands.LiteralCommandModified.*;

public class KiloCommands {
    private static List<String> initializedPerms = new ArrayList<>();
    private List<EssentialCommand> commands;
    private CommandDispatcher<ServerCommandSource> dispatcher;
    private SimpleCommandManager simpleCommandManager;
    private static MessageUtil messageUtil = ModConstants.getMessageUtil();
    public static String PERMISSION_PREFIX = "kiloessentials.command.";
    private static LiteralCommandNode<ServerCommandSource> rootNode;

    public KiloCommands() {
        this.dispatcher = KiloEssentialsImpl.commandDispatcher;
        this.simpleCommandManager = new SimpleCommandManager(KiloServer.getServer(), this.dispatcher);
        this.commands = new ArrayList<>();
        rootNode = literal("essentials").executes(this::sendInfo).build();
        register();
    }

    public static boolean hasPermission(ServerCommandSource src, CommandPermission perm) {
        return hasPermissionOrOp(src, perm.getNode(), 2);
    }

    public static boolean hasPermission(ServerCommandSource src, CommandPermission perm, int minOpLevel) {
        return hasPermissionOrOp(src, perm.getNode(), minOpLevel);
    }

    @Deprecated
    public static boolean hasPermission(ServerCommandSource src, String cmdPerm, int minOpLevel) {
        return hasPermissionOrOp(src, cmdPerm, minOpLevel);
    }

    private void register() {
        permissionWriters.add((map, server) -> {
            for (CommandPermission perm : CommandPermission.values()) {
                map.registerPermission(perm.getNode(), PermChangeBehavior.UPDATE_COMMAND_TREE);
            }
        });

        List<EssentialCommand> commandsList = new ArrayList<EssentialCommand>() {{
            add(new SmiteCommand());
            add(new NicknameCommand());
            add(new SayasCommand());
            add(new SudoCommand());
            add(new ItemCommand());
            add(new WorkbenchCommand());
            add(new AnvilCommand());
            add(new SigneditCommand());
            add(new HatCommand());
            add(new VersionCommand());
            add(new ReloadCommand());
            add(new ColorsCommand());
            add(new GamemodeCommand());
            add(new RtpCommand());
            add(new BroadcastCommand());
            add(new UsageCommand());
            add(new HealCommand());
            add(new FeedCommand());
            add(new TimeCommand());
            add(new FlyCommand());
            add(new InvulnerablemodeCommand());
            add(new FormatPreviewCommand());
            add(new PingCommand());
            add(new ClearchatCommand());
            add(new EnderchestCommand());
            add(new StatusCommand());
            add(new StaffmsgCommand());
            add(new BuildermsgCommand());
            add(new SocialspyCommand());
            add(new CommandspyCommand());
            add(new BackCommand());
            add(new ShootCommand());
            add(new ModsCommand());
            add(new TpsCommand());
            add(new LocateCommand());
            add(new MessageCommand());
            add(new IgnoreCommand());
            add(new IgnorelistCommand());
            add(new ReplyCommand());
            add(new RelnameCommand());
            add(new IpInfoCommand());
            add(new HelpCommand());
            add(new WhoisCommand());
            add(new PlaytimeCommand());
        }};

        this.commands.addAll(commandsList);

        for (EssentialCommand command : this.commands) {
            registerCommand(command);
        }

        dispatcher.getRoot().addChild(rootNode);

        TpaCommand.register(this.dispatcher);
        StopCommand.register(this.dispatcher);
        RestartCommand.register(this.dispatcher);
        OperatorCommand.register(this.dispatcher);
        TeleportCommands.register(this.dispatcher);
        //InventoryCommand.register(this.dispatcher);
    }

    public <C extends EssentialCommand> void register(C c) {
        registerCommand(c);
    }

    private <C extends EssentialCommand> void registerCommand(C command) {
        command.register(this.dispatcher);
        rootNode.addChild(command.getArgumentBuilder().build());
        rootNode.addChild(command.getCommandNode());

        if (command.getAlias() != null) {
            for (String alias : command.getAlias()) {
                LiteralArgumentBuilder<ServerCommandSource> argumentBuilder = literal(alias)
                        .requires(command.getRootPermissionPredicate())
                        .executes(command.getArgumentBuilder().getCommand());

                for (CommandNode<ServerCommandSource> child : command.getCommandNode().getChildren()) {
                    argumentBuilder.then(child);
                }

                dispatcher.register(argumentBuilder);
            }
        }

        dispatcher.getRoot().addChild(command.getCommandNode());
        dispatcher.register(command.getArgumentBuilder());
    }

    public static CompletableFuture<Suggestions> toastSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        List<String> suggestions = new ArrayList<>();

        for (SimpleCommand command : KiloEssentials.getInstance().getCommandHandler().simpleCommandManager.getCommands()) {
            suggestions.add(command.getLabel());
        }

        getDispatcher().getRoot().getChildren().stream().filter((child) ->
                  child instanceof LiteralCommandNode && canSourceUse(child, context.getSource()) &&
                        !isVanillaCommand(child.getName()) && shouldUse(child.getName()))
                .map(CommandNode::getName).forEach(suggestions::add);


        return CommandSource.suggestMatching(suggestions, builder);
    }

    public static int executeUsageFor(String langKey, ServerCommandSource source) {
        String fromLang = ModConstants.getLang().getProperty(langKey);
        if (fromLang != null)
            KiloChat.sendMessageToSource(source, new ChatMessage("&6Command usage:\n" + fromLang, true));
        else
            KiloChat.sendLangMessageTo(source, "general.usage.help");
        return 1;
    }

    public static int executeSmartUsage(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String command = ctx.getInput().replace("/", "");
        ParseResults<ServerCommandSource> parseResults = getDispatcher().parse(command, ctx.getSource());
        if (parseResults.getContext().getNodes().isEmpty())
            throw getException(ExceptionMessageNode.UNKNOWN_COMMAND_EXCEPTION).create();

        Map<CommandNode<ServerCommandSource>, String> commandNodeStringMap = getDispatcher().getSmartUsage(((ParsedCommandNode) Iterables.getLast(parseResults.getContext().getNodes())).getNode(), ctx.getSource());
        Iterator<String> iterator = commandNodeStringMap.values().iterator();

        KiloChat.sendLangMessageTo(ctx.getSource(), "command.usage.firstRow", command);
        KiloChat.sendLangMessageTo(ctx.getSource(), "command.usage.commandRow", command, "");

        while (iterator.hasNext()) {
            if (iterator.next().equals("/" + command)) continue;
            KiloChat.sendLangMessageTo(ctx.getSource(), "command.usage.commandRow", command, iterator.next());
        }

        return 1;
    }

    public static int executeSmartUsageFor(String command, ServerCommandSource source) throws CommandSyntaxException {
        ParseResults<ServerCommandSource> parseResults = getDispatcher().parse(command, source);
        if (parseResults.getContext().getNodes().isEmpty())
            throw getException(ExceptionMessageNode.UNKNOWN_COMMAND_EXCEPTION).create();

        Map<CommandNode<ServerCommandSource>, String> commandNodeStringMap = getDispatcher().getSmartUsage(((ParsedCommandNode)Iterables.getLast(parseResults.getContext().getNodes())).getNode(), source);
        Iterator<String> iterator = commandNodeStringMap.values().iterator();

        KiloChat.sendLangMessageTo(source, "command.usage.firstRow", parseResults.getReader().getString());
        if (parseResults.getContext().getNodes().get(0).getNode().getCommand() != null)
            KiloChat.sendLangMessageTo(source, "command.usage.commandRow", parseResults.getReader().getString(), "");

        int usages = 0;
        while (iterator.hasNext()) {
            usages++;
            String usage = iterator.next();
            KiloChat.sendLangMessageTo(source, "command.usage.commandRow", parseResults.getReader().getString(), usage);
        }

        if (usages == 0) KiloChat.sendLangMessageTo(source, "command.usage.commandRow", parseResults.getReader().getString(), "");

        return commandNodeStringMap.size();
    }

    private int sendInfo(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(
                LangText.getFormatter(true, "command.info", ModConstants.getMinecraftVersion())
                        .formatted(Formatting.GRAY)
                        .append("\n")
                        .append(new LiteralText("GitHub: ").formatted(Formatting.GRAY))
                        .append(Texts.bracketed(new LiteralText("github.com/KiloCraft/KiloEssentials/").styled((style) -> {
                            style.setColor(Formatting.GOLD);
                            style.setClickEvent(TextUtils.Events.onClickOpen("https://github.com/KiloCraft/KiloEssentials/"));
                            style.setHoverEvent(TextUtils.Events.onHover("&eClick to open"));
                        })))
                , false);

        return 1;
    }

    public static LiteralText getPermissionError(String hoverText) {
        LiteralText literalText = LangText.get(true, "command.exception.permission");
        literalText.styled((style) -> {
           style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(hoverText).formatted(Formatting.YELLOW)));
        });
        return literalText;
    }

    public static void sendPermissionError(ServerCommandSource source) {
        KiloChat.sendMessageToSource(source, new ChatMessage(
                KiloConfig.messages().commands().context().permissionException
                ,true));
    }

    public static SimpleCommandExceptionType getException(ExceptionMessageNode node, Object... objects) {
        String message = ModConstants.getMessageUtil().fromExceptionNode(node);
        return commandException(
                new LiteralText((objects != null) ? String.format(message, objects) : message).formatted(Formatting.RED));
    }

    public static SimpleCommandExceptionType getException(CommandMessageNode node, Object... objects) {
        String message = ModConstants.getMessageUtil().fromCommandNode(node);
        return commandException(
                new LiteralText((objects != null) ? String.format(message, objects) : message).formatted(Formatting.RED));
    }

    public static SimpleCommandExceptionType commandException(String message) {
        return new SimpleCommandExceptionType(
                new LiteralText(TextFormat.translateAlternateColorCodes('&', message)));
    }

    public static SimpleCommandExceptionType commandException(Text text) {
        return new SimpleCommandExceptionType(text);
    }

    public static SimpleCommandExceptionType getArgException(ArgExceptionMessageNode node, Object... objects) {
        String message = ModConstants.getMessageUtil().fromArgumentExceptionNode(node);
        return commandException(
                new LiteralText((objects != null) ? String.format(message, objects) : message).formatted(Formatting.RED));
    }

    public static void updateCommandTreeForEveryone() {
        for (ServerPlayerEntity playerEntity : KiloServer.getServer().getPlayerManager().getPlayerList()) {
            KiloServer.getServer().getPlayerManager().sendCommandTree(playerEntity);
        }
    }

    public int execute(ServerCommandSource executor, String commandToExecute) {
        OnCommandExecutionEvent event = new OnCommandExecutionEventImpl(executor, commandToExecute);
        String cmd = commandToExecute;

        if (!commandToExecute.endsWith("--push") && !executor.hasPermissionLevel(4))
            KiloServer.getServer().triggerEvent(event);
        else
            cmd = commandToExecute.replace(" --push", "");

        if (event.isCancelled()) return 0;

        if (this.simpleCommandManager.canExecute(cmd))
            return this.simpleCommandManager.execute(cmd, executor);

        StringReader stringReader = new StringReader(cmd);

        if (stringReader.canRead() && stringReader.peek() == '/')
            stringReader.skip();

        getServer().getVanillaServer().getProfiler().push(cmd);

        byte var = 0;
        try {
            try {
                return this.dispatcher.execute(stringReader, executor);
            } catch (CommandException e) {
                executor.sendError(e.getTextMessage());
                var = 0;
                return var;
            } catch (CommandSyntaxException e) {
                if (e.getRawMessage().getString().startsWith("Unknown or incomplete")) {
                    String literalName = cmd.split(" ")[0].replace("/", "");
                    CommandPermission reqPerm = CommandPermission.getByNode(literalName);

                    if (isCommand(literalName) && (reqPerm != null && !hasPermission(executor, reqPerm)))
                        sendPermissionError(executor);
                    else
                        KiloChat.sendMessageToSource(executor, new ChatMessage(
                                KiloConfig.messages().commands().context().executionException, true));

                } else {
                    executor.sendError(Texts.toText(e.getRawMessage()));

                    if (e.getInput() != null && e.getCursor() >= 0) {
                        int cursor = Math.min(e.getInput().length(), e.getCursor());
                        Text text = (new LiteralText("")).formatted(Formatting.GRAY).styled((style) -> {
                            style.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandToExecute));
                            style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(commandToExecute).formatted(Formatting.YELLOW)));
                        });

                        if (cursor > 10) text.append("...");

                        text.append(e.getInput().substring(Math.max(0, cursor - 10), cursor));
                        if (cursor < e.getInput().length()) {
                            Text errorAtPointMesssage = (new LiteralText(e.getInput().substring(cursor))).formatted(Formatting.RED, Formatting.UNDERLINE);
                            text.append(errorAtPointMesssage);
                        }

                        text.append(new LiteralText("<--[HERE]").formatted(Formatting.RED, Formatting.ITALIC));
                        executor.sendError(text);
                    }
                }

            }
        } catch (Exception e) {
            Text text = new LiteralText(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            if (SharedConstants.isDevelopment) {
                getLogger().error("Command exception: {}", commandToExecute, e);
                StackTraceElement[] stackTraceElements = e.getStackTrace();

                for(int i = 0; i < Math.min(stackTraceElements.length, 3); ++i) {
                    text.append("\n\n").append(stackTraceElements[i].getMethodName()).append("\n ").append(stackTraceElements[i].getFileName()).append(":").append(String.valueOf(stackTraceElements[i].getLineNumber()));
                }
            }

            executor.sendError((new TranslatableText("command.failed")).styled((style) -> {
                style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text));
            }));

            if (SharedConstants.isDevelopment) {
                executor.sendError(new LiteralText(Util.getInnermostMessage(e)));
                getLogger().error("'" + commandToExecute + "' threw an exception", e);
            }

            return (byte) 0;

        } finally {
            getServer().getVanillaServer().getProfiler().pop();
        }

        return var;
    }

    public static CommandDispatcher<ServerCommandSource> getDispatcher() {
        return KiloEssentialsImpl.commandDispatcher;
    }

    private boolean isCommand(String literal) {
        return dispatcher.getRoot().getChild(literal) != null;
    }

    public List<EssentialCommand> getCommands() {
        return this.commands;
    }

    @Deprecated
    public static int SUCCESS() {
        return 1;
    }

}
