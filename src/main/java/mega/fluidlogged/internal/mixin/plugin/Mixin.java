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

package mega.fluidlogged.internal.mixin.plugin;

import com.falsepattern.lib.mixin.IMixin;
import com.falsepattern.lib.mixin.ITargetedMod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Predicate;

import static com.falsepattern.lib.mixin.IMixin.PredicateHelpers.always;

@RequiredArgsConstructor
public enum Mixin implements IMixin {
    // @formatter:off
    common_ChunkCacheMixin(Side.COMMON, always(), "ChunkCacheMixin"),
    common_ChunkMixin(Side.COMMON, always(), "ChunkMixin"),
    common_ExtendedBlockStorageMixin(Side.COMMON, always(), "ExtendedBlockStorageMixin"),
    common_S23PacketBlockChangeMixin(Side.COMMON, always(), "S23PacketBlockChangeMixin"),
    common_WorldMixin(Side.COMMON, always(), "WorldMixin"),

    client_BlockLiquidMixin(Side.CLIENT, always(), "BlockLiquidMixin"),
    client_RenderBlocksMixin(Side.CLIENT, always(), "RenderBlocksMixin"),
    ;
    // @formatter:on

    @Getter
    private final Side side;
    @Getter
    private final Predicate<List<ITargetedMod>> filter;
    @Getter
    private final String mixin;
}

