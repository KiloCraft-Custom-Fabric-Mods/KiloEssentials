package org.kilocraft.essentials.user;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.feature.ConfigurableFeature;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.config.KiloConfig;
import org.kilocraft.essentials.extensions.homes.api.Home;
import org.kilocraft.essentials.extensions.homes.api.UnsafeHomeException;
import org.kilocraft.essentials.extensions.homes.commands.DelhomeCommand;
import org.kilocraft.essentials.extensions.homes.commands.HomeCommand;
import org.kilocraft.essentials.extensions.homes.commands.HomesCommand;
import org.kilocraft.essentials.extensions.homes.commands.SethomeCommand;
import org.kilocraft.essentials.util.commands.KiloCommands;
import org.kilocraft.essentials.util.registry.RegistryUtils;
import org.kilocraft.essentials.util.settings.ServerSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * @author CODY_AI
 * A better way of handeling the User (Instance of player) Homes
 * @see ServerUser
 * @see ServerUserManager
 */

public class UserHomeHandler implements ConfigurableFeature {
    private static boolean isEnabled = false;
    private static final List<Home> loadedHomes = new ArrayList<>();
    private List<Home> userHomes;
    private ServerUser serverUser;

    public UserHomeHandler() {
    }

    public UserHomeHandler(ServerUser serverUser) {
        if (isEnabled()) {
            this.serverUser = serverUser;
            this.userHomes = new ArrayList<>();
        }
    }

    public static boolean isEnabled() {
        return isEnabled;
    }

    @Deprecated
    public static List<Home> getHomesOf(UUID uuid) {
        List<Home> list = new ArrayList<>();
        for (Home home : loadedHomes) {
            if (home.getOwner().equals(uuid)) list.add(home);
        }

        return list;
    }

    public static CompletableFuture<Suggestions> suggestHomes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        return SharedSuggestionProvider.suggest(KiloEssentials.getUserManager().getOnline(
                context.getSource()).getHomesHandler().getHomes().stream().map(Home::getName), builder);
    }

    @Override
    public boolean register() {
        isEnabled = true;

        List<EssentialCommand> commands = Arrays.asList(new HomeCommand(), new HomesCommand(), new SethomeCommand(), new DelhomeCommand());

        for (final EssentialCommand command : commands) {
            KiloCommands.register(command);
        }

        return true;
    }

    public void addHome(Home home) {
        this.userHomes.add(home);
        loadedHomes.add(home);
    }

    public void removeHome(String name) {
        this.removeHome(this.getHome(name));
    }

    public void removeHome(Home home) {
        this.userHomes.remove(home);
        loadedHomes.remove(home);
    }

    public Home getHome(String name) {
        for (Home home : this.userHomes) {
            if (home.getName().equals(name))
                return home;
        }

        return new Home();
    }

    public List<Home> getHomes() {
        return this.userHomes;
    }

    public int homes() {
        return this.userHomes.size();
    }

    public boolean hasHome(String name) {
        for (Home userHome : this.userHomes) {
            if (userHome.getName().equals(name))
                return true;
        }

        return false;
    }

    public void teleportToHome(OnlineUser user, String name) throws UnsafeHomeException {
        this.teleportToHome(user, this.getHome(name));
    }

    public void teleportToHome(OnlineUser user, Home home) throws UnsafeHomeException {
        if (home == null) throw new UnsafeHomeException(null, Reason.NO_HOME);
        if (user.isOnline()) {
            ServerLevel world = KiloEssentials.getMinecraftServer().getLevel(RegistryUtils.dimensionTypeToRegistryKey(home.getLocation().getDimensionType()));

            if (world == null) {
                throw new UnsafeHomeException(home, Reason.MISSING_DIMENSION);
            }

            if (!this.userHomes.contains(home)) {
                user.sendLangMessage("command.home.invalid_home");
                return;
            }

            home.teleportTo(user);
        }
    }

    public void prepareHomeLocation(OnlineUser user, Home home) {
        if (home == null) return;
        if (user.isOnline()) {
            ServerLevel world = KiloEssentials.getMinecraftServer().getLevel(RegistryUtils.dimensionTypeToRegistryKey(home.getLocation().getDimensionType()));

            if (world == null) {
                return;
            }

            if (!this.userHomes.contains(home)) {
                return;
            }

            // Add a custom ticket to gradually preload chunks
            world.getChunkSource().addRegionTicket(TicketType.create("home", Integer::compareTo, (KiloConfig.main().server().cooldown + 1) * 20), new ChunkPos(home.getLocation().toPos()), 1, user.asPlayer().getId()); // Lag reduction
        }
    }

    public CompoundTag serialize(CompoundTag tag) {
        for (Home userHome : this.userHomes) {
            tag.put(userHome.getName(), userHome.toTag());
        }

        return tag;
    }

    public void deserialize(CompoundTag NbtCompound) {
        for (String key : NbtCompound.getAllKeys()) {
            Home home = new Home(NbtCompound.getCompound(key));
            home.setName(key);
            home.setOwner(this.serverUser.uuid);
            this.userHomes.add(home);
            loadedHomes.add(home);
        }
    }

    public enum Reason {
        MISSING_DIMENSION, NO_HOME
    }

}
