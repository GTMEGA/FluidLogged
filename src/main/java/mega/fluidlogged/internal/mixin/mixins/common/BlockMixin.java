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

import lombok.val;
import mega.fluidlogged.internal.FLUtil;
import mega.fluidlogged.internal.mixin.hook.FLBlockAccess;
import mega.fluidlogged.internal.mixin.hook.FLBlockRoot;
import mega.fluidlogged.internal.mixin.hook.FLWorld;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.Random;

@Mixin(Block.class)
public abstract class BlockMixin implements FLBlockRoot {
    @Override
    public void fl$updateTick(@NotNull World world, int x, int y, int z, @NotNull Random random) {
        val fluid = ((FLBlockAccess) world).fl$getFluid(x, y, z);
        if (fluid == null) {
            return;
        }
        FLUtil.simulate(world, x, y, z, random, fluid);
    }

    @Override
    public void fl$onNeighborChange(@NotNull World world, int x, int y, int z, @NotNull Block neighbor) {
        val fluid = ((FLBlockAccess) world).fl$getFluid(x, y, z);
        if (fluid == null) {
            return;
        }
        val block = fluid.getBlock();
        if (block == null) {
            return;
        }
        ((FLWorld) world).fl$scheduleFluidUpdate(x, y, z, (Block) (Object) this, block.tickRate(world));
    }
}
