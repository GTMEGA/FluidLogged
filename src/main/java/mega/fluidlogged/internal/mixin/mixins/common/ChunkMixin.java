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

import mega.fluidlogged.internal.mixin.hook.FLChunk;
import mega.fluidlogged.internal.mixin.hook.FLSubChunk;
import mega.fluidlogged.api.IFluid;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

@Mixin(Chunk.class)
public abstract class ChunkMixin implements FLChunk {
    @Shadow public abstract ExtendedBlockStorage[] getBlockStorageArray();

    @Shadow public boolean isModified;

    @Override
    public @Nullable IFluid fl$getFluid(int x, int y, int z) {
        val Y = y >> 4;
        if (Y < 0)
            return null;
        val subChunks = getBlockStorageArray();
        if (subChunks == null || Y >= subChunks.length) {
            return null;
        }
        val subChunk = subChunks[Y];
        if (subChunk == null)
            return null;
        return ((FLSubChunk)subChunk).fl$getFluid(x, y & 0xf, z);
    }

    @Override
    public void fl$setFluid(int x, int y, int z, @Nullable IFluid fluid) {
        val Y = y >> 4;
        if (Y < 0)
            return;
        val subChunks = getBlockStorageArray();
        if (subChunks == null || Y >= subChunks.length) {
            return;
        }
        val subChunk = subChunks[Y];
        if (subChunk == null)
            return;
        ((FLSubChunk)subChunk).fl$setFluid(x, y & 0xf, z, fluid);
        isModified = true;
    }
}
