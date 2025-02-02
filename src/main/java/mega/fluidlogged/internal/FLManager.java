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

import com.falsepattern.chunk.api.ArrayUtil;
import com.falsepattern.chunk.api.DataManager;
import gnu.trove.list.array.TIntArrayList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import mega.fluidlogged.Tags;
import mega.fluidlogged.internal.mixin.hook.FLChunk;
import mega.fluidlogged.internal.mixin.hook.FLPacket;
import mega.fluidlogged.internal.mixin.hook.FLSubChunk;
import mega.fluidlogged.internal.mixin.hook.FLWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class FLManager implements DataManager.PacketDataManager, DataManager.ChunkDataManager, DataManager.SubChunkDataManager, DataManager.BlockPacketDataManager {
    private static final int SUB_CHUNK_COUNT = 16;
    private static final int BLOCKS_PER_SUB_CHUNK = 16 * 16 * 16;
    private static final int BITS_PER_BYTE = 8;
    private static final int BITS_PER_INT = BITS_PER_BYTE * 4;
    private static final int BITS_PER_BLOCK = BITS_PER_INT + 1;
    private static final int EXTRA_BITS_PER_SUB_CHUNK = BITS_PER_INT * 3 + BITS_PER_BYTE;
    private static final int BITS_PER_CHUNK = SUB_CHUNK_COUNT * (BLOCKS_PER_SUB_CHUNK * BITS_PER_BLOCK + EXTRA_BITS_PER_SUB_CHUNK);
    private static final int BYTES_PER_CHUNK = BITS_PER_CHUNK / BITS_PER_BYTE;
    @Override
    public int maxPacketSize() {
        return BYTES_PER_CHUNK;
    }

    @Override
    public void writeChunkToNBT(Chunk chunk, NBTTagCompound nbt) {
        val world = chunk.worldObj;
        val fl$world = (FLWorld)world;
        val updates = fl$world.fl$getPendingFluidUpdates(chunk, false);
        if (updates == null) {
            return;
        }
        long time = world.getTotalWorldTime();
        val nbtList = new NBTTagList();
        for (val update: updates) {
            nbtList.appendTag(new NBTTagIntArray(new int[]{
                    update.xCoord,
                    update.yCoord,
                    update.zCoord,
                    Block.getIdFromBlock(update.func_151351_a()),
                    (int) (update.scheduledTime - time),
                    update.priority
            }));
        }
        nbt.setByte("v", (byte) 1);
        nbt.setTag("FluidTicks", nbtList);
    }

    @Override
    public void readChunkFromNBT(Chunk chunk, NBTTagCompound nbt) {
        if (nbt.getByte("v") != 1) {
            return;
        }
        if (!nbt.hasKey("FluidTicks", Constants.NBT.TAG_LIST)) {
            return;
        }
        val nbtList = nbt.getTagList("FluidTicks", Constants.NBT.TAG_COMPOUND);
        if (nbtList == null) {
            return;
        }
        val fl$world = (FLWorld)chunk.worldObj;
        val count = nbtList.tagCount();
        for (int i = 0; i < count; i++) {
            val entry = nbtList.func_150306_c(i);
            fl$world.fl$insertUpdate(entry[0], entry[1], entry[2], Block.getBlockById(entry[3]), entry[4], entry[5]);
        }
    }

    @Override
    public void cloneChunk(Chunk from, Chunk to) {
        // Only used for rendering stuff atm, so copying tick info is not necessary
    }

    @RequiredArgsConstructor
    private static class FluidIDList {
        public final @NotNull BitSet presence;
        public final int @NotNull [] ids;
    }
    private static @Nullable FluidIDList serialize(Fluid @NotNull [] fluids) {
        BitSet presence = null;
        val ids = new TIntArrayList();
        val length = fluids.length;
        for (int i = 0; i < length; i++) {
            val fluid = fluids[i];
            if (fluid == null) {
                continue;
            }
            val block = fluid.getBlock();
            if (block == null) {
                continue;
            }
            if (presence == null) {
                presence = new BitSet(length);
            }

            presence.set(i);
            ids.add(Block.getIdFromBlock(block));
        }
        if (presence == null)
            return null;
        return new FluidIDList(presence, ids.toArray());
    }

    private static Fluid @Nullable [] deserialize(int length, @NotNull BitSet presence, int @NotNull [] ids) {
        int idsI = 0;
        Fluid[] fluids = null;
        for (int i = 0; i < length; i++) {
            if (presence.get(i)) {
                val block = Block.getBlockById(ids[idsI]);
                idsI++;
                if (block == null) {
                    continue;
                }
                val fluid = FluidRegistry.lookupFluidForBlock(block);
                if (fluid == null) {
                    continue;
                }
                if (fluids == null) {
                    fluids = new Fluid[length];
                }
                fluids[i] = fluid;
            }
        }
        return fluids;
    }

    @Override
    public void writeToBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        val subChunks = chunk.getBlockStorageArray();
        for (int i = 0; i < subChunks.length; i++) {
            if ((subChunkMask & (1 << i)) != 0) {
                val subChunk = subChunks[i];
                if (subChunk != null) {
                    val wlSubChunk = (FLSubChunk) subChunk;
                    val fl = wlSubChunk.fl$getFluidLog();
                    if (fl == null) {
                        buffer.put((byte) 0);
                        continue;
                    }
                    val flState = serialize(fl);
                    if (flState == null) {
                        buffer.put((byte)0);
                        continue;
                    }
                    buffer.put((byte)1);
                    buffer.putInt(fl.length);
                    val presenceBytes = flState.presence.toByteArray();
                    buffer.putInt(presenceBytes.length);
                    buffer.putInt(flState.ids.length);
                    buffer.put(presenceBytes);
                    for (val id: flState.ids) {
                        buffer.putInt(id);
                    }
                }
            }
        }
    }

    @Override
    public void readFromBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        val subChunks = chunk.getBlockStorageArray();
        for (int i = 0; i < subChunks.length; i++) {
            if ((subChunkMask & (1 << i)) != 0) {
                val subChunk = subChunks[i];
                if (subChunk != null) {
                    val wlSubChunk = (FLSubChunk) subChunk;
                    if (buffer.get() == (byte) 0) {
                        wlSubChunk.fl$setFluidLog(null);
                        continue;
                    }
                    val length = buffer.getInt();
                    val presenceBytesLength = buffer.getInt();
                    val idsLength = buffer.getInt();
                    val slice = buffer.slice();
                    slice.limit(presenceBytesLength);
                    buffer.position(buffer.position() + presenceBytesLength);
                    val presence = BitSet.valueOf(slice);
                    val ids = new int[idsLength];
                    for (int j = 0; j < idsLength; j++) {
                        ids[j] = buffer.getInt();
                    }
                    val arr = deserialize(length, presence, ids);
                    wlSubChunk.fl$setFluidLog(arr);
                }
            }
        }
    }

    @Override
    public void writeSubChunkToNBT(Chunk chunk, ExtendedBlockStorage subChunk, NBTTagCompound nbt) {
        val flSubChunk = (FLSubChunk) subChunk;
        val fluidLog = flSubChunk.fl$getFluidLog();
        if (fluidLog == null) {
            return;
        }
        val serialized = serialize(fluidLog);
        if (serialized == null) {
            return;
        }
        nbt.setInteger("fl$len", fluidLog.length);
        nbt.setByteArray("fl$pres", serialized.presence.toByteArray());
        nbt.setIntArray("fl$ids", serialized.ids);
    }

    @Override
    public void readSubChunkFromNBT(Chunk chunk, ExtendedBlockStorage subChunk, NBTTagCompound nbt) {
        val flSubChunk = (FLSubChunk) subChunk;
        if (!nbt.hasKey("fl$len", Constants.NBT.TAG_INT) || !nbt.hasKey("fl$pres", Constants.NBT.TAG_BYTE_ARRAY) || !nbt.hasKey("fl$ids", Constants.NBT.TAG_INT_ARRAY)) {
            flSubChunk.fl$setFluidLog(null);
            return;
        }
        val len = nbt.getInteger("fl$len");
        val presence = BitSet.valueOf(nbt.getByteArray("fl$pres"));
        val ids = nbt.getIntArray("fl$ids");
        val deserialized = deserialize(len, presence, ids);
        flSubChunk.fl$setFluidLog(deserialized);
    }

    @Override
    public void cloneSubChunk(Chunk fromChunk, ExtendedBlockStorage from, ExtendedBlockStorage to) {
        val wlFrom = (FLSubChunk) from;
        val wlTo = (FLSubChunk) to;
        wlTo.fl$setFluidLog(ArrayUtil.copyArray(wlFrom.fl$getFluidLog(), wlTo.fl$getFluidLog()));
    }

    @Override
    public @NotNull String version() {
        return Tags.MOD_VERSION;
    }

    @Override
    public @Nullable String newInstallDescription() {
        return null;
    }

    @Override
    public @NotNull String uninstallMessage() {
        return "Fluidlogged blocks will lose their fluidlogged-ness!";
    }

    @Override
    public @Nullable String versionChangeMessage(String priorVersion) {
        return null;
    }

    @Override
    public String domain() {
        return Tags.MOD_ID;
    }

    @Override
    public String id() {
        return "fluidLog";
    }

    @Override
    public void writeBlockToPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {
        ((FLPacket)packet).fl$setFluidLog(((FLChunk)chunk).fl$getFluid(x, y, z));
    }

    @Override
    public void readBlockFromPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {
        ((FLChunk)chunk).fl$setFluid(x, y, z, ((FLPacket)packet).fl$getFluidLog());
    }

    @Override
    public void writeBlockPacketToBuffer(S23PacketBlockChange packet, PacketBuffer buffer) {
        val fluid = ((FLPacket)packet).fl$getFluidLog();
        if (fluid == null) {
            buffer.writeBoolean(false);
            return;
        }
        val block = fluid.getBlock();
        if (block == null) {
            buffer.writeBoolean(false);
            return;
        }
        val id = Block.getIdFromBlock(block);
        buffer.writeBoolean(true);
        buffer.writeInt(id);
    }

    @Override
    public void readBlockPacketFromBuffer(S23PacketBlockChange packet, PacketBuffer buffer) {
        val fl$packet = (FLPacket) packet;
        if (!buffer.readBoolean()) {
            fl$packet.fl$setFluidLog(null);
            return;
        }
        val id = buffer.readInt();
        val block = Block.getBlockById(id);
        if (block == null) {
            fl$packet.fl$setFluidLog(null);
            return;
        }
        val fluid = FluidRegistry.lookupFluidForBlock(block);
        fl$packet.fl$setFluidLog(fluid);
    }
}
