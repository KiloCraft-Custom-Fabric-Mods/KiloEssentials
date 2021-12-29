package org.kilocraft.essentials.util.commands.misc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.kilocraft.essentials.api.ModConstants;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.user.CommandSourceUser;
import org.kilocraft.essentials.api.user.User;
import org.kilocraft.essentials.util.CommandPermission;
import org.kilocraft.essentials.util.TimeDifferenceUtil;
import org.kilocraft.essentials.util.commands.CommandUtils;
import org.kilocraft.essentials.util.text.Texter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class PlaytimeCommand extends EssentialCommand {
    private final Predicate<CommandSourceStack> PERMISSION_CHECK_MODIFY = src -> this.hasPermission(src, CommandPermission.PLAYTIME_MODIFY);

    public PlaytimeCommand() {
        super("playtime", CommandPermission.PLAYTIME_SELF, new String[]{"pt"});
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, String> userArgument = this.getUserArgument("user")
                .requires(src -> this.hasPermission(src, CommandPermission.PLAYTIME_OTHERS))
                .executes(this::executeOther);

        LiteralArgumentBuilder<CommandSourceStack> increaseArg = this.literal("increase")
                .requires(this.PERMISSION_CHECK_MODIFY)
                .then(this.argument("seconds", integer(0))
                        .executes(ctx -> this.set(ctx, "increase")));
        LiteralArgumentBuilder<CommandSourceStack> decreaseArg = this.literal("decrease")
                .requires(this.PERMISSION_CHECK_MODIFY)
                .then(this.argument("seconds", integer(0))
                        .executes(ctx -> this.set(ctx, "decrease")));
        LiteralArgumentBuilder<CommandSourceStack> setArg = this.literal("set")
                .requires(this.PERMISSION_CHECK_MODIFY)
                .then(this.argument("seconds", integer(0))
                        .executes(ctx -> this.set(ctx, "set")));

        userArgument.then(increaseArg);
        userArgument.then(decreaseArg);
        userArgument.then(setArg);
        this.argumentBuilder.executes(this::executeSelf);
        this.commandNode.addChild(userArgument.build());
    }

    private int set(CommandContext<CommandSourceStack> ctx, String type) {
        CommandSourceUser src = this.getCommandSource(ctx);
        int ticks = getInteger(ctx, "seconds") * 20;

        AtomicInteger atomicInteger = new AtomicInteger(AWAIT);
        this.getUserManager().getUserThenAcceptAsync(src, this.getUserArgumentInput(ctx, "user"), (user) -> {
            try {
                user.setTicksPlayed(
                        type.equals("set") ? ticks :
                                type.equals("increase") ? user.getTicksPlayed() + ticks : user.getTicksPlayed() - ticks
                );

                user.saveData();
            } catch (IOException e) {
                src.sendError(e.getMessage());
            }

            src.sendLangMessage("command.playtime.set", user.getNameTag(),
                    TimeDifferenceUtil.convertSecondsToString(user.getTicksPlayed() / 20, 'e', '6'));
            atomicInteger.set(user.getTicksPlayed());
        });

        return atomicInteger.get();
    }

    private int executeSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return this.execute(this.getCommandSource(ctx), this.getOnlineUser(ctx));
    }

    private int executeOther(CommandContext<CommandSourceStack> ctx) {
        CommandSourceUser src = this.getCommandSource(ctx);
        String inputName = this.getUserArgumentInput(ctx, "user");

        if (this.getOnlineUser(inputName) != null)
            return this.execute(src, this.getOnlineUser(inputName));

        this.getUserManager().getUserThenAcceptAsync(src, this.getUserArgumentInput(ctx, "user"), (user) -> {
            this.execute(src, user);
        });

        return AWAIT;
    }

    private int execute(CommandSourceUser src, User target) {
        String pt = target.getTicksPlayed() <= 0 ? ModConstants.translation("general.not_present") :
                TimeDifferenceUtil.convertSecondsToString(target.getTicksPlayed() / 20, '6', 'e');
        String firstJoin = target.getFirstJoin() != null ? TimeDifferenceUtil.formatDateDiff(target.getFirstJoin().getTime()) : ModConstants.translation("general.not_present");

        String title = CommandUtils.areTheSame(src, target) ? ModConstants.translation("command.playtime.title.self") : this.tl("command.playtime.title.others", target.getNameTag());
        Texter.InfoBlockStyle text = Texter.InfoBlockStyle.of(title);

        text.append(ModConstants.translation("command.playtime.total"), pt)
                .append(ModConstants.translation("command.playtime.first_join"), firstJoin);

        src.sendMessage(text.build());
        return target.getTicksPlayed();
    }

}
