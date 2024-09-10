package dev.lucaargolo.mekanismcovers.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityCollisionContext.class)
public interface EntityCollisionContextAccessor {

    @Accessor
    ItemStack getHeldItem();

}
