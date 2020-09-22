package me.steven.indrev.mixin;

import com.google.common.collect.Multimap;
import me.steven.indrev.api.AttributeModifierProvider;
import me.steven.indrev.items.armor.IRModularArmor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.reborn.energy.Energy;

import java.util.Random;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {

    @Inject(method = "getAttributeModifiers", at = @At("TAIL"), cancellable = true)
    private void indrev_modifiableAttributeModifiers(EquipmentSlot equipmentSlot, CallbackInfoReturnable<Multimap<EntityAttribute, EntityAttributeModifier>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof AttributeModifierProvider) {
            cir.setReturnValue(((AttributeModifierProvider) stack.getItem()).getAttributeModifiers(stack, equipmentSlot));
        }
    }

    @Inject(method = "damage(ILjava/util/Random;Lnet/minecraft/server/network/ServerPlayerEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void indrev_useArmorEnergy(int amount, Random random, ServerPlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof IRModularArmor) {
            if (Energy.valid(stack)) {
                Energy.of(stack).extract(amount);
            }
            cir.setReturnValue(false);
        }
    }
}
