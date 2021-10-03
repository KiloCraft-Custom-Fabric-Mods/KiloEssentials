package org.kilocraft.essentials.util.commands.teleport;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.command.ArgumentSuggestions;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.api.user.preference.Preference;
import org.kilocraft.essentials.api.util.schedule.SinglePlayerScheduler;
import org.kilocraft.essentials.chat.KiloChat;
import org.kilocraft.essentials.config.KiloConfig;
import org.kilocraft.essentials.config.main.sections.RtpSpecsConfigSection;
import org.kilocraft.essentials.user.CommandSourceServerUser;
import org.kilocraft.essentials.user.preference.Preferences;
import org.kilocraft.essentials.util.EssentialPermission;
import org.kilocraft.essentials.util.commands.CommandUtils;
import org.kilocraft.essentials.util.commands.KiloCommands;
import org.kilocraft.essentials.util.registry.RegistryUtils;
import org.kilocraft.essentials.util.settings.ServerSettings;
import org.kilocraft.essentials.util.text.Texter;

import java.util.Random;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static net.minecraft.command.argument.EntityArgumentType.getPlayer;

public class RtpCommand extends EssentialCommand {
    private static final Predicate<ServerCommandSource> PERMISSION_CHECK_SELF = (src) -> KiloEssentials.hasPermissionNode(src, EssentialPermission.RTP_SELF);
    private static final Predicate<ServerCommandSource> PERMISSION_CHECK_OTHERS = (src) -> KiloEssentials.hasPermissionNode(src, EssentialPermission.RTP_OTHERS);
    private static final Predicate<ServerCommandSource> PERMISSION_CHECK_IGNORE_LIMIT = (src) -> KiloEssentials.hasPermissionNode(src, EssentialPermission.RTP_BYPASS);
    private static final Predicate<ServerCommandSource> PERMISSION_CHECK_OTHER_DIMENSIONS = (src) -> KiloEssentials.hasPermissionNode(src, EssentialPermission.RTP_OTHERDIMENSIONS);
    private static final Predicate<ServerCommandSource> PERMISSION_CHECK_MANAGE = (src) -> KiloEssentials.hasPermissionNode(src, EssentialPermission.RTP_MANAGE);
    private static final Preference<Integer> RTP_LEFT = Preferences.RANDOM_TELEPORTS_LEFT;

    public RtpCommand() {
        super("rtp", PERMISSION_CHECK_SELF, new String[]{"wilderness", "wild"});
    }

    void teleport(ServerCommandSource src, ServerPlayerEntity target) {
        OnlineUser targetUser = this.getUserManager().getOnline(target);
        RtpSpecsConfigSection cfg = KiloConfig.main().rtpSpecs();
        if (targetUser.getPreference(RTP_LEFT) < 0) {
            targetUser.getPreferences().set(RTP_LEFT, 0);
        }

        // Check if the player has any rtps left or permission to ignore the limit
        if (CommandUtils.areTheSame(src, target) && targetUser.getPreference(RTP_LEFT) <= 0 && !PERMISSION_CHECK_IGNORE_LIMIT.test(src)) {
            targetUser.sendMessage(KiloConfig.messages().commands().rtp().empty);
            return;
        }

        // Check if the target is in the correct dimension or has permission to perform the command in other dimensions
        if (RegistryUtils.dimensionTypeToRegistryKey(target.getWorld().getDimension()) != World.OVERWORLD && !PERMISSION_CHECK_OTHER_DIMENSIONS.test(src)) {
            targetUser.sendMessage(KiloConfig.messages().commands().rtp().dimensionException);
            return;
        }
        if (!cfg.broadcastMessage.isEmpty()) {
            KiloChat.broadCast(String.format(cfg.broadcastMessage, targetUser.getFormattedDisplayName()));
        }

        if (!PERMISSION_CHECK_IGNORE_LIMIT.test(src)) {
            targetUser.getPreferences().set(RTP_LEFT, targetUser.getPreference(RTP_LEFT) - 1);
        }
        boolean done = false;
        BlockPos pos = null;
        int x = 0;
        int z = 0;
        int tries = 0;
        final ServerWorld world = src.getWorld();
        while (!done) {
            if (tries >= cfg.maxTries) {
                targetUser.sendLangMessage("command.rtp.failed");
                return;
            }
            Random r = new Random();
            x = r.nextInt(cfg.max - cfg.min) + cfg.min * (r.nextBoolean() ? 1 : -1);
            x += cfg.centerX;
            z = r.nextInt(cfg.max - cfg.min) + cfg.min * (r.nextBoolean() ? 1 : -1);
            z += cfg.centerZ;
            pos = new BlockPos(x, 64, z);
            Biome biome = world.getBiome(pos);
            tries++;
            String biomeId = src.getRegistryManager().get(Registry.BIOME_KEY).getId(biome).toString();
            done = true;
            for (String blackListedBiome : cfg.blackListedBiomes) {
                if (biomeId.equals(blackListedBiome)) {
                    done = false;
                    break;
                }
            }
        }
        // Add a custom ticket to gradually preload chunks
        world.getChunkManager().addTicket(ChunkTicketType.create("rtp", Integer::compareTo, 300), new ChunkPos(pos), ServerSettings.getViewDistance() + 1, target.getId()); // Lag reduction
        final int finalX = x;
        final int finalZ = z;
        new SinglePlayerScheduler(targetUser, -1, cfg.teleportCooldown, () -> {
            target.teleport(world, finalX, world.getTopY(Heightmap.Type.WORLD_SURFACE, finalX, finalZ) + 1, finalZ, target.getYaw(), target.getPitch());
        });
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> addArgument = this.literal("add")
                .requires(PERMISSION_CHECK_MANAGE)
                .then(this.argument("target", EntityArgumentType.player())
                        .suggests(ArgumentSuggestions::allPlayers).then(
                                this.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(this::executeAdd)
                        )
                );

        LiteralArgumentBuilder<ServerCommandSource> setArgument = this.literal("set")
                .requires(PERMISSION_CHECK_MANAGE)
                .then(this.argument("target", EntityArgumentType.player())
                        .suggests(ArgumentSuggestions::allPlayers).then(
                                this.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(this::executeSet)
                        )
                );

        LiteralArgumentBuilder<ServerCommandSource> removeArgument = this.literal("remove")
                .requires(PERMISSION_CHECK_MANAGE)
                .then(this.argument("target", EntityArgumentType.player())
                        .suggests(ArgumentSuggestions::allPlayers).then(
                                this.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(this::executeRemove)
                        )
                );

        LiteralArgumentBuilder<ServerCommandSource> sendArgument = this.literal("send")
                .requires(PERMISSION_CHECK_MANAGE)
                .then(this.argument("target", EntityArgumentType.player())
                        .suggests(ArgumentSuggestions::allPlayers)
                        .executes(this::executeOthers)
                );

        LiteralArgumentBuilder<ServerCommandSource> checkArgument = this.literal("check")
                .executes(this::executeLeft)
                .then(this.argument("target", EntityArgumentType.player())
                        .requires(PERMISSION_CHECK_OTHERS)
                        .suggests(ArgumentSuggestions::allPlayers)
                        .executes(this::executeGet)
                );

        LiteralArgumentBuilder<ServerCommandSource> performArgument = this.literal("perform")
                .executes(this::executePerform);

        this.commandNode.addChild(addArgument.build());
        this.commandNode.addChild(setArgument.build());
        this.commandNode.addChild(removeArgument.build());
        this.commandNode.addChild(sendArgument.build());
        this.commandNode.addChild(sendArgument.build());
        this.commandNode.addChild(checkArgument.build());
        this.commandNode.addChild(performArgument.build());
        this.argumentBuilder.executes(this::executeSelf);
    }

    private int executeLeft(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        OnlineUser user = this.getUserManager().getOnline(ctx.getSource());
        CommandSourceServerUser.of(ctx)
                .sendLangMessage("command.rtp.get", user.getDisplayName(), user.getPreference(RTP_LEFT));

        return user.getPreference(RTP_LEFT);
    }

    private int executeAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        OnlineUser user = this.getUserManager().getOnline(getPlayer(ctx, "target"));
        int amountToAdd = getInteger(ctx, "amount");
        user.getPreferences().set(RTP_LEFT, user.getPreference(RTP_LEFT) + amountToAdd);
        CommandSourceServerUser.of(ctx)
                .sendLangMessage("template.#1", "RTPs left", user.getPreference(RTP_LEFT), user.getDisplayName());

        return user.getPreference(RTP_LEFT);
    }

    private int executeSet(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        OnlineUser user = this.getUserManager().getOnline(getPlayer(ctx, "target"));
        int amountToSet = getInteger(ctx, "amount");
        user.getPreferences().set(RTP_LEFT, amountToSet);
        CommandSourceServerUser.of(ctx)
                .sendLangMessage("template.#1", "RTPs left", user.getPreference(RTP_LEFT), user.getDisplayName());

        return user.getPreference(RTP_LEFT);
    }

    private int executeGet(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        OnlineUser user = this.getUserManager().getOnline(getPlayer(ctx, "target"));
        CommandSourceServerUser.of(ctx)
                .sendLangMessage("command.rtp.get", user.getDisplayName(), user.getPreference(RTP_LEFT));

        return user.getPreference(RTP_LEFT);
    }

    private int executeRemove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        OnlineUser user = this.getUserManager().getOnline(getPlayer(ctx, "target"));
        int amountToRemove = getInteger(ctx, "amount");

        if ((user.getPreference(RTP_LEFT) - amountToRemove) < 0)
            throw KiloCommands.getException("argument.general.negative_values").create();

        user.getPreferences().set(RTP_LEFT, user.getPreference(RTP_LEFT) - amountToRemove);
        CommandSourceServerUser.of(ctx)
                .sendLangMessage("template.#1", "RTPs left", user.getPreference(RTP_LEFT), user.getDisplayName());

        return user.getPreference(RTP_LEFT);
    }

    private int executeSelf(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Text text = Texter.confirmationMessage(
                "command.rtp.confirm",
                Texter.getButton("&8[&aClick Here to perform&8]", "/rtp perform", Texter.newText("&dConfirm"))
        );
        this.getOnlineUser(ctx).sendMessage(text);
        return SUCCESS;
    }

    private int executePerform(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return this.execute(ctx.getSource(), ctx.getSource().getPlayer());
    }

    private int executeOthers(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        OnlineUser target = this.getUserManager().getOnline(getPlayer(ctx, "target"));

        return this.execute(ctx.getSource(), target.asPlayer());
    }

    private int execute(ServerCommandSource source, ServerPlayerEntity target) {
        this.getUserManager().getOnline(target).sendMessage(this.messages.commands().rtp().start);
        this.teleport(source, target);
        return SUCCESS;
    }
}