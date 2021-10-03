package org.kilocraft.essentials.util.commands.misc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.world.SpawnHelper;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.text.ComponentText;
import org.kilocraft.essentials.chat.StringText;
import org.kilocraft.essentials.patch.optimizedSpawning.SpawnUtil;
import org.kilocraft.essentials.util.CommandPermission;
import org.kilocraft.essentials.util.commands.KiloCommands;
import org.kilocraft.essentials.util.registry.RegistryKeyID;
import org.kilocraft.essentials.util.settings.ServerSettings;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MobCapCommand extends EssentialCommand {

    public MobCapCommand() {
        super("mobcap", CommandPermission.MOBCAP_QUERY);
    }

    public static CompletableFuture<Suggestions> suggestSpawnGroups(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        List<String> list = Arrays.stream(SpawnGroup.values()).map(SpawnGroup::getName).collect(Collectors.toList());
        list.add("global");
        return CommandSource.suggestMatching(list, builder);
    }

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        RequiredArgumentBuilder<ServerCommandSource, Float> multiplier = this.argument("multiplier", FloatArgumentType.floatArg(0, 100));
        multiplier.executes(ctx -> this.execute(ctx, DimensionArgumentType.getDimensionArgument(ctx, "dimension")));
        final RequiredArgumentBuilder<ServerCommandSource, String> spawnGroup = this.argument("name", StringArgumentType.word())
                .suggests(MobCapCommand::suggestSpawnGroups)
                .requires(src -> KiloCommands.hasPermission(src, CommandPermission.MOBCAP_SET));
        final RequiredArgumentBuilder<ServerCommandSource, Identifier> world = this.argument("dimension", DimensionArgumentType.dimension());
        world.executes(ctx -> this.info(ctx, DimensionArgumentType.getDimensionArgument(ctx, "dimension")));
        spawnGroup.then(multiplier);
        world.then(spawnGroup);
        this.argumentBuilder.executes(ctx -> this.info(ctx, ctx.getSource().getWorld()));
        this.commandNode.addChild(world.build());
    }

    private int execute(CommandContext<ServerCommandSource> ctx, ServerWorld world) throws CommandSyntaxException {
        float f = FloatArgumentType.getFloat(ctx, "multiplier");
        String name = StringArgumentType.getString(ctx, "name");
        Optional<SpawnGroup> spawnGroup = Optional.ofNullable(SpawnGroup.byName(name));
        Identifier id = world.getRegistryKey().getValue();
        if (spawnGroup.isPresent()) {
            ServerSettings.setFloat("mobcap." + id.getPath() + "." + spawnGroup.get().getName().toLowerCase(), f);
        } else if (name.equals("global")) {
            ServerSettings.setFloat("mobcap." + id.getPath(), f);
        } else {
            throw new SimpleCommandExceptionType(new LiteralText("Invalid spawn group: " + name)).create();
        }
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        player.sendMessage(StringText.of("command.mobpsawn", f, name), false);
        return SUCCESS;
    }

    private int info(CommandContext<ServerCommandSource> ctx, ServerWorld world) throws CommandSyntaxException {
        SpawnHelper.Info info = world.getChunkManager().getSpawnInfo();
        Objects.requireNonNull(info, "SpawnHelper.Info must not be null");
        sendMobCap(ctx.getSource().getPlayer(), world, "Global MobCap", info.getGroupToCount(), group -> SpawnUtil.getGlobalMobCap(info, world, group));
        return SUCCESS;
    }

    public static void sendMobCap(ServerPlayerEntity player, ServerWorld world, String title, Object2IntMap<SpawnGroup> spawnGroupCounts, Function<SpawnGroup, Integer> getSpawnGroupMobCap) {
        TextComponent.Builder text = Component.text();
        text.content(title).color(NamedTextColor.YELLOW)
                .append(Component.text(" (").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(String.format("%.1f", ServerSettings.tick_utils_global_mobcap)).color(NamedTextColor.RED))
                .append(Component.text(", ").color(NamedTextColor.GRAY))
                .append(Component.text(SpawnUtil.getMobCapMultiplier(world, 0)).color(NamedTextColor.GREEN))
                .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(":\n").color(NamedTextColor.YELLOW));
        for (SpawnGroup group : SpawnGroup.values()) {
            int count = spawnGroupCounts.getOrDefault(group, 0);
            int cap = getSpawnGroupMobCap.apply(group);
            String name = group.getName();
            text.append(Component.text(name + ": ").color(NamedTextColor.GRAY))
                    .append(Component.text(count).color(NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text("/").color(NamedTextColor.DARK_GRAY))
                    .append(Component.text(cap).color(NamedTextColor.GOLD))
                    .append(Component.text(" (").color(NamedTextColor.DARK_GRAY))
                    .append(Component.text(SpawnUtil.getMobCapMultiplier(world, group)).color(NamedTextColor.AQUA))
                    .append(Component.text(")\n").color(NamedTextColor.DARK_GRAY));
        }
        player.sendMessage(ComponentText.toText(text.build()), false);
    }

}
