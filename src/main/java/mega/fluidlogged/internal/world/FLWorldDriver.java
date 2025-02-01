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

package mega.fluidlogged.internal.world;

import lombok.val;
import mega.fluidlogged.api.IFluid;
import mega.fluidlogged.api.world.WorldDriver;

import net.minecraft.block.Block;

import java.util.ArrayList;
import java.util.List;

public class FLWorldDriver {
    public static final FLWorldDriver INSTANCE = new FLWorldDriver();
    private final List<WorldDriver> drivers = new ArrayList<>();

    public void registerDriver(WorldDriver driver) {
        drivers.add(driver);
    }

    public boolean canBeFluidLogged(Block block, int meta, IFluid fluid) {
        for (val driver: drivers) {
            if (driver.canBeFluidLogged(block, meta, fluid)) {
                return true;
            }
        }
        return false;
    }
}
