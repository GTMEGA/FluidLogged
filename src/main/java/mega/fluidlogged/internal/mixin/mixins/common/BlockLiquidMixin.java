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

import mega.fluidlogged.internal.FLUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

@Mixin(BlockLiquid.class)
public abstract class BlockLiquidMixin {
    @Redirect(method = "func_149804_e",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"),
              require = 1)
    private Block hijackGetBlock(World world, int x, int y, int z) {
        return FLUtil.getFluidOrBlock(world, x, y, z);
    }

    @Redirect(method = "func_149804_e",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"),
              require = 1)
    private int hijackGetMeta(World world, int x, int y, int z) {
        return FLUtil.getFluidMeta(world, x, y, z);
    }

    @Redirect(method = "getEffectiveFlowDecay",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/IBlockAccess;getBlock(III)Lnet/minecraft/block/Block;"),
              require = 1)
    private Block hijackGetBlockDecay(IBlockAccess instance, int x, int y, int z) {
        return FLUtil.getFluidOrBlock(instance, x, y, z);
    }

    @Redirect(method = "getEffectiveFlowDecay",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/IBlockAccess;getBlockMetadata(III)I"),
              require = 1)
    private int hijackMetaDecay(IBlockAccess instance, int x, int y, int z) {
        return FLUtil.getFluidMeta(instance, x, y, z);
    }

    @Redirect(method = "func_149805_n",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"),
              require = 6)
    private Block hijackGetBlockLavaSolidify(World world, int x, int y, int z) {
        return FLUtil.getFluidOrBlock(world, x, y, z);
    }
}
