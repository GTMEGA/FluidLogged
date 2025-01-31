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

import mega.fluidlogged.internal.FluidLogBlockAccess;
import lombok.val;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.shader.TesselatorVertexState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.HashSet;
import java.util.List;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow public boolean needsUpdate;

    @Shadow public int posX;

    @Shadow public int posY;

    @Shadow public int posZ;

    @Shadow public boolean[] skipRenderPass;

    @Shadow public List<TileEntity> tileEntityRenderers;

    @Shadow public World worldObj;

    @Shadow public static int chunksUpdated;

    @Shadow private int bytesDrawn;

    @Shadow private TesselatorVertexState vertexState;

    @Shadow protected abstract void preRenderBlocks(int renderPass);

    @Shadow private List tileEntities;

    @Shadow public boolean isChunkLit;

    @Shadow private boolean isInitialized;

    @Shadow protected abstract void postRenderBlocks(int renderPass, EntityLivingBase cameraEntity);

    /**
     * TODO FIXME
     * do NOT leave this as an overwrite, replace with injections or asm! Breaks with FalseTweaks/Neodymium!
     * @author FalsePattern
     * @reason Fluidlogging
     */
    @Overwrite
    public void updateRenderer(EntityLivingBase cameraEntity)
    {
        if (this.needsUpdate)
        {
            this.needsUpdate = false;
            int xMin = this.posX;
            int yMin = this.posY;
            int zMin = this.posZ;
            int xMax = this.posX + 16;
            int yMax = this.posY + 16;
            int zMax = this.posZ + 16;

            for (int k1 = 0; k1 < 2; ++k1)
            {
                this.skipRenderPass[k1] = true;
            }

            Chunk.isLit = false;
            HashSet hashset = new HashSet();
            hashset.addAll(this.tileEntityRenderers);
            this.tileEntityRenderers.clear();
            Minecraft minecraft = Minecraft.getMinecraft();
            EntityLivingBase entitylivingbase1 = minecraft.renderViewEntity;
            int entX = MathHelper.floor_double(entitylivingbase1.posX);
            int entY = MathHelper.floor_double(entitylivingbase1.posY);
            int entZ = MathHelper.floor_double(entitylivingbase1.posZ);
            byte b0 = 1;
            ChunkCache chunkcache = new ChunkCache(this.worldObj, xMin - b0, yMin - b0, zMin - b0, xMax + b0, yMax + b0, zMax + b0, b0);

            if (!chunkcache.extendedLevelsInChunkCache())
            {
                ++chunksUpdated;
                RenderBlocks renderblocks = new RenderBlocks(chunkcache);
                net.minecraftforge.client.ForgeHooksClient.setWorldRendererRB(renderblocks);
                this.bytesDrawn = 0;
                this.vertexState = null;

                for (int pass = 0; pass < 2; ++pass)
                {
                    boolean nextPass = false;
                    boolean renderedAnything = false;
                    boolean tessStarted = false;

                    for (int y = yMin; y < yMax; ++y)
                    {
                        for (int z = zMin; z < zMax; ++z)
                        {
                            for (int x = xMin; x < xMax; ++x)
                            {
                                Block block = chunkcache.getBlock(x, y, z);

                                if (block.getMaterial() != Material.air)
                                {
                                    if (!tessStarted)
                                    {
                                        tessStarted = true;
                                        this.preRenderBlocks(pass);
                                    }

                                    if (pass == 0 && block.hasTileEntity(chunkcache.getBlockMetadata(x, y, z)))
                                    {
                                        TileEntity tileentity = chunkcache.getTileEntity(x, y, z);

                                        if (TileEntityRendererDispatcher.instance.hasSpecialRenderer(tileentity))
                                        {
                                            this.tileEntityRenderers.add(tileentity);
                                        }
                                    }

                                    int blockPass = block.getRenderBlockPass();

                                    val fluid = ((FluidLogBlockAccess)chunkcache).fl$getFluid(x, y, z);
                                    val fluidBlock = fluid == null ? null : fluid.toBlock();

                                    if (blockPass > pass || (pass < 1 && fluidBlock != null && fluidBlock.getRenderBlockPass() > 0))
                                    {
                                        nextPass = true;
                                    }

                                    if (block.canRenderInPass(pass)) {
                                        renderedAnything |= renderblocks.renderBlockByRenderType(block, x, y, z);

                                        if (block.getRenderType() == 0 && x == entX && y == entY && z == entZ)
                                        {
                                            renderblocks.setRenderFromInside(true);
                                            renderblocks.setRenderAllFaces(true);
                                            renderblocks.renderBlockByRenderType(block, x, y, z);
                                            renderblocks.setRenderFromInside(false);
                                            renderblocks.setRenderAllFaces(false);
                                        }
                                    }

                                    if (fluidBlock != null && fluidBlock.canRenderInPass(pass)) {
                                        renderedAnything |= renderblocks.renderBlockByRenderType(fluidBlock, x, y, z);
                                    }
                                }
                            }
                        }
                    }

                    if (renderedAnything)
                    {
                        this.skipRenderPass[pass] = false;
                    }

                    if (tessStarted)
                    {
                        this.postRenderBlocks(pass, cameraEntity);
                    }
                    else
                    {
                        renderedAnything = false;
                    }

                    if (!nextPass)
                    {
                        break;
                    }
                }
                net.minecraftforge.client.ForgeHooksClient.setWorldRendererRB(null);
            }

            HashSet hashset1 = new HashSet();
            hashset1.addAll(this.tileEntityRenderers);
            hashset1.removeAll(hashset);
            this.tileEntities.addAll(hashset1);
            hashset.removeAll(this.tileEntityRenderers);
            this.tileEntities.removeAll(hashset);
            this.isChunkLit = Chunk.isLit;
            this.isInitialized = true;
        }
    }
}
