package org.kilocraft.essentials.mixin.patch.performance;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.GameRules;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.kilocraft.essentials.api.util.TickManager;
import org.kilocraft.essentials.util.settings.ServerSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin {

    /*
     * Copied from:
     * https://github.com/Wesley1808/ServerCore-Fabric/blob/1.17.1/src/main/java/org/provim/servercore/mixin/performance/ServerChunkManagerMixin.java
     * */

    @Unique
    private final HashSet<ChunkHolder> active = new HashSet<>();
    @Shadow
    @Final
    public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;
    @Shadow
    @Final
    ServerWorld world;
    @Shadow
    private boolean spawnMonsters;
    @Shadow
    private boolean spawnAnimals;
    @Shadow
    @Nullable
    private SpawnHelper.Info spawnInfo;
    private int count;
    private long lastMobSpawningTime;

    /**
     * Stops chunk random ticking and mob spawning if they are outside of the tick distance.
     * This does not affect redstone and block updates.
     */

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", ordinal = 0))
    private <T> void onlyTickActiveChunks(List<ChunkHolder> list, Consumer<? super T> action) {
        var chunkStorage = (ThreadedAnvilChunkStorageAccessor) this.threadedAnvilChunkStorage;
        if (ServerSettings.tick_utils_tick_distance >= 0) {
            if (this.count++ % 20 == 0) {
                // Add active chunks
                for (ChunkHolder holder : chunkStorage.getChunkHolders().values()) {
                    Optional<WorldChunk> optional = holder.getTickingFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK).left();
                    if (optional.isPresent()) {
                        if (TickManager.shouldTick(holder.getPos(), this.world)) {
                            this.active.add(holder);
                        } else {
                            // Sends block updates to clients from inactive chunks.
                            holder.flushUpdates(optional.get());
                        }
                    }
                }

                // Remove inactive chunks
                this.active.removeIf(holder -> holder.getTickingFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK).left().isEmpty() || !TickManager.shouldTick(holder.getPos(), this.world));
            }
        }

        long worldTime = this.world.getTime();
        long timeDifference = worldTime - this.lastMobSpawningTime;
        this.lastMobSpawningTime = worldTime;
        var rules = this.world.getGameRules();
        boolean rareSpawn = this.world.getLevelProperties().getTime() % 400L == 0L;
        for (ChunkHolder holder : ServerSettings.tick_utils_tick_distance >= 0 ? this.active : chunkStorage.getChunkHolders().values()) {
            var chunk = holder.getTickingFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK).left().orElse(null);
            if (chunk != null) {
                var pos = chunk.getPos();
                if (this.world.method_37115(pos) && !chunkStorage.tooFarFromPlayersToSpawnMobs(pos)) {
                    chunk.setInhabitedTime(chunk.getInhabitedTime() + timeDifference);
                    if (rules.getBoolean(GameRules.DO_MOB_SPAWNING) && (this.spawnMonsters || this.spawnAnimals) && this.world.getWorldBorder().contains(pos)) {
                        SpawnHelper.spawn(this.world, chunk, this.spawnInfo, this.spawnAnimals, this.spawnMonsters, rareSpawn);
                    }
                    this.world.tickChunk(chunk, rules.getInt(GameRules.RANDOM_TICK_SPEED));
                }
                this.world.getProfiler().push("broadcast");
                holder.flushUpdates(chunk);
                this.world.getProfiler().pop();
            }
        }
    }

    /**
     * Stops minecraft from initializing a massive list of ChunkHolders, as we already get those directly from TACS.
     * The drawback is that it will also stop the order of ticking chunks from being shuffled.
     * This is an unnoticeable change besides maybe specific complex redstone contraptions that can detect it.
     */

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;entryIterator()Ljava/lang/Iterable;"))
    private Iterable<ChunkHolder> emptyList(ThreadedAnvilChunkStorage threadedAnvilChunkStorage) {
        return Collections.emptyList();
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Ljava/util/Collections;shuffle(Ljava/util/List;)V"))
    private void cancelShuffle(List<?> list) {
    }

    // Chunk flushing is already done in the iteration above, because these iterations are expensive.
    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", ordinal = 1))
    private <T> void cancelChunkFlushing(List<ChunkHolder> list, Consumer<? super T> action) {
    }
}