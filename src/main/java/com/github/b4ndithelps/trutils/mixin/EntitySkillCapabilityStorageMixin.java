package com.github.b4ndithelps.trutils.mixin;

import com.github.b4ndithelps.trutils.Trutils;
import com.github.b4ndithelps.trutils.config.TrutilsConfig;
import com.github.manasmods.manascore.api.skills.ManasSkillInstance;
import com.github.manasmods.manascore.capability.skill.EntitySkillCapabilityStorage;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = EntitySkillCapabilityStorage.class, remap = false)
public class EntitySkillCapabilityStorageMixin {

    @Inject(
            method = "learnSkill",
            at = @At("HEAD"),
            cancellable = true
    )
    private void trutils$maybeCancelLearnSkill(ManasSkillInstance instance, CallbackInfoReturnable<Boolean> cir) {
        Optional<ResourceLocation> maybeId = Optional.of(instance.getSkillId());
        if (TrutilsConfig.isSkillBlocked(maybeId.get())) {
            Trutils.LOGGER.info("TRUtils: blocking learning of skill {}", maybeId.get());
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

}
