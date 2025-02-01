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

package mega.fluidlogged.internal.driver.drivers;

import lombok.val;
import mega.fluidlogged.api.IFluid;
import mega.fluidlogged.api.bucket.BucketDriver;
import mega.fluidlogged.api.bucket.BucketEmptyResults;
import mega.fluidlogged.api.bucket.BucketState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;

public class MinecraftDriver implements BucketDriver.Fill, BucketDriver.Empty, BucketDriver.Query {
    @Override
    public @Nullable ItemStack fillBucket(@NotNull IFluid fluid, @NotNull ItemStack bucket) {
        val block = fluid.toBlock();
        if (block == Blocks.flowing_water || block == Blocks.water) {
            return new ItemStack(Items.water_bucket);
        } else if (block == Blocks.flowing_lava || block == Blocks.lava) {
            return new ItemStack(Items.lava_bucket);
        }
        return null;
    }

    @Override
    public @Nullable BucketEmptyResults emptyBucket(@NotNull ItemStack bucket) {
        val item = bucket.getItem();
        if (!(item instanceof ItemBucket)) {
            return null;
        }
        val block = ((ItemBucket) item).isFull;
        val fluid = IFluid.fromBucketBlock(block);
        if (fluid == null) {
            return null;
        }
        return new BucketEmptyResults(new ItemStack(Items.bucket), fluid);
    }

    @Override
    public @Nullable BucketState queryState(@NotNull ItemStack bucket) {
        val theItem = bucket.getItem();
        if (!(theItem instanceof ItemBucket)) {
            return null;
        }
        val bucketItem = (ItemBucket) theItem;
        if (bucketItem.isFull == Blocks.air) {
            return BucketState.Empty;
        } else {
            return BucketState.Filled;
        }
    }
}
