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

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

@ApiStatus.NonExtendable
public interface IFluid {
    byte TYPE_NONE = 0;
    byte TYPE_VANILLA = 1;
    byte TYPE_FORGE = 2;

    @NotNull SerializedFluid serialize();
    @Nullable Block toBlock();
    @Nullable ItemBucket toBucket();

    static @Nullable IFluid deserialize(byte type, int id) {
        switch (type) {
            case TYPE_VANILLA: return VanillaFluid.deserialize(id);
            case TYPE_FORGE: return ForgeFluid.deserialize(id);
            default: return null;
        }
    }

    static @Nullable IFluid fromBucketBlock(Block block) {
        if (block instanceof BlockLiquid) {
            if (block instanceof BlockDynamicLiquid) {
                val staticLiquid = Block.getBlockById(Block.getIdFromBlock(block) + 1);
                if (staticLiquid instanceof BlockStaticLiquid && staticLiquid.getMaterial() == block.getMaterial()) {
                    return new VanillaFluid((BlockLiquid) staticLiquid);
                }
            }
            return new VanillaFluid((BlockLiquid) block);
        } else if (block instanceof BlockFluidBase) {
            val fluidBlock = (BlockFluidBase) block;
            return new ForgeFluid(fluidBlock.getFluid());
        } else {
            return null;
        }
    }

    static @Nullable IFluid fromWorldBlock(World world, int x, int y, int z, Block block) {
        if (block instanceof BlockLiquid) {
            val meta = world.getBlockMetadata(x, y, z);
            if (meta != 0)
                return null;
            if (block instanceof BlockDynamicLiquid) {
                val staticLiquid = Block.getBlockById(Block.getIdFromBlock(block) + 1);
                if (staticLiquid instanceof BlockStaticLiquid && staticLiquid.getMaterial() == block.getMaterial()) {
                    return new VanillaFluid((BlockLiquid) staticLiquid);
                }
            }
            return new VanillaFluid((BlockLiquid) block);
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
        private final BlockLiquid liquid;

        @Override
        public @NotNull SerializedFluid serialize() {
            return new SerializedFluid(TYPE_VANILLA,
                                       Block.getIdFromBlock(liquid));
        }

        @Override
        public @NotNull Block toBlock() {
            if (liquid instanceof BlockStaticLiquid) {
                val staticLiquid = Block.getBlockById(Block.getIdFromBlock(liquid) - 1);
                if (staticLiquid instanceof BlockDynamicLiquid && staticLiquid.getMaterial() == liquid.getMaterial()) {
                    return staticLiquid;
                }
            }
            return liquid;
        }

        @Override
        public @Nullable ItemBucket toBucket() {
            if (liquid == Blocks.flowing_water || liquid == Blocks.water) {
                return (ItemBucket) Items.water_bucket;
            } else if (liquid == Blocks.flowing_lava || liquid == Blocks.lava) {
                return (ItemBucket) Items.lava_bucket;
            } else {
                return null;
            }
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
            if (!(obj instanceof VanillaFluid))
                return false;
            return liquid.equals(((VanillaFluid) obj).liquid);
        }
    }

    @RequiredArgsConstructor
    final class ForgeFluid implements IFluid {
        @NotNull
        private final Fluid fluid;

        @Override
        public @NotNull SerializedFluid serialize() {
            return new SerializedFluid(TYPE_FORGE,
                                       fluid.getID());
        }

        @Override
        public @Nullable Block toBlock() {
            return fluid.getBlock();
        }

        @Override
        public @Nullable ItemBucket toBucket() {
            // TODO
            return null;
        }

        public static @Nullable ForgeFluid deserialize(int id) {
            val fluid = FluidRegistry.getFluid(id);
            if (fluid == null) {
                return null;
            }
            return new ForgeFluid(fluid);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ForgeFluid))
                return false;
            return fluid.equals(((ForgeFluid) obj).fluid);
        }
    }

    @RequiredArgsConstructor
    class SerializedFluid {
        public final byte type;
        public final int id;
    }
}
