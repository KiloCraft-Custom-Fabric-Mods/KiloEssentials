package org.kilocraft.essentials.api.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.OperatorList;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.Logger;
import org.kilocraft.essentials.api.Mod;
import org.kilocraft.essentials.api.event.Event;
import org.kilocraft.essentials.api.event.EventHandler;
import org.kilocraft.essentials.api.event.EventRegistry;
import org.kilocraft.essentials.api.util.MinecraftServerLoggable;
import org.kilocraft.essentials.api.world.World;
import org.kilocraft.essentials.api.world.worldimpl.WorldImpl;
import org.kilocraft.essentials.craft.mixin.MinecraftServerAccessor;
import org.kilocraft.essentials.craft.user.UserManager;

import java.util.*;

public class ServerImpl implements Server { // TODO I don't think the impl should be in API packages
    private final MinecraftServer server;
    private final EventRegistry eventRegistry;
    private final UserManager userManager;
    private final String serverBrand;
    private String serverDisplayBrand;
    private String serverName = "Minecraft server";

    public ServerImpl(MinecraftServer minecraftServer, EventRegistry eventManager, UserManager userManager, String serverBrand) {
        this.server = minecraftServer;
        this.serverBrand = serverBrand;
        this.userManager = userManager;
        this.serverDisplayBrand = serverBrand;
        this.eventRegistry = eventManager;
    }

    public void savePlayers() {
    }

    @Override
    public MinecraftServer getVanillaServer() {
        return this.server;
    }

    @Override
    public PlayerManager getPlayerManager() {
        return this.server.getPlayerManager();
    }

    @Override
    public UserManager getUserManager() {
        return this.userManager;
    }


    @Override
    public ServerPlayerEntity getPlayer(String name) {
        return getPlayerManager().getPlayer(name);
    }

    @Override
    public ServerPlayerEntity getPlayer(UUID uuid) {
        return getPlayerManager().getPlayer(uuid);
    }

    @Override
    public String getName() {
        return this.serverName;
    }

    @Override
    public void setName(String name) {
        this.serverName = name;
    }

    @Override
    public String getVersion() {
        return Mod.getVersion();
    }

    @Override
    public Logger getLogger() {
        return ((MinecraftServerLoggable) server).getLogger();
    }

    @Override
    public Collection<ServerPlayerEntity> getPlayerList() {
        return server.getPlayerManager().getPlayerList();
    }

    @Override
    public List<World> getWorlds() {
        List<World> worlds = new ArrayList<>();
        server.getWorlds().forEach(world -> worlds.add(new WorldImpl(world)));

        return worlds;
    }

    @Override
    public boolean isMainThread() {
        return Thread.currentThread().equals(((MinecraftServerAccessor) (server)).getServerThread());
    }

    @Override
    public void registerEvent(EventHandler e) {
        eventRegistry.register(e);
    }

    @Override
    public EventRegistry getEventRegistry() {
        return eventRegistry;
    }


    @Override
    public <E extends Event> E triggerEvent(E e) {
        return eventRegistry.trigger(e);
    }

    @Override
    public Optional<PlayerEntity> getPlayerByName(String playerName) {
        ServerPlayerEntity e = server.getPlayerManager().getPlayer(playerName);
        if (e == null)
            return Optional.empty();

        return Optional.of(e);
    }

    @Override
    public void execute(String command) {
        server.getCommandManager().execute(server.getCommandSource(), command);
    }

    @Override
    public void execute(ServerCommandSource source, String command) {
        server.getCommandManager().execute(source, command);
    }

    @Override
    public void setDisplayBrandName(String brand) {
        this.serverDisplayBrand = brand;
    }

    @Override
    public String getDisplayBrandName() {
        return this.serverDisplayBrand;
    }

    @Override
    public void shutdown() {
        savePlayers();

        this.server.stop(false);
    }

    @Override
    public void shutdown(String reason) {
        kickAll(reason);
        shutdown();
    }

    @Override
    public void shutdown(Text reason) {
        kickAll(reason);
        shutdown();
    }

    @Override
    public void kickAll(String reason) {
        kickAll(new LiteralText(reason));
    }

    @Override
    public void kickAll(Text reason) {
        for(ServerPlayerEntity playerEntity : this.server.getPlayerManager().getPlayerList()) {
            playerEntity.networkHandler.disconnect(reason);
        }
    }

    @Override
    public void sendMessage(String message) {
        getLogger().info(message);
    }

    @Override
    public OperatorList getOperatorList() {
        return server.getPlayerManager().getOpList();
    }

    public String getBrandName() {
        return serverBrand;
    }
}
