package org.kilocraft.essentials.config.main.sections.chat;

import net.minecraft.sound.SoundEvents;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ChatPingConfigSection {

    @Setting(value = "sound", comment = "The sound you hear when someone pings you")
    private final ChatPingSoundConfigSection chatPingSound = new ChatPingSoundConfigSection(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 3.0D, 1.0D);

    public ChatPingSoundConfigSection pingSound() {
        return this.chatPingSound;
    }
}
