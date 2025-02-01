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

package mega.fluidlogged.api;

import lombok.RequiredArgsConstructor;
import lombok.val;
import mega.fluidlogged.internal.FLUtil;
import mega.fluidlogged.internal.mixin.hook.FLWorld;
import mega.fluidlogged.internal.sim.VanillaFluidSim;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import java.util.Random;

@ApiStatus.NonExtendable
public interface IFluid {
    byte TYPE_NONE = 0;
    byte TYPE_VANILLA = 1;
    byte TYPE_FORGE = 2;

    @Nullable SerializedFluid serialize();

    @Nullable Block toBlock();

    void simulate(@NotNull World world, int x, int y, int z, @NotNull Random random);

    void onFluidPlacedInto(@NotNull World world, int x, int y, int z, @NotNull Block block);

    static @Nullable IFluid deserialize(byte type, int id) {
        switch (type) {
            case TYPE_VANILLA:
                return VanillaFluid.deserialize(id);
            case TYPE_FORGE:
                return ForgeFluid.deserialize(id);
            default:
                return null;
        }
    }

    static @Nullable IFluid fromBucketBlock(@NotNull Block block) {
        if (block instanceof BlockLiquid) {
            return FLUtil.resolveVanillaFluid(block);
        } else if (block instanceof BlockFluidBase) {
            val fluidBlock = (BlockFluidBase) block;
            return new ForgeFluid(fluidBlock.getFluid());
        } else {
            return null;
        }
    }

    static @Nullable IFluid fromWorldBlock(@NotNull World world, int x, int y, int z, @NotNull Block block) {
        if (block instanceof BlockLiquid) {
            val meta = world.getBlockMetadata(x, y, z);
            if (meta != 0) {
                return null;
            }
            return FLUtil.resolveVanillaFluid(block);
        } else if (block instanceof BlockFluidBase) {
            val fluidBlock = (BlockFluidBase) block;
            return new ForgeFluid(fluidBlock.getFluid());
        } else {
            return null;
        }
    }

    @RequiredArgsConstructor
    final class VanillaFluid implements IFluid {
        @NotNull
        public final BlockLiquid liquid;

        @Override
        public @NotNull SerializedFluid serialize() {
            return new SerializedFluid(TYPE_VANILLA, Block.getIdFromBlock(liquid));
        }

        @Override
        public @Nullable Block toBlock() {
            if (liquid instanceof BlockStaticLiquid) {
                val dynamicLiquid = Block.getBlockById(Block.getIdFromBlock(liquid) - 1);
                if (dynamicLiquid instanceof BlockDynamicLiquid && dynamicLiquid.getMaterial() == liquid.getMaterial()) {
                    return dynamicLiquid;
                }
                return null;
            } else {
                return liquid;
            }
        }


        @Override
        public void simulate(@NotNull World world, int x, int y, int z, @NotNull Random random) {
            VanillaFluidSim.simulate(this, world, x, y, z, random);
        }

        public void onFluidPlacedInto(@NotNull World world, int x, int y, int z, @NotNull Block block) {
            ((FLWorld)world).fl$scheduleFluidUpdate(x, y, z, block, liquid.tickRate(world));
        }

        public static @Nullable VanillaFluid deserialize(int id) {
            val block = Block.getBlockById(id);
            if (!(block instanceof BlockLiquid)) {
                return null;
            }
            return new VanillaFluid((BlockLiquid) block);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof VanillaFluid)) {
                return false;
            }
            return liquid.equals(((VanillaFluid) obj).liquid);
        }
    }

    @RequiredArgsConstructor
    final class ForgeFluid implements IFluid {
        @NotNull
        public final Fluid fluid;

        @Override
        public @Nullable SerializedFluid serialize() {
            val block = fluid.getBlock();
            if (block == null)
                return null;
            return new SerializedFluid(TYPE_FORGE, Block.getIdFromBlock(block));
        }

        @Override
        public @Nullable Block toBlock() {
            return fluid.getBlock();
        }

        @Override
        public void simulate(@NotNull World world, int x, int y, int z, @NotNull Random random) {
            // TODO
        }

        @Override
        public void onFluidPlacedInto(@NotNull World world, int x, int y, int z, @NotNull Block block) {
            // TODO
        }

        public static @Nullable ForgeFluid deserialize(int id) {
            val block = Block.getBlockById(id);
            if (block == null)
                return null;
            val fluid = FluidRegistry.lookupFluidForBlock(block);
            if (fluid == null)
                return null;
            return new ForgeFluid(fluid);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ForgeFluid)) {
                return false;
            }
            return fluid.equals(((ForgeFluid) obj).fluid);
        }
    }

    @RequiredArgsConstructor
    class SerializedFluid {
        public final byte type;
        public final int id;
    }
}
