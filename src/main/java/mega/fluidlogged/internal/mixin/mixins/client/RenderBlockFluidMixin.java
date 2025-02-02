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

package mega.fluidlogged.internal.mixin.mixins.client;

import com.llamalad7.mixinextras.sugar.Local;
import lombok.val;
import mega.fluidlogged.internal.mixin.hook.FLBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.RenderBlockFluid;

@Mixin(value = RenderBlockFluid.class,
       remap = false)
public abstract class RenderBlockFluidMixin {
    @Redirect(method = "getFluidHeightForRender",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/IBlockAccess;getBlock(III)Lnet/minecraft/block/Block;"),
              require = 4)
    private Block fluidHeightGetBlock(IBlockAccess world, int x, int y, int z) {
        val fluid = ((FLBlockAccess)world).fl$getFluid(x, y, z);
        val blockFluid = fluid == null ? null : fluid.getBlock();
        if (blockFluid == null) {
            return world.getBlock(x, y, z);
        }
        return blockFluid;
    }

    @Redirect(method = "getFluidHeightForRender",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/IBlockAccess;getBlockMetadata(III)I"),
              require = 1)
    private int fluidHeightGetBlockMeta(IBlockAccess world, int x, int y, int z, @Local(argsOnly = true) BlockFluidBase inputBlock) {
        val fluid = ((FLBlockAccess)world).fl$getFluid(x, y, z);
        val blockFluid = fluid == null ? null : fluid.getBlock();
        if (blockFluid == null) {
            return world.getBlockMetadata(x, y, z);
        }
        return inputBlock.getMaxRenderHeightMeta();
    }

    @Redirect(method = "getFluidHeightForRender",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraftforge/fluids/BlockFluidBase;getQuantaPercentage(Lnet/minecraft/world/IBlockAccess;III)F"),
              require = 1)
    private float fluidHeightQuanta(BlockFluidBase instance, IBlockAccess world, int x, int y, int z) {
        if (((FLBlockAccess)world).fl$isFluidLogged(x, y, z, null)) {
            return 1;
        }
        return instance.getQuantaPercentage(world, x, y, z);
    }
}
