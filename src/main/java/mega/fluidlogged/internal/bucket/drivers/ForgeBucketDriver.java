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

package mega.fluidlogged.internal.bucket.drivers;

import lombok.val;
import mega.fluidlogged.api.bucket.BucketDriver;
import mega.fluidlogged.api.bucket.BucketEmptyResults;
import mega.fluidlogged.api.bucket.BucketState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;

public class ForgeBucketDriver implements BucketDriver.Fill, BucketDriver.Empty, BucketDriver.Query {
    @Override
    public @Nullable BucketEmptyResults emptyBucket(@NotNull ItemStack bucket) {
        if (!FluidContainerRegistry.isBucket(bucket)) {
            return null;
        }
        val fluidStack = FluidContainerRegistry.getFluidForFilledItem(bucket);
        if (fluidStack.amount != FluidContainerRegistry.BUCKET_VOLUME) {
            return null;
        }
        val fluid = fluidStack.getFluid();
        val fluidBlock = fluid.getBlock();
        if (fluidBlock == null) {
            return null;
        }
        val drained = FluidContainerRegistry.drainFluidContainer(bucket);
        return new BucketEmptyResults(drained, fluid);
    }

    @Override
    public @Nullable ItemStack fillBucket(@NotNull Fluid fluid, @NotNull ItemStack bucket) {
        return FluidContainerRegistry.fillFluidContainer(new FluidStack(fluid, 1000), bucket);
    }

    @Override
    public @Nullable BucketState queryState(@NotNull ItemStack bucket) {
        if (!FluidContainerRegistry.isBucket(bucket)) {
            return null;
        }
        if (FluidContainerRegistry.isEmptyContainer(bucket)) {
            return BucketState.Empty;
        } else {
            return BucketState.Filled;
        }
    }
}
