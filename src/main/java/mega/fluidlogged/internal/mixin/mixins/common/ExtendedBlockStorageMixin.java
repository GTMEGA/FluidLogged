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

import mega.fluidlogged.internal.mixin.hook.FLSubChunk;
import mega.fluidlogged.api.IFluid;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

@Mixin(ExtendedBlockStorage.class)
public abstract class ExtendedBlockStorageMixin implements FLSubChunk {
    @Unique
    private IFluid[] fl$fluidLog;

    @Override
    public IFluid @Nullable [] fl$getFluidLog() {
        return fl$fluidLog;
    }

    @Override
    public void fl$setFluidLog(IFluid @Nullable [] fluidLog) {
        this.fl$fluidLog = fluidLog;
    }

    @Override
    public @Nullable IFluid fl$getFluid(int x, int y, int z) {
        if (fl$fluidLog == null)
            return null;
        return fl$fluidLog[y << 8 | z << 4 | x];
    }

    @Override
    public void fl$setFluid(int x, int y, int z, @Nullable IFluid fluid) {
        if (fl$fluidLog == null) {
            if (fluid == null) {
                return;
            }
            fl$fluidLog = new IFluid[16 * 16 * 16];
        }
        fl$fluidLog[y << 8 | z << 4 | x] = fluid;
    }
}
