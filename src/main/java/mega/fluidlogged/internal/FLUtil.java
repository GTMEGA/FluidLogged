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
import mega.fluidlogged.internal.mixin.hook.FLBlockAccess;
import mega.fluidlogged.internal.mixin.hook.FLWorld;
import mega.fluidlogged.internal.sim.ForgeFluidSim;
import mega.fluidlogged.internal.sim.VanillaFluidSim;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;
import cpw.mods.fml.common.eventhandler.Event;

import java.util.Random;
import java.util.function.Consumer;

public class FLUtil {
    public static Block getFluidOrBlock(IBlockAccess access, int x, int y, int z) {
        val b = access.getBlock(x, y, z);
        if (!(access instanceof FLBlockAccess))
            return b;
        val fluid = ((FLBlockAccess)access).fl$getFluid(x, y, z);
        if (fluid == null)
            return b;
        val block = fluid.getBlock();
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
        val block = fluid.getBlock();
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

    public static void onFluidPlacedInto(@NotNull World world, int x, int y, int z, @NotNull Block block, @NotNull Block fluidBlock) {
        if (fluidBlock instanceof BlockLiquid) {
            ((FLWorld)world).fl$scheduleFluidUpdate(x, y, z, block, fluidBlock.tickRate(world));
        } else if (fluidBlock instanceof IFluidBlock) {
            // TODO
        }
    }

    public static void simulate(@NotNull World world, int x, int y, int z, @NotNull Random random, Fluid fluid) {
        val block = fluid.getBlock();
        if (block == null)
            return;
        if (block instanceof BlockLiquid) {
            VanillaFluidSim.simulate(world, x, y, z, random, (BlockLiquid) resolveVanillaSimulationLiquid(block));
        } else if (block instanceof BlockFluidClassic) {
            ForgeFluidSim.simulate(world, x, y, z, random, (BlockFluidClassic) block);
        }
    }

    private static Block resolveVanillaSimulationLiquid(Block block) {
        if (block instanceof BlockStaticLiquid) {
            val dynamic = Block.getBlockById(Block.getIdFromBlock(block) - 1);
            if (dynamic instanceof BlockDynamicLiquid && dynamic.getMaterial() == block.getMaterial()) {
                return dynamic;
            }
        }
        return block;
    }

    public static @Nullable Fluid fromWorldBlock(@NotNull World world, int x, int y, int z, @NotNull Block block) {
        val meta = world.getBlockMetadata(x, y, z);
        if (meta != 0) {
            return null;
        }
        return fromBucketBlock(block);
    }

    public static @Nullable Fluid fromBucketBlock(@NotNull Block block) {
        if (block == Blocks.flowing_water) {
            block = Blocks.water;
        } else if (block == Blocks.flowing_lava) {
            block = Blocks.lava;
        }
        return FluidRegistry.lookupFluidForBlock(block);
    }
}
