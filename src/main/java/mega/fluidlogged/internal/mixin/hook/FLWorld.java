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

package mega.fluidlogged.internal.mixin.hook;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

public interface FLWorld {
    void fl$scheduleFluidUpdate(int x, int y, int z, @NotNull Block block, int delay);
    void fl$scheduleFluidUpdateWithPriority(int x, int y, int z, @NotNull Block block, int delay, int priority);
    void fl$insertUpdate(int x, int y, int z, @NotNull Block block, int delay, int priority);
    @Nullable List<@NotNull NextTickListEntry> fl$getPendingFluidUpdates(@NotNull Chunk chunk, boolean remove);
}
