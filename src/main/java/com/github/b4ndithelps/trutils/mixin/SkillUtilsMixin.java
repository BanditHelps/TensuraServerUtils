package com.github.b4ndithelps.trutils.mixin;

import com.github.b4ndithelps.trutils.Trutils;
import com.github.b4ndithelps.trutils.config.TrutilsConfig;
import com.github.manasmods.manascore.api.skills.ManasSkill;
import com.github.manasmods.manascore.api.skills.ManasSkillInstance;
import com.github.manasmods.manascore.api.skills.SkillAPI;
import com.github.manasmods.manascore.api.skills.capability.SkillStorage;
import com.github.manasmods.tensura.ability.SkillUtils;
import com.github.manasmods.tensura.ability.TensuraSkillInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = SkillUtils.class, remap = false)
public class SkillUtilsMixin {

    @Inject(
            method = "learnSkill(Lnet/minecraft/world/entity/LivingEntity;Lcom/github/manasmods/manascore/api/skills/ManasSkill;I)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void trutils$dontLearnBadSkillCommand(LivingEntity entity, ManasSkill skill, int removeTime, CallbackInfoReturnable<Boolean> cir) {
		TensuraSkillInstance instance = new TensuraSkillInstance(skill);
		Optional<ResourceLocation> maybeId = Optional.of(instance.getSkillId());

		if (TrutilsConfig.isSkillBlocked(maybeId.get())) {
			// Allow if this entity's UUID is explicitly bypassed
			if (entity != null && TrutilsConfig.isUuidBypassed(entity.getUUID())) {
				return;
			}
			cir.setReturnValue(false);
			cir.cancel();
		}
    }

	@Inject(
			method = "learnSkill(Lnet/minecraft/world/entity/LivingEntity;Lcom/github/manasmods/manascore/api/skills/ManasSkillInstance;)Z",
			at = @At("HEAD"),
			cancellable = true
	)
	private static void trutils$dontLearnBadSkill(LivingEntity entity, ManasSkillInstance skill, CallbackInfoReturnable<Boolean> cir) {
		Optional<ResourceLocation> maybeId = Optional.of(skill.getSkillId());
		if (TrutilsConfig.isSkillBlocked(maybeId.get())) {
			// Allow if this entity's UUID is explicitly bypassed
			if (entity != null && TrutilsConfig.isUuidBypassed(entity.getUUID())) {
				return;
			}
			cir.setReturnValue(false);
			cir.cancel();
		}
	}
}