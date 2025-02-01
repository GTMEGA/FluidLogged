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
import mega.fluidlogged.api.IFluid;
import mega.fluidlogged.internal.mixin.hook.FLBlockAccess;
import org.jetbrains.annotations.NotNull;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import cpw.mods.fml.common.eventhandler.Event;

import java.util.function.Consumer;

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

    public static void fireBucketEvent(ItemStack item, World world, EntityPlayer player, Consumer<ItemStack> resultCallback, MovingObjectPosition pos) {
        val event = new FillBucketEvent(player, item, world, pos);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            resultCallback.accept(item);
            return;
        }

        if (event.getResult() != Event.Result.ALLOW) {
            return;
        }

        if (player.capabilities.isCreativeMode) {
            resultCallback.accept(item);
            return;
        }

        if (--item.stackSize <= 0) {
            resultCallback.accept(event.result);
            return;
        }

        if (!player.inventory.addItemStackToInventory(event.result)) {
            player.dropPlayerItemWithRandomChoice(event.result, false);
        }

        resultCallback.accept(item);
    }

    @NotNull
    public static IFluid resolveVanillaFluid(Block block) {
        if (block instanceof BlockDynamicLiquid) {
            val staticLiquid = Block.getBlockById(Block.getIdFromBlock(block) + 1);
            if (staticLiquid instanceof BlockStaticLiquid && staticLiquid.getMaterial() == block.getMaterial()) {
                return new IFluid.VanillaFluid((BlockLiquid) staticLiquid);
            }
        }
        return new IFluid.VanillaFluid((BlockLiquid) block);
    }
}
