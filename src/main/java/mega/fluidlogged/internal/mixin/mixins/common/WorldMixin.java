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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import lombok.val;
import mega.fluidlogged.api.IFluid;
import mega.fluidlogged.internal.world.FLWorldDriver;
import mega.fluidlogged.internal.mixin.hook.FLBlockAccess;
import mega.fluidlogged.internal.mixin.hook.FLBlockRoot;
import mega.fluidlogged.internal.mixin.hook.FLChunk;
import mega.fluidlogged.internal.mixin.hook.FLWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.List;

@Mixin(World.class)
public abstract class WorldMixin implements FLBlockAccess, FLWorld {
    // region hooks
    @Shadow public abstract Chunk getChunkFromBlockCoords(int x, int z);

    @Shadow public abstract void markBlockForUpdate(int p_147471_1_, int p_147471_2_, int p_147471_3_);

    @Shadow public abstract boolean setBlock(int x, int y, int z, Block blockType);

    @Shadow public abstract int getBlockMetadata(int x, int y, int z);

    @Inject(method = "setBlockToAir",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void fluidLoggedSetBlock(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        val fluid = fl$getFluid(x, y, z);
        if (fluid != null) {
            val block = fluid.toBlock();
            if (block != null) {
                cir.setReturnValue(setBlock(x, y, z, block));
            }
        }
    }

    @WrapOperation(method = "setBlock(IIILnet/minecraft/block/Block;II)Z",
                   at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/chunk/Chunk;func_150807_a(IIILnet/minecraft/block/Block;I)Z"),
                   require = 1)
    private boolean unFluidLog(Chunk chunk, int cX, int y, int cZ, Block block, int meta, Operation<Boolean> original,
                               @Local(ordinal = 1) Block originalBlock,
                               @Local(ordinal = 0,
                                      argsOnly = true) int wX,
                               @Local(ordinal = 2,
                                      argsOnly = true) int wZ) {
        val fl$chunk = (FLChunk) chunk;
        val currentFluid = fl$chunk.fl$getFluid(cX, y, cZ);
        if (!FLWorldDriver.INSTANCE.canBeFluidLogged(block, meta, currentFluid)) {
            fl$chunk.fl$setFluid(cX, y, cZ, null);
        } else {
            val originalMeta = chunk.getBlockMetadata(cX, y, cZ);
            if (!FLWorldDriver.INSTANCE.canBeFluidLogged(originalBlock, originalMeta, currentFluid)) {
                val fluidFromBlock = IFluid.fromWorldBlock((World)(Object)this, wX, y, wZ, originalBlock);
                if (fluidFromBlock != null && FLWorldDriver.INSTANCE.canBeFluidLogged(block, meta, fluidFromBlock)) {
                    fl$chunk.fl$setFluid(cX, y, cZ, fluidFromBlock);
                } else {
                    fl$chunk.fl$setFluid(cX, y, cZ, null);
                }
            }
        }
        return original.call(chunk, cX, y, cZ, block, meta);
    }

    @WrapOperation(method = "notifyBlockOfNeighborChange",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/block/Block;onNeighborBlockChange(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;)V"),
                   require = 1)
    private void onNeighborFluidChange(Block instance, World worldIn, int x, int y, int z, Block neighbor, Operation<Void> original) {
        ((FLBlockRoot)instance).fl$onNeighborChange(worldIn, x, y, z, neighbor);
        original.call(instance, worldIn, x, y, z, neighbor);
    }

    @WrapOperation(method = "handleMaterialAcceleration",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"),
                   require = 1)
    private Block accelerationGetBlock(World world, int x, int y, int z, Operation<Block> original, @Share("logged") LocalBooleanRef logged) {
        val fluid = ((FLBlockAccess)world).fl$getFluid(x, y, z);
        val fluidBlock = fluid == null ? null : fluid.toBlock();
        if (fluidBlock != null) {
            logged.set(true);
            return fluidBlock;
        }
        logged.set(false);
        return original.call(world, x, y, z);
    }

    @WrapOperation(method = "handleMaterialAcceleration",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"),
                   require = 1)
    private int accelerationGetMeta(World world, int x, int y, int z, Operation<Integer> original, @Share("logged") LocalBooleanRef logged) {
        if (logged.get()) {
            return 0;
        }
        return original.call(world, x, y, z);
    }

    @WrapOperation(method = "isAnyLiquid",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"),
                   require = 1)
    private Block isAnyLiquidGetBlock(World world, int x, int y, int z, Operation<Block> original) {
        val fluid = ((FLBlockAccess)world).fl$getFluid(x, y, z);
        val fluidBlock = fluid == null ? null : fluid.toBlock();
        if (fluidBlock != null) {
            return fluidBlock;
        }
        return original.call(world, x, y, z);
    }

    @WrapOperation(method = "isMaterialInBB",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"),
                   require = 1)
    private Block isMaterialInBBGetBlock(World world, int x, int y, int z, Operation<Block> original, @Local(argsOnly = true) Material material) {
        val ogBlock = original.call(world, x, y, z);
        if (ogBlock.getMaterial() == material) {
            return ogBlock;
        }
        val fluid = ((FLBlockAccess)world).fl$getFluid(x, y, z);
        val fluidBlock = fluid == null ? null : fluid.toBlock();
        if (fluidBlock != null) {
            return fluidBlock;
        }
        return ogBlock;
    }

    // endregion

    // region FLBlockAccess

    @Override
    public void fl$setFluid(int x, int y, int z, @Nullable IFluid fluid) {
        val chunk = getChunkFromBlockCoords(x, z);
        if (chunk == null || chunk instanceof EmptyChunk)
            return;
        ((FLChunk)chunk).fl$setFluid(x & 0xF, y, z & 0xF, fluid);
        markBlockForUpdate(x, y, z);
    }

    @Override
    public @Nullable IFluid fl$getFluid(int x, int y, int z) {
        val chunk = getChunkFromBlockCoords(x, z);
        if (chunk == null || chunk instanceof EmptyChunk)
            return null;
        return ((FLChunk)chunk).fl$getFluid(x & 0xf, y, z & 0xf);
    }

    // endregion

    // region FLWorld


    @Override
    public void fl$scheduleFluidUpdate(int x, int y, int z, Block block, int delay) {}

    @Override
    public void fl$insertUpdate(int x, int y, int z, Block block, int delay, int priority) {}

    @Override
    public @Nullable List<NextTickListEntry> fl$getPendingFluidUpdates(@NotNull Chunk chunk, boolean remove) {
        return null;
    }

    // endregion
}
