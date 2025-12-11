package com.github.b4ndithelps.trutils.mixin;

import com.github.b4ndithelps.trutils.Trutils;
import com.github.b4ndithelps.trutils.config.TrutilsConfig;
import com.github.manasmods.manascore.api.skills.ManasSkillInstance;
import com.github.manasmods.manascore.capability.skill.EntitySkillCapabilityStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = EntitySkillCapabilityStorage.class, remap = false)
public class EntitySkillCapabilityStorageMixin {

    @Shadow
    private @Nullable Entity owner;

    @Inject(
            method = "learnSkill",
            at = @At("HEAD"),
            cancellable = true
    )
    private void trutils$maybeCancelLearnSkill(ManasSkillInstance instance, CallbackInfoReturnable<Boolean> cir) {
        Optional<ResourceLocation> maybeId = Optional.of(instance.getSkillId());
        if (TrutilsConfig.isSkillBlocked(maybeId.get())) {
            // Allow if this entity's UUID is explicitly bypassed
            if (owner != null && TrutilsConfig.isUuidBypassed(owner.getUUID())) {
                return;
            }
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

}
