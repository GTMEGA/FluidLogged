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

import mega.fluidlogged.internal.bucket.FLBucketDriver;
import mega.fluidlogged.internal.mixin.hook.FLWorld;
import mega.fluidlogged.internal.world.FLWorldDriver;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

/**
 * This is used for implementing custom buckets.
 * <p>
 * If a mod's buckets don't work, make sure it fires a FillBucketEvent in its {@link net.minecraft.item.Item#onItemRightClick}. If it uses some sort of custom "bucket" item not
 * registered in the forge registry, then implement the respective subclasses of this class.
 * <p>
 * See {@link mega.fluidlogged.internal.mixin.mixins.common.compat.cofh.ItemBucketMixin} for how to implement an event hook like that.
 */
public interface BucketDriver {
    /**
     * Used to check the "fullness" of a bucket item
     */
    @ApiStatus.OverrideOnly
    interface Query extends BucketDriver {
        /**
         *
         * @param bucket A possible bucket item
         * @return Whether the bucket is filled or empty.
         * @implSpec Return null if this driver cannot handle the given item.
         */
        @Nullable BucketState queryState(@NotNull ItemStack bucket);
    }

    /**
     * Used to fill empty buckets with fluids
     */
    @ApiStatus.OverrideOnly
    interface Fill extends BucketDriver {
        /**
         * Fills the given empty bucket with a fluid
         * @param fluid The fluid to fill the bucket with
         * @param bucket The bucket to fill
         * @return The filled bucket
         * @implSpec Return null if this driver cannot fill the given bucket.
         */
        @Nullable ItemStack fillBucket(@NotNull Fluid fluid, @NotNull ItemStack bucket);
    }

    /**
     * Used to empty out buckets into a fluid + empty bucket pair
     */
    @ApiStatus.OverrideOnly
    interface Empty extends BucketDriver {
        /**
         * Empties a fluid out of the given bucket
         * @param bucket The bucket to empty out
         * @return The empty bucket and its fluid
         * @implSpec Return null if this driver cannot empty the given bucket.
         */
        @Nullable BucketEmptyResults emptyBucket(@NotNull ItemStack bucket);
    }

    /**
     * Register the provided driver.
     * @implSpec The driver must implement at least one of {@link Query}, {@link Fill}, or {@link Empty}, or this method throws an exception.
     */
    static void register(@NotNull BucketDriver driver) {
        FLBucketDriver.INSTANCE.registerDriver(driver);
    }
}
