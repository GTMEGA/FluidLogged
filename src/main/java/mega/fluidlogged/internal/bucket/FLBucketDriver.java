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

package mega.fluidlogged.internal.bucket;

import lombok.val;
import mega.fluidlogged.api.bucket.BucketDriver;
import mega.fluidlogged.api.bucket.BucketEmptyResults;
import mega.fluidlogged.api.bucket.BucketState;
import mega.fluidlogged.internal.FLUtil;
import mega.fluidlogged.internal.world.FLWorldDriver;
import mega.fluidlogged.internal.mixin.hook.FLBlockAccess;

import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.Fluid;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class FLBucketDriver {
    public static final FLBucketDriver INSTANCE = new FLBucketDriver();
    private final List<BucketDriver.Query> queryDrivers = new ArrayList<>();
    private final List<BucketDriver.Fill> fillDrivers = new ArrayList<>();
    private final List<BucketDriver.Empty> emptyDrivers = new ArrayList<>();

    public void registerDriver(BucketDriver driver) {
        boolean valid = false;
        if (driver instanceof BucketDriver.Query) {
            queryDrivers.add((BucketDriver.Query) driver);
            valid = true;
        }
        if (driver instanceof BucketDriver.Fill) {
            fillDrivers.add((BucketDriver.Fill) driver);
            valid = true;
        }
        if (driver instanceof BucketDriver.Empty) {
            emptyDrivers.add((BucketDriver.Empty) driver);
            valid = true;
        }
        if (!valid) {
            throw new IllegalArgumentException("Subclasses of BucketDriver MUST implement Query/Fill/Empty!");
        }
    }

    @SubscribeEvent(
            priority = EventPriority.HIGHEST
    )
    public void onBucketEvent(FillBucketEvent event) {
        val hit = event.target;
        if (hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        val newBucket = handleBucket(event.current, event.world, hit.blockX, hit.blockY, hit.blockZ);
        if (newBucket != null) {
            event.result = newBucket;
            event.setResult(Event.Result.ALLOW);
        }
    }

    private ItemStack fillBucket(Fluid fluid, ItemStack bucket) {
        for (val driver: fillDrivers) {
            val item = driver.fillBucket(fluid, bucket);
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    private BucketEmptyResults emptyBucket(ItemStack bucket) {
        for (val driver: emptyDrivers) {
            val pair = driver.emptyBucket(bucket);
            if (pair != null) {
                return pair;
            }
        }
        return null;
    }

    private BucketState queryState(ItemStack bucket) {
        for (val driver: queryDrivers) {
            val state = driver.queryState(bucket);
            if (state != null) {
                return state;
            }
        }
        return null;
    }

    private ItemStack handleBucket(ItemStack bucket, World world, int x, int y, int z) {
        val bucketState = queryState(bucket);
        if (bucketState == null) {
            return null;
        }
        switch (bucketState) {
            case Empty: return handleBucketFill(bucket, world, x, y, z);
            case Filled: return handleBucketDrain(bucket, world, x, y, z);
            default: return null;
        }
    }

    private ItemStack handleBucketDrain(ItemStack bucket, World world, int x, int y, int z) {
        val result = emptyBucket(bucket);
        if (result == null) {
            return null;
        }
        val emptyBucket = result.getItem();
        val fluid = result.getFluid();
        val fluidBlock = fluid.getBlock();
        if (fluidBlock == null) {
            return null;
        }
        val block = world.getBlock(x, y, z);
        val meta = world.getBlockMetadata(x, y, z);
        val isFluidLoggable = FLWorldDriver.INSTANCE.canBeFluidLogged(block, meta, fluid);
        val wlWorld = (FLBlockAccess) world;
        if (!isFluidLoggable || wlWorld.fl$isFluidLogged(x, y, z, null)) {
            return null;
        }
        wlWorld.fl$setFluid(x, y, z, fluid);
        FLUtil.onFluidPlacedInto(world, x, y, z, block, fluidBlock);
        world.notifyBlocksOfNeighborChange(x, y, z, block);
        world.markBlockForUpdate(x, y, z);
        return emptyBucket;
    }

    private ItemStack handleBucketFill(ItemStack bucket, World world, int x, int y, int z) {
        val wlWorld = (FLBlockAccess) world;
        val fluid = wlWorld.fl$getFluid(x, y, z);
        if (fluid != null) {
            val newBucket = fillBucket(fluid, bucket);
            if (newBucket == null)
                return null;
            wlWorld.fl$setFluid(x, y, z, null);
            val block = world.getBlock(x, y, z);
            world.notifyBlocksOfNeighborChange(x, y, z, block);
            world.markBlockForUpdate(x, y, z);
            return newBucket;
        }
        return null;
    }

}
