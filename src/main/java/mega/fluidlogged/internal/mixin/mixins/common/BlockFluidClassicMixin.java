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

import com.llamalad7.mixinextras.sugar.Local;
import lombok.val;
import mega.fluidlogged.api.FLBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;

@Mixin(BlockFluidClassic.class)
public abstract class BlockFluidClassicMixin extends BlockFluidBase {
    public BlockFluidClassicMixin(Fluid fluid, Material material) {
        super(fluid, material);
    }

    @Redirect(method = "updateTick",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"),
              require = 1)
    private int metaOnlyIfNotLogged(World instance, int x, int y, int z) {
        if (instance.getBlock(x, y, z) != this) {
            return 0;
        }
        return instance.getBlockMetadata(x, y, z);
    }

    @Redirect(method = "updateTick",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/World;setBlockMetadataWithNotify(IIIII)Z",
                       ordinal = 1),
              require = 1)
    private boolean setBlockOnlyIfNotLogged(World instance, int x, int y, int z, int meta, int flag) {
        if (instance.getBlock(x, y, z) != this) {
            return false;
        }
        return instance.setBlockMetadataWithNotify(x, y, z, meta, flag);
    }

    @ModifyConstant(method = "getQuantaValue",
                    constant = @Constant(intValue = -1),
                    remap = false,
                    require = 1)
    private int hackGetBlock(int constant,
                             @Local(argsOnly = true) IBlockAccess world,
                             @Local(ordinal = 0,
                                    argsOnly = true) int x,
                             @Local(ordinal = 1,
                                    argsOnly = true) int y,
                             @Local(ordinal = 2,
                                    argsOnly = true) int z) {
        if (!(world instanceof FLBlockAccess)) {
            return constant;
        }
        val fluid = ((FLBlockAccess)world).fl$getFluid(x, y, z);
        if (fluid == null) {
            return constant;
        }

        val fluidBlock = fluid.getBlock();
        if (fluidBlock != this) {
            return constant;
        }

        return quantaPerBlock;
    }
}
