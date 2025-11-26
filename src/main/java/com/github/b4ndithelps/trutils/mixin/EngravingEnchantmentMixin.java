package com.github.b4ndithelps.trutils.mixin;

import com.github.manasmods.tensura.enchantment.EngravingEnchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;
import java.util.Random;

@Mixin(value = EngravingEnchantment.class, remap = false)
public class EngravingEnchantmentMixin {

    @Inject(
            method = "getRandomEngravingFromList",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void trutils$addNullCheckToMethod(List<Enchantment> list, ItemStack stack, CallbackInfoReturnable<Enchantment> cir) {

        List<Enchantment> filtered = list.stream()
                .filter(Objects::nonNull)
                .filter(enchantment ->
                        enchantment.category.equals(EnchantmentCategory.WEAPON)
                                ? (!EnchantmentCategory.WEARABLE.canEnchant(stack.getItem()))
                                : enchantment.category.canEnchant(stack.getItem())
                )
                .toList();

        if(filtered.isEmpty()) {
            cir.setReturnValue(null);
            return;
        }

        cir.setReturnValue(filtered.get(new Random().nextInt(filtered.size())));
    }

}
