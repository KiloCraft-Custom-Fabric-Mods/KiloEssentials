package org.kilocraft.essentials.api.world;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ParticleAnimation {
    private List<ParticleFrame<?>> particleFrames;
    private Identifier id;
    private String name;

    public ParticleAnimation(Identifier id, String name) {
        this.particleFrames = new ArrayList<>();
        this.id = id;
        this.name = name;
    }

    public Identifier getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ParticleAnimation append(ParticleFrame<?> frame) {
        particleFrames.add(frame);
        return this;
    }

    public List<ParticleFrame<?>> getFrames() {
        return particleFrames;
    }

    public int frames() {
        return particleFrames.size();
    }
}
