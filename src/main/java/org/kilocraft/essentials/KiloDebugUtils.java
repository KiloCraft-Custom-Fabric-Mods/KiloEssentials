package org.kilocraft.essentials;

import net.minecraft.SharedConstants;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.BossBarManager;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.ModConstants;
import org.kilocraft.essentials.api.chat.TextFormat;
import org.kilocraft.essentials.api.server.Server;
import org.kilocraft.essentials.api.world.MonitorableWorld;
import org.kilocraft.essentials.util.TPSTracker;

import java.io.File;

public class KiloDebugUtils {
    public static KiloDebugUtils INSTANCE;
    private KiloEssentials ess;
    private Server server;
    private MinecraftServer minecraftServer;
    private CommandBossBar bossBar;
    private Identifier DEBUG_BAR = new Identifier("kiloessentials", "debug_bar");

    public KiloDebugUtils(KiloEssentials ess) {
        INSTANCE = this;
        this.ess = ess;
        this.server = KiloServer.getServer();
        this.minecraftServer = server.getVanillaServer();

        setupBossBar();
    }

    public static void validateDebugMode() {
        File debugFile = new File(KiloEssentials.getWorkingDirectory() + "/kiloessentials.debug");
        if (debugFile.exists()) {
            KiloEssentials.getServer().getLogger().warn("**** SERVER IS RUNNING IN DEBUG/DEVELOPMENT MODE!");
            KiloEssentials.getServer().getLogger().warn("To change this simply remove the \"kiloessentials.debug\" file");
            setDebugMode(true);
        } else {
            setDebugMode(false);
            if (INSTANCE != null)
                INSTANCE.removeBossBar();
        }
    }

    public static void setDebugMode(boolean set) {
        SharedConstants.isDevelopment = set;
    }

    public void onScheduledUpdate() {
        if (SharedConstants.isDevelopment)
            updateBossbar();
        else
            removeBossBar();
    }

    private void removeBossBar() {
        BossBarManager manager = minecraftServer.getBossBarManager();
        manager.remove(bossBar);
    }

    private void setupBossBar() {
        BossBarManager manager = minecraftServer.getBossBarManager();
        bossBar = manager.add(DEBUG_BAR, new LiteralText("DebugBar"));
        bossBar.setMaxValue(20);
        bossBar.setOverlay(BossBar.Style.NOTCHED_20);
    }

    public void updateBossbar() {
        int loadedChunks = 0;
        int entities = 0;
        for (ServerWorld world : minecraftServer.getWorlds()) {
            MonitorableWorld moWorld = ((MonitorableWorld) world);
            loadedChunks = loadedChunks + moWorld.totalLoadedChunks();
            entities = entities + moWorld.loadedEntities();
        }

        int tps = (int) TPSTracker.tps1.getAverage();
        bossBar.setValue(tps);

        String debugText = String.format(ModConstants.getProperties().getProperty("debug_bar_text"),
                TextFormat.getFormattedTPS(TPSTracker.tps1.getAverage()), tps, entities, loadedChunks, ModConstants.getVersionInt());

        Text text = getDebugText().append(TextFormat.translateToLiteralText('&', debugText));

        if (tps > 15) {
            bossBar.setColor(BossBar.Color.GREEN);
        } else if (tps > 10) {
            bossBar.setColor(BossBar.Color.YELLOW);
        } else {
            bossBar.setColor(BossBar.Color.RED);
        }

        bossBar.setName(text);
        if (minecraftServer.getPlayerManager().getPlayerList() != null)
            bossBar.addPlayers(minecraftServer.getPlayerManager().getPlayerList());
    }

    private Text getDebugText() {
        return new LiteralText("[").formatted(Formatting.WHITE).append(new LiteralText("Debug")
                .formatted(Formatting.YELLOW)).append("] ").formatted(Formatting.WHITE).formatted(Formatting.BOLD);
    }

}
