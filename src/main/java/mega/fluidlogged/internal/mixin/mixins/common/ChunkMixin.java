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
import lombok.var;
import mega.fluidlogged.internal.FLUtil;
import mega.fluidlogged.api.FLChunk;
import mega.fluidlogged.internal.mixin.hook.FLSubChunk;
import mega.fluidlogged.internal.world.FLWorldDriver;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fluids.Fluid;

@Mixin(Chunk.class)
public abstract class ChunkMixin implements FLChunk {
    @Shadow public abstract ExtendedBlockStorage[] getBlockStorageArray();

    @Shadow public boolean isModified;

    @Shadow public abstract Block getBlock(int posX, int posY, int posZ);

    @Override
    public @Nullable Fluid fl$getFluid(int x, int y, int z) {
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
    public void fl$setFluid(int x, int y, int z, @Nullable Fluid fluid) {
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

    @Inject(method = "func_150807_a",
            at = @At("HEAD"),
            require = 1)
    private void setBlock(int x, int y, int z, Block block, int meta, CallbackInfoReturnable<Boolean> cir) {
        val originalBlock = getBlock(x, y, z);
        var currentFluid = fl$getFluid(x, y, z);
        if (currentFluid == null) {
            currentFluid = FLUtil.fromChunkBlock(fl$this(), x, y, z, originalBlock);
        }
        if (currentFluid == null || !FLWorldDriver.INSTANCE.canBeFluidLogged(block, meta, currentFluid)) {
            fl$setFluid(x, y, z, null);
        } else {
            fl$setFluid(x, y, z, currentFluid);
        }
    }

    @Unique
    private Chunk fl$this() {
        return (Chunk)(Object)this;
    }
}
