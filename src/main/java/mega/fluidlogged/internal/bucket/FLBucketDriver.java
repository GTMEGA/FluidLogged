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
import mega.fluidlogged.api.FLBlockAccess;
import mega.fluidlogged.internal.world.FLWorldDriver;

import net.minecraft.block.Block;
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

        val bucketState = queryState(event.current);
        if (bucketState == null) {
            return;
        }

        ItemStack newBucket = null;
        if (bucketState == BucketState.Empty) {
            newBucket = handleBucketFill(event.current, event.world, hit.blockX, hit.blockY, hit.blockZ);
        } else if (bucketState == BucketState.Filled) {
            newBucket = handleBucketDrain(event.current, event.world, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit);
        } else {
            return;
        }

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

    private ItemStack handleBucketDrain(ItemStack bucket, World world, int x, int y, int z, int sideHit) {
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

        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        val hitIsFluidLoggable = FLWorldDriver.INSTANCE.canBeFluidLogged(block, meta, fluid);
        val wlWorld = (FLBlockAccess) world;
        boolean flag = false;
        // If the hit block can't be fluidlogged, or if it is already fluid logged, then we want to
        // adjust the hit coordinates to place against that block. For example, if we hit a stone block
        // underneath a fence, then we want to adjust the Y coordinate up one, and re-try the fill on the fence
        // block rather than the block we actually clicked on.
        if (!hitIsFluidLoggable || wlWorld.fl$isFluidLogged(x, y, z, null)) {
            if (sideHit == 0) {
                --y;
            } else if (sideHit == 1) {
                ++y;
            } else if (sideHit == 2) {
                --z;
            } else if (sideHit == 3) {
                ++z;
            } else if (sideHit == 4) {
                --x;
            } else if (sideHit == 5) {
                ++x;
            }

            block = world.getBlock(x, y, z);
            meta = world.getBlockMetadata(x, y, z);
            flag = true;
        }

        if (flag) {
            val adjustedFluidLoggable = FLWorldDriver.INSTANCE.canBeFluidLogged(block, meta, fluid);
            if (!adjustedFluidLoggable || wlWorld.fl$isFluidLogged(x, y, z, null)) {
                return null;
            }
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
