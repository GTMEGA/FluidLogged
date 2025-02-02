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

package mega.fluidlogged.api.world;

import mega.fluidlogged.internal.world.FLWorldDriver;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import net.minecraft.block.Block;
import net.minecraftforge.fluids.Fluid;

public interface WorldDriver {
    @ApiStatus.OverrideOnly
    boolean canBeFluidLogged(@NotNull Block block, int meta, @NotNull Fluid fluid);

    static void register(@NotNull WorldDriver driver) {
        FLWorldDriver.INSTANCE.registerDriver(driver);
    }

    /**
     * Check whether a given block can be fluidlogged by a fluid.
     * @param block The block to check
     * @param meta The checked block's metadata
     * @param fluid The fluid that is trying to fluidlog the block
     * @return true to allow fluidlogging
     */
    static boolean getCanBeFluidLogged(@NotNull Block block, int meta, @NotNull Fluid fluid) {
        return FLWorldDriver.INSTANCE.canBeFluidLogged(block, meta, fluid);
    }
}
