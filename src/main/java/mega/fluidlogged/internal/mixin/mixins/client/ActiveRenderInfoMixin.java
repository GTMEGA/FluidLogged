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

package mega.fluidlogged.internal.mixin.mixins.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import lombok.val;
import mega.fluidlogged.api.FLBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.world.World;

@Mixin(ActiveRenderInfo.class)
public abstract class ActiveRenderInfoMixin {
    @WrapOperation(method = "getBlockAtEntityViewpoint",
                   at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"),
                   require = 2)
    private static Block getBlockHijack(World world, int x, int y, int z, Operation<Block> original, @Share("logged") LocalBooleanRef logged) {
        logged.set(false);
        val fluid = ((FLBlockAccess)world).fl$getFluid(x, y, z);
        if (fluid == null) {
            return original.call(world, x, y, z);
        }
        val fluidBlock = fluid.getBlock();
        if (fluidBlock == null) {
            return original.call(world, x, y, z);
        }
        logged.set(true);
        return fluidBlock;
    }

    @WrapOperation(method = "getBlockAtEntityViewpoint",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"),
                   require = 1)
    private static int getMetaHijack(World world, int x, int y, int z, Operation<Integer> original, @Share("logged") LocalBooleanRef logged) {
        if (logged.get()) {
            return 0;
        }
        return original.call(world, x, y, z);
    }
}
