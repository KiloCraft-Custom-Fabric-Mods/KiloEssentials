package org.kilocraft.essentials.config.main;

import com.google.common.collect.Lists;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.kilocraft.essentials.config.main.sections.*;
import org.kilocraft.essentials.config.main.sections.chat.ChatConfigSection;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class Config {
    public static final String HEADER = "KiloEssentials! Main Configuration File\n" +
            "Licensed Under the MIT License, Copyright (c) 2020 KiloCraft\n" +
            "KiloEssentials is using HOCON for its configuration files\n learn more about it here: " +
            "https://docs.spongepowered.org/stable/en/server/getting-started/configuration/hocon.html" +
            "\nYou can use Color Codes in string parameters, the character is \"&\" " +
            "More info at: https://minecraft.tools/en/color-code.php \ne.g: \"&eThe Yellow Thing\" will be yellow";

    @Setting(value = "autoUserUpgrade", comment = "Automatically update the user data on the startup")
    public boolean autoUserUpgrade = true;

    @Setting(value = "server")
    private final ServerConfigSection serverSection = new ServerConfigSection();

    @Setting(value = "permissionManager", comment = "Select the PermissionManager to use! Options: vanilla, luckperms")
    private final String permissionManager = "luckperms";

    @Setting(value = "world")
    private final WorldConfigSection worldSection = new WorldConfigSection();

    @Setting(value = "playerList", comment = "The appearance of the Player list.\n" +
            "Variables: User: %PLAYER_NAME%, %PLAYER_DISPLAYNAME%, %PLAYER_PING%, %PLAYER_FORMATTED_PING%, %USER_DISPLAYNAME%\n" +
            "Server: %SERVER_NAME%, %SERVER_TPS%, %SERVER_FORMATTED_TPS%, %SERVER_PLAYER_COUNT%, %SERVER_MEMORY_MAX%," +
            " %SERVER_MEMORY_USAGE_PERCENTAGE%, %SERVER_FORMATTED_MEMORY_USAGE_PERCENTAGE%, %SERVER_MEMORY_USAGE_MB%")
    private final PlayerListConfigSection playerListSection = new PlayerListConfigSection();

    @Setting(value = "features", comment = "A Set of features you want to enable")
    private final FeaturesConfigSection featuresSection = new FeaturesConfigSection();

    @Setting(value = "chat", comment = "Configure the appearance of the Chat Channels")
    private final ChatConfigSection chatSection = new ChatConfigSection();

    @Setting("moderation")
    private final ModerationConfigSection moderationSection = new ModerationConfigSection();

    @Setting(value = "rtpSpecs", comment = "Configure the RTP range")
    private final RtpSpecsConfigSection rtpSpecs = new RtpSpecsConfigSection();

    @Setting(value = "motd", comment = "Configure the message of the day")
    private final MotdConfigSection motd = new MotdConfigSection();

    @Setting(value = "homesLimit", comment = "The maximum amount of homes a player can set")
    public int homesLimit = 10;

    @Setting(value = "playerWarpTypes", comment = "The Types that you can select for making a player warp")
    public List<String> playerWarpTypes = new ArrayList<>() {{
        this.add("shop");
        this.add("farm");
        this.add("build");
        this.add("biome");
        this.add("misc");
        this.add("others");
    }};

    @Setting(value = "commandSpyIgnored", comment = "The commands that the command logger will ignore")
    public List<String> ignoredCommandsForLogging = new ArrayList<>() {{
        this.add("trigger");
        this.add("msg");
        this.add("tell");
        this.add("whisper");
        this.add("r");
        this.add("reply");
        this.add("staffmsg");
        this.add("sm");
        this.add("buildermsg");
        this.add("hug");
    }};

    @Setting(value = "nicknameMaxLength", comment = "The maximum length for a nickname")
    public int nicknameMaxLength = 35;

    public ServerConfigSection server() {
        return this.serverSection;
    }

    public WorldConfigSection world() {
        return this.worldSection;
    }

    @Setting(value = "disabledItems", comment = "Put the identifier of an item here to disable modifyitem on it!")
    public List<String> disabledItems = Lists.newArrayList("minecraft:barrier");

    public PlayerListConfigSection playerList() {
        return this.playerListSection;
    }

    public FeaturesConfigSection features() {
        return this.featuresSection;
    }

    public ChatConfigSection chat() {
        return this.chatSection;
    }

    public ModerationConfigSection moderation() {
        return this.moderationSection;
    }

    public RtpSpecsConfigSection rtpSpecs() {
        return this.rtpSpecs;
    }

    public MotdConfigSection motd() {
        return this.motd;
    }

}
