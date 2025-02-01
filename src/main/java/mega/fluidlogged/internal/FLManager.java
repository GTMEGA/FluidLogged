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
import mega.fluidlogged.api.IFluid;
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

import java.io.IOException;
import java.nio.ByteBuffer;

import static mega.fluidlogged.api.IFluid.TYPE_NONE;

public class FLManager implements DataManager.PacketDataManager, DataManager.ChunkDataManager, DataManager.SubChunkDataManager, DataManager.BlockPacketDataManager {
    @Override
    public int maxPacketSize() {
        return 16 * (16 * 16 * 16) * 5 + 16 * 9;
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
        public final byte @NotNull [] types;
        public final int @NotNull [] ids;
    }
    private static @Nullable FluidIDList serialize(IFluid @Nullable [] fluids) {
        if (fluids == null)
            return null;
        byte[] types = null;
        val ids = new TIntArrayList();
        val length = fluids.length;
        for (int i = 0; i < length; i++) {
            val fluid = fluids[i];
            if (fluid != null) {
                if (types == null) {
                    types = new byte[length];
                }
                val entry = fluid.serialize();
                types[i] = entry.type;
                ids.add(entry.id);
            }
        }
        if (types == null)
            return null;
        return new FluidIDList(types, ids.toArray());
    }

    private static IFluid @Nullable [] deserialize(byte @NotNull [] types, int @NotNull [] ids) {
        val length = types.length;
        int idsI = 0;
        IFluid[] fluids = null;
        for (int i = 0; i < length; i++) {
            val type = types[i];
            if (type != TYPE_NONE) {
                val fluid = IFluid.deserialize(type, ids[idsI]);
                idsI++;
                if (fluid == null)
                    continue;
                if (fluids == null) {
                    fluids = new IFluid[length];
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
                    val flState = serialize(wlSubChunk.fl$getFluidLog());
                    if (flState == null) {
                        buffer.put((byte)0);
                        continue;
                    }
                    buffer.put((byte)1);
                    buffer.putInt(flState.types.length);
                    buffer.putInt(flState.ids.length);
                    buffer.put(flState.types);
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
                    val typesLength = buffer.getInt();
                    val idsLength = buffer.getInt();
                    val types = new byte[typesLength];
                    val ids = new int[idsLength];
                    buffer.get(types);
                    for (int j = 0; j < idsLength; j++) {
                        ids[j] = buffer.getInt();
                    }
                    val arr = deserialize(types, ids);
                    wlSubChunk.fl$setFluidLog(arr);
                }
            }
        }
    }

    @Override
    public void writeSubChunkToNBT(Chunk chunk, ExtendedBlockStorage subChunk, NBTTagCompound nbt) {
        val wlSubChunk = (FLSubChunk) subChunk;
        val wl = serialize(wlSubChunk.fl$getFluidLog());
        if (wl == null) {
            return;
        }
        nbt.setByteArray("fl$types", wl.types);
        nbt.setIntArray("fl$ids", wl.ids);
    }

    @Override
    public void readSubChunkFromNBT(Chunk chunk, ExtendedBlockStorage subChunk, NBTTagCompound nbt) {
        val wlSubChunk = (FLSubChunk) subChunk;
        if (!nbt.hasKey("fl$types", Constants.NBT.TAG_BYTE_ARRAY) || !nbt.hasKey("fl$ids", Constants.NBT.TAG_INT_ARRAY)) {
            wlSubChunk.fl$setFluidLog(null);
            return;
        }
        val types = nbt.getByteArray("fl$types");
        val ids = nbt.getIntArray("fl$ids");
        val wl = deserialize(types, ids);
        wlSubChunk.fl$setFluidLog(wl);
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
        ((FLPacket)packet).wl$setFluidLog(((FLChunk)chunk).fl$getFluid(x, y, z));
    }

    @Override
    public void readBlockFromPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {
        ((FLChunk)chunk).fl$setFluid(x, y, z, ((FLPacket)packet).wl$getFluidLog());
    }

    @Override
    public void writeBlockPacketToBuffer(S23PacketBlockChange packet, PacketBuffer buffer) throws IOException {
        val fluid = ((FLPacket)packet).wl$getFluidLog();
        if (fluid == null) {
            buffer.writeByte(TYPE_NONE);
            return;
        }
        val data = fluid.serialize();
        val type = data.type;
        buffer.writeByte(type);
        if (type == TYPE_NONE)
            return;
        buffer.writeInt(data.id);
    }

    @Override
    public void readBlockPacketFromBuffer(S23PacketBlockChange packet, PacketBuffer buffer) throws IOException {
        val type = buffer.readByte();
        if (type == TYPE_NONE) {
            ((FLPacket)packet).wl$setFluidLog(null);
            return;
        }
        val id = buffer.readInt();
        val fluid = IFluid.deserialize(type, id);
        ((FLPacket)packet).wl$setFluidLog(fluid);
    }
}
