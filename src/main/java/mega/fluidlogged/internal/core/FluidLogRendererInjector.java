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

import com.falsepattern.lib.asm.ASMUtil;
import com.falsepattern.lib.mapping.MappingManager;
import com.falsepattern.lib.mapping.types.MappingType;
import com.falsepattern.lib.mapping.types.NameType;
import com.falsepattern.lib.turboasm.ClassNodeHandle;
import com.falsepattern.lib.turboasm.TurboClassTransformer;
import lombok.SneakyThrows;
import lombok.val;
import mega.fluidlogged.Tags;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;


/**
 * Waterlogging renderer hook
 *
 * Injection point:
 * <pre>{@code
 *    <------------ here
 *  int k3 = block.getRenderBlockPass();
 *
 *  if (k3 > k2)
 *  {
 *      flag = true;
 *  }
 *
 *  if (!block.canRenderInPass(k2)) continue;
 * }</pre>
 *
 * Injected code snippet:
 * <pre>{@code
 * int tmp = ASMHooks.drawFluidLogged(renderblocks, x, y, z, pass);
 * nextPass |= tmp & 1;
 * renderedAnything |= (tmp >>> 1) & 1;
 * }</pre>
 */
public class FluidLogRendererInjector implements TurboClassTransformer {
    @Override
    public String owner() {
        return Tags.MOD_ID;
    }

    @Override
    public String name() {
        return "FluidLogRendererInjector";
    }

    @Override
    public boolean shouldTransformClass(@NotNull String className, @NotNull ClassNodeHandle classNode) {
        return "net.minecraft.client.renderer.WorldRenderer".equals(className);
    }

    @SneakyThrows
    @Override
    public boolean transformClass(@NotNull String className, @NotNull ClassNodeHandle classNode) {
        val cn = classNode.getNode();
        if (cn == null) {
            return false;
        }
        val type = ASMUtil.discoverClassMappingType(cn);
        val block = MappingManager.classForName(NameType.Regular, MappingType.MCP, "net.minecraft.block.Block");
        val blockMethod = block.getMethod(MappingType.MCP, "getRenderBlockPass", "()I");
        val blockClassNameInternal = block.internalName().get(type);
        val blockMethodName = blockMethod.name().get(type);
        val blockMethodDesc = blockMethod.descriptor().get(type);
        val method = ASMUtil.findMethodFromMCP(cn, "updateRenderer", "(Lnet/minecraft/entity/EntityLivingBase;)V", false);
        val iter = method.instructions.iterator();
        while (iter.hasNext()) {
            val insn = iter.next();
            if (!(insn instanceof MethodInsnNode))
                continue;
            val mInsn = (MethodInsnNode) insn;
            if (!blockClassNameInternal.equals(mInsn.owner) ||
                !blockMethodName.equals(mInsn.name) ||
                !blockMethodDesc.equals(mInsn.desc)) {
                continue;
            }
            iter.previous();
            iter.add(new VarInsnNode(Opcodes.ALOAD, 16));
            iter.add(new VarInsnNode(Opcodes.ILOAD, 23));
            iter.add(new VarInsnNode(Opcodes.ILOAD, 21));
            iter.add(new VarInsnNode(Opcodes.ILOAD, 22));
            iter.add(new VarInsnNode(Opcodes.ILOAD, 17));
            iter.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                        Tags.ROOT_PKG.replace('.', '/') + "/internal/core/ASMHooks",
                                        "drawFluidLogged",
                                        "(Lnet/minecraft/client/renderer/RenderBlocks;IIII)I",
                                        false));
            iter.add(new InsnNode(Opcodes.DUP));
            iter.add(new InsnNode(Opcodes.ICONST_1));
            iter.add(new InsnNode(Opcodes.IAND));
            iter.add(new VarInsnNode(Opcodes.ILOAD, 18));
            iter.add(new InsnNode(Opcodes.IOR));
            iter.add(new VarInsnNode(Opcodes.ISTORE, 18));
            iter.add(new InsnNode(Opcodes.ICONST_1));
            iter.add(new InsnNode(Opcodes.IUSHR));
            iter.add(new InsnNode(Opcodes.ICONST_1));
            iter.add(new InsnNode(Opcodes.IAND));
            iter.add(new VarInsnNode(Opcodes.ILOAD, 19));
            iter.add(new InsnNode(Opcodes.IOR));
            iter.add(new VarInsnNode(Opcodes.ISTORE, 19));
            return true;
        }
        return false;
    }
}
