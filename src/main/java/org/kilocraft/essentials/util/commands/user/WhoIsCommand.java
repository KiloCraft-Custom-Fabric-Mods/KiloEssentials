package org.kilocraft.essentials.util.commands.user;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import org.kilocraft.essentials.api.ModConstants;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.user.CommandSourceUser;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.api.user.User;
import org.kilocraft.essentials.api.user.preference.UserPreferences;
import org.kilocraft.essentials.api.world.location.Vec3dLocation;
import org.kilocraft.essentials.user.ServerUser;
import org.kilocraft.essentials.user.UserHomeHandler;
import org.kilocraft.essentials.user.preference.Preferences;
import org.kilocraft.essentials.util.CommandPermission;
import org.kilocraft.essentials.util.TimeDifferenceUtil;
import org.kilocraft.essentials.util.text.Texter;

public class WhoIsCommand extends EssentialCommand {
    public WhoIsCommand() {
        super("whois", CommandPermission.WHOIS_SELF, new String[]{"info"});
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, String> userArgument = this.getUserArgument("user")
                .requires(src -> this.hasPermission(src, CommandPermission.WHOIS_OTHERS))
                .executes(this::executeOthers);

        this.argumentBuilder.executes(this::executeSelf);
        this.commandNode.addChild(userArgument.build());
    }

    private int executeSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceUser user = this.getCommandSource(ctx);
        return this.execute(user, this.getOnlineUser(ctx));
    }

    private int executeOthers(CommandContext<CommandSourceStack> ctx) {
        CommandSourceUser src = this.getCommandSource(ctx);
        this.getUserManager().getUserThenAcceptAsync(src, this.getUserArgumentInput(ctx, "user"), (user) -> {
            this.execute(src, user);
        });

        return AWAIT;
    }

    private int execute(CommandSourceUser src, User target) {
        Texter.InfoBlockStyle text = new Texter.InfoBlockStyle("Who's " + target.getNameTag(), ChatFormatting.GOLD, ChatFormatting.AQUA, ChatFormatting.GRAY);

        text.append("DisplayName", target.getFormattedDisplayName())
                .space()
                .append("(").append(target.getUsername()).append(")")
                .space()
                .append(
                        Texter.appendButton(
                                Texter.newText("( More )").withStyle(ChatFormatting.GRAY),
                                Texter.newText("Click to see the name history"),
                                ClickEvent.Action.RUN_COMMAND,
                                "/whowas " + target.getUsername()
                        )
                );

        text.append("UUID",
                Texter.appendButton(
                        new TextComponent(target.getUuid().toString()),
                        new TextComponent(ModConstants.translation("general.click_copy")),
                        ClickEvent.Action.COPY_TO_CLIPBOARD,
                        target.getUuid().toString()
                )
        );
        text.append("IP (Last Saved)",
                Texter.appendButton(
                        new TextComponent(target.getLastSocketAddress()),
                        new TextComponent(ModConstants.translation("general.click_copy")),
                        ClickEvent.Action.COPY_TO_CLIPBOARD,
                        target.getLastSocketAddress()
                )
        );

        UserPreferences settings = target.getPreferences();
        text.append("Status",
                new String[]{"Invulnerable", "Online"},
                settings.get(Preferences.INVULNERABLE),
                target.isOnline()
        );

        if (target.isOnline()) {
            OnlineUser user = (OnlineUser) target;
            text.append("Survival status",
                    new String[]{"Health", "FoodLevel", "Saturation"},
                    ModConstants.DECIMAL_FORMAT.format(user.asPlayer().getHealth()),
                    ModConstants.DECIMAL_FORMAT.format(user.asPlayer().getFoodData().getFoodLevel()),
                    ModConstants.DECIMAL_FORMAT.format(user.asPlayer().getFoodData().getSaturationLevel())
            );
        }

        text.append("Artifacts",
                new String[]{"IsStaff", "May Sit"},
                ((ServerUser) target).isStaff(),
                settings.get(Preferences.CAN_SEAT)
        );

        if (target.getTicksPlayed() >= 0) {
            text.append("Playtime", TimeDifferenceUtil.convertSecondsToString(target.getTicksPlayed() / 20, '6', 'e'));
        }
        if (target.getFirstJoin() != null) {
            text.append("First joined", Texter.newText("&e" + TimeDifferenceUtil.formatDateDiff(target.getFirstJoin().getTime())).withStyle((style) -> {
                return style.withHoverEvent(Texter.Events.onHover("&d" + ModConstants.DATE_FORMAT.format(target.getFirstJoin())));
            }));
        }

        if (!target.isOnline() && target.getLastOnline() != null) {
            text.append("Last Online", Texter.newText("&e" + TimeDifferenceUtil.formatDateDiff(target.getLastOnline().getTime())).withStyle((style) -> {
                return style.withHoverEvent(Texter.Events.onHover("&d" + ModConstants.DATE_FORMAT.format(target.getLastOnline())));
            }));
        }

        text.append("Meta", new String[]{"Homes", "RTP", "Selected channel"},
                UserHomeHandler.isEnabled() ? target.getHomesHandler().homes() : 0,
                target.getPreference(Preferences.RANDOM_TELEPORTS_LEFT),
                target.getPreference(Preferences.CHAT_CHANNEL).getId());

        text.append("Is Spying", new String[]{"On Commands", "On Social"},
                target.getPreference(Preferences.COMMAND_SPY),
                target.getPreference(Preferences.SOCIAL_SPY));

        Vec3dLocation vec = ((Vec3dLocation) target.getLocation()).shortDecimals();
        assert vec.getDimension() != null;
        MutableComponent loc = Texter.newText(vec.asFormattedString());
        text.append("Location", this.getButtonForVec(loc, vec));

        if (target.getLastSavedLocation() != null) {
            Vec3dLocation savedVec = ((Vec3dLocation) target.getLastSavedLocation()).shortDecimals();
            MutableComponent lastLoc = Texter.newText(savedVec.asFormattedString());
            text.append("Saved Location", this.getButtonForVec(lastLoc, savedVec));
        }

        src.sendMessage(text.build());
        return SUCCESS;
    }

    private MutableComponent getButtonForVec(MutableComponent text, Vec3dLocation vec) {
        assert vec.getDimension() != null;
        return Texter.appendButton(
                text,
                new TextComponent(ModConstants.translation("general.click_tp")),
                ClickEvent.Action.SUGGEST_COMMAND,
                "/tpin " + vec.getDimension().toString() + " " +
                        vec.getX() + " " + vec.getY() + " " + vec.getZ() + " @s"
        );
    }
}