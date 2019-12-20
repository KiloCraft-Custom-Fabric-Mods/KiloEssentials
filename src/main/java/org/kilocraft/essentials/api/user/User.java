package org.kilocraft.essentials.api.user;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.kilocraft.essentials.api.feature.FeatureType;
import org.kilocraft.essentials.api.feature.UserProvidedFeature;
import org.kilocraft.essentials.user.UserHomeHandler;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface User {
    UUID getUuid();

    String getUsername();

    boolean isOnline();

    boolean hasNickname();

    String getDisplayname();

    Text getRankedDisplayname();

    List<String> getSubscriptionChannels();

    String getUpstreamChannelId();

    Optional<String> getNickname();

    void setNickname(String name);

    void clearNickname();

    @Nullable
    Identifier getBackDimId();

    @Nullable
    Vec3d getBackPos();

    void setBackPos(Vec3d position);

    default void setBackPos(BlockPos blockPos) {
        setBackPos(new Vec3d(blockPos));
    }

    default void setBackPos(double x, double y, double z) {
        setBackPos(new Vec3d(x, y, z));
    }

    void setBackDim(Identifier dim);

    Identifier getPosDim();

    @Nullable
    Vec3d getPos();

    boolean canFly();

    void setFlight(boolean set);

    boolean isSocialSpyOn();

    void setSocialSpyOn(boolean on);

    boolean isCommandSpyOn();

    void setCommandSpyOn(boolean on);

    boolean hasJoinedBefore();

    @Nullable
    Date getFirstJoin();

    void addSubscriptionChannel(String id);

    void removeSubscriptionChannel(String id);

    void setUpstreamChannelId(String id);

    boolean isInvulnerable();

    void setInvulnerable(boolean set);

    int getRTPsLeft();

    void setRTPsLeft(int amount);

    @Nullable
    UUID getLastPrivateMessageSender();

    @Nullable
    String getLastPrivateMessage();

    void setLastMessageSender(UUID uuid);

    void setLastPrivateMessage(String message);

    <F extends UserProvidedFeature> F feature(FeatureType<F> type);

    UserHomeHandler getHomesHandler();

    /**
     * This should be moved to it's own FeatureType
     * @return
     */
    @Deprecated
    int getDisplayParticleId();

    /**
     * This should be moved to it's own FeatureType
     * @return
     */
    @Deprecated
    void setDisplayParticleId(int i);
}
