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
import mega.fluidlogged.internal.mixin.hook.FLWorld;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

@ApiStatus.NonExtendable
public interface IFluid {
    byte TYPE_NONE = 0;
    byte TYPE_VANILLA = 1;
    byte TYPE_FORGE = 2;

    @NotNull SerializedFluid serialize();

    @Nullable Block toBlock();

    @Nullable ItemBucket toBucket();

    void simulate(World world, int x, int y, int z, Random random);

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
            if (meta != 0) {
                return null;
            }
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

    void onFluidPlacedInto(World world, int x, int y, int z, Block block);

    @RequiredArgsConstructor
    final class VanillaFluid implements IFluid {
        @NotNull
        private final BlockLiquid liquid;

        @Override
        public @NotNull SerializedFluid serialize() {
            return new SerializedFluid(TYPE_VANILLA, Block.getIdFromBlock(liquid));
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

        private final ReentrantLock FLOW_LOCK = new ReentrantLock();
        private final boolean[] flow = new boolean[4];
        private final int[] flowDist = new int[4];

        @Override
        public void simulate(World world, int x, int y, int z, Random random) {
            int level = 0;
            int levelDropPerBlock = 1;

            if (liquid.getMaterial() == Material.lava && !world.provider.isHellWorld) {
                levelDropPerBlock = 2;
            }

            int targetLevel;

            if (this.canSpreadInto(world, x, y - 1, z)) {
                if (liquid.getMaterial() == Material.lava && world.getBlock(x, y - 1, z).getMaterial() == Material.water) {
                    world.setBlock(x, y - 1, z, Blocks.stone);
                    this.playSmoke(world, x, y - 1, z);
                    return;
                }

                this.doFluidSpread(world, x, y - 1, z, level + 8);
            } else {
                boolean flowXn;
                boolean flowXp;
                boolean flowZn;
                boolean flowZp;
                FLOW_LOCK.lock();
                try {
                    boolean[] flow = this.horizontalSpread(world, x, y, z);
                    flowXn = flow[0];
                    flowXp = flow[1];
                    flowZn = flow[2];
                    flowZp = flow[3];
                } finally {
                    FLOW_LOCK.unlock();
                }
                targetLevel = level + levelDropPerBlock;

                if (flowXn) {
                    this.doFluidSpread(world, x - 1, y, z, targetLevel);
                }

                if (flowXp) {
                    this.doFluidSpread(world, x + 1, y, z, targetLevel);
                }

                if (flowZn) {
                    this.doFluidSpread(world, x, y, z - 1, targetLevel);
                }

                if (flowZp) {
                    this.doFluidSpread(world, x, y, z + 1, targetLevel);
                }
            }
        }

        public void onFluidPlacedInto(World world, int x, int y, int z, Block block) {
            ((FLWorld)world).fl$scheduleFluidUpdate(x, y, z, block, liquid.tickRate(world));
        }


        private boolean[] horizontalSpread(World world, int x, int y, int z) {
            for (int dir = 0; dir < 4; ++dir) {
                flowDist[dir] = 1000;
                int flowX = x;
                int flowZ = z;

                if (dir == 0) {
                    flowX = x - 1;
                }

                if (dir == 1) {
                    ++flowX;
                }

                if (dir == 2) {
                    flowZ = z - 1;
                }

                if (dir == 3) {
                    ++flowZ;
                }

                if (!blocksFluidSpread(world, flowX, y, flowZ) &&
                    (world.getBlock(flowX, y, flowZ).getMaterial() != liquid.getMaterial() || world.getBlockMetadata(flowX, y, flowZ) != 0)) {
                    if (blocksFluidSpread(world, flowX, y - 1, flowZ)) {
                        flowDist[dir] = spreadAlong(world, flowX, y, flowZ, 1, dir);
                    } else {
                        flowDist[dir] = 0;
                    }
                }
            }

            int dist = flowDist[0];

            for (int i1 = 1; i1 < 4; ++i1) {
                if (flowDist[i1] < dist) {
                    dist = flowDist[i1];
                }
            }

            for (int i1 = 0; i1 < 4; ++i1) {
                flow[i1] = flowDist[i1] == dist;
            }

            return flow;
        }


        private int spreadAlong(World world, int x, int y, int z, int distance, int alongDir) {
            int resultDistance = 1000;

            for (int dir = 0; dir < 4; ++dir) {
                if ((dir != 0 || alongDir != 1) && (dir != 1 || alongDir != 0) && (dir != 2 || alongDir != 3) && (dir != 3 || alongDir != 2)) {
                    int X = x;
                    int Z = z;

                    if (dir == 0) {
                        X = x - 1;
                    }

                    if (dir == 1) {
                        ++X;
                    }

                    if (dir == 2) {
                        Z = z - 1;
                    }

                    if (dir == 3) {
                        ++Z;
                    }

                    if (!this.blocksFluidSpread(world, X, y, Z) && (world.getBlock(X, y, Z).getMaterial() != liquid.getMaterial() || world.getBlockMetadata(X, y, Z) != 0)) {
                        if (!this.blocksFluidSpread(world, X, y - 1, Z)) {
                            return distance;
                        }

                        if (distance < 4) {
                            int newDistance = this.spreadAlong(world, X, y, Z, distance + 1, dir);

                            if (newDistance < resultDistance) {
                                resultDistance = newDistance;
                            }
                        }
                    }
                }
            }

            return resultDistance;
        }

        private int levelDropPerBlock(World world) {
            if (liquid.getMaterial() == Material.lava && !world.provider.isHellWorld) {
                return 2;
            }
            return 1;
        }

        private boolean canSpreadInto(World world, int x, int y, int z) {
            val block = toBlock();
            Material material = world.getBlock(x, y, z).getMaterial();
            return material != block.getMaterial() && (material != Material.lava && !this.blocksFluidSpread(world, x, y, z));
        }


        private void doFluidSpread(World world, int x, int y, int z, int level) {
            val fluidBlock = toBlock();
            if (this.canSpreadInto(world, x, y, z)) {
                Block block = world.getBlock(x, y, z);

                if (fluidBlock.getMaterial() == Material.lava) {
                    playSmoke(world, x, y, z);
                } else {
                    block.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
                }

                world.setBlock(x, y, z, fluidBlock, level, 3);
            }
        }


        private void playSmoke(World world, int x, int y, int z) {
            world.playSoundEffect(((float) x + 0.5F), ((float) y + 0.5F), ((float) z + 0.5F), "random.fizz", 0.5F, 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);

            for (int l = 0; l < 8; ++l) {
                world.spawnParticle("largesmoke", (double) x + Math.random(), (double) y + 1.2D, (double) z + Math.random(), 0.0D, 0.0D, 0.0D);
            }
        }


        private boolean blocksFluidSpread(World world, int x, int y, int z) {
            Block block = world.getBlock(x, y, z);
            return block == Blocks.wooden_door || block == Blocks.iron_door || block == Blocks.standing_sign || block == Blocks.ladder || block == Blocks.reeds ||
                   block.getMaterial() == Material.portal || block.getMaterial().blocksMovement();
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
        private final Fluid fluid;

        @Override
        public @NotNull SerializedFluid serialize() {
            return new SerializedFluid(TYPE_FORGE, fluid.getID());
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

        @Override
        public void simulate(World world, int x, int y, int z, Random random) {
            // TODO
        }

        @Override
        public void onFluidPlacedInto(World world, int x, int y, int z, Block block) {
            // TODO
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
