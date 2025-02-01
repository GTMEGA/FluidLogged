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

package mega.fluidlogged.api.bucket;

import mega.fluidlogged.api.IFluid;
import mega.fluidlogged.internal.driver.FLBucketDriver;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;

public interface BucketDriver {
    @ApiStatus.OverrideOnly
    interface Query extends BucketDriver {
        @Nullable BucketState queryState(@NotNull ItemStack bucket);
    }

    @ApiStatus.OverrideOnly
    interface Fill extends BucketDriver {
        @Nullable ItemStack fillBucket(@NotNull IFluid fluid, @NotNull ItemStack bucket);
    }

    @ApiStatus.OverrideOnly
    interface Empty extends BucketDriver {
        @Nullable BucketEmptyResults emptyBucket(@NotNull ItemStack bucket);
    }

    static void register(@NotNull BucketDriver driver) {
        FLBucketDriver.INSTANCE.registerDriver(driver);
    }
}
