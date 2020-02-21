package org.kilocraft.essentials.user;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kilocraft.essentials.EssentialPermission;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.chat.TextFormat;
import org.kilocraft.essentials.api.feature.FeatureType;
import org.kilocraft.essentials.api.feature.UserProvidedFeature;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.api.user.User;
import org.kilocraft.essentials.api.world.location.Location;
import org.kilocraft.essentials.api.world.location.Vec3dLocation;
import org.kilocraft.essentials.chat.channels.GlobalChat;
import org.kilocraft.essentials.config.KiloConfig;
import org.kilocraft.essentials.util.NBTTypes;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author CODY_AI
 * An easy way to handle the User (Instance of player)
 *
 * @see ServerUserManager
 * @see UserHomeHandler
 */

public class ServerUser implements User {
    protected static ServerUserManager manager = (ServerUserManager) KiloServer.getServer().getUserManager();
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    UUID uuid;
    String name = "";
    private UserHomeHandler homeHandler;
    private Vec3dLocation location;
    private Vec3dLocation lastLocation;
    private String nickname;
    private boolean canFly = false;
    private boolean invulnerable = false;
    private UUID lastPrivateMessageGetterUUID;
    private String lastPrivateMessageText = "";
    private boolean hasJoinedBefore = true;
    private Date firstJoin = new Date();
    private int randomTeleportsLeft = 3;
    public int messageCooldown;
    private List<String> subscriptions;
    private String upstreamChannelId;
    private boolean socialSpy = false;
    private boolean commandSpy = false;
    private boolean canSit = false;
    private Map<String, UUID> ignoreList;
    boolean isStaff = false;
    String lastSocketAddress;
    GameMode gameMode = GameMode.NOT_SET;
    int ticksPlayed = 0;

    public ServerUser(UUID uuid) {
        this.uuid = uuid;
        if (UserHomeHandler.isEnabled()) // TODO Use new feature provider in future
            this.homeHandler = new UserHomeHandler(this);
        try {
            manager.getHandler().handleUser(this);
        } catch (IOException e) {
            KiloEssentials.getLogger().error("Failed to Load User Data [" + uuid.toString() + "]");
            e.printStackTrace();
        }

        this.subscriptions = new ArrayList<>();
    }

    protected CompoundTag serialize() {
        CompoundTag mainTag = new CompoundTag();
        CompoundTag metaTag = new CompoundTag();
        CompoundTag cacheTag = new CompoundTag();

        // Here we store the players current location
        if (this.location != null) {
            mainTag.put("loc", this.location.toTag());
            this.location.shortDecimals();
        }

        if (this.lastLocation != null)
            cacheTag.put("lastLoc", this.lastLocation.toTag());

        // Private messaging stuff
        if (this.getLastPrivateMessageSender() != null) {
            CompoundTag lastMessageTag = new CompoundTag();
            lastMessageTag.putString("destUUID", this.getLastPrivateMessageSender().toString());
            if (this.getLastPrivateMessage() != null) {
                lastMessageTag.putString("text", this.getLastPrivateMessage());
            }
            cacheTag.put("lastMessage", lastMessageTag);
        }

        // Chat channels stuff
        CompoundTag channelsCache = new CompoundTag();

        if (this.upstreamChannelId != null) {
            channelsCache.putString("upstreamChannelId", this.upstreamChannelId);
            cacheTag.put("channels", channelsCache);
        }

        if (this.lastSocketAddress != null) {
            cacheTag.putString("lIP", this.lastSocketAddress);
        }

        if (this.gameMode != GameMode.NOT_SET) {
            cacheTag.putInt("gameMode", this.gameMode.getId());
        }

        // Abilities
        if (this.canFly)
            cacheTag.putBoolean("isFlyEnabled", true);

        if (this.invulnerable)
            cacheTag.putBoolean("isInvulnerable", true);

        if (this.socialSpy)
            cacheTag.putBoolean("socialSpy", true);

        if (this.socialSpy)
            cacheTag.putBoolean("commandSpy", true);

        if (this.canSit)
            cacheTag.putBoolean("canSit", true);

        if (this.ignoreList != null) {
            ListTag listTag = new ListTag();
            this.ignoreList.forEach((name, uuid) -> {
                CompoundTag ignoredOne = new CompoundTag();
                ignoredOne.putUuid("uuid", uuid);
                ignoredOne.putString("name", name);

                System.out.println("Saving ignored: " + name);

                listTag.add(ignoredOne);
            });

            cacheTag.put("ignored", listTag);
        }

        metaTag.putBoolean("hasJoinedBefore", this.hasJoinedBefore);
        metaTag.putString("firstJoin", dateFormat.format(this.firstJoin));

        if (this.ticksPlayed != -1)
            metaTag.putInt("ticksPlayed", this.ticksPlayed);

        if (this.nickname != null) // Nicknames are Optional now.
            metaTag.putString("nick", this.nickname);

        if (this.isStaff)
            metaTag.putBoolean("isStaff", true);

        // Home logic, TODO Abstract this with features in future.
        CompoundTag homeTag = new CompoundTag();
        this.homeHandler.serialize(homeTag);
        mainTag.put("homes", homeTag);

        // Misc stuff now.
        mainTag.putInt("rtpLeft", this.randomTeleportsLeft);
        mainTag.put("meta", metaTag);
        mainTag.put("cache", cacheTag);
        mainTag.putString("name", this.name);
        return mainTag;
    }

    protected void deserialize(@NotNull CompoundTag compoundTag) {
    	CompoundTag metaTag = compoundTag.getCompound("meta");
        CompoundTag cacheTag = compoundTag.getCompound("cache");

        if (cacheTag.contains("lastLoc")) {
            this.lastLocation = Vec3dLocation.dummy();
            this.lastLocation.fromTag(cacheTag.getCompound("lastLoc"));
        }

        if (compoundTag.contains("loc")) {
        	this.location = Vec3dLocation.dummy();
        	this.location.fromTag(compoundTag.getCompound("loc"));
        	this.location.shortDecimals();
        }

        if (cacheTag.contains("lastMessage", NBTTypes.COMPOUND)) {
            CompoundTag lastMessageTag = cacheTag.getCompound("lastMessage");
            if (lastMessageTag.contains("destUUID", NBTTypes.STRING))
                this.lastPrivateMessageGetterUUID = UUID.fromString(lastMessageTag.getString("destUUID"));

            if (lastMessageTag.contains("text", NBTTypes.STRING))
                this.lastPrivateMessageText = lastMessageTag.getString("text");

        }

        if (cacheTag.contains("channels", NBTTypes.COMPOUND)) {
            CompoundTag channelsTag = cacheTag.getCompound("channels");

            if (channelsTag.contains("upstreamChannelId", NBTTypes.STRING))
                this.upstreamChannelId = channelsTag.getString("upstreamChannelId");
            else
                this.upstreamChannelId = GlobalChat.getChannelId();
        }

        if (cacheTag.contains("lIP")) {
            this.lastSocketAddress = cacheTag.getString("lIP");
        }

        if (cacheTag.contains("gameMode")) {
            this.gameMode = GameMode.byId(cacheTag.getInt("gameMode"));
        }

        if (cacheTag.getBoolean("isFlyEnabled")) {
            this.canFly = true;
        }
        
        if (cacheTag.getBoolean("isInvulnerable")) {
            this.invulnerable = true;
        }

        if (cacheTag.contains("socialSpy"))
            this.socialSpy = cacheTag.getBoolean("socialSpy");
        if (cacheTag.contains("commandSpy"))
            this.commandSpy = cacheTag.getBoolean("commandSpy");

        if (cacheTag.contains("canSit"))
            this.canSit = cacheTag.getBoolean("canSit");

        ListTag ignoreList = cacheTag.getList("ignored", 10);
        if (ignoreList != null && !ignoreList.isEmpty()) {
            this.ignoreList = new HashMap<>();
            for (int i = 0; i < ignoreList.size(); i++) {
                CompoundTag ignoredOne = ignoreList.getCompound(i);
                this.ignoreList.put(ignoredOne.getString("name"), ignoredOne.getUuid("uuid"));
            }
        }

        this.hasJoinedBefore = metaTag.getBoolean("hasJoinedBefore");
        this.firstJoin = getUserFirstJoinDate(metaTag.getString("firstJoin"));

        if (metaTag.contains("ticksPlayed"))
            this.ticksPlayed = metaTag.getInt("ticksPlayed");

        if (metaTag.contains("nick")) // Nicknames are an Optional, so we compensate for that.
            this.nickname = metaTag.getString("nick");

        if (metaTag.contains("isStaff"))
            this.isStaff = true;

        this.homeHandler.deserialize(compoundTag.getCompound("homes"));
        this.randomTeleportsLeft = compoundTag.getInt("rtpLeft");
    }

    public void updateLocation() {
        if (this instanceof OnlineUser && ((OnlineUser) this).getPlayer().getPos() != null) {
            this.location = Vec3dLocation.of((OnlineUser) this).shortDecimals();
        }
    }

    private Date getUserFirstJoinDate(String stringToParse) {
        Date date = new Date();
        try {
            date = dateFormat.parse(stringToParse);
        } catch (ParseException e) {
            //Pass, this is the first time that user is joined.
        }
        return date;
    }

    public UserHomeHandler getHomesHandler() {
        return this.homeHandler;
    }

    @Nullable
    @Override
    public String getLastSocketAddress() {
        return this.lastSocketAddress;
    }

    @Override
    public GameMode getGameMode() {
        return this.gameMode;
    }

    @Override
    public void setGameMode(GameMode mode) {
        this.gameMode = mode;

        if (this.isOnline())
            ((OnlineUser) this).getPlayer().setGameMode(mode);
    }

    @Override
    public boolean canSit() {
        return this.canSit;
    }

    @Override
    public void setCanSit(boolean set) {
        this.canSit = set;
    }

    @Override
    public int getTicksPlayed() {
        return this.ticksPlayed;
    }

    @Override
    public void setTicksPlayed(int ticks) {
        this.ticksPlayed = ticks;
    }

    @Override
    public boolean isOnline() {
        return this instanceof OnlineUser || KiloServer.getServer().getUserManager().isOnline(this);
    }

    @Override
    public boolean hasNickname() {
        return this.getNickname().isPresent();
    }

    public String getDisplayName() {
        return (this.getNickname().isPresent() || this.nickname != null) ? this.nickname : this.name;
    }

    @Override
    public String getFormattedDisplayName() {
        return TextFormat.translate(getDisplayName() + "&r");
    }

    @Override
    public Text getRankedDisplayName() {
        return Team.modifyText(((OnlineUser) this).getPlayer().getScoreboardTeam(), new LiteralText(getFormattedDisplayName()));
    }

    @Override
    public Text getRankedName() {
        return Team.modifyText(((OnlineUser) this).getPlayer().getScoreboardTeam(), new LiteralText(this.name));
    }

    @Override
    public String getNameTag() {
        String str = this.isOnline() ? KiloConfig.messages().general().userTags().online :
                KiloConfig.messages().general().userTags().offline;
        return str.replace("{USER_NAME}", this.name)
                .replace("{USER_DISPLAYNAME}", this.getFormattedDisplayName());
    }

    @Override
    public List<String> getSubscriptionChannels() {
        return this.subscriptions;
    }

    @Override
    public String getUpstreamChannelId() {
        return (this.upstreamChannelId != null) ? this.upstreamChannelId : GlobalChat.getChannelId();
    }

    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public String getUsername() {
        return this.name;
    }

    @Override
    public Optional<String> getNickname() {
        return Optional.ofNullable(this.nickname);
    }

    @Override
    public Location getLocation() {
        if (this instanceof OnlineUser && this.location == null && ((OnlineUser) this).getPlayer() != null)
            updateLocation();

        return this.location;
    }

    @Nullable
    @Override
    public Location getLastSavedLocation() {
        return this.lastLocation;
    }

    @Override
    public void saveLocation() {
        if (this instanceof OnlineUser)
            this.lastLocation = Vec3dLocation.of((OnlineUser) this).shortDecimals();
    }

    @Override
    public void setNickname(String name) {
        String oldNick = this.nickname;
        this.nickname = name;
        KiloServer.getServer().getUserManager().onChangeNickname(this, oldNick); // This is to update the entries in UserManager.
    }

    @Override
    public void clearNickname() {
        KiloServer.getServer().getUserManager().onChangeNickname(this, null); // This is to update the entries in UserManager.
        this.nickname = null;
    }

    @Override
    public void setLastLocation(Location loc) {
        this.lastLocation = (Vec3dLocation) loc;
    }

    @Override
    public boolean canFly() {
        return this.canFly;
    }

    @Override
    public boolean isSocialSpyOn() {
        return this.socialSpy;
    }

    @Override
    public void setSocialSpyOn(boolean on) {
        this.socialSpy = on;
    }

    @Override
    public boolean isCommandSpyOn() {
        return this.commandSpy;
    }

    @Override
    public void setCommandSpyOn(boolean on) {
        this.commandSpy = on;
    }

    @Override
    public UUID getLastPrivateMessageSender() {
        return this.lastPrivateMessageGetterUUID;
    }

    @Override
    public String getLastPrivateMessage() {
        return this.lastPrivateMessageText;
    }

    @Override
    public boolean hasJoinedBefore() {
        return this.hasJoinedBefore;
    }

    @Override
    public Date getFirstJoin() {
        return this.firstJoin;
    }

    @Override
    public void setUpstreamChannelId(String id) {
        this.upstreamChannelId = id;
    }

    @Override
    public boolean isInvulnerable() {
        return this.invulnerable;
    }

    public int getRTPsLeft() {
    	return this.randomTeleportsLeft;
    }

    public void setRTPsLeft(int amount) {
        this.randomTeleportsLeft = amount;
    }

    public void setFlight(boolean set) {
        canFly = set;
    }

    public void setInvulnerable(boolean set) {
        this.invulnerable = set;
    }

    public void setLastMessageSender(UUID uuid) {
        this.lastPrivateMessageGetterUUID = uuid;
    }

    @Override
    public void setLastPrivateMessage(String message) {
        this.lastPrivateMessageText = message;
    }

    @Override
    public <F extends UserProvidedFeature> F feature(FeatureType<F> type) {
        return null; // TODO Impl
    }

    @Override
    public void saveData() throws IOException {
        if (!this.isOnline())
            manager.getHandler().saveData(this);
    }

    @Override
    public void trySave() throws CommandSyntaxException {
        if (this.isOnline())
            return;

        try {
            this.saveData();
        } catch (IOException e) {
            throw new SimpleCommandExceptionType(new LiteralText(e.getMessage()).formatted(Formatting.RED)).create();
        }
    }

    public Map<String, UUID> getIgnoreList() {
        if (this.ignoreList == null)
            this.ignoreList = new HashMap<>();

        return this.ignoreList;
    }

    public boolean isStaff() {
        if (this.isOnline())
            this.isStaff = KiloEssentials.hasPermissionNode(((OnlineUser) this).getCommandSource(), EssentialPermission.STAFF);

        return this.isStaff;
    }

    @SuppressWarnings({"untested", "Do Not Run If the User is Online"})
    public void clear() {
        if (this.isOnline())
            return;

        manager = null;
        dateFormat = null;
        uuid = null;
        name = null;
        homeHandler = null;
        location = null;
        lastLocation = null;
        nickname = null;
        lastPrivateMessageGetterUUID = null;
        lastPrivateMessageText = null;
        firstJoin = null;
        subscriptions = null;
        upstreamChannelId = null;
        ignoreList = null;
    }

    @Override
    public boolean equals(User anotherUser) {
        return anotherUser.getUuid().equals(this.uuid) || anotherUser.getUsername().equals(this.getUsername());
    }

    public static void saveLocationOf(ServerPlayerEntity player) {
        OnlineUser user = KiloServer.getServer().getOnlineUser(player);

        if (user != null) {
            user.saveLocation();
        }
    }

}