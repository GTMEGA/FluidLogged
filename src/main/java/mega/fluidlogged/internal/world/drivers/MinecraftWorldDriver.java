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

package mega.fluidlogged.internal.world.drivers;

import lombok.val;
import mega.fluidlogged.api.world.WorldDriver;
import org.jetbrains.annotations.NotNull;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.init.Blocks;
import net.minecraftforge.fluids.Fluid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MinecraftWorldDriver implements WorldDriver {
    private final List<Class<? extends Block>> waterLoggableClasses = new ArrayList<>();
    private final Set<Block> nonWaterLoggable = new HashSet<>();
    private final Set<Block> waterLoggable = new HashSet<>();
    private final Set<Block> lavaLoggable = new HashSet<>();

    {
        waterLoggableClasses.add(BlockChest.class);
        waterLoggableClasses.add(BlockEnderChest.class);
        waterLoggableClasses.add(BlockFence.class);
        waterLoggableClasses.add(BlockPane.class);
        waterLoggableClasses.add(BlockLadder.class);
        waterLoggableClasses.add(BlockRailBase.class);
        waterLoggableClasses.add(BlockSign.class);
        waterLoggableClasses.add(BlockSlab.class);
        waterLoggableClasses.add(BlockStairs.class);
        waterLoggableClasses.add(BlockTrapDoor.class);
        lavaLoggable.add(Blocks.nether_brick_fence);
        lavaLoggable.add(Blocks.iron_bars);
        lavaLoggable.add(Blocks.stone_slab);
        lavaLoggable.add(Blocks.stone_stairs);
        lavaLoggable.add(Blocks.brick_stairs);
        lavaLoggable.add(Blocks.stone_brick_stairs);
        lavaLoggable.add(Blocks.nether_brick_stairs);
        lavaLoggable.add(Blocks.sandstone_stairs);
        lavaLoggable.add(Blocks.quartz_stairs);
    }

    @Override
    public boolean canBeFluidLogged(@NotNull Block block, int meta, @NotNull Fluid fluid) {
        if (block.isOpaqueCube())
            return false;

        val temp = fluid.getTemperature();
        if (temp >= 373) {
            return lavaLoggable.contains(block);
        } else if (temp >= 273) {
            if (nonWaterLoggable.contains(block)) {
                return false;
            }
            if (waterLoggable.contains(block)) {
                return true;
            }
            for (val klass: waterLoggableClasses) {
                if (klass.isInstance(block)) {
                    waterLoggable.add(block);
                    return true;
                }
            }
            nonWaterLoggable.add(block);
            return false;
        } else {
            return lavaLoggable.contains(block);
        }
    }

    private enum Type {
        Wood,
        Rock,
        Biological
    }
}
