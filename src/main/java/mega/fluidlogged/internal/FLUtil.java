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

package mega.fluidlogged.internal;

import lombok.val;
import mega.fluidlogged.api.FLBlock;
import mega.fluidlogged.api.IFluid;
import mega.fluidlogged.internal.mixin.hook.FLBlockAccess;
import mega.fluidlogged.internal.mixin.hook.FLBlockRoot;
import mega.fluidlogged.internal.mixin.hook.FLWorld;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class FLUtil {
    public static Block getFluidOrBlock(IBlockAccess access, int x, int y, int z) {
        val b = access.getBlock(x, y, z);
        if (!(access instanceof FLBlockAccess))
            return b;
        val fluid = ((FLBlockAccess)access).fl$getFluid(x, y, z);
        if (fluid == null)
            return b;
        val block = fluid.toBlock();
        if (block == null)
            return b;
        return block;
    }

    public static int getFluidMeta(IBlockAccess access, int x, int y, int z) {
        if (!(access instanceof FLBlockAccess))
            return access.getBlockMetadata(x, y, z);
        val fluid = ((FLBlockAccess)access).fl$getFluid(x, y, z);
        if (fluid == null)
            return access.getBlockMetadata(x, y, z);
        val block = fluid.toBlock();
        if (block == null)
            return access.getBlockMetadata(x, y, z);
        return 0;
    }

    public static boolean isFluidLoggable(World world, int x, int y, int z, Block block, IFluid fluid) {
        if (block instanceof FLBlock) {
            return ((FLBlock) block).fl$isFluidLoggable(world, x, y, z, fluid);
        } else if (block instanceof BlockFence) {
            return true;
        } else if (block instanceof BlockStairs) {
            return true;
        } else if (block instanceof BlockSlab) {
            return !block.isOpaqueCube();
        }
        return false;
    }

    public static ItemStack doFluidLog(ItemStack stack, World world, int x, int y, int z) {
        val bucket = (ItemBucket) stack.getItem();
        if (bucket == null) {
            return null;
        }
        if (bucket.isFull == Blocks.air) {
            val wlWorld = (FLBlockAccess) world;
            val fluid = wlWorld.fl$getFluid(x, y, z);
            if (fluid != null) {
                val newBucket = fluid.toBucket();
                if (newBucket == null)
                    return null;
                wlWorld.fl$setFluid(x, y, z, null);
                val block = world.getBlock(x, y, z);
                world.notifyBlocksOfNeighborChange(x, y, z, block);
                world.markBlockForUpdate(x, y, z);
                return new ItemStack(newBucket);
            }
            return null;
        }

        val fluid = IFluid.fromBucketBlock(bucket.isFull);
        if (fluid == null) {
            return null;
        }
        val block = world.getBlock(x, y, z);
        val isFluidLoggable = isFluidLoggable(world, x, y, z, block, fluid);
        val wlWorld = (FLBlockAccess) world;
        if (!isFluidLoggable || wlWorld.fl$isFluidLogged(x, y, z, null)) {
            return null;
        }
        wlWorld.fl$setFluid(x, y, z, fluid);
        fluid.onFluidPlacedInto(world, x, y, z, block);
        world.notifyBlocksOfNeighborChange(x, y, z, block);
        world.markBlockForUpdate(x, y, z);
        return new ItemStack(Items.bucket);
    }
}
