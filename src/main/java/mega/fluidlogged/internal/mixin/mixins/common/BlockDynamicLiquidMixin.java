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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.world.World;

@Mixin(BlockDynamicLiquid.class)
public abstract class BlockDynamicLiquidMixin {
    @Shadow protected abstract void func_149811_n(World p_149811_1_, int p_149811_2_, int p_149811_3_, int p_149811_4_);

    @Redirect(method = "updateTick",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/block/BlockDynamicLiquid;func_149811_n(Lnet/minecraft/world/World;III)V"),
              require = 2)
    private void safeMakeStatic(BlockDynamicLiquid instance, World world, int x, int y, int z) {
        if (world.getBlock(x, y, z) != instance) {
            return;
        }
        ((BlockDynamicLiquidMixin)(Object)instance).func_149811_n(world, x, y, z);
    }
}
