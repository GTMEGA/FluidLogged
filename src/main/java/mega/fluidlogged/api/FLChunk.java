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

package mega.fluidlogged.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fluids.Fluid;

/**
 * Implemented on {@link net.minecraft.world.chunk.Chunk} via a mixin.
 */
@ApiStatus.NonExtendable
public interface FLChunk {
    /**
     * Retrieves the fluid from a fluidlogged block. Null if not fluidlogged. Note that this returns null if the block itself is a fluid block!
     */
    @Nullable Fluid fl$getFluid(int x, int y, int z);

    /**
     * Makes a block fluidlogged with the given fluid. Does NOT verify whether the block can be fluidlogged!
     * Pass in null to clear.
     */
    void fl$setFluid(int x, int y, int z, @Nullable Fluid fluid);

    static FLChunk of(Chunk chunk) {
        return (FLChunk) chunk;
    }
}
