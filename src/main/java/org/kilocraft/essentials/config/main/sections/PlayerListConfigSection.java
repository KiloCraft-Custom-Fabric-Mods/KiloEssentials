package org.kilocraft.essentials.config.main.sections;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class PlayerListConfigSection {

    @Setting(value = "useNickNames", comment = "If set to true the server will attempt to load the player nicknames in the list instead of their username")
    public boolean useNicknames = false;

    @Setting(value = "header", comment = "Sets the (Tab) PlayerList Header")
    public List<String> header = new ArrayList<String>(){{
        add("&aMinecraft Server");
        add("&7Welcome &6%USER_DISPLAYNAME%&r");
    }};

    @Setting(value = "footer", comment = "Sets the (Tab) PlayerList Footer")
    public List<String> footer = new ArrayList<String>(){{
        add("&7Ping: %PLAYER_FORMATTED_PING% &8-&7 Online: &b%SERVER_PLAYER_COUNT% &8-&7 &7TPS: &r%SERVER_FORMATTED_TPS%");
        add("&7Use &3/help&7 for more info");
    }};

    public String getHeader() {
        StringBuilder str = new StringBuilder();
        for (String s : header) str.append("\n").append(s);
        return str.toString();
    }

    public String getFooter() {
        StringBuilder str = new StringBuilder();
        for (String s : footer) str.append("\n").append(s);
        return str.toString();
    }

}
