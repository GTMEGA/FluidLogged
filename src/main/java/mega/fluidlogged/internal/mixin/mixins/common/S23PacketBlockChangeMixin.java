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

import mega.fluidlogged.api.IFluid;
import mega.fluidlogged.internal.mixin.hook.FLPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.network.play.server.S23PacketBlockChange;

@Mixin(S23PacketBlockChange.class)
public abstract class S23PacketBlockChangeMixin implements FLPacket {
    @Unique
    private IFluid wl$fluidLog;

    @Override
    public @Nullable IFluid wl$getFluidLog() {
        return wl$fluidLog;
    }

    @Override
    public void wl$setFluidLog(@Nullable IFluid fluid) {
        this.wl$fluidLog = fluid;
    }
}
