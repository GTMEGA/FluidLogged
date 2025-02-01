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

package mega.fluidlogged.internal.mixin.mixins.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import lombok.val;
import mega.fluidlogged.api.IFluid;
import mega.fluidlogged.internal.mixin.hook.FLBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidBlock;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @WrapOperation(method = "isInsideOfMaterial",
                   at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"),
                   require = 1)
    private Block checkFluidMaterial(World world, int x, int y, int z, Operation<Block> original, @Local(argsOnly = true) Material material, @Share("logged")LocalRef<IFluid> logged) {
        logged.set(null);
        val theBlock = original.call(world, x, y, z);
        if (theBlock.getMaterial() == material) {
            return theBlock;
        }
        val fluid = ((FLBlockAccess)world).fl$getFluid(x, y, z);
        if (fluid == null) {
            return theBlock;
        }
        val fluidBlock = fluid.toBlock();
        if (fluidBlock == null) {
            return theBlock;
        }
        logged.set(fluid);
        return fluidBlock;
    }

    @Redirect(method = "isInsideOfMaterial",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraftforge/fluids/IFluidBlock;getFilledPercentage(Lnet/minecraft/world/World;III)F"),
              require = 1)
    private float hackFilledPercentage(IFluidBlock instance, World world, int x, int y, int z) {
        // TODO
        return instance.getFilledPercentage(world, x, y, z);
    }

}
