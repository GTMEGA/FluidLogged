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

package mega.fluidlogged.internal.sim;

import lombok.val;
import mega.fluidlogged.api.IFluid.VanillaFluid;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class VanillaFluidSim {
    private static final ReentrantLock FLOW_LOCK = new ReentrantLock();
    private static final boolean[] flow = new boolean[4];
    private static final int[] flowDist = new int[4];

    public static void simulate(VanillaFluid fluid, World world, int x, int y, int z, Random random) {
        int level = 0;
        int levelDropPerBlock = 1;

        val fluidBlock = fluid.liquid;
        val fluidMaterial = fluidBlock.getMaterial();

        if (fluidMaterial == Material.lava && !world.provider.isHellWorld) {
            levelDropPerBlock = 2;
        }

        int targetLevel;

        if (canSpreadInto(world, x, y - 1, z, fluidMaterial)) {
            if (fluidMaterial == Material.lava && world.getBlock(x, y - 1, z).getMaterial() == Material.water) {
                world.setBlock(x, y - 1, z, Blocks.stone);
                playSmoke(world, x, y - 1, z);
                return;
            }

            doFluidSpread(world, x, y - 1, z, level + 8, fluidBlock);
        } else {
            boolean flowXn;
            boolean flowXp;
            boolean flowZn;
            boolean flowZp;
            FLOW_LOCK.lock();
            try {
                boolean[] flow = horizontalSpread(world, x, y, z, fluidMaterial);
                flowXn = flow[0];
                flowXp = flow[1];
                flowZn = flow[2];
                flowZp = flow[3];
            } finally {
                FLOW_LOCK.unlock();
            }
            targetLevel = level + levelDropPerBlock;

            if (flowXn) {
                doFluidSpread(world, x - 1, y, z, targetLevel, fluidBlock);
            }

            if (flowXp) {
                doFluidSpread(world, x + 1, y, z, targetLevel, fluidBlock);
            }

            if (flowZn) {
                doFluidSpread(world, x, y, z - 1, targetLevel, fluidBlock);
            }

            if (flowZp) {
                doFluidSpread(world, x, y, z + 1, targetLevel, fluidBlock);
            }
        }
    }


    private static boolean[] horizontalSpread(World world, int x, int y, int z, Material fluidMaterial) {
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

            if (!blocksFluidSpread(world, flowX, y, flowZ) && (world.getBlock(flowX, y, flowZ).getMaterial() != fluidMaterial || world.getBlockMetadata(flowX, y, flowZ) != 0)) {
                if (blocksFluidSpread(world, flowX, y - 1, flowZ)) {
                    flowDist[dir] = spreadAlong(world, flowX, y, flowZ, 1, dir, fluidMaterial);
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


    private static int spreadAlong(World world, int x, int y, int z, int distance, int alongDir, Material liquidMaterial) {
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

                if (!blocksFluidSpread(world, X, y, Z) && (world.getBlock(X, y, Z).getMaterial() != liquidMaterial || world.getBlockMetadata(X, y, Z) != 0)) {
                    if (!blocksFluidSpread(world, X, y - 1, Z)) {
                        return distance;
                    }

                    if (distance < 4) {
                        int newDistance = spreadAlong(world, X, y, Z, distance + 1, dir, liquidMaterial);

                        if (newDistance < resultDistance) {
                            resultDistance = newDistance;
                        }
                    }
                }
            }
        }

        return resultDistance;
    }

    private static boolean canSpreadInto(World world, int x, int y, int z, Material liquidMaterial) {
        Material material = world.getBlock(x, y, z).getMaterial();
        return material != liquidMaterial && (material != Material.lava && !blocksFluidSpread(world, x, y, z));
    }


    private static void doFluidSpread(World world, int x, int y, int z, int level, Block fluidBlock) {
        val fluidMaterial = fluidBlock.getMaterial();
        if (canSpreadInto(world, x, y, z, fluidMaterial)) {
            Block block = world.getBlock(x, y, z);

            if (fluidMaterial == Material.lava) {
                playSmoke(world, x, y, z);
            } else {
                block.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            }

            world.setBlock(x, y, z, fluidBlock, level, 3);
        }
    }


    private static void playSmoke(World world, int x, int y, int z) {
        world.playSoundEffect(((float) x + 0.5F), ((float) y + 0.5F), ((float) z + 0.5F), "random.fizz", 0.5F, 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);

        for (int l = 0; l < 8; ++l) {
            world.spawnParticle("largesmoke", (double) x + Math.random(), (double) y + 1.2D, (double) z + Math.random(), 0.0D, 0.0D, 0.0D);
        }
    }


    private static boolean blocksFluidSpread(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block == Blocks.wooden_door ||
               block == Blocks.iron_door ||
               block == Blocks.standing_sign ||
               block == Blocks.ladder ||
               block == Blocks.reeds ||
               block.getMaterial() == Material.portal ||
               block.getMaterial().blocksMovement();
    }
}
