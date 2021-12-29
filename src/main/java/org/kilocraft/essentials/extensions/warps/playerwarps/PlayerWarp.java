package org.kilocraft.essentials.extensions.warps.playerwarps;

import org.jetbrains.annotations.NotNull;
import org.kilocraft.essentials.api.world.location.Location;
import org.kilocraft.essentials.extensions.warps.Warp;
import org.kilocraft.essentials.util.nbt.NBTUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public class PlayerWarp extends Warp implements Comparable<PlayerWarp> {
    private UUID owner;
    private String type;
    private String description;

    public PlayerWarp(String name, Location location, UUID owner, String type, String description) {
        super(name, location);
        this.owner = owner;
        this.type = type;
        this.description = description;
    }

    public PlayerWarp(String name, CompoundTag tag) {
        super(name, null);
        this.fromTag(tag);
    }

    public String getType() {
        return this.type;
    }

    public UUID getOwner() {
        return this.owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag NbtCompound = super.toTag();
        NbtCompound.putString("type", this.type);
        NbtCompound.putString("desc", this.description);
        NBTUtils.putUUID(NbtCompound, "owner", this.owner);

        return NbtCompound;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        super.fromTag(tag);
        this.type = tag.getString("type");
        this.description = tag.getString("desc");
        this.owner = NBTUtils.getUUID(tag, "owner");
    }

    @Override
    public int compareTo(@NotNull PlayerWarp o) {
        return this.getName().compareTo(o.getName());
    }

    public static class Type {
        private static final List<String> types = new ArrayList<>();
        private final String name;

        public Type(String name) {
            this.name = name;
            types.add(name);
        }

        public static void add(String type) {
            types.add(type);
        }

        public static List<String> getTypes() {
            return types;
        }

        public static boolean isValid(String name) {
            return types.contains(name);
        }

        public String getName() {
            return this.name;
        }
    }
}
