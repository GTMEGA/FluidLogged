/*
 * This file is part of FluidLogged.
 *
 * Copyright (C) 2025 The MEGA Team, FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice, this permission notice and the word "MEGA"
 * shall be included in all copies or substantial portions of the Software.
 *
 * FluidLogged is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * FluidLogged is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FluidLogged.  If not, see <https://www.gnu.org/licenses/>.
 */

package mega.fluidlogged.internal.mixin.mixins.common.compat.cofh;

import cofh.core.item.ItemBucket;
import com.llamalad7.mixinextras.sugar.Local;
import mega.fluidlogged.internal.FLUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

@Mixin(ItemBucket.class)
public abstract class ItemBucketMixin {
    @SuppressWarnings("DiscouragedShift")
    @Inject(method = "onItemRightClick",
            at = @At(value = "INVOKE_ASSIGN",
                     target = "Lcofh/core/item/ItemBucket;getMovingObjectPositionFromPlayer(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Z)Lnet/minecraft/util/MovingObjectPosition;",
                     shift = At.Shift.AFTER),
            cancellable = true,
            require = 1)
    private void fireBucketEvent(ItemStack item, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir, @Local MovingObjectPosition pos) {
        FLUtil.fireBucketEvent(item, world, player, cir::setReturnValue, pos);
    }
}
