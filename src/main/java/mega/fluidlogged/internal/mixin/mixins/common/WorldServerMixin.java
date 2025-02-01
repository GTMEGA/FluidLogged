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

package mega.fluidlogged.internal.mixin.mixins.common;

import lombok.val;
import mega.fluidlogged.internal.mixin.hook.FLBlockRoot;
import mega.fluidlogged.internal.mixin.hook.FLWorld;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.ISaveHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Mixin(WorldServer.class)
public abstract class WorldServerMixin extends World implements FLWorld {
    @Shadow
    @Final
    private static Logger logger;
    @Unique
    private Set<NextTickListEntry> fl$pendingTicksUnSored;
    @Unique
    private TreeSet<NextTickListEntry> fl$pendingTicksSorted;
    @Unique
    private List<NextTickListEntry> fl$pendingTicksCurrentTick;

    public WorldServerMixin(ISaveHandler p_i45368_1_, String p_i45368_2_, WorldProvider p_i45368_3_, WorldSettings p_i45368_4_, Profiler p_i45368_5_) {
        super(p_i45368_1_, p_i45368_2_, p_i45368_3_, p_i45368_4_, p_i45368_5_);
    }

    @Inject(method = "<init>",
            at = @At("RETURN"),
            require = 1)
    private void initialize(MinecraftServer p_i45284_1_, ISaveHandler p_i45284_2_, String p_i45284_3_, int p_i45284_4_, WorldSettings p_i45284_5_, Profiler p_i45284_6_, CallbackInfo ci) {
        fl$initFields();
        fl$pendingTicksCurrentTick = new ArrayList<>();
    }

    @Inject(method = "initialize",
            at = @At("HEAD"),
            require = 1)
    private void initialize(WorldSettings settings, CallbackInfo ci) {
        fl$initFields();
    }

    @Inject(method = "tickUpdates",
            at = @At("RETURN"),
            cancellable = true,
            require = 1)
    private void hookTickUpdates(boolean runAllPending, CallbackInfoReturnable<Boolean> cir) {
        if (fl$tickFluidUpdates(runAllPending)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean fl$tickFluidUpdates(boolean runAllPending) {
        int i = fl$pendingTicksSorted.size();

        if (i != fl$pendingTicksUnSored.size()) {
            throw new IllegalStateException("TickNextTick list out of synch");
        } else {
            if (i > 1000) {
                i = 1000;
            }

            theProfiler.startSection("cleaning");

            for (int j = 0; j < i; ++j) {
                val entry = (NextTickListEntry) fl$pendingTicksSorted.first();

                if (!runAllPending && entry.scheduledTime > this.worldInfo.getWorldTotalTime()) {
                    break;
                }

                fl$pendingTicksSorted.remove(entry);
                fl$pendingTicksUnSored.remove(entry);
                fl$pendingTicksCurrentTick.add(entry);
            }

            this.theProfiler.endSection();
            this.theProfiler.startSection("ticking");
            Iterator<NextTickListEntry> iterator = fl$pendingTicksCurrentTick.iterator();

            while (iterator.hasNext()) {
                val entry = iterator.next();
                iterator.remove();
                //Keeping here as a note for future when it may be restored.
                //boolean isForced = getPersistentChunks().containsKey(new ChunkCoordIntPair(nextticklistentry.xCoord >> 4, nextticklistentry.zCoord >> 4));
                //byte b0 = isForced ? 0 : 8;
                byte radius = 0;

                if (this.checkChunksExist(entry.xCoord - radius, entry.yCoord - radius, entry.zCoord - radius, entry.xCoord + radius, entry.yCoord + radius, entry.zCoord + radius)) {
                    Block block = this.getBlock(entry.xCoord, entry.yCoord, entry.zCoord);

                    if (block.getMaterial() != Material.air && Block.isEqualTo(block, entry.func_151351_a())) {
                        try {
                            ((FLBlockRoot) block).fl$updateTick(this, entry.xCoord, entry.yCoord, entry.zCoord, rand);
                        } catch (Throwable throwable1) {
                            CrashReport crashreport = CrashReport.makeCrashReport(throwable1, "Exception while ticking a fluidlogged block");
                            CrashReportCategory crashreportcategory = crashreport.makeCategory("FluidLogging being ticked");
                            int k;

                            try {
                                k = this.getBlockMetadata(entry.xCoord, entry.yCoord, entry.zCoord);
                            } catch (Throwable throwable) {
                                k = -1;
                            }

                            CrashReportCategory.func_147153_a(crashreportcategory, entry.xCoord, entry.yCoord, entry.zCoord, block, k);
                            throw new ReportedException(crashreport);
                        }
                    }
                } else {
                    this.scheduleBlockUpdate(entry.xCoord, entry.yCoord, entry.zCoord, entry.func_151351_a(), 0);
                }
            }

            this.theProfiler.endSection();
            fl$pendingTicksCurrentTick.clear();
            return !fl$pendingTicksUnSored.isEmpty();
        }
    }

    @Unique
    private void fl$initFields() {
        if (fl$pendingTicksUnSored == null) {
            fl$pendingTicksUnSored = new HashSet<>();
        }

        if (fl$pendingTicksSorted == null) {
            fl$pendingTicksSorted = new TreeSet<>();
        }
    }

    @Override
    public void fl$scheduleFluidUpdate(int x, int y, int z, Block block, int delay) {
        fl$scheduleFluidUpdateWithPriority(x, y, z, block, delay, 0);
    }

    @Override
    public void fl$scheduleFluidUpdateWithPriority(int x, int y, int z, Block block, int delay, int priority) {
        val entry = new NextTickListEntry(x, y, z, block);
        //Keeping here as a note for future when it may be restored.
        //boolean isForced = getPersistentChunks().containsKey(new ChunkCoordIntPair(nextticklistentry.xCoord >> 4, nextticklistentry.zCoord >> 4));
        //byte b0 = isForced ? 0 : 8;
        byte radius = 0;

        if (scheduledUpdatesAreImmediate && block.getMaterial() != Material.air) {
            if (block.func_149698_L()) {
                radius = 8;

                if (checkChunksExist(entry.xCoord - radius, entry.yCoord - radius, entry.zCoord - radius, entry.xCoord + radius,
                                          entry.yCoord + radius, entry.zCoord + radius)) {
                    Block block1 = getBlock(entry.xCoord, entry.yCoord, entry.zCoord);

                    if (block1.getMaterial() != Material.air && block1 == entry.func_151351_a()) {
                        ((FLBlockRoot)block1).fl$updateTick(this, entry.xCoord, entry.yCoord, entry.zCoord, rand);
                    }
                }
                return;
            }

            delay = 1;
        }

        if (checkChunksExist(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius)) {
            if (block.getMaterial() != Material.air) {
                entry.setScheduledTime((long) delay + worldInfo.getWorldTotalTime());
                entry.setPriority(priority);
            }

            if (!fl$pendingTicksUnSored.contains(entry)) {
                fl$pendingTicksUnSored.add(entry);
                fl$pendingTicksSorted.add(entry);
            }
        }
    }

    @Override
    public void fl$insertUpdate(int x, int y, int z, Block block, int delay, int priority) {
        val entry = new NextTickListEntry(x, y, z, block);
        entry.setPriority(priority);
        if (block.getMaterial() != Material.air) {
            entry.setScheduledTime((long) priority + worldInfo.getWorldTotalTime());
        }
        if (!fl$pendingTicksUnSored.contains(entry)) {
            fl$pendingTicksUnSored.add(entry);
            fl$pendingTicksSorted.add(entry);
        }
    }

    @Override
    public @Nullable List<@NotNull NextTickListEntry> fl$getPendingFluidUpdates(@NotNull Chunk chunk, boolean remove) {

        ArrayList<@NotNull NextTickListEntry> result = null;
        ChunkCoordIntPair chunkcoordintpair = chunk.getChunkCoordIntPair();
        int i = (chunkcoordintpair.chunkXPos << 4) - 2;
        int j = i + 16 + 2;
        int k = (chunkcoordintpair.chunkZPos << 4) - 2;
        int l = k + 16 + 2;

        for (int pass = 0; pass < 2; ++pass) {
            Iterator<NextTickListEntry> iterator;

            if (pass == 0) {
                iterator = fl$pendingTicksSorted.iterator();
            } else {
                iterator = fl$pendingTicksCurrentTick.iterator();

                if (!fl$pendingTicksCurrentTick.isEmpty()) {
                    logger.debug("toBeFluidTicked = {}", fl$pendingTicksCurrentTick.size());
                }
            }

            while (iterator.hasNext()) {
                val entry = iterator.next();

                if (entry.xCoord >= i && entry.xCoord < j && entry.zCoord >= k && entry.zCoord < l) {
                    if (remove) {
                        fl$pendingTicksUnSored.remove(entry);
                        iterator.remove();
                    }

                    if (result == null) {
                        result = new ArrayList<>();
                    }

                    result.add(entry);
                }
            }
        }

        return result;
    }
}
