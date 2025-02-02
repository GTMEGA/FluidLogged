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

package mega.fluidlogged.internal.core;

import lombok.val;
import mega.fluidlogged.internal.mixin.hook.FLBlockAccess;

import net.minecraft.client.renderer.RenderBlocks;

@SuppressWarnings("unused") // Called by ASM
public class ASMHooks {
    public static final int BIT_NEXT_PASS = 0b1;
    public static final int BIT_RENDERED_ANYTHING = 0b10;
    public static int drawFluidLogged(RenderBlocks renderBlocks, int x, int y, int z, int pass) {

        val fluid = ((FLBlockAccess)renderBlocks.blockAccess).fl$getFluid(x, y, z);
        val fluidBlock = fluid == null ? null : fluid.getBlock();

        int result = 0;

        if (pass < 1 && fluidBlock != null && fluidBlock.getRenderBlockPass() > 0) {
            result |= 0b1;
        }
        if (fluidBlock != null && fluidBlock.canRenderInPass(pass) && renderBlocks.renderBlockByRenderType(fluidBlock, x, y, z)) {
            result |= 0b10;
        }

        return result;
    }
}
