package org.kilocraft.essentials.mixin.patch.performance.entityActivationRange;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.kilocraft.essentials.patch.entityActivationRange.IGoalSelector;
import org.kilocraft.essentials.patch.entityActivationRange.InactiveEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity implements InactiveEntity {

    @Shadow
    @Final
    protected GoalSelector goalSelector;

    @Shadow
    @Final
    protected GoalSelector targetSelector;

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void inactiveTick() {
        ++this.despawnCounter;
        if (((IGoalSelector) this.goalSelector).inactiveTick()) {
            this.goalSelector.tick();
        }
        if (((IGoalSelector) this.targetSelector).inactiveTick()) {
            this.targetSelector.tick();
        }
    }
}

